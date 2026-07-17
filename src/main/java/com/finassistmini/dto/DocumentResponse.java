package com.finassistmini.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Summary of an uploaded document")
public record DocumentResponse(

        @Schema(example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        String documentId,

        @Schema(example = "fees.pdf")
        String name,

        @Schema(example = "user-123")
        String ownerId,

        @Schema(example = "johndoe")
        String ownerUsername,

        @Schema(description = "Number of non-empty PDF pages", example = "18")
        int pageCount,

        @Schema(description = "Number of vector chunks stored", example = "42")
        int chunkCount,

        @Schema(description = "ISO-8601 upload timestamp")
        Instant uploadedAt,

        @Schema(description = "PENDING | INDEXED | FAILED", example = "INDEXED")
        String status
) {}