package com.finassistmini.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "finassist")
public record FinassistProperties(
        String appName,
        String appVersion,
        Path docsDirectory,
        Path vectorStoreFile,
        @Min(1) int retrievalK,
        @Min(50) int chunkSizeWords,
        @Min(0) int chunkOverlapWords,
        @Min(1) int llmTimeoutSeconds,
        @Min(256) int maxPromptChars,
        @Min(1) int maxUploadSizeMb,
        @Min(1024) int uploadWriteChunkBytes,
        @Min(1) int uploadMaxConcurrency,
        @Min(1) int chatMaxConcurrency,
        @DecimalMin("0.05") double admissionWaitSeconds
) {
}
