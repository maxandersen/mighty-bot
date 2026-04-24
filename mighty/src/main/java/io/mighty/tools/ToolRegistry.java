package io.mighty.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ToolRegistry {

    private final Map<String, Tool> toolsByName = new LinkedHashMap<>();

    public ToolRegistry(ReadTool readTool, WriteTool writeTool, EditTool editTool, BashTool bashTool) {
        List<Tool> builtins = new ArrayList<>();
        builtins.add(readTool);
        builtins.add(writeTool);
        builtins.add(editTool);
        builtins.add(bashTool);
        for (Tool tool : builtins) {
            register(tool);
        }
    }

    public void register(Tool tool) {
        toolsByName.put(tool.name(), tool);
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public Collection<Tool> all() {
        return toolsByName.values();
    }
}
