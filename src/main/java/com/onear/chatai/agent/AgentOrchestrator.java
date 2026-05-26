package com.onear.chatai.agent;

import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.memory.MemoryManager;
import com.onear.chatai.model.ModelProvider;
import com.onear.chatai.model.ModelRegistry;
import com.onear.chatai.skill.Skill;
import com.onear.chatai.skill.SkillRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);
    private final ModelRegistry modelRegistry;
    private final SkillRegistry skillRegistry;
    private final MemoryManager memoryManager;

    public AgentOrchestrator(ModelRegistry modelRegistry, SkillRegistry skillRegistry, MemoryManager memoryManager) {
        this.modelRegistry = modelRegistry;
        this.skillRegistry = skillRegistry;
        this.memoryManager = memoryManager;
    }

    public String chat(String sessionId, String userMessage) {
        var model = modelRegistry.getActive();
        if (model == null) {
            return "No active model configured. Please configure a model in application.yml.";
        }

        var skills = skillRegistry.getAllEnabled();
        var history = new java.util.ArrayList<>(memoryManager.getHistory(sessionId, 20));
        var currentMsg = new ChatMessage("user", userMessage);
        history.add(currentMsg);
        memoryManager.add(sessionId, currentMsg);

        String systemPrompt = buildSystemPrompt(skills);
        Map<String, Object> options = Map.of("temperature", 0.7, "max_tokens", 2048);

        String response = model.chat(systemPrompt, history, options);

        // Check for tool calls in response
        response = processToolCalls(response, sessionId, model, systemPrompt, options);

        memoryManager.add(sessionId, new ChatMessage("assistant", response));
        return response;
    }

    private String processToolCalls(String response, String sessionId, ModelProvider model,
                                    String systemPrompt, Map<String, Object> options) {
        int maxIterations = 3;
        for (int i = 0; i < maxIterations; i++) {
            if (!response.contains("[TOOL:")) break;

            String processed = response;
            int idx = 0;
            while ((idx = processed.indexOf("[TOOL:", idx)) != -1) {
                int end = processed.indexOf("]", idx);
                if (end == -1) break;
                String toolCall = processed.substring(idx + 6, end);
                String toolResult = executeToolCall(toolCall);
                processed = processed.substring(0, idx) + toolResult + processed.substring(end + 1);
                idx += toolResult.length();
            }

            if (!processed.contains("[TOOL:")) {
                response = processed;
                break;
            }

            // If we still have tool calls and more iterations, let the model continue
            var history = memoryManager.getHistory(sessionId, 20);
            history.add(new ChatMessage("assistant", processed));
            response = model.chat(systemPrompt, history, options);
        }
        return response;
    }

    private String executeToolCall(String toolCall) {
        // Parse: skillName {"key": "value"}
        String skillName;
        String argsJson = "{}";
        int braceIdx = toolCall.indexOf('{');
        if (braceIdx != -1) {
            skillName = toolCall.substring(0, braceIdx).trim();
            argsJson = toolCall.substring(braceIdx);
        } else {
            skillName = toolCall.trim();
        }

        var skillOpt = skillRegistry.get(skillName);
        if (skillOpt.isEmpty()) {
            return "[Unknown skill: " + skillName + "]";
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = new tools.jackson.databind.ObjectMapper()
                    .readValue(argsJson, Map.class);
            var result = skillOpt.get().execute(args);
            return result.success() ? result.content() : "[Error: " + result.content() + "]";
        } catch (Exception e) {
            return "[Error calling " + skillName + ": " + e.getMessage() + "]";
        }
    }

    private String buildSystemPrompt(List<Skill> skills) {
        var sb = new StringBuilder();
        sb.append("You are an AI assistant with access to tools.\n\n");
        sb.append("To use a tool, output exactly: [TOOL:toolName {\"param\":\"value\"}]\n");
        sb.append("Use the exact parameter names described below.\n\n");
        sb.append("Available tools:\n");
        for (Skill skill : skills) {
            sb.append(skill.toToolSchema()).append("\n");
        }
        return sb.toString();
    }
}
