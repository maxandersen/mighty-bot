package io.mighty.session;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record SessionEntry(
        String id,
        String parentId,
        String type,
        Instant timestamp,
        String text,
        String tool,
        Map<String, Object> payload) {

    public SessionEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public static SessionEntry user(String id, String parentId, String text) {
        return new SessionEntry(id, parentId, "user", Instant.now(), text, null, null);
    }

    public static SessionEntry assistant(String id, String parentId, String text) {
        return new SessionEntry(id, parentId, "assistant", Instant.now(), text, null, null);
    }

    public static SessionEntry toolCall(String id, String parentId, String tool, Map<String, Object> payload) {
        return new SessionEntry(id, parentId, "toolCall", Instant.now(), null, tool, payload);
    }

    public static SessionEntry toolResult(String id, String parentId, String tool, String text, boolean success,
            Map<String, Object> payload) {
        return new SessionEntry(id, parentId, "toolResult", Instant.now(), text, tool, payload);
    }
}
