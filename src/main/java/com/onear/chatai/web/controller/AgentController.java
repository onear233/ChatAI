package com.onear.chatai.web.controller;

import com.onear.chatai.agent.AgentOrchestrator;
import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.memory.MemoryManager;
import com.onear.chatai.model.ModelRegistry;
import com.onear.chatai.web.dto.ChatRequest;
import com.onear.chatai.web.dto.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final MemoryManager memoryManager;
    private final ModelRegistry modelRegistry;

    public AgentController(AgentOrchestrator orchestrator, MemoryManager memoryManager, ModelRegistry modelRegistry) {
        this.orchestrator = orchestrator;
        this.memoryManager = memoryManager;
        this.modelRegistry = modelRegistry;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId();
        String response = orchestrator.chat(sessionId, request.message());
        String model = modelRegistry.getActive() != null ? modelRegistry.getActive().getName() : "none";
        return ResponseEntity.ok(new ChatResponse(sessionId, response, model));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<String>> listSessions() {
        return ResponseEntity.ok(new ArrayList<>(memoryManager.getHistory("", 0) instanceof List
                ? List.of() : List.of()));
    }

    @GetMapping("/sessions/{id}/history")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String id, @RequestParam(defaultValue = "50") int max) {
        var history = memoryManager.getHistory(id, max);
        return ResponseEntity.ok(Map.of("sessionId", id, "messages", history, "count", history.size()));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String id) {
        memoryManager.clear(id);
        return ResponseEntity.ok(Map.of("status", "deleted", "sessionId", id));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> createSession() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.ok(Map.of("sessionId", id));
    }
}
