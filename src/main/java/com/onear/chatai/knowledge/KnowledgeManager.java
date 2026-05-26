package com.onear.chatai.knowledge;

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
import java.util.*;

@Component
public class KnowledgeManager {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeManager.class);
    private final Map<String, KnowledgeEntry> entries = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path storageFile;

    public KnowledgeManager() {
        this.storageFile = Paths.get("data", "knowledge", "knowledge.json");
        try {
            Files.createDirectories(storageFile.getParent());
        } catch (IOException e) {
            log.warn("Could not create knowledge dir: {}", e.getMessage());
        }
        load();
    }

    public KnowledgeEntry add(String name, String type, String content, boolean active) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        if (active) deactivateAll();
        var entry = new KnowledgeEntry(id, name, type, content, active);
        entries.put(id, entry);
        save();
        return entry;
    }

    public boolean remove(String id) {
        var removed = entries.remove(id);
        if (removed != null) save();
        return removed != null;
    }

    public void setActive(String id, boolean active) {
        var existing = entries.get(id);
        if (existing == null) return;
        if (active) deactivateAll();
        entries.put(id, new KnowledgeEntry(existing.id(), existing.name(), existing.type(),
            existing.content(), existing.createdAt(), active));
        save();
    }

    public List<KnowledgeEntry> list() {
        return entries.values().stream()
            .sorted(Comparator.comparing(KnowledgeEntry::createdAt).reversed())
            .toList();
    }

    public KnowledgeEntry get(String id) {
        return entries.get(id);
    }

    public KnowledgeEntry getActive() {
        return entries.values().stream().filter(KnowledgeEntry::active).findFirst().orElse(null);
    }

    public String getSystemPrompt() {
        var active = getActive();
        if (active == null) return "";
        if ("role".equals(active.type())) {
            return "=== ROLE ===\nYou are now: " + active.name() + "\n\n"
                + active.content() + "\n\n"
                + "Stay in character at all times. Respond as this persona would.\n";
        }
        return "=== KNOWLEDGE BASE ===\nUse the following reference knowledge:\n\n"
            + active.content() + "\n";
    }

    private void deactivateAll() {
        entries.forEach((k, v) -> {
            if (v.active()) {
                entries.put(k, new KnowledgeEntry(v.id(), v.name(), v.type(),
                    v.content(), v.createdAt(), false));
            }
        });
    }

    private void load() {
        try {
            if (Files.exists(storageFile)) {
                List<KnowledgeEntry> list = mapper.readValue(storageFile.toFile(),
                    new TypeReference<List<KnowledgeEntry>>() {});
                list.forEach(e -> entries.put(e.id(), e));
            }
        } catch (JacksonException e) {
            log.warn("Failed to load knowledge: {}", e.getMessage());
        }
    }

    private void save() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(),
                new ArrayList<>(entries.values()));
        } catch (JacksonException e) {
            log.warn("Failed to save knowledge: {}", e.getMessage());
        }
    }
}
