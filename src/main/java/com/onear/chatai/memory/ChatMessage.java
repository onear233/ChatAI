package com.onear.chatai.memory;

import java.time.Instant;

public record ChatMessage(
    String role,
    String content,
    Instant timestamp
) {
    public ChatMessage(String role, String content) {
        this(role, content, Instant.now());
    }
}
