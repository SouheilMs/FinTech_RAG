package com.finassistmini.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateAnswer(String prompt) {
        String answer = chatClient.prompt(prompt).call().content();
        return answer == null ? "" : answer.trim();
    }
}
