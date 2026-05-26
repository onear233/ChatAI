package com.onear.chatai.model;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelRegistry {

    private final Map<String, ModelProvider> providers = new ConcurrentHashMap<>();
    private final List<ModelProvider> providerList;
    private volatile String activeModelId;

    public ModelRegistry(List<ModelProvider> providerList) {
        this.providerList = providerList;
    }

    @PostConstruct
    public void init() {
        for (ModelProvider p : providerList) {
            providers.put(p.getId(), p);
        }
        if (!providers.isEmpty()) {
            activeModelId = providerList.getFirst().getId();
        }
    }

    public void register(ModelProvider provider) {
        providers.put(provider.getId(), provider);
        if (activeModelId == null) {
            activeModelId = provider.getId();
        }
    }

    public void unregister(String id) {
        providers.remove(id);
        if (id.equals(activeModelId) && !providers.isEmpty()) {
            activeModelId = providers.keySet().iterator().next();
        }
    }

    public ModelProvider getActive() {
        ModelProvider p = providers.get(activeModelId);
        if (p == null && !providers.isEmpty()) {
            activeModelId = providers.keySet().iterator().next();
            p = providers.get(activeModelId);
        }
        return p;
    }

    public Optional<ModelProvider> get(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public void setActive(String id) {
        if (!providers.containsKey(id)) {
            throw new IllegalArgumentException("Unknown model: " + id);
        }
        activeModelId = id;
    }

    public String getActiveModelId() {
        return activeModelId;
    }

    public Collection<ModelProvider> listAll() {
        return List.copyOf(providers.values());
    }
}
