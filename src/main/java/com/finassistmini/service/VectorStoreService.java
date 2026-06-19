package com.finassistmini.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.config.FinassistProperties;
import com.finassistmini.model.DocumentChunk;
import com.finassistmini.model.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class VectorStoreService {

    private final Path storeFile;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<VectorRecord> records;

    public VectorStoreService(FinassistProperties properties, ObjectMapper objectMapper) {
        this.storeFile = properties.vectorStoreFile();
        this.objectMapper = objectMapper;
        this.records = loadRecords();
    }

    public void upsertChunks(List<DocumentChunk> chunks, List<List<Double>> embeddings) {
        if (chunks.isEmpty()) {
            return;
        }
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Chunks and embeddings must have the same size.");
        }

        lock.writeLock().lock();
        try {
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                records.removeIf(record -> record.chunkId().equals(chunk.chunkId()));
                records.add(VectorRecord.from(chunk, embeddings.get(i)));
            }
            persistRecords();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean documentExists(String documentHash) {
        lock.readLock().lock();
        try {
            return records.stream().anyMatch(record -> documentHash.equals(record.documentHash()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RetrievedChunk> search(List<Double> queryEmbedding, int topK) {
        lock.readLock().lock();
        try {
            return records.stream()
                    .map(record -> record.toRetrievedChunk(cosineDistance(queryEmbedding, record.embedding())))
                    .sorted(Comparator.comparingDouble(RetrievedChunk::distance))
                    .limit(topK)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void deleteDocument(String documentId) {
        lock.writeLock().lock();
        try {
            records.removeIf(record -> documentId.equals(record.documentId()));
            persistRecords();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<VectorRecord> loadRecords() {
        if (!Files.exists(storeFile)) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(storeFile.toFile(), new TypeReference<List<VectorRecord>>() {
            }));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load vector store file '" + storeFile + "'.", ex);
        }
    }

    private void persistRecords() {
        try {
            Path parent = storeFile.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storeFile.toFile(), records);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist vector store file '" + storeFile + "'.", ex);
        }
    }

    private double cosineDistance(List<Double> left, List<Double> right) {
        int dimensions = Math.min(left.size(), right.size());
        if (dimensions == 0) {
            return 1.0;
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < dimensions; i++) {
            double a = left.get(i);
            double b = right.get(i);
            dot += a * b;
            leftNorm += a * a;
            rightNorm += b * b;
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 1.0;
        }
        return 1.0 - (dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)));
    }

    private record VectorRecord(
            String chunkId,
            String documentId,
            String documentHash,
            String documentName,
            int pageNumber,
            String text,
            List<Double> embedding
    ) {

        static VectorRecord from(DocumentChunk chunk, List<Double> embedding) {
            return new VectorRecord(
                    chunk.chunkId(),
                    chunk.documentId(),
                    chunk.documentId(),
                    chunk.documentName(),
                    chunk.pageNumber(),
                    chunk.text(),
                    embedding
            );
        }

        RetrievedChunk toRetrievedChunk(double distance) {
            return new RetrievedChunk(chunkId, documentId, documentName, pageNumber, text, distance);
        }
    }
}
