package com.onear.chatai.model;

import com.onear.chatai.memory.ChatMessage;
import java.util.List;
import java.util.Map;

public interface ModelProvider {

    String getId();

    String getName();

    String getType();

    String chat(String systemPrompt, List<ChatMessage> history, Map<String, Object> options);
}
