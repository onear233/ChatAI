package com.onear.chatai.web.dto;

public record ChatRequest(String message, String sessionId) {
    public ChatRequest {
        if (sessionId == null || sessionId.isBlank()) sessionId = "default";
    }
}
