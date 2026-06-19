package com.finassistmini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentUploadResponse(
        @JsonProperty("job_id") String jobId,
        String status,
        String id,
        String filename,
        @JsonProperty("document_hash") String documentHash,
        @JsonProperty("chunks_indexed") Integer chunksIndexed,
        String detail
) {
}
