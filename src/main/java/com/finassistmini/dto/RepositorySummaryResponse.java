package com.finassistmini.dto;

public record RepositorySummaryResponse(
        Long repositoryId,
        String repositoryName,
        String summary,
        String fileTree,
        boolean cached
) {}