package com.onear.chatai.web.controller;

import com.onear.chatai.knowledge.KnowledgeEntry;
import com.onear.chatai.knowledge.KnowledgeManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeManager manager;

    public KnowledgeController(KnowledgeManager manager) {
        this.manager = manager;
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeEntry>> list() {
        return ResponseEntity.ok(manager.list());
    }

    @PostMapping("/text")
    public ResponseEntity<KnowledgeEntry> addText(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "Untitled");
        String type = body.getOrDefault("type", "knowledge");
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
        boolean active = Boolean.parseBoolean(body.getOrDefault("active", "true"));
        var entry = manager.add(name, type, content, active);
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/upload")
    public ResponseEntity<KnowledgeEntry> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "knowledge") String type) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String name = file.getOriginalFilename();
            if (name != null && name.contains(".")) {
                name = name.substring(0, name.lastIndexOf('.'));
            }
            var entry = manager.add(name, type, content, true);
            return ResponseEntity.ok(entry);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<Map<String, Object>> setActive(@PathVariable String id) {
        manager.setActive(id, true);
        KnowledgeEntry entry = manager.get(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "activated");
        result.put("id", id);
        result.put("name", entry != null ? entry.name() : "unknown");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        boolean removed = manager.remove(id);
        if (removed) {
            return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Not found: " + id));
    }
}
