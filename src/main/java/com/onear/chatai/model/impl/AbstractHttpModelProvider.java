package com.onear.chatai.model.impl;

import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.model.ModelInfo;
import com.onear.chatai.model.ModelProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractHttpModelProvider implements ModelProvider {

    protected final ModelInfo info;
    protected final HttpClient httpClient;
    protected final com.onear.chatai.model.ApiKeyStore keyStore;

    protected AbstractHttpModelProvider(ModelInfo info) {
        this(info, null);
    }

    protected AbstractHttpModelProvider(ModelInfo info, com.onear.chatai.model.ApiKeyStore keyStore) {
        this.info = info;
        this.keyStore = keyStore;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String getId() { return info.id(); }

    @Override
    public String getName() { return info.name(); }

    @Override
    public String getType() { return info.type(); }

    private String resolveApiKey() {
        if (keyStore != null) {
            return keyStore.resolveKey(info.id(), info.apiKey());
        }
        return info.apiKey();
    }

    @Override
    public String chat(String systemPrompt, List<ChatMessage> history, Map<String, Object> options) {
        String requestBody = buildRequestBody(systemPrompt, history, options);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(info.apiEndpoint()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + resolveApiKey())
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "[Model error: HTTP " + response.statusCode() + " - " + response.body() + "]";
            }
            return extractContent(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return "[Model error: " + e.getMessage() + "]";
        }
    }

    protected abstract String buildRequestBody(String systemPrompt, List<ChatMessage> history, Map<String, Object> options);

    protected abstract String extractContent(String responseBody);

    protected List<Map<String, String>> buildMessageList(String systemPrompt, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }
        return messages;
    }
}
