package com.finassistmini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.config.AppProperties;
import com.finassistmini.model.DocumentMeta;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, DocumentMeta> documents = new ConcurrentHashMap<>();

    public DocumentService(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        Path index = indexFile();
        if (!Files.exists(index)) return;
        try {
            List<DocumentMeta> metas = objectMapper.readValue(
                    index.toFile(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, DocumentMeta.class));
            metas.forEach(m -> documents.put(m.getDocumentId(), m));
            log.info("Loaded {} document entries from index", documents.size());
        } catch (IOException e) {
            log.warn("Could not load document index: {}", e.getMessage());
        }
    }

    public DocumentMeta register(String documentId, String name, Path filePath) {
        DocumentMeta meta = new DocumentMeta();
        meta.setDocumentId(documentId);
        meta.setName(name);
        meta.setFilePath(filePath.toAbsolutePath().toString());
        meta.setUploadedAt(Instant.now());
        meta.setStatus("PENDING");
        documents.put(documentId, meta);
        persist();
        return meta;
    }

    public void markIndexed(String documentId, int pageCount, int chunkCount) {
        DocumentMeta meta = documents.get(documentId);
        if (meta == null) return;
        meta.setPageCount(pageCount);
        meta.setChunkCount(chunkCount);
        meta.setStatus("INDEXED");
        persist();
    }

    public void markFailed(String documentId, String errorMessage) {
        DocumentMeta meta = documents.get(documentId);
        if (meta == null) return;
        meta.setStatus("FAILED");
        meta.setErrorMessage(errorMessage);
        persist();
    }

    public void markPending(String documentId) {
        DocumentMeta meta = documents.get(documentId);
        if (meta == null) return;
        meta.setStatus("PENDING");
        meta.setPageCount(0);
        meta.setChunkCount(0);
        meta.setErrorMessage(null);
        persist();
    }

    public void delete(String documentId) {
        DocumentMeta meta = documents.remove(documentId);
        if (meta == null) return;
        try {
            Files.deleteIfExists(Path.of(meta.getFilePath()));
        } catch (IOException e) {
            log.warn("Could not delete PDF file '{}': {}", meta.getFilePath(), e.getMessage());
        }
        persist();
    }

    public List<DocumentMeta> findAll() {
        return new ArrayList<>(documents.values());
    }

    public Optional<DocumentMeta> findById(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    public Path getDocsDirectory() {
        return Path.of(props.docsDirectory());
    }

    private Path indexFile() {
        return Path.of(props.docsDirectory()).resolve("index.json");
    }

    private synchronized void persist() {
        try {
            Path index = indexFile();
            Files.createDirectories(index.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(index.toFile(), new ArrayList<>(documents.values()));
        } catch (IOException e) {
            log.error("Failed to persist document index: {}", e.getMessage());
        }
    }
}