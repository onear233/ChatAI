package com.onear.chatai.memory;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MemoryManager {

    private final List<ChatMemory> memories = new CopyOnWriteArrayList<>();

    public MemoryManager(List<ChatMemory> memories) {
        this.memories.addAll(memories);
    }

    public void add(String sessionId, ChatMessage message) {
        for (ChatMemory m : memories) {
            m.add(sessionId, message);
        }
    }

    public List<ChatMessage> getHistory(String sessionId, int maxMessages) {
        if (memories.isEmpty()) return List.of();
        return memories.get(0).getHistory(sessionId, maxMessages);
    }

    public void clear(String sessionId) {
        for (ChatMemory m : memories) {
            m.clear(sessionId);
        }
    }
}
