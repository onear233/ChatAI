package com.onear.chatai.web.controller;

import com.onear.chatai.mcp.McpClient;
import com.onear.chatai.mcp.McpProtocolHandler;
import com.onear.chatai.mcp.McpServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final List<McpServer> servers;
    private final McpProtocolHandler protocolHandler;
    private final Map<String, McpClient> externalClients = new LinkedHashMap<>();

    public McpController(List<McpServer> servers, McpProtocolHandler protocolHandler) {
        this.servers = servers;
        this.protocolHandler = protocolHandler;
    }

    @GetMapping("/servers")
    public ResponseEntity<List<Map<String, Object>>> listServers() {
        var list = new ArrayList<Map<String, Object>>();
        for (McpServer server : servers) {
            list.add(Map.of(
                "name", server.getName(),
                "version", server.getVersion(),
                "type", "builtin",
                "toolCount", server.getTools().size()
            ));
        }
        for (var entry : externalClients.entrySet()) {
            list.add(Map.of(
                "name", entry.getKey(),
                "type", "external",
                "toolCount", entry.getValue().listTools().size()
            ));
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, Object>>> listTools() {
        var tools = new ArrayList<Map<String, Object>>();
        for (McpServer server : servers) {
            for (var tool : server.getTools()) {
                var m = new LinkedHashMap<String, Object>();
                m.put("server", server.getName());
                m.put("name", tool.name());
                m.put("description", tool.description());
                m.put("inputSchema", tool.inputSchema());
                tools.add(m);
            }
        }
        for (var entry : externalClients.entrySet()) {
            for (var tool : entry.getValue().listTools()) {
                var m = new LinkedHashMap<String, Object>();
                m.put("server", entry.getKey());
                m.put("name", tool.name());
                m.put("description", tool.description());
                m.put("inputSchema", tool.inputSchema());
                tools.add(m);
            }
        }
        return ResponseEntity.ok(tools);
    }

    @PostMapping("/servers")
    public ResponseEntity<Map<String, Object>> connectServer(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String endpoint = body.get("endpoint");
        if (name == null || endpoint == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name and endpoint required"));
        }
        var client = new McpClient(name, endpoint);
        if (client.connect()) {
            externalClients.put(name, client);
            return ResponseEntity.ok(Map.of("status", "connected", "name", name, "tools", client.listTools().size()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Failed to connect to MCP server"));
    }

    @DeleteMapping("/servers/{name}")
    public ResponseEntity<Map<String, String>> disconnectServer(@PathVariable String name) {
        externalClients.remove(name);
        return ResponseEntity.ok(Map.of("status", "disconnected", "name", name));
    }

    @PostMapping("/tools/{toolName}/call")
    public ResponseEntity<Map<String, Object>> callTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments) {
        for (McpServer server : servers) {
            for (var tool : server.getTools()) {
                if (tool.name().equals(toolName)) {
                    var result = server.callTool(toolName, arguments);
                    return ResponseEntity.ok(Map.of(
                        "tool", toolName,
                        "result", result.content(),
                        "isError", result.isError()
                    ));
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/jsonrpc")
    public ResponseEntity<Map<String, Object>> jsonRpc(@RequestBody Map<String, Object> body) {
        try {
            String raw = new tools.jackson.databind.ObjectMapper().writeValueAsString(body);
            String result = protocolHandler.handleRequest(raw);
            if (result.isEmpty()) return ResponseEntity.ok(Map.of());
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = new tools.jackson.databind.ObjectMapper()
                    .readValue(result, Map.class);
            return ResponseEntity.ok(resultMap);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
