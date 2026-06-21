package com.finassistmini.web;

import com.finassistmini.dto.ChatRequest;
import com.finassistmini.dto.ChatResponse;
import com.finassistmini.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "RAG-powered Q&A grounded on uploaded financial documents")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
        return chatService.chat(request.question());
    }
}