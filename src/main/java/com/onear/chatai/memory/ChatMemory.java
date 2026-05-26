package com.onear.chatai.memory;

import java.util.List;

public interface ChatMemory {

    void add(String sessionId, ChatMessage message);

    List<ChatMessage> getHistory(String sessionId, int maxMessages);

    void clear(String sessionId);

    List<String> getSessionIds();
}
