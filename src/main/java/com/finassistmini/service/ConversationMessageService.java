package com.finassistmini.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finassistmini.dto.MessageResponse;
import com.finassistmini.dto.SourceReference;
import com.finassistmini.model.ConversationMessage;
import com.finassistmini.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageService {

    private final ConversationMessageRepository repo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String conversationId) {
        return repo.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void saveUserMessage(String conversationId, String content) {
        repo.save(ConversationMessage.builder()
                .conversationId(conversationId)
                .role("user")
                .content(content)
                .build());
    }

    @Transactional
    public void saveAssistantMessage(String conversationId, String content, List<SourceReference> sources) {
        repo.save(ConversationMessage.builder()
                .conversationId(conversationId)
                .role("assistant")
                .content(content)
                .sources(serialize(sources))
                .build());
    }

    private String serialize(List<SourceReference> sources) {
        if (sources == null || sources.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(sources); }
        catch (JsonProcessingException e) { log.debug("Could not serialize sources: {}", e.getMessage()); return null; }
    }

    private List<SourceReference> deserialize(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { return List.of(); }
    }

    private MessageResponse toResponse(ConversationMessage m) {
        return new MessageResponse(
                m.getId(), m.getRole(), m.getContent(),
                deserialize(m.getSources()), m.getCreatedAt());
    }
}