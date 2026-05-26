package com.onear.chatai.knowledge;

import java.time.Instant;

public record KnowledgeEntry(
    String id,
    String name,
    String type,
    String content,
    Instant createdAt,
    boolean active
) {
    public KnowledgeEntry(String id, String name, String type, String content, boolean active) {
        this(id, name, type, content, Instant.now(), active);
    }
}
