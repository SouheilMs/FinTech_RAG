package com.finassistmini.web;

import com.finassistmini.config.FinassistProperties;
import com.finassistmini.dto.DocumentInfo;
import com.finassistmini.dto.DocumentUploadResponse;
import com.finassistmini.dto.IngestionJobStatusResponse;
import com.finassistmini.service.DocumentFileService;
import com.finassistmini.service.DocumentIngestionService;
import com.finassistmini.service.IngestionJob;
import com.finassistmini.service.IngestionJobService;
import com.finassistmini.service.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/documents")
public class DocumentsController {

    private static final Logger log = LoggerFactory.getLogger(DocumentsController.class);

    private final FinassistProperties properties;
    private final DocumentFileService documentFileService;
    private final IngestionJobService ingestionJobService;
    private final DocumentIngestionService documentIngestionService;
    private final VectorStoreService vectorStoreService;
    private final ExecutorService ingestionExecutor;
    private final Semaphore uploadSemaphore;

    public DocumentsController(
            FinassistProperties properties,
            DocumentFileService documentFileService,
            IngestionJobService ingestionJobService,
            DocumentIngestionService documentIngestionService,
            VectorStoreService vectorStoreService,
            ExecutorService ingestionExecutor,
            @Qualifier("uploadSemaphore") Semaphore uploadSemaphore
    ) {
        this.properties = properties;
        this.documentFileService = documentFileService;
        this.ingestionJobService = ingestionJobService;
        this.documentIngestionService = documentIngestionService;
        this.vectorStoreService = vectorStoreService;
        this.ingestionExecutor = ingestionExecutor;
        this.uploadSemaphore = uploadSemaphore;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a PDF document", description = "Upload a PDF document to be ingested and indexed for RAG queries")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentUploadResponse uploadDocument(@RequestParam("file") MultipartFile file)
            throws InterruptedException, IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (originalFilename.isBlank() || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted.");
        }
        if (!uploadSemaphore.tryAcquire((long) (properties.admissionWaitSeconds() * 1000), TimeUnit.MILLISECONDS)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Upload service is busy. Please retry shortly.");
        }

        Path tmpOutputPath = documentFileService.docsDirectory().resolve("tmp__" + Path.of(originalFilename).getFileName());
        try {
            Files.createDirectories(documentFileService.docsDirectory());
            StoredUpload storedUpload = streamUpload(file, tmpOutputPath);
            String documentHash = storedUpload.documentHash();
            String documentId = documentHash;

            if (vectorStoreService.documentExists(documentHash)) {
                Files.deleteIfExists(tmpOutputPath);
                IngestionJob job = ingestionJobService.createJob(documentId, originalFilename, documentHash);
                ingestionJobService.markCompleted(job.jobId(), 0);
                log.info("upload_deduplicated filename={} bytes={} document_hash={}", originalFilename, storedUpload.bytesWritten(), documentHash);
                return new DocumentUploadResponse(
                        job.jobId(),
                        "completed",
                        documentId,
                        originalFilename,
                        documentHash,
                        0,
                        "Document already indexed. Reused existing embeddings."
                );
            }

            documentFileService.deleteDocumentFiles(documentId);
            Path outputPath = documentFileService.docsDirectory().resolve(documentId + "__" + originalFilename);
            Files.move(tmpOutputPath, outputPath);
            IngestionJob job = ingestionJobService.createJob(documentId, originalFilename, documentHash);
            ingestionExecutor.submit(() -> documentIngestionService.ingest(job.jobId(), documentId, originalFilename, outputPath));
            log.info("upload_request_metrics filename={} bytes={} document_hash={} status=processing", originalFilename, storedUpload.bytesWritten(), documentHash);
            return new DocumentUploadResponse(job.jobId(), job.status(), documentId, originalFilename, documentHash, null, null);
        } finally {
            uploadSemaphore.release();
            Files.deleteIfExists(tmpOutputPath);
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get ingestion job status", description = "Retrieve the status of a document ingestion job")
    public IngestionJobStatusResponse getIngestionJobStatus(@PathVariable String jobId) {
        IngestionJob job = ingestionJobService.getJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job not found."));
        return toResponse(job);
    }

    @GetMapping
    @Operation(summary = "List all documents", description = "Retrieve a list of all ingested documents")
    public List<DocumentInfo> listDocuments() {
        return documentFileService.listDocuments();
    }

    @PostMapping("/{documentId}/reindex")
    @Operation(summary = "Reindex a document", description = "Trigger reindexing of an existing document")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentUploadResponse reindexDocument(@PathVariable String documentId) {
        List<Path> targetFiles = documentFileService.findFilesByDocumentId(documentId);
        if (targetFiles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
        }
        Path documentPath = targetFiles.get(0);
        String filename = documentFileService.extractOriginalFilename(documentPath.getFileName().toString());
        vectorStoreService.deleteDocument(documentId);
        IngestionJob job = ingestionJobService.createJob(documentId, filename, documentId);
        ingestionExecutor.submit(() -> documentIngestionService.ingest(job.jobId(), documentId, filename, documentPath));
        return new DocumentUploadResponse(job.jobId(), job.status(), documentId, filename, documentId, null, null);
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document", description = "Delete a document and remove it from the vector store")
    @ResponseStatus(HttpStatus.OK)
    public void deleteDocument(@PathVariable String documentId) {
        List<Path> targetFiles = documentFileService.findFilesByDocumentId(documentId);
        if (targetFiles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
        }
        documentFileService.deleteDocumentFiles(documentId);
        vectorStoreService.deleteDocument(documentId);
    }

    private StoredUpload streamUpload(MultipartFile file, Path targetPath) throws IOException {
        long maxUploadSizeBytes = properties.maxUploadSizeMb() * 1024L * 1024L;
        MessageDigest digest = sha256();
        long bytesWritten = 0;
        byte[] buffer = new byte[properties.uploadWriteChunkBytes()];

        try (InputStream inputStream = new DigestInputStream(file.getInputStream(), digest);
             var outputStream = Files.newOutputStream(targetPath)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                bytesWritten += read;
                if (bytesWritten > maxUploadSizeBytes) {
                    throw new ResponseStatusException(
                            HttpStatus.PAYLOAD_TOO_LARGE,
                            "File exceeds " + properties.maxUploadSizeMb() + " MB limit."
                    );
                }
                outputStream.write(buffer, 0, read);
            }
        }
        return new StoredUpload(HexFormat.of().formatHex(digest.digest()), bytesWritten);
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available.", ex);
        }
    }

    private IngestionJobStatusResponse toResponse(IngestionJob job) {
        return new IngestionJobStatusResponse(
                job.jobId(),
                job.status(),
                job.documentId(),
                job.filename(),
                job.documentHash(),
                job.chunksIndexed(),
                job.detail(),
                job.createdAt(),
                job.updatedAt()
        );
    }

    private record StoredUpload(String documentHash, long bytesWritten) {
    }
}
