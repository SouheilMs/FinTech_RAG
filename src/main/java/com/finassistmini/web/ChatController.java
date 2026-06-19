package com.finassistmini.web;

import com.finassistmini.config.FinassistProperties;
import com.finassistmini.dto.ChatRequest;
import com.finassistmini.dto.ChatResponse;
import com.finassistmini.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final RagService ragService;
    private final Semaphore chatSemaphore;
    private final FinassistProperties properties;

    public ChatController(
            RagService ragService,
            @Qualifier("chatSemaphore") Semaphore chatSemaphore,
            FinassistProperties properties
    ) {
        this.ragService = ragService;
        this.chatSemaphore = chatSemaphore;
        this.properties = properties;
    }

    @PostMapping
    @Operation(summary = "Chat with RAG assistant", description = "Send a question to the RAG assistant and get an answer based on ingested documents")
    @ResponseStatus(HttpStatus.OK)
    public ChatResponse chat(@Valid @RequestBody ChatRequest payload) throws InterruptedException {
        if (!chatSemaphore.tryAcquire((long) (properties.admissionWaitSeconds() * 1000), TimeUnit.MILLISECONDS)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Chat service is busy. Please retry shortly.");
        }
        try {
            RagService.RagAnswer answer = ragService.answerQuestion(payload.question());
            return new ChatResponse(answer.answer(), answer.sources());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        } finally {
            chatSemaphore.release();
        }
    }
}
