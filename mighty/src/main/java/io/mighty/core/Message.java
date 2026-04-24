package io.mighty.core;

public record Message(Role role, String text) {
    public static Message system(String text) {
        return new Message(Role.SYSTEM, text);
    }

    public static Message user(String text) {
        return new Message(Role.USER, text);
    }

    public static Message assistant(String text) {
        return new Message(Role.ASSISTANT, text);
    }

    public static Message tool(String text) {
        return new Message(Role.TOOL_RESULT, text);
    }
}
