package io.mighty.tools;

public record ToolResult(boolean success, String text) {
    public static ToolResult success(String text) {
        return new ToolResult(true, text);
    }

    public static ToolResult error(String text) {
        return new ToolResult(false, text);
    }
}
