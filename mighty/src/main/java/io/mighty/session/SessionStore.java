package io.mighty.session;

import java.io.IOException;
import java.util.Optional;

public interface SessionStore {
    AgentSession create(String sessionId) throws IOException;

    Optional<AgentSession> load(String sessionId) throws IOException;

    void append(String sessionId, SessionEntry entry) throws IOException;
}
