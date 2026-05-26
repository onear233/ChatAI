package com.onear.chatai.mcp;

import java.util.Map;

public record McpToolDefinition(
    String name,
    String description,
    Map<String, Object> inputSchema
) {}
