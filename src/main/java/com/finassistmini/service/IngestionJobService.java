package com.finassistmini.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class IngestionJobService {

    private final ConcurrentMap<String, IngestionJob> jobs = new ConcurrentHashMap<>();

    public IngestionJob createJob(String documentId, String filename, String documentHash) {
        double now = System.currentTimeMillis() / 1000.0;
        IngestionJob job = new IngestionJob(
                UUID.randomUUID().toString(),
                "processing",
                documentId,
                filename,
                documentHash,
                0,
                null,
                now,
                now
        );
        jobs.put(job.jobId(), job);
        return job;
    }

    public void markCompleted(String jobId, int chunksIndexed) {
        jobs.computeIfPresent(jobId, (id, job) -> job.withStatus("completed", chunksIndexed, null));
    }

    public void markFailed(String jobId, String detail) {
        jobs.computeIfPresent(jobId, (id, job) -> job.withStatus("failed", job.chunksIndexed(), detail));
    }

    public Optional<IngestionJob> getJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }
}
