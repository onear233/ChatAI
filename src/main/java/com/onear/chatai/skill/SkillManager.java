package com.onear.chatai.skill;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class SkillManager {

    private static final Logger log = LoggerFactory.getLogger(SkillManager.class);
    private final SkillRegistry registry;
    private final SkillLoader loader;
    private final Path skillsDir;

    public SkillManager(SkillRegistry registry, SkillLoader loader) {
        this.registry = registry;
        this.loader = loader;
        this.skillsDir = Paths.get("skills");
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(skillsDir);
            loader.loadFromDirectory(skillsDir).forEach(s -> registry.register(s, false));
        } catch (Exception e) {
            log.warn("Skill directory scan failed: {}", e.getMessage());
        }
        startWatcher();
    }

    public Skill loadSkillFromJar(Path jarPath) throws Exception {
        var skills = loader.loadFromJar(jarPath);
        if (skills.isEmpty()) {
            throw new IllegalArgumentException("No Skill implementation found in: " + jarPath.getFileName());
        }
        for (Skill s : skills) {
            registry.register(s, false);
        }
        return skills.getFirst();
    }

    private void startWatcher() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            skillsDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            var executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "skill-watcher");
                t.setDaemon(true);
                return t;
            });

            executor.scheduleWithFixedDelay(() -> {
                WatchKey key = watcher.poll();
                if (key == null) return;
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path file = skillsDir.resolve((Path) event.context());
                    if (file.toString().endsWith(".jar")) {
                        try {
                            loader.loadFromJar(file).forEach(s -> registry.register(s, false));
                            log.info("Hot-loaded skill from {}", file.getFileName());
                        } catch (Exception e) {
                            log.warn("Hot-load failed for {}: {}", file.getFileName(), e.getMessage());
                        }
                    }
                }
                key.reset();
            }, 2, 5, TimeUnit.SECONDS);

        } catch (IOException e) {
            log.warn("Skill directory watcher could not start: {}", e.getMessage());
        }
    }
}
