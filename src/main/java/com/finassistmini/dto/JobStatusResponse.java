package com.finassistmini.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Current state of an ingestion job")
public record JobStatusResponse(

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String jobId,

        @Schema(example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        String documentId,

        @Schema(example = "fees.pdf")
        String documentName,

        @Schema(description = "One of: QUEUED | RUNNING | COMPLETED | FAILED", example = "COMPLETED")
        String status,

        @Schema(description = "Human-readable progress or error detail",
                example = "Indexed 42 chunks from 18 pages")
        String message,

        @Schema(description = "ISO-8601 timestamp when the job was created")
        Instant createdAt,

        @Schema(description = "ISO-8601 timestamp of the last status change")
        Instant updatedAt
) {}