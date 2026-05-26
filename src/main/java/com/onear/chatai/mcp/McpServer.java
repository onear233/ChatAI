package com.onear.chatai.mcp;

import java.util.List;
import java.util.Map;

public interface McpServer {

    String getName();

    String getVersion();

    List<McpToolDefinition> getTools();

    McpToolResult callTool(String toolName, Map<String, Object> arguments);

    record McpToolResult(String content, boolean isError) {}
}
