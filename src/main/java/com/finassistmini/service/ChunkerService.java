package com.finassistmini.service;

import com.finassistmini.config.FinassistProperties;
import com.finassistmini.model.DocumentChunk;
import com.finassistmini.model.ParsedPage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ChunkerService {

    private final int chunkSizeWords;
    private final int chunkOverlapWords;

    public ChunkerService(FinassistProperties properties) {
        if (properties.chunkOverlapWords() >= properties.chunkSizeWords()) {
            throw new IllegalArgumentException("chunk-overlap-words must be smaller than chunk-size-words");
        }
        this.chunkSizeWords = properties.chunkSizeWords();
        this.chunkOverlapWords = properties.chunkOverlapWords();
    }

    public List<DocumentChunk> chunkPages(String documentId, String documentName, List<ParsedPage> pages) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int step = chunkSizeWords - chunkOverlapWords;

        for (ParsedPage page : pages) {
            List<String> words = Arrays.stream(page.text().split("\\s+"))
                    .filter(word -> !word.isBlank())
                    .toList();
            if (words.isEmpty()) {
                continue;
            }
            int chunkIndex = 0;
            for (int start = 0; start < words.size(); start += step) {
                int end = Math.min(start + chunkSizeWords, words.size());
                List<String> chunkWords = words.subList(start, end);
                if (chunkWords.isEmpty()) {
                    continue;
                }
                chunks.add(new DocumentChunk(
                        documentId + "_p" + page.pageNumber() + "_c" + chunkIndex,
                        documentId,
                        documentName,
                        page.pageNumber(),
                        String.join(" ", chunkWords)
                ));
                chunkIndex++;
            }
        }
        return chunks;
    }
}
