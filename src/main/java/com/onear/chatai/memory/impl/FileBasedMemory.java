package com.onear.chatai.memory.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import com.onear.chatai.memory.ChatMemory;
import com.onear.chatai.memory.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileBasedMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(FileBasedMemory.class);
    private final Path storageDir;
    private final ObjectMapper mapper;

    public FileBasedMemory() {
        this.storageDir = Paths.get("data", "memory");
        this.mapper = new ObjectMapper();
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.warn("Could not create memory storage dir: {}", e.getMessage());
        }
    }

    @Override
    public void add(String sessionId, ChatMessage message) {
        try {
            var messages = loadSession(sessionId);
            messages.add(message);
            saveSession(sessionId, messages);
        } catch (IOException e) {
            log.warn("Failed to persist message: {}", e.getMessage());
        }
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId, int maxMessages) {
        try {
            var messages = loadSession(sessionId);
            int from = Math.max(0, messages.size() - maxMessages);
            return messages.subList(from, messages.size());
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public void clear(String sessionId) {
        try {
            Files.deleteIfExists(sessionFile(sessionId));
        } catch (IOException e) {
            log.warn("Failed to clear memory: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getSessionIds() {
        try (var stream = Files.list(storageDir)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Path sessionFile(String sessionId) {
        return storageDir.resolve(sanitize(sessionId) + ".json");
    }

    private List<ChatMessage> loadSession(String sessionId) throws IOException {
        Path file = sessionFile(sessionId);
        if (!Files.exists(file)) return new ArrayList<>();
        return mapper.readValue(file.toFile(), new TypeReference<List<ChatMessage>>() {});
    }

    private void saveSession(String sessionId, List<ChatMessage> messages) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile(sessionId).toFile(), messages);
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
