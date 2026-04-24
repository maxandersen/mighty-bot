package io.mighty.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JsonlSessionStore {

    private final ObjectMapper mapper;

    @Inject
    public JsonlSessionStore(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public AgentSession load(Path sessionsDir, String sessionId) throws IOException {
        Files.createDirectories(sessionsDir);
        Path path = sessionFile(sessionsDir, sessionId);
        AgentSession session = new AgentSession(sessionId, sessionsDir.getParent() == null ? Path.of(".") : sessionsDir.getParent());
        if (!Files.exists(path)) {
            return session;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            SessionEntry entry = mapper.readValue(line, SessionEntry.class);
            session.add(entry);
        }
        return session;
    }

    public void append(Path sessionsDir, String sessionId, SessionEntry entry) throws IOException {
        Files.createDirectories(sessionsDir);
        Path path = sessionFile(sessionsDir, sessionId);
        String json = mapper.writeValueAsString(entry);
        Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    private Path sessionFile(Path sessionsDir, String sessionId) {
        return sessionsDir.resolve(sessionId + ".jsonl");
    }
}
