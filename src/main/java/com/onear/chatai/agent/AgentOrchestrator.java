package com.onear.chatai.agent;

import com.onear.chatai.knowledge.KnowledgeManager;
import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.memory.LongTermMemory;
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
    private final KnowledgeManager knowledgeManager;
    private final LongTermMemory longTermMemory;

    public AgentOrchestrator(ModelRegistry modelRegistry, SkillRegistry skillRegistry,
                             MemoryManager memoryManager, KnowledgeManager knowledgeManager,
                             LongTermMemory longTermMemory) {
        this.modelRegistry = modelRegistry;
        this.skillRegistry = skillRegistry;
        this.memoryManager = memoryManager;
        this.knowledgeManager = knowledgeManager;
        this.longTermMemory = longTermMemory;
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

        // Process tool calls
        response = processToolCalls(response, sessionId, model, systemPrompt, options);

        // Extract any memory tags from the response
        response = extractMemories(response);

        memoryManager.add(sessionId, new ChatMessage("assistant", response));
        return response;
    }

    public void resetSession(String sessionId) {
        memoryManager.clear(sessionId);
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

            var history = memoryManager.getHistory(sessionId, 20);
            history.add(new ChatMessage("assistant", processed));
            response = model.chat(systemPrompt, history, options);
        }
        return response;
    }

    private String extractMemories(String response) {
        int idx = 0;
        while ((idx = response.indexOf("[MEMORY:", idx)) != -1) {
            int end = response.indexOf("]", idx);
            if (end == -1) break;
            String tag = response.substring(idx + 8, end).trim();

            // Content is only until end of line (newline or end of string)
            int contentStart = end + 1;
            int lineEnd = response.indexOf('\n', contentStart);
            if (lineEnd == -1) lineEnd = response.length();
            String content = response.substring(contentStart, lineEnd).trim();

            if (!content.isEmpty() && !"fact content".equals(content)) {
                longTermMemory.add(tag, content);
                log.info("Memory extracted: {} -> {}", tag, content);
            }
            // Remove only this memory line from the response
            response = response.substring(0, idx) + response.substring(lineEnd);
        }
        return response.trim();
    }

    private String executeToolCall(String toolCall) {
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

        // 1. Role / Knowledge base
        String kbPrompt = knowledgeManager.getSystemPrompt();
        if (!kbPrompt.isEmpty()) {
            sb.append(kbPrompt).append("\n");
        }

        // 2. Long-term memory
        String memPrompt = longTermMemory.getMemoryPrompt();
        if (!memPrompt.isEmpty()) {
            sb.append(memPrompt).append("\n");
        }

        // 3. Tools
        sb.append("=== TOOLS ===\n");
        sb.append("To use a tool, output: [TOOL:toolName {\"param\":\"value\"}]\n");
        sb.append("Available:\n");
        for (Skill skill : skills) {
            sb.append(skill.toToolSchema()).append("\n");
        }

        // 4. Memory extraction instruction
        sb.append("\n=== MEMORY ===\n");
        sb.append("If the user shares important facts or preferences, record them on a SEPARATE LINE:\n");
        sb.append("[MEMORY:<topic>] <the fact>\n");
        sb.append("Then continue your response normally on the next line.\n");
        sb.append("Example: if user says their name is John, output:\n");
        sb.append("[MEMORY:name] John\n");
        sb.append("Only record explicit, important information. Do NOT use placeholder text.\n");

        return sb.toString();
    }
}
