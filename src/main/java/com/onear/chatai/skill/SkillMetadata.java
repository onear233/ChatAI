package com.onear.chatai.skill;

import java.util.List;
import java.util.Map;

public record SkillMetadata(
    String name,
    String description,
    String version,
    Map<String, Object> parameterSchema,
    List<String> dependencies,
    String author
) {
    public SkillMetadata {
        if (dependencies == null) dependencies = List.of();
    }
}
