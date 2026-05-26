package com.onear.chatai.mcp;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class McpClient {

    private final String name;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private String serverVersion;

    public McpClient(String name, String endpoint) {
        this.name = name;
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getName() { return name; }

    public boolean connect() {
        try {
            String body = mapper.writeValueAsString(Map.of(
                "jsonrpc", "2.0",
                "id", "init-1",
                "method", "initialize",
                "params", Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of(),
                    "clientInfo", Map.of("name", "ChatAI", "version", "1.0.0")
                )
            ));
            String response = sendRequest(body);
            var node = mapper.readTree(response);
            serverVersion = node.path("result").path("serverInfo").path("version").asText();
            return !node.has("error");
        } catch (Exception e) {
            return false;
        }
    }

    public List<McpToolDefinition> listTools() {
        try {
            String body = mapper.writeValueAsString(Map.of(
                "jsonrpc", "2.0", "id", "tools-1", "method", "tools/list"
            ));
            String response = sendRequest(body);
            var node = mapper.readTree(response);
            var tools = new ArrayList<McpToolDefinition>();
            for (JsonNode tool : node.path("result").path("tools")) {
                tools.add(new McpToolDefinition(
                    tool.path("name").asText(),
                    tool.path("description").asText(),
                    mapper.convertValue(tool.path("inputSchema"), Map.class)
                ));
            }
            return tools;
        } catch (Exception e) {
            return List.of();
        }
    }

    public McpServer.McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                "jsonrpc", "2.0",
                "id", "call-" + UUID.randomUUID().toString().substring(0, 8),
                "method", "tools/call",
                "params", Map.of("name", toolName, "arguments", arguments)
            ));
            String response = sendRequest(body);
            var node = mapper.readTree(response);
            if (node.has("error")) {
                return new McpServer.McpToolResult(node.path("error").path("message").asText(), true);
            }
            var content = node.path("result").path("content").get(0).path("text").asText();
            return new McpServer.McpToolResult(content, node.path("result").path("isError").asBoolean());
        } catch (Exception e) {
            return new McpServer.McpToolResult(e.getMessage(), true);
        }
    }

    private String sendRequest(String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
