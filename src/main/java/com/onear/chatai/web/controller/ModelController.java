package com.onear.chatai.web.controller;

import com.onear.chatai.model.ApiKeyStore;
import com.onear.chatai.model.ModelProperties;
import com.onear.chatai.model.ModelProvider;
import com.onear.chatai.model.ModelRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelRegistry registry;
    private final ModelProperties properties;
    private final ApiKeyStore keyStore;

    public ModelController(ModelRegistry registry, ModelProperties properties, ApiKeyStore keyStore) {
        this.registry = registry;
        this.properties = properties;
        this.keyStore = keyStore;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listModels() {
        var activeId = registry.getActiveModelId();
        var list = properties.getModels().stream().map(cfg -> {
            boolean hasKey = keyStore.hasKey(cfg.getId())
                || (cfg.getApiKey() != null && !cfg.getApiKey().isBlank());
            boolean enabled = keyStore.isRuntimeEnabled(cfg.getId()) || cfg.isEnabled();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cfg.getId());
            m.put("name", cfg.getName());
            m.put("type", cfg.getType());
            m.put("active", cfg.getId().equals(activeId));
            m.put("enabled", enabled);
            m.put("apiEndpoint", cfg.getApiEndpoint());
            m.put("modelName", cfg.getModelName());
            m.put("hasKey", hasKey);
            m.put("description", cfg.getConfig().getOrDefault("description", ""));
            m.put("icon", cfg.getConfig().getOrDefault("icon", "smart_toy"));
            m.put("color", cfg.getConfig().getOrDefault("color", "#58a6ff"));
            return m;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/active/{id}")
    public ResponseEntity<Map<String, Object>> setActive(@PathVariable String id) {
        var cfgOpt = properties.getModels().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst();

        if (cfgOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown model: " + id));
        }

        var cfg = cfgOpt.get();
        boolean enabled = keyStore.isRuntimeEnabled(cfg.getId()) || cfg.isEnabled();

        if (!enabled) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Model '" + cfg.getName() + "' is not enabled. Enable it first."
            ));
        }

        if (!"MOCK".equalsIgnoreCase(cfg.getType()) && !hasKey(cfg)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Model '" + cfg.getName() + "' requires an API key. Set it first."
            ));
        }

        try {
            registry.setActive(id);
            ModelProvider active = registry.getActive();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "switched");
            result.put("activeId", id);
            result.put("activeName", active != null ? active.getName() : "unknown");
            result.put("activeType", active != null ? active.getType() : "unknown");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/key")
    public ResponseEntity<Map<String, Object>> setApiKey(@PathVariable String id, @RequestBody Map<String, String> body) {
        var cfgOpt = properties.getModels().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst();

        if (cfgOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown model: " + id));
        }

        String apiKey = body.getOrDefault("apiKey", "");
        keyStore.setKey(id, apiKey);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("modelId", id);
        result.put("hasKey", keyStore.hasKey(id));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Map<String, Object>> enableModel(@PathVariable String id) {
        var cfgOpt = properties.getModels().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst();

        if (cfgOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown model: " + id));
        }

        var cfg = cfgOpt.get();
        if (!"MOCK".equalsIgnoreCase(cfg.getType()) && !hasKey(cfg)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Set an API key before enabling this model."
            ));
        }

        keyStore.setEnabled(id, true);
        return ResponseEntity.ok(Map.of("status", "enabled", "modelId", id));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Map<String, Object>> disableModel(@PathVariable String id) {
        keyStore.setEnabled(id, false);

        // If the disabled model is currently active, fall back to first available
        if (id.equals(registry.getActiveModelId())) {
            properties.getModels().stream()
                .filter(c -> !c.getId().equals(id))
                .filter(c -> "MOCK".equalsIgnoreCase(c.getType()) || c.isEnabled())
                .findFirst()
                .ifPresent(c -> {
                    try { registry.setActive(c.getId()); } catch (Exception ignored) {}
                });
        }

        return ResponseEntity.ok(Map.of("status", "disabled", "modelId", id));
    }

    private boolean hasKey(ModelProperties.ModelConfig cfg) {
        return keyStore.hasKey(cfg.getId())
            || (cfg.getApiKey() != null && !cfg.getApiKey().isBlank());
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActive() {
        ModelProvider active = registry.getActive();
        if (active == null) {
            return ResponseEntity.ok(Map.of("status", "no model configured"));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", active.getId());
        result.put("name", active.getName());
        result.put("type", active.getType());
        properties.getModels().stream()
            .filter(c -> c.getId().equals(active.getId()))
            .findFirst()
            .ifPresent(c -> {
                result.put("description", c.getConfig().getOrDefault("description", ""));
                result.put("icon", c.getConfig().getOrDefault("icon", "smart_toy"));
                result.put("color", c.getConfig().getOrDefault("color", "#58a6ff"));
            });
        return ResponseEntity.ok(result);
    }
}
