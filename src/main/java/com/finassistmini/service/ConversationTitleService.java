package com.finassistmini.service;

import org.springframework.stereotype.Component;

@Component
public class ConversationTitleService {

    private static final int MAX_TITLE_LENGTH = 60;

    public String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) return "New Conversation";

        String cleaned = firstMessage.strip()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ");

        if (cleaned.length() <= MAX_TITLE_LENGTH) return capitalize(cleaned);

        // Trim to the last complete word before the limit
        int cutoff = cleaned.lastIndexOf(' ', MAX_TITLE_LENGTH);
        if (cutoff < 10) cutoff = MAX_TITLE_LENGTH;

        return capitalize(cleaned.substring(0, cutoff).strip()) + "…";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}