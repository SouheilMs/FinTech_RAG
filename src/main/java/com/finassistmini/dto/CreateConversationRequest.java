package com.finassistmini.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank @Size(max = 500) String firstMessage
) {}
