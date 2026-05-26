package com.onear.chatai.agent;

import com.onear.chatai.memory.ChatMessage;
import com.onear.chatai.memory.MemoryManager;
import com.onear.chatai.model.ModelProvider;
import com.onear.chatai.skill.Skill;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AgentSession {

    private final MemoryManager memoryManager;
    private final List<Skill> availableSkills;

    public AgentSession(MemoryManager memoryManager, List<Skill> availableSkills) {
        this.memoryManager = memoryManager;
        this.availableSkills = availableSkills;
    }

    public AgentContext createContext(String sessionId, ModelProvider model, List<Skill> activeSkills) {
        return new AgentContext(model,
            activeSkills != null ? activeSkills : List.copyOf(availableSkills),
            null,
            sessionId);
    }
}
