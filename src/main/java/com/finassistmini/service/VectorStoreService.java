package com.finassistmini.service;

import com.finassistmini.model.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addAll(List<Document> documents) {
        vectorStore.add(documents);
        log.info("Added {} documents to vector store", documents.size());
        }

    public void removeByDocumentId(String documentId, String ownerId) {
        String filter = "documentId == '" + documentId + "' && ownerId == '" + ownerId + "'";
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(" ")
                        .topK(10_000)
                        .filterExpression(filter)
                        .build());
        List<String> ids = docs.stream()
                .map(Document::getId)
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            vectorStore.delete(ids);
            log.info("Removed {} vectors for documentId='{}' and ownerId='{}'", ids.size(), documentId, ownerId);
        } else {
            log.warn("No vectors found for documentId='{}' and ownerId='{}' to remove", documentId, ownerId);
        }
    }

    public List<RetrievedChunk> search(String query, String ownerId, int k) {
        String ownerFilter = "ownerId == '" + ownerId + "'";
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(k)
                        .similarityThreshold(0.65)
                        .filterExpression(ownerFilter)
                        .build()
        );
        log.debug("Retrieved {} chunks for ownerId='{}', query='{}'",
                results.size(), ownerId, query.length() > 60 ? query.substring(0, 60) + "…" : query);
        return results.stream()
                .map(this::documentToRetrievedChunk)
                .collect(Collectors.toList());
    }

    public int size() {
        // Count through similarity search (all documents)
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("*")
                        .topK(Integer.MAX_VALUE)
                        .build())
                .size();
    }

    private RetrievedChunk documentToRetrievedChunk(Document doc) {
            return new RetrievedChunk(
                    (String) doc.getMetadata().get("chunkId"),
                    (String) doc.getMetadata().get("documentId"),
                    (String) doc.getMetadata().get("documentName"),
                    (Integer) doc.getMetadata().get("pageNumber"),
                    doc.getText(),
                    0.0  // Distance not exposed by Spring AI, but can be tracked separately if needed
            );
        }
    }