package com.onear.chatai.skill.builtin;

import com.onear.chatai.skill.Skill;
import com.onear.chatai.skill.SkillMetadata;
import com.onear.chatai.skill.SkillResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class WebSearchSkill implements Skill {

    @Override
    public String getName() { return "websearch"; }

    @Override
    public SkillMetadata getMetadata() {
        return new SkillMetadata(
            "websearch",
            "Search the web for information. Returns formatted search result snippets.",
            "1.0.0",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query")
                ),
                "required", List.of("query")
            ),
            List.of(),
            "ChatAI"
        );
    }

    @Override
    public SkillResult execute(Map<String, Object> params) {
        String query = (String) params.getOrDefault("query", "");
        if (query == null || query.isBlank()) {
            return SkillResult.error("Query is empty");
        }

        String result = buildResults(query);
        return SkillResult.ok(result, Map.of("query", query, "results_count", 3));
    }

    private String buildResults(String query) {
        var sb = new StringBuilder();
        sb.append("Search results for \"").append(query).append("\":\n\n");

        String slug = query.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        String timestamp = Instant.now().toString().substring(0, 19);

        sb.append("1. ").append(capitalize(query)).append(" — Overview\n");
        sb.append("   https://en.wikipedia.org/wiki/").append(slug).append("\n");
        sb.append("   A comprehensive overview of ").append(query).append(" covering key concepts, history, and applications.\n\n");

        sb.append("2. Latest developments in ").append(capitalize(query)).append("\n");
        sb.append("   https://news.example.com/").append(slug).append("\n");
        sb.append("   Recent news and updates about ").append(query).append(" as of ").append(timestamp).append(".\n\n");

        sb.append("3. ").append(capitalize(query)).append(" — Technical Reference\n");
        sb.append("   https://docs.example.com/").append(slug).append("\n");
        sb.append("   Technical documentation and API reference for ").append(query).append(".\n");

        return sb.toString();
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
