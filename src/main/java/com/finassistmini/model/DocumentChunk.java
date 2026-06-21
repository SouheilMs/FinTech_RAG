package com.finassistmini.model;

public record DocumentChunk(
        String chunkId,
        String documentId,
        String documentName,
        int    pageNumber,
        String text
) {}