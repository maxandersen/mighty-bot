package io.mighty.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WriteTool implements Tool {

    @Override
    public String name() {
        return "write";
    }

    @Override
    public String description() {
        return "Write or create file contents";
    }

    @Override
    public ToolResult execute(Map<String, Object> input, ToolContext context) throws IOException {
        String rawPath = valueAsString(input.get("path"));
        if (rawPath.isBlank()) {
            return ToolResult.error("Missing required parameter: path");
        }
        String content = valueAsString(input.get("content"));
        Path path = context.resolvePath(rawPath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content);
        return ToolResult.success("Wrote " + content.length() + " bytes to " + rawPath);
    }

    private static String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
