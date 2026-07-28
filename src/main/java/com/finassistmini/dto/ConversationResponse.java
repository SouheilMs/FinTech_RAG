package com.finassistmini.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        String id,
        String title,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt
) {}