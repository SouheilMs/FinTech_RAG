package com.finassistmini.web;

import com.finassistmini.config.AppProperties;
import com.finassistmini.dto.DocumentResponse;
import com.finassistmini.dto.JobStatusResponse;
import com.finassistmini.dto.UploadResponse;
import com.finassistmini.model.DocumentMeta;
import com.finassistmini.model.IngestionJob;
import com.finassistmini.service.CurrentUserService;
import com.finassistmini.service.DocumentService;
import com.finassistmini.service.IngestionService;
import com.finassistmini.service.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Upload and manage financial PDF documents")
public class DocumentController {

    private final DocumentService documentService;
    private final IngestionService ingestionService;
    private final VectorStoreService vectorStoreService;
    private final AppProperties props;
    private final CurrentUserService currentUserService;

    public DocumentController(DocumentService documentService, IngestionService ingestionService,
                              VectorStoreService vectorStoreService, AppProperties props, CurrentUserService currentUserService) {
        this.documentService = documentService;
        this.ingestionService = ingestionService;
        this.vectorStoreService = vectorStoreService;
        this.props = props;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary     = "Upload a PDF document",
            description = "Accepts a single PDF file  and queues it for asynchronous ingestion (parse → chunk → embed → store).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted — ingestion job queued",
                    content = @Content(schema = @Schema(implementation = UploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "No file supplied, or file is not a PDF",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "413", description = "File exceeds the configured size limit"),
            @ApiResponse(responseCode = "503", description = "Ingestion worker pool is saturated")
    })
    public UploadResponse upload(
            @Parameter(description = "PDF file to ingest (multipart/form-data)", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {

        String ownerId       = currentUserService.getUserId();
        String ownerUsername = currentUserService.getUsername();
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }

        String filename = Objects.requireNonNull(
                file.getOriginalFilename(), "Filename must not be null").trim();

        if (!filename.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PDF files are accepted (got: '" + filename + "')");
        }

        String documentId = UUID.randomUUID().toString();
        Path   docsDir    = documentService.getDocsDirectory();
        Files.createDirectories(docsDir);

        Path dest = docsDir.resolve(documentId + "_" + filename);
        writeInChunks(file.getInputStream(), dest);

        documentService.register(documentId, filename, dest, ownerId, ownerUsername);
        IngestionJob job = ingestionService.submitJob(documentId, filename, dest, ownerId, ownerUsername);

        return new UploadResponse(
                job.getJobId(), documentId, "ACCEPTED", "Document queued for ingestion");
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(
            summary     = "Poll ingestion job status",
            description = "Returns the current status and a human-readable progress message.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job found",
                    content = @Content(schema = @Schema(implementation = JobStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "No job with that ID")
    })
    public JobStatusResponse getJob(
            @Parameter(description = "Job ID returned by POST /documents/upload", required = true)
            @PathVariable String jobId) {

        return ingestionService.findJob(jobId)
                .map(j -> new JobStatusResponse(
                        j.getJobId(), j.getDocumentId(), j.getDocumentName(),
                        j.getStatus().name(), j.getMessage(),
                        j.getCreatedAt(), j.getUpdatedAt()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    @GetMapping
    @Operation(
            summary     = "List all documents",
            description = "Returns metadata for every document that has been uploaded.")
    @ApiResponse(responseCode = "200", description = "List returned (may be empty)",
            content = @Content(schema = @Schema(implementation = DocumentResponse.class)))
    public List<DocumentResponse> listDocuments() {
        String ownerId = currentUserService.getUserId();
        return documentService.findAllByOwner(ownerId).stream()
                .map(m -> new DocumentResponse(
                        m.getDocumentId(), m.getName(),
                        m.getOwnerId(), m.getOwnerUsername(),
                        m.getPageCount(), m.getChunkCount(),
                        m.getUploadedAt(), m.getStatus()))
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary     = "Re-index a document",
            description = """
            Removes all existing vector entries for the document then re-runs
            the full ingestion pipeline (useful after changing the chunking
            or embedding configuration).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Re-indexing job queued"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public UploadResponse reindex(
            @Parameter(description = "Document ID", required = true)
            @PathVariable String id) {

        String ownerId = currentUserService.getUserId();
        String ownerUsername = currentUserService.getUsername();
        DocumentMeta meta = documentService.findByIdAndOwner(id, ownerId);
        vectorStoreService.removeByDocumentId(id, ownerId);
        documentService.markPending(id);

        IngestionJob job = ingestionService.submitJob( id, meta.getName(), Path.of(meta.getFilePath()), ownerId, ownerUsername);

        return new UploadResponse(job.getJobId(), id, "ACCEPTED", "Re-indexing queued");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary     = "Delete a document",
            description = "Removes the PDF file, all vector entries, and the metadata record.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public void deleteDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable String id) {

        String ownerId = currentUserService.getUserId();
        documentService.findByIdAndOwner(id, ownerId);
        vectorStoreService.removeByDocumentId(id, ownerId);
        documentService.deleteByIdAndOwner(id, ownerId);
    }

    private void writeInChunks(InputStream in, Path dest) throws IOException {
        byte[] buf = new byte[(int) Math.max(1, props.uploadWriteChunkBytes())];
        try (OutputStream out = Files.newOutputStream(dest)) {
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
    }
}