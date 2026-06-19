package com.finassistmini.service;

import com.finassistmini.model.DocumentChunk;
import com.finassistmini.model.ParsedPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final PdfParserService parserService;
    private final ChunkerService chunkerService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final IngestionJobService ingestionJobService;
    private final DocumentFileService documentFileService;

    public DocumentIngestionService(
            PdfParserService parserService,
            ChunkerService chunkerService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            IngestionJobService ingestionJobService,
            DocumentFileService documentFileService
    ) {
        this.parserService = parserService;
        this.chunkerService = chunkerService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ingestionJobService = ingestionJobService;
        this.documentFileService = documentFileService;
    }

    public void ingest(String jobId, String documentId, String filename, Path filePath) {
        long started = System.nanoTime();
        try {
            List<ParsedPage> pages = parserService.parsePdf(filePath);
            if (pages.isEmpty()) {
                documentFileService.deleteDocumentFiles(documentId);
                ingestionJobService.markFailed(jobId, "The PDF does not contain extractable text.");
                return;
            }

            List<DocumentChunk> chunks = chunkerService.chunkPages(documentId, filename, pages);
            List<List<Double>> embeddings = embeddingService.embedTexts(chunks.stream()
                    .map(DocumentChunk::text)
                    .toList());
            vectorStoreService.upsertChunks(chunks, embeddings);
            ingestionJobService.markCompleted(jobId, chunks.size());
            log.info(
                    "upload_pipeline_metrics filename={} pages={} chunks={} embeddings={} total_ms={}",
                    filename,
                    pages.size(),
                    chunks.size(),
                    embeddings.size(),
                    (System.nanoTime() - started) / 1_000_000
            );
        } catch (Exception ex) {
            documentFileService.deleteDocumentFiles(documentId);
            vectorStoreService.deleteDocument(documentId);
            ingestionJobService.markFailed(jobId, "Failed to process uploaded document.");
            log.error("Ingestion failed for job_id={} filename={}", jobId, filename, ex);
        }
    }
}
