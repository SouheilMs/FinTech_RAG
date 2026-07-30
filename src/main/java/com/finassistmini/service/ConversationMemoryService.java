package com.finassistmini.service;

import com.finassistmini.model.ConversationMessage;
import com.finassistmini.model.ConversationSummary;
import com.finassistmini.repository.ConversationMessageRepository;
import com.finassistmini.repository.ConversationSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private static final int MAX_RECENT_MESSAGES   = 20;
    private static final int SUMMARY_THRESHOLD     = 30;
    private static final int SUMMARY_UPDATE_INTERVAL = 15;

    private final ConversationMessageRepository messageRepo;
    private final ConversationSummaryRepository summaryRepo;
    private final ChatModel chatModel;

    public String buildMemoryContext(String conversationId) {
        StringBuilder ctx = new StringBuilder();

        // Cached summary of older messages (may not exist yet)
        summaryRepo.findByConversationId(conversationId).ifPresent(s ->
                ctx.append("Previous conversation summary:\n")
                        .append(s.getSummary()).append("\n\n"));

        // Recent messages in chronological order
        List<ConversationMessage> recent = new ArrayList<>(
                messageRepo.findRecentDesc(
                        conversationId, PageRequest.of(0, MAX_RECENT_MESSAGES)));
        Collections.reverse(recent);

        if (!recent.isEmpty()) {
            ctx.append("Conversation so far:\n");
            recent.forEach(m -> {
                String role = "user".equals(m.getRole()) ? "User" : "Assistant";
                ctx.append(role).append(": ").append(m.getContent()).append("\n");
            });
            ctx.append("\n");
        }

        return ctx.toString();
    }

    @Async
    @Transactional
    public void maybeGenerateSummary(String conversationId) {
        long count = messageRepo.countByConversationId(conversationId);
        if (count < SUMMARY_THRESHOLD) return;
        Optional<ConversationSummary> existing = summaryRepo.findByConversationId(conversationId);
        if (existing.isPresent()
                && (count - existing.get().getMessageCount()) < SUMMARY_UPDATE_INTERVAL) {
            return;
        }
        generateAndStore(conversationId, (int) count, existing.orElse(null));
    }

    private void generateAndStore(String conversationId, int messageCount, ConversationSummary existing) {
        List<ConversationMessage> all = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        StringBuilder history = new StringBuilder();
        all.forEach(m -> {
            String role = "user".equals(m.getRole()) ? "User" : "Assistant";
            history.append(role).append(": ").append(m.getContent()).append("\n");
        });
        String prompt = """
                Summarize the following conversation in 3-5 concise sentences.
                Preserve the key topics, decisions, and conclusions.
                
                Conversation:
                %s
                
                Summary:
                """.formatted(history.toString());

        try {
            String summary = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();

            if (existing != null) {
                existing.setSummary(summary);
                existing.setMessageCount(messageCount);
                summaryRepo.save(existing);
            } else {
                summaryRepo.save(ConversationSummary.builder()
                        .conversationId(conversationId)
                        .summary(summary)
                        .messageCount(messageCount)
                        .build());
            }
            log.info("Summary stored for conversation {} ({} messages)", conversationId, messageCount);
        } catch (Exception e) {
            log.warn("Summary generation failed for conversation {}: {}", conversationId, e.getMessage());
        }
    }
}