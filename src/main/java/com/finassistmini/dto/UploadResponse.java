package com.finassistmini.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Immediate response after a PDF is accepted for ingestion")
public record UploadResponse(

        @Schema(description = "ID of the background ingestion job — poll with GET /documents/jobs/{jobId}",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String jobId,

        @Schema(description = "Auto-generated document ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        String documentId,

        @Schema(description = "HTTP-level status of this response", example = "ACCEPTED")
        String status,

        @Schema(description = "Human-readable message", example = "Document queued for ingestion")
        String message
) {}