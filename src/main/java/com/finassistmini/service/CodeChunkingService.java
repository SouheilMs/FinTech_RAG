package com.finassistmini.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CodeChunkingService {

    private static final int LINES_PER_CHUNK = 60;
    private static final int OVERLAP_LINES   = 10;

    public List<String> chunk(String content, String relativePath) {
        if (content == null || content.isBlank()) return List.of();

        String[] lines  = content.split("\n", -1);
        List<String> chunks = new ArrayList<>();

        int step = LINES_PER_CHUNK - OVERLAP_LINES;

        for (int start = 0; start < lines.length; start += step) {
            int end = Math.min(start + LINES_PER_CHUNK, lines.length);

            StringBuilder chunk = new StringBuilder();
            // Header gives the LLM essential context about where this code lives
            chunk.append("// File: ").append(relativePath).append("\n");
            chunk.append("// Lines: ").append(start + 1).append("-").append(end).append("\n\n");

            for (int i = start; i < end; i++) {
                chunk.append(lines[i]).append("\n");
            }

            String text = chunk.toString().strip();
            if (!text.isBlank()) {
                chunks.add(text);
            }

            // Avoid an infinite loop on small files
            if (end >= lines.length) break;
        }

        return chunks;
    }
}