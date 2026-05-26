package com.onear.chatai.model;

import java.util.Map;

public record ModelInfo(
    String id,
    String name,
    String type,
    String apiEndpoint,
    String apiKey,
    String modelName,
    boolean enabled,
    Map<String, Object> config
) {}
