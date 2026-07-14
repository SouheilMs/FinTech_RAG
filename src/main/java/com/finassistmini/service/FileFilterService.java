package com.finassistmini.service;

import com.finassistmini.config.RepositoryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileFilterService {

    private static final Logger log = LoggerFactory.getLogger(FileFilterService.class);

    private final RepositoryProperties props;

    public FileFilterService(RepositoryProperties props) {
        this.props = props;
    }

    public List<EligibleFile> collectFiles(Path repoRoot) throws IOException {
        Set<String> allowedExt = props.getAllowedExtensions().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> ignoredDirs = props.getIgnoredDirectories().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<EligibleFile> result = new ArrayList<>();

        Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {

            private int depth = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName() != null
                        ? dir.getFileName().toString().toLowerCase()
                        : "";

                // Skip hidden directories and ignored directories
                if (dirName.startsWith(".") || ignoredDirs.contains(dirName)) {
                    log.debug("Skipping directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                if (++depth > props.getMaxDepth()) {
                    log.debug("Max depth reached, skipping: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                depth--;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (result.size() >= props.getMaxFilesPerRepo()) {
                    log.warn("Max file limit ({}) reached, stopping traversal",
                            props.getMaxFilesPerRepo());
                    return FileVisitResult.TERMINATE;
                }

                String fileName  = file.getFileName().toString();
                String extension = extractExtension(fileName);
                long   size      = attrs.size();

                // Special-case filenames that have no extension
                boolean isSpecialFile = isSpecialFilename(fileName);

                if (!isSpecialFile && !allowedExt.contains(extension.toLowerCase())) {
                    return FileVisitResult.CONTINUE;
                }

                if (size > props.getMaxFileSizeBytes()) {
                    log.info("Skipping large file ({} bytes): {}", size, file);
                    result.add(EligibleFile.skipped(
                            repoRoot.relativize(file).toString(),
                            extension,
                            size,
                            "File size exceeds limit: " + size + " bytes"
                    ));
                    return FileVisitResult.CONTINUE;
                }

                // Skip binary files (check for null bytes in first 8KB)
                if (looksLikeBinary(file)) {
                    return FileVisitResult.CONTINUE;
                }

                result.add(EligibleFile.indexable(
                        repoRoot.relativize(file).toString(),
                        extension,
                        size
                ));

                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    private boolean isSpecialFilename(String fileName) {
        String upper = fileName.toUpperCase();
        return upper.equals("DOCKERFILE")
                || upper.equals("MAKEFILE")
                || upper.equals("README")
                || upper.equals("LICENSE")
                || upper.equals("CONTRIBUTING")
                || upper.equals("CHANGELOG");
    }

    private boolean looksLikeBinary(Path file) {
        try {
            byte[] sample = Files.readAllBytes(file);
            int check = Math.min(sample.length, 8192);
            for (int i = 0; i < check; i++) {
                if (sample[i] == 0) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    /** Represents a file candidate found during directory traversal. */
    public record EligibleFile(
            String relativePath,
            String extension,
            long   sizeBytes,
            boolean eligible,
            String skipReason
    ) {
        public static EligibleFile indexable(String path, String ext, long size) {
            return new EligibleFile(path, ext, size, true, null);
        }

        public static EligibleFile skipped(String path, String ext, long size, String reason) {
            return new EligibleFile(path, ext, size, false, reason);
        }
    }
}