package com.onear.chatai.web.controller;

import com.onear.chatai.skill.Skill;
import com.onear.chatai.skill.SkillManager;
import com.onear.chatai.skill.SkillRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRegistry registry;
    private final SkillManager manager;

    public SkillController(SkillRegistry registry, SkillManager manager) {
        this.registry = registry;
        this.manager = manager;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSkills() {
        var list = registry.listAll().stream().map(entry -> {
            Skill s = entry.skill;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", s.getName());
            m.put("description", s.getMetadata().description());
            m.put("version", s.getMetadata().version());
            m.put("enabled", entry.enabled);
            m.put("builtin", entry.builtin);
            m.put("schema", s.getMetadata().parameterSchema());
            return m;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadSkill(@RequestParam("file") MultipartFile file) {
        try {
            Path skillsDir = Paths.get("skills");
            Files.createDirectories(skillsDir);
            Path target = skillsDir.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            var skills = manager.loadSkillFromJar(target);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "loaded");
            result.put("skillName", skills.getName());
            result.put("version", skills.getMetadata().version());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{name}/enable")
    public ResponseEntity<Map<String, String>> enableSkill(@PathVariable String name) {
        registry.enable(name);
        return ResponseEntity.ok(Map.of("status", "enabled", "skill", name));
    }

    @PutMapping("/{name}/disable")
    public ResponseEntity<Map<String, String>> disableSkill(@PathVariable String name) {
        registry.disable(name);
        return ResponseEntity.ok(Map.of("status", "disabled", "skill", name));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> unregisterSkill(@PathVariable String name) {
        registry.unregister(name);
        return ResponseEntity.ok(Map.of("status", "unregistered", "skill", name));
    }
}
