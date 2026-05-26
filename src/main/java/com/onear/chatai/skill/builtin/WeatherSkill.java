package com.onear.chatai.skill.builtin;

import com.onear.chatai.skill.Skill;
import com.onear.chatai.skill.SkillMetadata;
import com.onear.chatai.skill.SkillResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class WeatherSkill implements Skill {

    private final Random random = new Random();

    private static final String[] CONDITIONS = {"Sunny", "Cloudy", "Rainy", "Snowy", "Windy", "Overcast"};
    private static final String[] CITIES = {"Beijing", "Shanghai", "Tokyo", "New York", "London", "Paris", "Sydney", "Singapore"};

    @Override
    public String getName() { return "weather"; }

    @Override
    public SkillMetadata getMetadata() {
        return new SkillMetadata(
            "weather",
            "Get current weather for a city",
            "1.0.0",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "city", Map.of("type", "string", "description", "City name")
                ),
                "required", List.of("city")
            ),
            List.of(),
            "ChatAI"
        );
    }

    @Override
    public SkillResult execute(Map<String, Object> params) {
        String city = (String) params.getOrDefault("city", "Unknown");
        boolean known = false;
        for (String c : CITIES) {
            if (c.equalsIgnoreCase(city)) { known = true; break; }
        }

        int temp = known ? random.nextInt(35) - 5 : random.nextInt(40) - 10;
        String condition = CONDITIONS[random.nextInt(CONDITIONS.length)];
        int humidity = random.nextInt(60) + 30;

        String result = String.format(
            "Weather in %s: %s, Temperature: %d°C, Humidity: %d%%",
            city, condition, temp, humidity
        );
        return SkillResult.ok(result, Map.of(
            "city", city, "temperature", temp, "condition", condition, "humidity", humidity
        ));
    }
}
