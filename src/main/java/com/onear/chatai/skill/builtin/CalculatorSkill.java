package com.onear.chatai.skill.builtin;

import com.onear.chatai.skill.Skill;
import com.onear.chatai.skill.SkillMetadata;
import com.onear.chatai.skill.SkillResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CalculatorSkill implements Skill {

    @Override
    public String getName() { return "calculator"; }

    @Override
    public SkillMetadata getMetadata() {
        return new SkillMetadata(
            "calculator",
            "Evaluate a mathematical expression. Supports +, -, *, /, parentheses, and exponentiation.",
            "1.0.0",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "expression", Map.of("type", "string", "description", "Math expression to evaluate")
                ),
                "required", List.of("expression")
            ),
            List.of(),
            "ChatAI"
        );
    }

    @Override
    public SkillResult execute(Map<String, Object> params) {
        String expr = (String) params.getOrDefault("expression", "");
        if (expr == null || expr.isBlank()) {
            return SkillResult.error("Expression is empty");
        }
        try {
            double result = evaluate(expr.trim());
            String output;
            if (result == (long) result) {
                output = String.format("%s = %d", expr, (long) result);
            } else {
                output = String.format("%s = %.6f", expr, result);
            }
            return SkillResult.ok(output, Map.of("expression", expr, "result", result));
        } catch (Exception e) {
            return SkillResult.error("Failed to evaluate: " + e.getMessage());
        }
    }

    private double evaluate(String expr) {
        return new ExprParser(expr).parse();
    }

    private static class ExprParser {
        private final String input;
        private int pos;

        ExprParser(String input) { this.input = input; this.pos = 0; }

        double parse() {
            double val = parseTerm();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '+') { pos++; val += parseTerm(); }
                else if (c == '-') { pos++; val -= parseTerm(); }
                else break;
            }
            return val;
        }

        double parseTerm() {
            double val = parsePower();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '*') { pos++; val *= parsePower(); }
                else if (c == '/') { pos++; val /= parsePower(); }
                else break;
            }
            return val;
        }

        double parsePower() {
            double val = parseAtom();
            while (pos < input.length() && input.charAt(pos) == '^') {
                pos++;
                val = Math.pow(val, parseAtom());
            }
            return val;
        }

        double parseAtom() {
            skipWhitespace();
            if (pos >= input.length()) throw new IllegalArgumentException("Unexpected end");
            char c = input.charAt(pos);
            if (c == '(') {
                pos++;
                double val = parse();
                if (pos < input.length() && input.charAt(pos) == ')') pos++;
                return val;
            }
            if (c == '-') {
                pos++;
                return -parseAtom();
            }
            return parseNumber();
        }

        double parseNumber() {
            skipWhitespace();
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos))
                    || input.charAt(pos) == '.' || input.charAt(pos) == 'e'
                    || input.charAt(pos) == 'E' || (input.charAt(pos) == '-' && pos > start
                    && (input.charAt(pos - 1) == 'e' || input.charAt(pos - 1) == 'E')))) {
                pos++;
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
        }
    }
}
