package com.onear.chatai.skill;

import java.util.Map;

public record SkillResult(
    boolean success,
    String content,
    Map<String, Object> data
) {
    public static SkillResult ok(String content) {
        return new SkillResult(true, content, null);
    }

    public static SkillResult ok(String content, Map<String, Object> data) {
        return new SkillResult(true, content, data);
    }

    public static SkillResult error(String content) {
        return new SkillResult(false, content, null);
    }
}
