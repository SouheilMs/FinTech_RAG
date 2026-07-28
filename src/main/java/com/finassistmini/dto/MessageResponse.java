package com.finassistmini.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        String id,
        String role,
        String content,
        List<SourceReference> sources,
        LocalDateTime createdAt
) {}