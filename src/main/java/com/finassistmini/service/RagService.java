package com.finassistmini.service;

import com.finassistmini.config.FinassistProperties;
import com.finassistmini.dto.ChatSource;
import com.finassistmini.model.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;
    private final int topK;
    private final int maxPromptChars;

    public RagService(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            LlmService llmService,
            FinassistProperties properties
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.llmService = llmService;
        this.topK = properties.retrievalK();
        this.maxPromptChars = properties.maxPromptChars();
    }

    public RagAnswer answerQuestion(String question) {
        long started = System.nanoTime();
        List<Double> queryEmbedding = embeddingService.embedText(question);
        List<RetrievedChunk> retrievedChunks = vectorStoreService.search(queryEmbedding, topK);
        String prompt = buildPrompt(question, retrievedChunks);
        String answer = llmService.generateAnswer(prompt);
        log.info(
                "chat_pipeline_metrics question_chars={} retrieved_chunks={} prompt_chars={} total_ms={}",
                question.length(),
                retrievedChunks.size(),
                prompt.length(),
                (System.nanoTime() - started) / 1_000_000
        );
        return new RagAnswer(answer, buildSources(retrievedChunks));
    }

    private String buildPrompt(String question, List<RetrievedChunk> retrievedChunks) {
        String context;
        if (retrievedChunks.isEmpty()) {
            context = "No relevant context was found in the uploaded documents.";
        } else {
            List<String> contextParts = new ArrayList<>();
            int currentSize = 0;
            for (RetrievedChunk chunk : retrievedChunks) {
                String chunkText = "[Document: " + chunk.documentName() + " | Page: " + chunk.pageNumber() + "]\n"
                        + chunk.text();
                int projected = currentSize + chunkText.length() + (contextParts.isEmpty() ? 0 : 2);
                if (projected > maxPromptChars) {
                    break;
                }
                contextParts.add(chunkText);
                currentSize = projected;
            }
            context = contextParts.isEmpty()
                    ? "Context was retrieved but exceeded prompt budget."
                    : String.join("\n\n", contextParts);
        }

        return "You are FinAssist, a financial assistant. "
                + "Answer strictly from the provided context. "
                + "If the answer is not in context, say you do not have enough information from uploaded documents.\n\n"
                + "Context:\n" + context + "\n\n"
                + "Question: " + question + "\n\n"
                + "Answer:";
    }

    private List<ChatSource> buildSources(List<RetrievedChunk> retrievedChunks) {
        Set<String> seen = new LinkedHashSet<>();
        List<ChatSource> sources = new ArrayList<>();
        for (RetrievedChunk chunk : retrievedChunks) {
            String key = chunk.documentName() + "\u0000" + chunk.pageNumber();
            if (seen.add(key)) {
                sources.add(new ChatSource(chunk.documentName(), chunk.pageNumber()));
            }
        }
        return sources;
    }

    public record RagAnswer(String answer, List<ChatSource> sources) {
    }
}
