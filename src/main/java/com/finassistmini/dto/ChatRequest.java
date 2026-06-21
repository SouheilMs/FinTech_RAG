package com.finassistmini.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A natural-language question about the uploaded financial documents")
public record ChatRequest(

        @NotBlank(message = "question must not be blank")
        @Size(min = 3, max = 1000, message = "question must be between 3 and 1000 characters")
        @Schema(description = "The question to answer", example = "What are the international transfer fees?")
        String question
) {}