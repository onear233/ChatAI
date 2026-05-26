package com.onear.chatai.skill;

import java.util.List;
import java.util.Map;

public interface Skill {

    String getName();

    SkillMetadata getMetadata();

    SkillResult execute(Map<String, Object> params);

    default boolean isEnabled() { return true; }

    default String toToolSchema() {
        var meta = getMetadata();
        var sb = new StringBuilder();
        sb.append("  - name: ").append(meta.name()).append("\n");
        sb.append("    description: ").append(meta.description()).append("\n");
        sb.append("    parameters: ").append(toJsonSchema(meta.parameterSchema())).append("\n");
        return sb.toString();
    }

    default String toJsonSchema(Map<String, Object> schema) {
        var sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        sb.append("\"type\": \"object\", \"properties\": {");
        @SuppressWarnings("unchecked")
        var props = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
        boolean propFirst = true;
        for (var entry : props.entrySet()) {
            if (!propFirst) sb.append(", ");
            propFirst = false;
            sb.append("\"").append(entry.getKey()).append("\": ");
            @SuppressWarnings("unchecked")
            var propDef = (Map<String, Object>) entry.getValue();
            sb.append(toJsonSchema(propDef));
        }
        sb.append("}");
        @SuppressWarnings("unchecked")
        var required = (List<String>) schema.getOrDefault("required", List.of());
        if (!required.isEmpty()) {
            sb.append(", \"required\": [");
            for (int i = 0; i < required.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(required.get(i)).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }
}
