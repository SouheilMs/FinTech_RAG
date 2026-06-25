package com.finassistmini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String docsDirectory,
        int retrievalK,
        int chunkSizeWords,
        int chunkOverlapWords,
        int maxPromptChars,
        long uploadWriteChunkBytes,
        int uploadMaxConcurrency,
        int chatMaxConcurrency,
        double admissionWaitSeconds
) {}