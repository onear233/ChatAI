package com.onear.chatai.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.stereotype.Component;

@Component
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);

    public List<Skill> loadFromJar(Path jarPath) throws Exception {
        List<Skill> result = new ArrayList<>();
        URL[] urls = { jarPath.toUri().toURL() };

        try (SkillClassLoader cl = new SkillClassLoader(urls);
             JarFile jar = new JarFile(jarPath.toFile())) {

            // SPI discovery: META-INF/services/com.onear.chatai.skill.Skill
            JarEntry spiEntry = jar.getJarEntry("META-INF/services/" + Skill.class.getName());
            if (spiEntry != null) {
                try (InputStream is = jar.getInputStream(spiEntry)) {
                    String content = new String(is.readAllBytes()).trim();
                    for (String line : content.split("\n")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        Class<?> cls = cl.loadSkillClass(line);
                        Object instance = cls.getDeclaredConstructor().newInstance();
                        if (instance instanceof Skill skill) {
                            result.add(skill);
                            log.info("Loaded skill '{}' from {}", skill.getName(), jarPath.getFileName());
                        }
                    }
                }
            }

            // Also scan for Skill implementations via class inspection
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.contains("$") && !name.startsWith("META-INF")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> cls = cl.loadSkillClass(className);
                        if (Skill.class.isAssignableFrom(cls) && !cls.isInterface()) {
                            Object instance = cls.getDeclaredConstructor().newInstance();
                            Skill skill = (Skill) instance;
                            if (result.stream().noneMatch(s -> s.getName().equals(skill.getName()))) {
                                result.add(skill);
                                log.info("Loaded skill '{}' from {}", skill.getName(), jarPath.getFileName());
                            }
                        }
                    } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
                        // skip non-skill classes
                    }
                }
            }
        }
        return result;
    }

    public List<Skill> loadFromDirectory(Path directory) throws Exception {
        List<Skill> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) return result;

        try (var stream = Files.list(directory)) {
            List<Path> jars = stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            for (Path jar : jars) {
                try {
                    result.addAll(loadFromJar(jar));
                } catch (Exception e) {
                    log.warn("Failed to load skills from {}: {}", jar.getFileName(), e.getMessage());
                }
            }
        }
        return result;
    }
}
