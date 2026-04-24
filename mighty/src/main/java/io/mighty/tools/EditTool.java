package io.mighty.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EditTool implements Tool {

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public String description() {
        return "Apply a simple old/new replacement to an existing file.";
    }

    @Override
    public ToolResult execute(Map<String, Object> input, ToolContext context) throws IOException {
        String pathValue = asString(input.get("path"));
        String oldText = asString(input.get("oldText"));
        String newText = asString(input.get("newText"));
        if (pathValue == null || pathValue.isBlank() || oldText == null || newText == null) {
            return ToolResult.error("edit requires path, oldText, and newText");
        }

        Path base = context.workspaceRoot();
        Path target = base.resolve(pathValue).normalize();
        if (!target.startsWith(base)) {
            return ToolResult.error("path escapes workspace");
        }
        if (!Files.exists(target)) {
            return ToolResult.error("file does not exist: " + target);
        }

        String existing = Files.readString(target);
        if (!existing.contains(oldText)) {
            return ToolResult.error("oldText not found in file");
        }
        String updated = existing.replace(oldText, newText);
        Files.writeString(target, updated);
        return ToolResult.success("edited " + target);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
