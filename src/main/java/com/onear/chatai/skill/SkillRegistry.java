package com.onear.chatai.skill;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SkillRegistry {

    private final Map<String, SkillEntry> skills = new ConcurrentHashMap<>();
    private final List<Skill> builtinSkills;

    public SkillRegistry(List<Skill> builtinSkills) {
        this.builtinSkills = builtinSkills;
    }

    @PostConstruct
    public void init() {
        for (Skill skill : builtinSkills) {
            register(skill, true);
        }
    }

    public void register(Skill skill, boolean builtin) {
        skills.put(skill.getName(), new SkillEntry(skill, builtin, null));
    }

    public void register(Skill skill, boolean builtin, String sourceJar) {
        skills.put(skill.getName(), new SkillEntry(skill, builtin, sourceJar));
    }

    public String unregister(String name) {
        var entry = skills.remove(name);
        if (entry != null && !entry.builtin) {
            return entry.sourceJar;
        }
        return null;
    }

    public void enable(String name) {
        var entry = skills.get(name);
        if (entry != null) entry.enabled = true;
    }

    public void disable(String name) {
        var entry = skills.get(name);
        if (entry != null) entry.enabled = false;
    }

    public Optional<Skill> get(String name) {
        var entry = skills.get(name);
        return entry != null && entry.enabled ? Optional.of(entry.skill) : Optional.empty();
    }

    public List<Skill> getAllEnabled() {
        return skills.values().stream()
                .filter(e -> e.enabled)
                .map(e -> e.skill)
                .collect(Collectors.toList());
    }

    public List<SkillEntry> listAll() {
        return List.copyOf(skills.values());
    }

    public List<Skill> getBuiltinSkills() {
        return List.copyOf(builtinSkills);
    }

    public static class SkillEntry {
        public final Skill skill;
        public volatile boolean enabled;
        public final boolean builtin;
        public final String sourceJar;

        SkillEntry(Skill skill, boolean builtin, String sourceJar) {
            this.skill = skill;
            this.enabled = true;
            this.builtin = builtin;
            this.sourceJar = sourceJar;
        }
    }
}
