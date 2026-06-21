package com.finassistmini.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A grounding source used to generate the answer")
public record SourceReference(

        @Schema(description = "Original PDF filename", example = "fees.pdf")
        String document,

        @Schema(description = "1-based page number in the source document", example = "2")
        int page
) {}