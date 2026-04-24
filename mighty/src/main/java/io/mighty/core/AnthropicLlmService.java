package io.mighty.core;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

@ApplicationScoped
public class AnthropicLlmService implements LlmService {

    private final ChatModel chatModel;

    @Inject
    public AnthropicLlmService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public LlmResponse generate(List<Message> conversation, List<String> toolNames) {
        List<ChatMessage> messages = conversation.stream()
            .map(this::toChatMessage)
            .toList();

        ChatResponse response = chatModel.chat(messages);
        AiMessage aiMessage = response.aiMessage();
        String text = aiMessage == null ? "" : aiMessage.text();
        if (text == null) {
            text = "";
        }
        Optional<ToolCall> toolCall = ToolCall.parseEmbedded(text)
            .map(parsed -> new ToolCall(
                java.util.UUID.randomUUID().toString(),
                parsed.name(),
                parsed.arguments()));
        return new LlmResponse(text, toolCall);
    }

    private ChatMessage toChatMessage(Message message) {
        return switch (message.role()) {
            case SYSTEM -> SystemMessage.from(message.text());
            case ASSISTANT -> AiMessage.from(message.text());
            case USER, TOOL_RESULT -> UserMessage.from(message.text());
        };
    }
}
