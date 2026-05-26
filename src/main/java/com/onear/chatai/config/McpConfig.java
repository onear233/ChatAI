package com.onear.chatai.config;

import com.onear.chatai.mcp.McpProtocolHandler;
import com.onear.chatai.mcp.McpServer;
import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.model.ApiKeyStore;
import com.onear.chatai.model.ModelInfo;
import com.onear.chatai.model.ModelProperties;
import com.onear.chatai.model.ModelProvider;
import com.onear.chatai.model.impl.DeepSeekProvider;
import com.onear.chatai.model.impl.LocalModelProvider;
import com.onear.chatai.model.impl.OpenAiProvider;
import com.onear.chatai.model.impl.QwenProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    @Bean
    public McpProtocolHandler mcpProtocolHandler(List<McpServer> servers) {
        return new McpProtocolHandler(servers);
    }

    @Bean
    public List<ModelProvider> modelProviders(ModelProperties properties, ApiKeyStore keyStore) {
        List<ModelProvider> providers = new ArrayList<>();
        for (var cfg : properties.getModels()) {
            ModelProvider provider = createProvider(cfg, keyStore);
            if (provider != null) {
                providers.add(provider);
            }
        }
        // Fallback mock if none configured
        if (providers.isEmpty()) {
            providers.add(createMockProvider("mock", "Mock Model", "balanced"));
        }
        return providers;
    }

    private ModelProvider createProvider(ModelProperties.ModelConfig cfg, ApiKeyStore keyStore) {
        var info = new ModelInfo(
            cfg.getId(), cfg.getName(), cfg.getType(),
            cfg.getApiEndpoint(), cfg.getApiKey(), cfg.getModelName(),
            cfg.isEnabled(), cfg.getConfig()
        );
        return switch (cfg.getType().toUpperCase()) {
            case "OPENAI" -> new OpenAiProvider(info, keyStore);
            case "QWEN" -> new QwenProvider(info, keyStore);
            case "DEEPSEEK" -> new DeepSeekProvider(info, keyStore);
            case "LOCAL" -> new LocalModelProvider(info, keyStore);
            case "MOCK" -> {
                String style = cfg.getConfig() != null
                    ? (String) cfg.getConfig().getOrDefault("style", "balanced")
                    : "balanced";
                yield createMockProvider(cfg.getId(), cfg.getName(), style);
            }
            default -> null;
        };
    }

    private ModelProvider createMockProvider(String id, String name, String style) {
        return new ModelProvider() {
            @Override public String getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getType() { return style.toUpperCase(); }

            @Override
            public String chat(String systemPrompt, List<ChatMessage> history, Map<String, Object> options) {
                if (history.isEmpty()) return pickGreeting();
                String lastMsg = history.getLast().content().toLowerCase();

                // Tool detection (same across all mock models)
                if (lastMsg.contains("weather") || lastMsg.contains("天气")) {
                    String city = extractCity(lastMsg);
                    return "[TOOL:weather {\"city\":\"" + city + "\"}]";
                }
                if (lastMsg.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*") || lastMsg.contains("calculate") || lastMsg.contains("计算")) {
                    String expr = extractExpression(lastMsg);
                    return "[TOOL:calculator {\"expression\":\"" + expr + "\"}]";
                }
                if (lastMsg.contains("search") || lastMsg.contains("搜索") || lastMsg.contains("find")) {
                    String query = lastMsg.replaceAll("(?i)search|搜索|find|for|about", "")
                        .replaceAll("[^a-zA-Z\\s]", "").trim();
                    if (query.isEmpty()) query = "latest news";
                    return "[TOOL:websearch {\"query\":\"" + query + "\"}]";
                }

                return pickResponse(history.getLast().content());
            }

            private String pickGreeting() {
                return switch (style) {
                    case "creative" -> "Hi there! I'm " + name + " — your creative companion. "
                        + "Let's brainstorm, write stories, or explore wild ideas together!";
                    case "precise" -> name + " ready. "
                        + "Optimized for technical analysis, code review, and precise answers. Send your query.";
                    case "fast" -> "Quick-response mode active. "
                        + "I'm optimized for speed — ask me anything and I'll keep it concise!";
                    default -> "Hello! I'm " + name + ", your AI assistant. "
                        + "I can help with weather, calculations, web searches, and more. What can I do for you?";
                };
            }

            private String pickResponse(String lastMsg) {
                String prefix = switch (style) {
                    case "creative" -> "[Creative mode] ";
                    case "precise" -> "[Analysis] ";
                    case "fast" -> "";
                    default -> "";
                };
                return prefix + "I'm " + name + ". I can help with:\n"
                    + "- Weather queries (e.g., \"weather in Beijing\")\n"
                    + "- Calculations (e.g., \"calculate 3 + 5 * 2\")\n"
                    + "- Web searches (e.g., \"search for AI news\")\n\n"
                    + "You said: " + lastMsg;
            }

            private String extractCity(String msg) {
                for (String c : new String[]{"Beijing","Shanghai","Tokyo","New York","London","Paris","Sydney"})
                    if (msg.toLowerCase().contains(c.toLowerCase())) return c;
                return "Beijing";
            }

            private String extractExpression(String msg) {
                return msg.replaceAll("(?i)calculate|compute|what is|eval|计算|算|等", "")
                        .replaceAll("[^0-9+\\-*/().^\\s]", "").trim();
            }
        };
    }
}
