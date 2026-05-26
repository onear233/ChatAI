package com.onear.chatai.model.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.model.ApiKeyStore;
import com.onear.chatai.model.ModelInfo;

import java.util.List;
import java.util.Map;

public class LocalModelProvider extends AbstractHttpModelProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    public LocalModelProvider(ModelInfo info) {
        super(info);
    }

    public LocalModelProvider(ModelInfo info, ApiKeyStore keyStore) {
        super(info, keyStore);
    }

    @Override
    protected String buildRequestBody(String systemPrompt, List<ChatMessage> history, Map<String, Object> options) {
        try {
            var body = Map.of(
                "model", info.modelName(),
                "messages", buildMessageList(systemPrompt, history),
                "temperature", options.getOrDefault("temperature", 0.7),
                "max_tokens", options.getOrDefault("max_tokens", 2048),
                "stream", false
            );
            return mapper.writeValueAsString(body);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        try {
            var node = mapper.readTree(responseBody);
            return node.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "[Parse error: " + e.getMessage() + "]";
        }
    }
}
