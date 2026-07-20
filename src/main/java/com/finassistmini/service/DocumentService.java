package com.finassistmini.service;

import com.finassistmini.config.AppProperties;
import com.finassistmini.model.DocumentMeta;
import com.finassistmini.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final AppProperties props;
    private final VectorStoreService vectorStoreService;
    private final DocumentRepository documentRepository;

    public DocumentService(AppProperties props, VectorStoreService vectorStoreService, DocumentRepository documentRepository) {
        this.props = props;
        this.vectorStoreService = vectorStoreService;
        this.documentRepository = documentRepository;
    }

    @PostConstruct
    public void load() {
        List<DocumentMeta> allDocs = documentRepository.findAll();
        if (!allDocs.isEmpty()) {
            log.info("Loaded {} document entries from index", allDocs.size());
        }
        else {
            log.info("No document entries found in index");
        }
    }

    @Transactional
    public DocumentMeta register(String documentId, String name, Path filePath, String ownerId, String ownerUsername) {
        DocumentMeta meta = new DocumentMeta();
        meta.setDocumentId(documentId);
        meta.setName(name);
        meta.setOwnerId(ownerId);
        meta.setOwnerUsername(ownerUsername);
        meta.setFilePath(filePath.toAbsolutePath().toString());
        meta.setUploadedAt(Instant.now());
        meta.setStatus("PENDING");
        documentRepository.saveAndFlush(meta);
        return meta;
    }

    @Transactional
    public void markIndexed(String documentId, int pageCount, int chunkCount) {
        DocumentMeta meta = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (meta == null) return;
        meta.setPageCount(pageCount);
        meta.setChunkCount(chunkCount);
        meta.setStatus("INDEXED");
        documentRepository.save(meta);
    }

    @Transactional
    public void markFailed(String documentId, String errorMessage) {
        DocumentMeta meta = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (meta == null) return;
        meta.setStatus("FAILED");
        meta.setErrorMessage(errorMessage);
        documentRepository.save(meta);
    }

    @Transactional
    public void markPending(String documentId) {
        DocumentMeta meta = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (meta == null) return;
        meta.setStatus("PENDING");
        meta.setPageCount(0);
        meta.setChunkCount(0);
        meta.setErrorMessage(null);
        documentRepository.save(meta);
    }

    @Transactional
    public void deleteByIdAndOwner(String id, String ownerId) {
        var doc = documentRepository.findByDocumentIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> {
                    // If document exists but owner differs, return 403 not 404
                    boolean exists = documentRepository.existsByDocumentId(id);
                    return exists
                            ? new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You do not have permission to delete document: " + id)
                            : new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Document not found: " + id);
                });
        // Remove pgvector chunks before removing the DB row
        vectorStoreService.removeByDocumentId(doc.getDocumentId(), ownerId);
        documentRepository.delete(doc);
    }

    public List<DocumentMeta> findAllByOwner(String ownerId) {
        return documentRepository.findAllByOwnerId(ownerId);
    }

    public DocumentMeta findByIdAndOwner(String id, String ownerId) {
        return documentRepository.findByDocumentIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    public Path getDocsDirectory() {
        return Path.of(props.docsDirectory());
    }
}