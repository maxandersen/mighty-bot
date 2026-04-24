package io.mighty.core;

import java.util.List;
import java.util.UUID;

import io.mighty.session.AgentSession;
import io.mighty.session.SessionEntry;
import io.mighty.tools.Tool;
import io.mighty.tools.ToolContext;
import io.mighty.tools.ToolRegistry;
import io.mighty.tools.ToolResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AgentLoop {

    @Inject
    LlmService llmService;

    @Inject
    ToolRegistry toolRegistry;

    public String runSingleTurn(AgentSession session) {
        List<Message> messages = buildMessages(session);
        List<String> toolNames = toolRegistry.all().stream().map(Tool::name).toList();
        LlmService.LlmResponse response = llmService.generate(messages, toolNames);

        String assistantText = response.text();
        if (response.toolCall().isEmpty()) {
            return assistantText;
        }
        ToolCall toolCall = response.toolCall().get();
        try {
            ToolResult result = executeTool(toolCall, session);
            return """
                    %s

                    Tool result (%s):
                    %s
                    """.formatted(assistantText, toolCall.name(), result.text()).trim();
        } catch (Exception e) {
            return """
                    %s

                    Tool execution failed (%s):
                    %s
                    """.formatted(assistantText, toolCall.name(), e.getMessage()).trim();
        }
    }

    private List<Message> buildMessages(AgentSession session) {
        List<Message> messages = new java.util.ArrayList<>();
        session.systemPrompt().ifPresent(system -> messages.add(Message.system(system)));
        for (SessionEntry entry : session.entries()) {
            if (entry.type().equals("user")) {
                messages.add(Message.user(entry.text()));
            } else if (entry.type().equals("assistant")) {
                messages.add(Message.assistant(entry.text()));
            } else if (entry.type().equals("toolResult")) {
                messages.add(Message.tool("Tool result from " + entry.tool() + ": " + entry.text()));
            }
        }
        return messages;
    }

    private ToolResult executeTool(ToolCall toolCall, AgentSession session) throws Exception {
        Tool tool = toolRegistry.find(toolCall.name())
            .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolCall.name()));
        java.util.Map<String, Object> input = parseJsonObject(toolCall.argumentsJson());
        ToolResult result = tool.execute(input, new ToolContext(session.workingDirectory()));

        String toolCallEntryId = UUID.randomUUID().toString();
        session.add(SessionEntry.toolCall(toolCallEntryId, session.lastEntryId().orElse(null),
            toolCall.name(), input));
        session.add(SessionEntry.toolResult(
            UUID.randomUUID().toString(),
            toolCallEntryId,
            toolCall.name(),
            result.text(),
            result.success(),
            java.util.Map.of("success", result.success())
        ));
        return result;
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return java.util.Map.of();
        }
        try {
            Object parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Object.class);
            if (parsed instanceof java.util.Map<?, ?> map) {
                return (java.util.Map<String, Object>) map;
            }
            return java.util.Map.of();
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }
}
