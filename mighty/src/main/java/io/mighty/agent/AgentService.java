package io.mighty.agent;

import java.io.IOException;
import java.nio.file.Path;

import io.mighty.config.MightyConfig;
import io.mighty.core.AgentLoop;
import io.mighty.core.AgentsInstructionsLoader;
import io.mighty.session.AgentSession;
import io.mighty.session.JsonlSessionStore;
import io.mighty.session.SessionEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AgentService {

    @Inject
    MightyConfig config;

    @Inject
    AgentLoop agentLoop;

    @Inject
    AgentsInstructionsLoader instructionsLoader;

    @Inject
    JsonlSessionStore sessionStore;

    public String runTurn(String sessionId, String prompt) {
        try {
            Path workspace = config.workdir();
            Path sessionsDir = config.sessionsDir();

            AgentSession session = sessionStore.load(sessionsDir, sessionId);
            instructionsLoader.loadMerged(workspace)
                    .ifPresent(session::setSystemPrompt);

            String userParentId = session.lastEntryId().orElse(null);
            SessionEntry userEntry = SessionEntry.user(
                java.util.UUID.randomUUID().toString(),
                userParentId,
                prompt
            );
            session.add(userEntry);
            sessionStore.append(sessionsDir, sessionId, userEntry);

            String response = agentLoop.runSingleTurn(session);

            SessionEntry assistantEntry = SessionEntry.assistant(
                java.util.UUID.randomUUID().toString(),
                userEntry.id(),
                response
            );
            session.add(assistantEntry);
            sessionStore.append(sessionsDir, sessionId, assistantEntry);
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("Session persistence failed: " + e.getMessage(), e);
        }
    }
}
