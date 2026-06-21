package com.finassistmini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.config.AppProperties;
import com.finassistmini.model.RetrievedChunk;
import com.finassistmini.model.VectorEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<VectorEntry> store = new CopyOnWriteArrayList<>();
    private final Object writeLock = new Object();

    public VectorStoreService(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        Path path = Path.of(props.vectorStoreFile());
        if (!Files.exists(path)) {
            log.info("No vector store found at '{}' — starting fresh", path);
            return;
        }
        try {
            List<VectorEntry> entries = objectMapper.readValue(
                    path.toFile(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, VectorEntry.class));
            store.addAll(entries);
            log.info("Loaded {} vector entries from '{}'", store.size(), path);
        } catch (IOException e) {
            log.warn("Could not load vector store: {}", e.getMessage());
        }
    }

    public void addAll(List<VectorEntry> entries) {
        synchronized (writeLock) {
            store.addAll(entries);
            persist();
        }
    }

    public void removeByDocumentId(String documentId) {
        synchronized (writeLock) {
            store.removeIf(e -> documentId.equals(e.getDocumentId()));
            persist();
            log.info("Removed vectors for document '{}'", documentId);
        }
    }

    public List<RetrievedChunk> search(float[] queryEmbedding, int k) {
        return store.stream()
                .map(entry -> {
                    double distance = 1.0 - cosineSimilarity(queryEmbedding, entry.getEmbedding());
                    return new RetrievedChunk(
                            entry.getChunkId(),
                            entry.getDocumentId(),
                            entry.getDocumentName(),
                            entry.getPageNumber(),
                            entry.getText(),
                            distance);
                })
                .sorted(Comparator.comparingDouble(RetrievedChunk::distance))
                .limit(k)
                .collect(Collectors.toList());
    }

    public int size() {
        return store.size();
    }

    private void persist() {
        try {
            Path path = Path.of(props.vectorStoreFile());
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), new ArrayList<>(store));
        } catch (IOException e) {
            log.error("Failed to persist vector store: {}", e.getMessage());
        }
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dot / denom;
    }
}