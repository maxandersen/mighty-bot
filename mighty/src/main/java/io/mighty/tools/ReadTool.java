package io.mighty.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReadTool implements Tool {
    @Override
    public String name() {
        return "read";
    }

    @Override
    public String description() {
        return "Read file contents";
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) throws IOException {
        String pathValue = String.valueOf(params.getOrDefault("path", ""));
        if (pathValue.isBlank()) {
            return ToolResult.error("Missing required parameter: path");
        }

        Path target = context.resolvePath(pathValue);
        if (!Files.exists(target)) {
            return ToolResult.error("File does not exist: " + target);
        }
        if (Files.isDirectory(target)) {
            return ToolResult.error("Path is a directory: " + target);
        }

        String content = Files.readString(target, StandardCharsets.UTF_8);
        return ToolResult.success(content);
    }
}
