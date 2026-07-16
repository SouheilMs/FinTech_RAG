package com.finassistmini.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IndexRepositoryRequest(
        @NotBlank(message = "Repository URL is required")
        @Size(max = 1024, message = "URL must not exceed 1024 characters")
        String url
) {}