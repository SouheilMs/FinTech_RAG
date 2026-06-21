package com.finassistmini.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentMeta {

    private String documentId;
    private String name;
    private String filePath;
    private int pageCount;
    private int chunkCount;
    private Instant uploadedAt;
    private String status;
    private String errorMessage;
}