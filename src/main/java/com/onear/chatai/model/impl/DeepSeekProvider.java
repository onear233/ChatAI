package com.onear.chatai.model.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.model.ApiKeyStore;
import com.onear.chatai.model.ModelInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeepSeekProvider extends AbstractHttpModelProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekProvider(ModelInfo info) {
        super(info);
    }

    public DeepSeekProvider(ModelInfo info, ApiKeyStore keyStore) {
        super(info, keyStore);
    }

    @Override
    protected String buildRequestBody(String systemPrompt, List<ChatMessage> history, Map<String, Object> options) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", info.modelName());
            body.put("messages", buildMessageList(systemPrompt, history));
            body.put("temperature", options.getOrDefault("temperature", 0.7));
            body.put("max_tokens", options.getOrDefault("max_tokens", 2048));
            body.put("stream", false);

            // DeepSeek V4 thinking/reasoning support
            Object thinkingCfg = info.config() != null ? info.config().get("thinking") : null;
            if (thinkingCfg instanceof Map<?,?> thinkingMap) {
                body.put("thinking", thinkingMap);
            } else if ("true".equals(thinkingCfg) || Boolean.TRUE.equals(thinkingCfg)) {
                body.put("thinking", Map.of("type", "enabled"));
            }

            Object effort = info.config() != null ? info.config().get("reasoning_effort") : null;
            if (effort instanceof String s && !s.isBlank()) {
                body.put("reasoning_effort", s);
            }

            return mapper.writeValueAsString(body);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        try {
            var node = mapper.readTree(responseBody);
            var choices = node.path("choices");
            if (choices.isEmpty()) return "[No response]";
            var msg = choices.get(0).path("message");
            String content = msg.path("content").asText();
            String reasoning = msg.path("reasoning_content").asText();

            String modelLine = info.name() + "\n";

            if (!reasoning.isBlank() && !content.isBlank()) {
                return modelLine + "[Thinking]\n" + reasoning + "\n\n[Response]\n" + content;
            }
            if (!content.isBlank()) {
                return modelLine + "[Response]\n" + content;
            }
            return modelLine + reasoning;
        } catch (Exception e) {
            return "[Parse error: " + e.getMessage() + "]";
        }
    }
}
