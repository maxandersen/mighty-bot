package io.mighty.session;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AgentSession {

    private final String id;
    private final Path workingDirectory;
    private String systemPrompt;
    private final List<SessionEntry> entries = new ArrayList<>();

    public AgentSession(String id, Path workingDirectory) {
        this.id = Objects.requireNonNull(id, "id");
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory");
    }

    public String id() {
        return id;
    }

    public Path workingDirectory() {
        return workingDirectory;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public java.util.Optional<String> systemPrompt() {
        return java.util.Optional.ofNullable(systemPrompt);
    }

    public List<SessionEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public void add(SessionEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry"));
    }

    public java.util.Optional<String> lastEntryId() {
        if (entries.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(entries.get(entries.size() - 1).id());
    }
}
