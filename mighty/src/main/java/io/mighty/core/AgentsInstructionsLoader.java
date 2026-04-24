package io.mighty.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AgentsInstructionsLoader {

    public Optional<String> loadMerged(Path workspaceRoot) {
        ArrayList<String> chunks = new ArrayList<>();
        Path cursor = workspaceRoot;
        while (cursor != null) {
            Path agentsFile = cursor.resolve("AGENTS.md");
            if (Files.exists(agentsFile)) {
                try {
                    chunks.add(Files.readString(agentsFile));
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read " + agentsFile, e);
                }
            }
            cursor = cursor.getParent();
        }
        if (chunks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("\n\n", chunks));
    }
}
