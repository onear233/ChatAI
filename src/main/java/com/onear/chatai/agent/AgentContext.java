package com.onear.chatai.agent;

import com.onear.chatai.memory.ChatMemory;
import com.onear.chatai.model.ModelProvider;
import com.onear.chatai.skill.Skill;
import java.util.List;

public class AgentContext {
    private volatile ModelProvider model;
    private final List<Skill> skills;
    private final ChatMemory memory;
    private final String sessionId;

    public AgentContext(ModelProvider model, List<Skill> skills, ChatMemory memory, String sessionId) {
        this.model = model;
        this.skills = skills;
        this.memory = memory;
        this.sessionId = sessionId;
    }

    public ModelProvider getModel() { return model; }
    public void setModel(ModelProvider model) { this.model = model; }
    public List<Skill> getSkills() { return List.copyOf(skills); }
    public ChatMemory getMemory() { return memory; }
    public String getSessionId() { return sessionId; }
}
