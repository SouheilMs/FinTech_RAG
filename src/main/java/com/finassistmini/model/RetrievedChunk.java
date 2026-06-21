package com.finassistmini.model;

public record RetrievedChunk(
        String chunkId,
        String documentId,
        String documentName,
        int    pageNumber,
        String text,
        double distance
) {}