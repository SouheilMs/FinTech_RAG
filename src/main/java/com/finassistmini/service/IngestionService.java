package com.finassistmini.service;

import com.finassistmini.config.AppProperties;
import com.finassistmini.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final PdfParserService pdfParser;
    private final ChunkingService chunker;
    private final VectorStoreService vectorStore;
    private final DocumentService documentService;
    private final EmbeddingModel embeddingModel;
    private final AppProperties props;
    private final Executor executor;
    private final ConcurrentHashMap<String, IngestionJob> jobs = new ConcurrentHashMap<>();
    private final Semaphore semaphore;

    public IngestionService(
            PdfParserService pdfParser,
            ChunkingService chunker,
            VectorStoreService vectorStore,
            DocumentService documentService,
            EmbeddingModel embeddingModel,
            AppProperties props,
            @Qualifier("ingestionExecutor") Executor executor) {

        this.pdfParser = pdfParser;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.documentService = documentService;
        this.embeddingModel = embeddingModel;
        this.props = props;
        this.executor = executor;
        this.semaphore = new Semaphore(props.uploadMaxConcurrency(), true);
    }

    public IngestionJob submitJob(String documentId, String documentName, Path pdfPath) {
        String jobId = UUID.randomUUID().toString();
        IngestionJob job = new IngestionJob(
                jobId, documentId, documentName, JobStatus.QUEUED,
                "Queued for processing", Instant.now());
        jobs.put(jobId, job);

        CompletableFuture.runAsync(() -> runJob(job, pdfPath), executor)
                .exceptionally(ex -> {
                    log.error("Unexpected error in job {}: {}", jobId, ex.getMessage(), ex);
                    transition(job, JobStatus.FAILED, "Unexpected error: " + ex.getMessage());
                    return null;
                });
        return job;
    }

    public Optional<IngestionJob> findJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private void runJob(IngestionJob job, Path pdfPath) {
        boolean acquired = false;
        try {
            long waitMs = (long) (props.admissionWaitSeconds() * 1000);
            acquired = semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                transition(job, JobStatus.FAILED, "Server is busy — please retry later");
                documentService.markFailed(job.getDocumentId(), "Admission timeout");
                return;
            }

            transition(job, JobStatus.RUNNING, "Parsing PDF…");
            var pages = pdfParser.parse(pdfPath);

            transition(job, JobStatus.RUNNING, "Splitting text into chunks…");
            List<DocumentChunk> chunks = chunker.chunk(
                    job.getDocumentId(), job.getDocumentName(), pages);

            transition(job, JobStatus.RUNNING,
                    "Embedding %d chunks via Ollama…".formatted(chunks.size()));
            List<float[]> embeddings = batchEmbed(
                    chunks.stream().map(DocumentChunk::text).collect(Collectors.toList()));

            List<VectorEntry> entries = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk c = chunks.get(i);
                entries.add(new VectorEntry(
                        c.chunkId(), c.documentId(), c.documentName(),
                        c.pageNumber(), c.text(), embeddings.get(i)));
            }
            vectorStore.addAll(entries);

            documentService.markIndexed(job.getDocumentId(), pages.size(), chunks.size());
            transition(job, JobStatus.COMPLETED,
                    "Indexed %d chunks from %d pages".formatted(chunks.size(), pages.size()));

            log.info("Job {} COMPLETED — {} chunks embedded for '{}'",
                    job.getJobId(), chunks.size(), job.getDocumentName());

        } catch (Exception ex) {
            log.error("Job {} FAILED: {}", job.getJobId(), ex.getMessage(), ex);
            transition(job, JobStatus.FAILED, "Error: " + ex.getMessage());
            documentService.markFailed(job.getDocumentId(), ex.getMessage());
        } finally {
            if (acquired) semaphore.release();
        }
    }

    private List<float[]> batchEmbed(List<String> texts) {
        try {
            return embeddingModel.embed(texts);
        } catch (Exception ex) {
            // Some Ollama builds do not support multi-input requests; fall back.
            log.warn("Batch embedding failed ({}), falling back to sequential", ex.getMessage());
            return texts.stream()
                    .map(embeddingModel::embed)
                    .collect(Collectors.toList());
        }
    }

    private void transition(IngestionJob job, JobStatus status, String message) {
        job.setStatus(status);
        job.setMessage(message);
        job.setUpdatedAt(Instant.now());
        if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
            job.setCompletedAt(Instant.now());
        }
    }
}