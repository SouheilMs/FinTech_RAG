package com.finassistmini.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class PathSupport {

    private PathSupport() {
    }

    static void createParentDirectories(Path path) throws IOException {
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
