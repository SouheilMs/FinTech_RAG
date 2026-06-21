package com.finassistmini.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "LLM answer grounded on retrieved document chunks")
public record ChatResponse(

        @Schema(description = "The generated answer",
                example = "International transfers incur a flat fee of $5 plus 1% of the transfer amount.")
        String answer,

        @Schema(description = "Deduplicated list of source pages used to generate the answer")
        List<SourceReference> sources
) {}