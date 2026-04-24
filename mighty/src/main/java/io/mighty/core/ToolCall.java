package io.mighty.core;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ToolCall(String id, String name, String argumentsJson) {

    private static final Pattern PATTERN = Pattern.compile(
        "<tool_call\\s+name=\"([^\"]+)\">(.*?)</tool_call>",
        Pattern.DOTALL
    );

    public static Optional<ParsedToolCall> parseEmbedded(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = PATTERN.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedToolCall(matcher.group(1).trim(), matcher.group(2).trim()));
    }

    public record ParsedToolCall(String name, String arguments) {
    }
}
