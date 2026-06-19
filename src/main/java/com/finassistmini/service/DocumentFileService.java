package com.finassistmini.service;

import com.finassistmini.config.FinassistProperties;
import com.finassistmini.dto.DocumentInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
public class DocumentFileService {

    private final Path docsDirectory;

    public DocumentFileService(FinassistProperties properties) {
        this.docsDirectory = properties.docsDirectory();
    }

    public Path docsDirectory() {
        return docsDirectory;
    }

    public List<Path> listDocumentFiles() {
        try {
            if (!Files.exists(docsDirectory)) {
                return List.of();
            }
            try (var stream = Files.list(docsDirectory)) {
                return stream
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list stored documents.", ex);
        }
    }

    public List<DocumentInfo> listDocuments() {
        return listDocumentFiles().stream()
                .map(path -> path.getFileName().toString())
                .filter(fileName -> !"tmp".equals(extractDocumentId(fileName)))
                .map(fileName -> new DocumentInfo(extractDocumentId(fileName), extractOriginalFilename(fileName)))
                .toList();
    }

    public String extractDocumentId(String fileName) {
        int separator = fileName.indexOf("__");
        return separator >= 0 ? fileName.substring(0, separator) : fileName;
    }

    public String extractOriginalFilename(String fileName) {
        int separator = fileName.indexOf("__");
        return separator >= 0 ? fileName.substring(separator + 2) : fileName;
    }

    public List<Path> findFilesByDocumentId(String documentId) {
        return listDocumentFiles().stream()
                .filter(path -> documentId.equals(extractDocumentId(path.getFileName().toString())))
                .toList();
    }

    public void deleteDocumentFiles(String documentId) {
        for (Path filePath : findFilesByDocumentId(documentId)) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to delete document '" + filePath.getFileName() + "'.", ex);
            }
        }
    }
}
