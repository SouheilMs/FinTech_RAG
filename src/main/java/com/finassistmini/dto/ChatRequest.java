package com.finassistmini.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question must not be empty.")
        String question
) {
}
