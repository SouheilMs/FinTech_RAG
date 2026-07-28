package com.finassistmini.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.dto.*;
import com.finassistmini.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationMessageService messageService;
    private final ConversationMemoryService memoryService;
    private final ChatService chatService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> list( @RequestParam(required = false) String q) {
        return ResponseEntity.ok( conversationService.list(currentUserService.getUserId(), q));
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create( @Valid @RequestBody CreateConversationRequest req) {
        ConversationResponse resp = conversationService.create(currentUserService.getUserId(), req.firstMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok( conversationService.getById(id, currentUserService.getUserId()));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<ConversationResponse> rename( @PathVariable String id, @Valid @RequestBody RenameConversationRequest req) {
        return ResponseEntity.ok( conversationService.rename(id, currentUserService.getUserId(), req.title()));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ConversationResponse> pin( @PathVariable String id, @RequestBody PinRequest req) {
        return ResponseEntity.ok( conversationService.setPin(id, currentUserService.getUserId(), req.pinned()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        conversationService.delete(id, currentUserService.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable String id) {
        String ownerId = currentUserService.getUserId();
        conversationService.findOrThrow(id, ownerId); // verify ownership
        return ResponseEntity.ok(messageService.getMessages(id));
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage( @PathVariable String id, @Valid @RequestBody SendMessageRequest req) {
        String ownerId  = currentUserService.getUserId();
        String question = req.message();
        conversationService.findOrThrow(id, ownerId); // 403 / 404 before emitter is opened
        SseEmitter emitter = new SseEmitter(180_000L);
        try {
            // 1. Persist the user message immediately
            messageService.saveUserMessage(id, question);

            // 2. Build conversation memory (recent messages + summary if cached)
            String memoryContext = memoryService.buildMemoryContext(id);

            // 3. RAG search + LLM stream setup
            ChatService.StreamingContext ctx = chatService.streamWithMemory(question, memoryContext, ownerId);

            // 4. Send sources before the first token
            emitter.send(SseEmitter.event()
                    .name("sources")
                    .data(objectMapper.writeValueAsString(ctx.sources())));

            // 5. Stream tokens, accumulate the full response for persistence
            StringBuilder accumulator = new StringBuilder();

            ctx.tokens().subscribe(
                    token -> {
                        try {
                            accumulator.append(token);
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (IOException e) {
                            log.warn("Client disconnected during stream: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("Streaming error for conversation {}: {}", id, error.getMessage());
                        try { emitter.send(SseEmitter.event().name("error").data(error.getMessage())); }
                        catch (IOException ignored) {}
                        emitter.completeWithError(error);
                    },
                    () -> {
                        try {
                            String fullResponse = accumulator.toString();

                            // Persist assistant message + update conversation metadata
                            messageService.saveAssistantMessage(id, fullResponse, ctx.sources());
                            conversationService.updateLastMessage(id);

                            // Trigger async summary if conversation is long
                            memoryService.maybeGenerateSummary(id);

                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
            );

        } catch (Exception e) {
            log.error("Failed to initiate conversation stream {}: {}", id, e.getMessage());
            emitter.completeWithError(e);
        }

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.error("SSE error: {}", e.getMessage()));
        return emitter;
    }
}