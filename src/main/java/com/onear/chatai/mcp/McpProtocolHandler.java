package com.onear.chatai.mcp;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class McpProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(McpProtocolHandler.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<McpServer> servers;

    public McpProtocolHandler(List<McpServer> servers) {
        this.servers = servers;
    }

    public String handleRequest(String jsonRpcMessage) {
        try {
            var node = mapper.readTree(jsonRpcMessage);
            String method = node.path("method").asText();
            String id = node.path("id").asText();
            var params = node.path("params");

            return switch (method) {
                case "initialize" -> handleInitialize(id);
                case "tools/list" -> handleToolsList(id);
                case "tools/call" -> handleToolsCall(id, params);
                case "notifications/initialized" -> ""; // no response for notifications
                default -> errorResponse(id, -32601, "Method not found: " + method);
            };
        } catch (JacksonException e) {
            return errorResponse(null, -32700, "Parse error: " + e.getMessage());
        }
    }

    private String handleInitialize(String id) {
        try {
            Map<String, Object> serverInfo = new LinkedHashMap<>();
            serverInfo.put("name", "ChatAI MCP Server");
            serverInfo.put("version", "1.0.0");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("protocolVersion", PROTOCOL_VERSION);
            result.put("serverInfo", serverInfo);
            result.put("capabilities", Map.of("tools", Map.of()));
            return successResponse(id, result);
        } catch (Exception e) {
            return errorResponse(id, -32603, e.getMessage());
        }
    }

    private String handleToolsList(String id) {
        try {
            var tools = new ArrayList<Map<String, Object>>();
            for (McpServer server : servers) {
                for (McpToolDefinition tool : server.getTools()) {
                    tools.add(Map.of(
                        "name", tool.name(),
                        "description", tool.description(),
                        "inputSchema", tool.inputSchema()
                    ));
                }
            }
            return successResponse(id, Map.of("tools", tools));
        } catch (Exception e) {
            return errorResponse(id, -32603, e.getMessage());
        }
    }

    private String handleToolsCall(String id, Object params) {
        try {
            String toolName = mapper.convertValue(params, Map.class).get("name").toString();
            @SuppressWarnings("unchecked")
            Map<String, Object> args = (Map<String, Object>) mapper.convertValue(params, Map.class)
                    .getOrDefault("arguments", Map.of());

            for (McpServer server : servers) {
                for (McpToolDefinition tool : server.getTools()) {
                    if (tool.name().equals(toolName)) {
                        var result = server.callTool(toolName, args);
                        return successResponse(id, Map.of(
                            "content", List.of(Map.of(
                                "type", "text",
                                "text", result.content()
                            )),
                            "isError", result.isError()
                        ));
                    }
                }
            }
            return errorResponse(id, -32602, "Tool not found: " + toolName);
        } catch (Exception e) {
            return errorResponse(id, -32603, e.getMessage());
        }
    }

    private String successResponse(String id, Object result) throws JacksonException {
        return mapper.writeValueAsString(Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", result
        ));
    }

    private String errorResponse(String id, int code, String message) {
        try {
            return mapper.writeValueAsString(Map.of(
                "jsonrpc", "2.0",
                "id", id != null ? id : "null",
                "error", Map.of("code", code, "message", message)
            ));
        } catch (JacksonException e) {
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }
}
