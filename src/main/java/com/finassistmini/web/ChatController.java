package com.finassistmini.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.dto.ChatRequest;
import com.finassistmini.dto.ChatResponse;
import com.finassistmini.service.ChatService;
import com.finassistmini.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static com.finassistmini.web.GlobalExceptionHandler.log;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "RAG-powered Q&A grounded on uploaded financial documents")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, ObjectMapper objectMapper, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @Operation(
            summary     = "Ask a question",
            description = """
            Embeds the question, retrieves the top-K most relevant chunks from the
            vector store, builds a grounded prompt, and returns the LLM answer
            together with explicit source references (document + page).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer generated",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body (blank or too-long question)"),
            @ApiResponse(responseCode = "503", description = "Chat workers saturated — retry shortly")
    })
    public ChatResponse chat(@Valid @RequestBody ChatRequest request)
            throws InterruptedException {
        String ownerId = currentUserService.getUserId();
        return chatService.chat(request.question(), ownerId);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        String ownerId = currentUserService.getUserId();
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout

        try {
            // Vector search happens here — synchronously and fast (~100 ms)
            ChatService.StreamingContext ctx = chatService.stream(request.question(), ownerId);

            // Send sources before the first token so the UI can render them immediately
            emitter.send(
                    SseEmitter.event()
                            .name("sources")
                            .data(objectMapper.writeValueAsString(ctx.sources()))
            );

            // Subscribe to the Ollama token Flux
            ctx.tokens().subscribe(
                    token -> {
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (IOException e) {
                            log.warn("Client disconnected during stream");
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("Streaming error: {}", error.getMessage());
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error").data(error.getMessage()));
                        } catch (IOException ignored) {}
                        emitter.completeWithError(error);
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
            );

        } catch (Exception e) {
            log.error("Failed to start stream: {}", e.getMessage());
            emitter.completeWithError(e);
        }

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.error("SSE error: {}", e.getMessage()));

        return emitter;
    }
}