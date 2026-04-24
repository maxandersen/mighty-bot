package io.mighty.tools;

public interface Tool {
    String name();

    String description();

    ToolResult execute(java.util.Map<String, Object> input, ToolContext context) throws Exception;
}
