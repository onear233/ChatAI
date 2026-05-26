package com.onear.chatai.memory.impl;

import com.onear.chatai.memory.ChatMemory;
import com.onear.chatai.memory.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryChatMemory implements ChatMemory {

    private final Map<String, List<ChatMessage>> store = new ConcurrentHashMap<>();
    private final int maxMessages;

    public InMemoryChatMemory() {
        this(50);
    }

    public InMemoryChatMemory(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String sessionId, ChatMessage message) {
        store.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
             .add(message);
        var list = store.get(sessionId);
        while (list.size() > maxMessages) {
            list.removeFirst();
        }
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId, int max) {
        var list = store.getOrDefault(sessionId, List.of());
        int from = Math.max(0, list.size() - max);
        return new ArrayList<>(list.subList(from, list.size()));
    }

    @Override
    public void clear(String sessionId) {
        store.remove(sessionId);
    }

    @Override
    public List<String> getSessionIds() {
        return List.copyOf(store.keySet());
    }
}
