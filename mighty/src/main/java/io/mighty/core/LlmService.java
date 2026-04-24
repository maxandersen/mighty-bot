package io.mighty.core;

import java.util.List;
import java.util.Optional;

public interface LlmService {

    LlmResponse generate(List<Message> messages, List<String> toolNames);

    record LlmResponse(String text, Optional<ToolCall> toolCall) {
    }
}
