package com.finassistmini.service;

public record IngestionJob(
        String jobId,
        String status,
        String documentId,
        String filename,
        String documentHash,
        int chunksIndexed,
        String detail,
        double createdAt,
        double updatedAt
) {

    IngestionJob withStatus(String newStatus, int newChunksIndexed, String newDetail) {
        return new IngestionJob(
                jobId,
                newStatus,
                documentId,
                filename,
                documentHash,
                newChunksIndexed,
                newDetail,
                createdAt,
                System.currentTimeMillis() / 1000.0
        );
    }
}
