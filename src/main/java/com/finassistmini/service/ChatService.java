package com.finassistmini.service;

import com.finassistmini.config.AppProperties;
import com.finassistmini.dto.ChatResponse;
import com.finassistmini.dto.SourceReference;
import com.finassistmini.model.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatModel chatModel;
    private final VectorStoreService vectorStore;
    private final AppProperties props;
    private final Semaphore semaphore;

    public ChatService( ChatModel chatModel, VectorStoreService vectorStore, AppProperties props) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.props = props;
        this.semaphore = new Semaphore(props.chatMaxConcurrency(), true);
    }

    public ChatResponse chat(String question, String ownerId) throws InterruptedException {
        long waitMs   = (long) (props.admissionWaitSeconds() * 1000);
        boolean acquired = semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chat server is busy — please retry shortly");
        }
        try {
            // Search directly with query text - Spring AI handles embedding
            List<RetrievedChunk> chunks = vectorStore.search(question, ownerId, props.retrievalK());
            log.debug("Retrieved {} chunks for question: '{}'", chunks.size(), question);
            if (chunks.isEmpty()) {
                return new ChatResponse(
                        "I couldn't find relevant information in the uploaded documents.",
                        List.of());
            }
            String context = buildContext(chunks);
            String prompt  = buildPrompt(question, context);
            String answer = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            List<SourceReference> sources = chunks.stream()
                    .map(c -> new SourceReference(c.documentName(), c.pageNumber()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            ArrayList::new));
            return new ChatResponse(answer.strip(), sources);
        } finally {
            semaphore.release();
        }
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            sb.append("[%d] Source: '%s', page %d\n%s\n\n"
                    .formatted(i + 1, c.documentName(), c.pageNumber(), c.text()));
        }
        String ctx = sb.toString();
        // Truncate to avoid exceeding model context window
        return ctx.length() > props.maxPromptChars()
                ? ctx.substring(0, props.maxPromptChars()) + "\n[...truncated]"
                : ctx;
    }

    private String buildPrompt(String question, String context) {
        return """
                You are a financial document assistant. \
                Answer the question using ONLY the information provided in the context below. \
                If the context does not contain enough information to answer, say: \
                "I cannot find this information in the provided documents."
                Do not speculate or add information not present in the context.

                Context:
                %s
                Question: %s

                Answer:""".formatted(context, question);
    }

    public record StreamingContext(List<SourceReference> sources, Flux<String> tokens) {}

    public StreamingContext stream(String question, String ownerId) throws InterruptedException {
        long waitMs  = (long) (props.admissionWaitSeconds() * 1000);
        boolean acquired = semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chat server is busy — please retry shortly");
        }
        try {
            List<RetrievedChunk> chunks = vectorStore.search(question, ownerId, props.retrievalK());
            log.debug("Retrieved {} chunks for streaming question: '{}'", chunks.size(), question);

            if (chunks.isEmpty()) {
                semaphore.release();
                return new StreamingContext(
                        List.of(),
                        Flux.just("I couldn't find relevant information in the uploaded documents.")
                );
            }

            String context = buildContext(chunks);
            String prompt  = buildPrompt(question, context);

            List<SourceReference> sources = chunks.stream()
                    .map(c -> new SourceReference(c.documentName(), c.pageNumber()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            ArrayList::new));

            Flux<String> tokens = chatModel.stream(new Prompt(prompt))
                    .map(chatResponse -> {
                        var generation = chatResponse.getResult();
                        if (generation == null) return "";
                        var output = generation.getOutput();
                        if (output == null) return "";
                        var text = output.getText();
                        return text != null ? text : "";
                    })
                    .filter(text -> !text.isEmpty())
                    .doFinally(signal -> semaphore.release());

            return new StreamingContext(sources, tokens);

        } catch (Exception e) {
            semaphore.release();
            throw e;
        }
    }
}