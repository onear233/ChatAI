package com.onear.chatai.model;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiKeyStore {

    private final Map<String, String> keys = new ConcurrentHashMap<>();
    private final Set<String> runtimeEnabled = ConcurrentHashMap.newKeySet();

    public void setKey(String modelId, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            keys.put(modelId, apiKey);
        } else {
            keys.remove(modelId);
        }
    }

    public String resolveKey(String modelId, String fallbackKey) {
        String stored = keys.get(modelId);
        return (stored != null && !stored.isBlank()) ? stored : fallbackKey;
    }

    public boolean hasKey(String modelId) {
        String stored = keys.get(modelId);
        return stored != null && !stored.isBlank();
    }

    public void removeKey(String modelId) {
        keys.remove(modelId);
    }

    public void setEnabled(String modelId, boolean enabled) {
        if (enabled) {
            runtimeEnabled.add(modelId);
        } else {
            runtimeEnabled.remove(modelId);
        }
    }

    public boolean isRuntimeEnabled(String modelId) {
        return runtimeEnabled.contains(modelId);
    }
}
