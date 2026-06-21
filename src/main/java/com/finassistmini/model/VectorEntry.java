package com.finassistmini.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VectorEntry {

    private String chunkId;
    private String documentId;
    private String documentName;
    private int pageNumber;
    private String text;
    private float[] embedding;

}