package com.finassistmini.service;

import com.finassistmini.model.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.List;
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

    public void removeByDocumentId(String documentId) {
        // Spring AI VectorStore deletes by filtering metadata
        vectorStore.delete(
                vectorStore.similaritySearch(
                                SearchRequest.builder()
                                        .query("*")
                                        .topK(Integer.MAX_VALUE)
                                        .build())
                .stream()
                .filter(doc -> documentId.equals(doc.getMetadata().get("documentId")))
                .map(doc -> (String) doc.getMetadata().get("id"))
                .collect(Collectors.toList())
                );
        log.info("Removed vectors for document '{}'", documentId);
    }

    public List<RetrievedChunk> search(String query, int k) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(k)
                        .build());
                log.debug("Retrieved {} chunks for query: '{}'", results.size(), query);
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