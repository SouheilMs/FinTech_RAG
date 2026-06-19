package com.finassistmini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IngestionJobStatusResponse(
        @JsonProperty("job_id") String jobId,
        String status,
        @JsonProperty("document_id") String documentId,
        String filename,
        @JsonProperty("document_hash") String documentHash,
        @JsonProperty("chunks_indexed") int chunksIndexed,
        String detail,
        @JsonProperty("created_at") double createdAt,
        @JsonProperty("updated_at") double updatedAt
) {
}
