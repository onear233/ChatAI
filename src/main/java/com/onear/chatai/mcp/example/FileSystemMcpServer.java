package com.onear.chatai.mcp.example;

import com.onear.chatai.mcp.McpServer;
import com.onear.chatai.mcp.McpToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileSystemMcpServer implements McpServer {

    private static final Logger log = LoggerFactory.getLogger(FileSystemMcpServer.class);
    private final Path rootDir;

    public FileSystemMcpServer() {
        this.rootDir = Paths.get("data", "mcp-fs").toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            log.warn("Could not create MCP FS root: {}", e.getMessage());
        }
    }

    @Override
    public String getName() { return "filesystem"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public List<McpToolDefinition> getTools() {
        return List.of(
            new McpToolDefinition("read_file", "Read contents of a file",
                Map.of("type", "object",
                    "properties", Map.of("path", Map.of("type", "string", "description", "File path relative to sandbox root")),
                    "required", List.of("path"))),
            new McpToolDefinition("list_directory", "List files in a directory",
                Map.of("type", "object",
                    "properties", Map.of("path", Map.of("type", "string", "description", "Directory path relative to sandbox root")),
                    "required", List.of("path"))),
            new McpToolDefinition("write_file", "Write content to a file",
                Map.of("type", "object",
                    "properties", Map.of(
                        "path", Map.of("type", "string", "description", "File path relative to sandbox root"),
                        "content", Map.of("type", "string", "description", "Content to write")
                    ),
                    "required", List.of("path", "content")))
        );
    }

    @Override
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "read_file" -> readFile((String) arguments.get("path"));
            case "list_directory" -> listDirectory((String) arguments.get("path"));
            case "write_file" -> writeFile((String) arguments.get("path"), (String) arguments.get("content"));
            default -> new McpToolResult("Unknown tool: " + toolName, true);
        };
    }

    private McpToolResult readFile(String path) {
        try {
            Path resolved = resolveSafe(path);
            if (resolved == null) return new McpToolResult("Access denied: path escapes sandbox", true);
            String content = Files.readString(resolved);
            return new McpToolResult(content, false);
        } catch (IOException e) {
            return new McpToolResult("Error reading file: " + e.getMessage(), true);
        }
    }

    private McpToolResult listDirectory(String path) {
        try {
            Path resolved = resolveSafe(path.isEmpty() ? "." : path);
            if (resolved == null) return new McpToolResult("Access denied: path escapes sandbox", true);
            if (!Files.isDirectory(resolved)) return new McpToolResult("Not a directory: " + path, true);
            try (var stream = Files.list(resolved)) {
                var listing = stream.map(p -> (Files.isDirectory(p) ? "[DIR]  " : "[FILE] ") + p.getFileName())
                        .collect(Collectors.joining("\n"));
                return new McpToolResult(listing.isEmpty() ? "(empty)" : listing, false);
            }
        } catch (IOException e) {
            return new McpToolResult("Error listing directory: " + e.getMessage(), true);
        }
    }

    private McpToolResult writeFile(String path, String content) {
        try {
            Path resolved = resolveSafe(path);
            if (resolved == null) return new McpToolResult("Access denied: path escapes sandbox", true);
            Files.createDirectories(resolved.getParent());
            Files.writeString(resolved, content != null ? content : "");
            return new McpToolResult("Written to " + path, false);
        } catch (IOException e) {
            return new McpToolResult("Error writing file: " + e.getMessage(), true);
        }
    }

    private Path resolveSafe(String path) {
        Path resolved = rootDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(rootDir)) return null;
        return resolved;
    }
}
