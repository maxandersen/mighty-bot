package io.mighty.tools;

import java.nio.file.Path;

public record ToolContext(Path workspaceRoot) {
    public Path resolvePath(String rawPath) {
        Path path = Path.of(rawPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return workspaceRoot.resolve(path).normalize();
    }
}
