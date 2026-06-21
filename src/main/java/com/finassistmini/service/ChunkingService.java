package com.finassistmini.service;

import com.finassistmini.config.AppProperties;
import com.finassistmini.model.DocumentChunk;
import com.finassistmini.model.ParsedPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private final AppProperties props;

    public ChunkingService(AppProperties props) {
        this.props = props;
    }

    public List<DocumentChunk> chunk(String documentId, String documentName,
                                     List<ParsedPage> pages) {
        List<DocumentChunk> chunks  = new ArrayList<>();
        int chunkIdx = 0;
        int stride   = props.chunkSizeWords() - props.chunkOverlapWords();

        for (ParsedPage page : pages) {
            String[] words = page.text().split("\\s+");
            for (int start = 0; start < words.length; start += stride) {
                int    end  = Math.min(start + props.chunkSizeWords(), words.length);
                String text = String.join(" ", Arrays.copyOfRange(words, start, end));
                chunks.add(new DocumentChunk(
                        documentId + "_c" + chunkIdx++,
                        documentId,
                        documentName,
                        page.pageNumber(),
                        text
                ));
            }
        }
        log.info("Produced {} chunks from {} pages for document '{}'",
                chunks.size(), pages.size(), documentName);
        return chunks;
    }
}