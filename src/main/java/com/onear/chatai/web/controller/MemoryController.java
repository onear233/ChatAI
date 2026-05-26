package com.onear.chatai.web.controller;

import com.onear.chatai.memory.LongTermMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/memory/longterm")
public class MemoryController {

    private final LongTermMemory longTermMemory;

    public MemoryController(LongTermMemory longTermMemory) {
        this.longTermMemory = longTermMemory;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(longTermMemory.list());
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Map<String, String> body) {
        String topic = body.getOrDefault("topic", "").trim();
        String content = body.getOrDefault("content", "").trim();
        if (topic.isEmpty() || content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "topic and content required"));
        }
        var entry = longTermMemory.add(topic, content);
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        boolean removed = longTermMemory.remove(id);
        if (removed) return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
        return ResponseEntity.badRequest().body(Map.of("error", "Not found: " + id));
    }

    @DeleteMapping
    public ResponseEntity<?> clear() {
        longTermMemory.clear();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
