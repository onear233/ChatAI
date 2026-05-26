package com.onear.chatai.memory;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LongTermMemory {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemory.class);
    private final List<MemoryEntry> entries = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path storageFile;

    public LongTermMemory() {
        this.storageFile = Paths.get("data", "memory", "longterm.json");
        try {
            Files.createDirectories(storageFile.getParent());
        } catch (IOException e) {
            log.warn("Could not create longterm memory dir: {}", e.getMessage());
        }
        load();
    }

    public MemoryEntry add(String topic, String content) {
        entries.removeIf(e -> e.topic().equalsIgnoreCase(topic));
        String id = UUID.randomUUID().toString().substring(0, 8);
        var entry = new MemoryEntry(id, topic, content, Instant.now());
        entries.add(entry);
        save();
        return entry;
    }

    public boolean remove(String id) {
        boolean removed = entries.removeIf(e -> e.id().equals(id));
        if (removed) save();
        return removed;
    }

    public List<MemoryEntry> list() {
        return entries.stream()
            .sorted(Comparator.comparing(MemoryEntry::createdAt).reversed())
            .toList();
    }

    public void clear() {
        entries.clear();
        save();
    }

    public String getMemoryPrompt() {
        if (entries.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("=== LONG-TERM MEMORY ===\n");
        sb.append("You remember these facts about the user:\n");
        for (MemoryEntry entry : entries) {
            sb.append("- ").append(entry.topic()).append(": ").append(entry.content()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private void load() {
        try {
            if (Files.exists(storageFile)) {
                List<MemoryEntry> list = mapper.readValue(storageFile.toFile(),
                    new TypeReference<List<MemoryEntry>>() {});
                entries.addAll(list);
            }
        } catch (JacksonException e) {
            log.warn("Failed to parse long-term memory: {}", e.getMessage());
        }
    }

    private void save() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(),
                new ArrayList<>(entries));
        } catch (JacksonException e) {
            log.warn("Failed to serialize long-term memory: {}", e.getMessage());
        }
    }

    public record MemoryEntry(String id, String topic, String content, Instant createdAt) {}
}
