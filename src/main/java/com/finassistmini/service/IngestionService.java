package com.finassistmini.service;

import com.finassistmini.config.AppProperties;
import com.finassistmini.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
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
    private final AppProperties props;
    private final Executor executor;
    private final ConcurrentHashMap<String, IngestionJob> jobs = new ConcurrentHashMap<>();
    private final Semaphore semaphore;

    public IngestionService( PdfParserService pdfParser, ChunkingService chunker, VectorStoreService vectorStore,
            DocumentService documentService, AppProperties props, @Qualifier("ingestionExecutor") Executor executor) {
        this.pdfParser = pdfParser;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.documentService = documentService;
        this.props = props;
        this.executor = executor;
        this.semaphore = new Semaphore(props.uploadMaxConcurrency(), true);
    }

    public IngestionJob submitJob(String documentId, String documentName, Path pdfPath,
                                  String ownerId, String ownerUsername) {
        String jobId = UUID.randomUUID().toString();
        IngestionJob job = new IngestionJob(
                jobId, documentId, documentName, JobStatus.QUEUED,
                "Queued for processing", Instant.now());
        jobs.put(jobId, job);

        CompletableFuture.runAsync(() -> runJob(job, pdfPath, ownerId, ownerUsername), executor)
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

    private void runJob(IngestionJob job, Path pdfPath, String ownerId, String ownerUsername) {
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
                    "Storing %d chunks in vector database…".formatted(chunks.size()));
            // Convert chunks to Spring AI Documents
            List<Document> documents = chunks.stream()
                    .map(chunk -> createDocument(chunk, ownerId, ownerUsername))
                    .collect(Collectors.toList());
            // Store embeddings in pgvector (Spring AI handles embedding generation)
            vectorStore.addAll(documents);


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

    private Document createDocument(DocumentChunk chunk, String ownerId, String ownerUsername) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunkId", chunk.chunkId());
        metadata.put("documentId", chunk.documentId());
        metadata.put("documentName", chunk.documentName());
        metadata.put("pageNumber", chunk.pageNumber());
        metadata.put("ownerId", ownerId);
        metadata.put("ownerUsername", ownerUsername);
        return new Document(chunk.text(), metadata);
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