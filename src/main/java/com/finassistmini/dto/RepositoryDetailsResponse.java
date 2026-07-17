package com.finassistmini.dto;

import java.time.LocalDateTime;

public record RepositoryDetailsResponse(
        Long id,
        String url,
        String name,
        String owner,
        String ownerId,
        String ownerUsername,
        String branch,
        String commitHash,
        String status,
        String errorMessage,
        Integer indexedFiles,
        Integer totalChunks,
        boolean hasSummary,
        LocalDateTime createdAt,
        LocalDateTime indexedAt
) {}