package com.finassistmini.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class IngestionJob {

    private final String jobId;
    private final String documentId;
    private final String documentName;
    private final Instant createdAt;
    private volatile JobStatus status;
    private volatile String message;
    private volatile Instant updatedAt;
    private volatile Instant completedAt;

    public IngestionJob(String jobId, String documentId, String documentName,
                        JobStatus status, String message, Instant createdAt) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.documentName = documentName;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
}