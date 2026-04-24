package io.mighty.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BashTool implements Tool {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public String name() {
        return "bash";
    }

    @Override
    public String description() {
        return "Execute a shell command in the current workspace.";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        Object rawCommand = args.get("command");
        if (!(rawCommand instanceof String command) || command.isBlank()) {
            return ToolResult.error("bash tool requires non-empty 'command' argument");
        }

        int timeoutSeconds = readInt(args.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS);

        ProcessBuilder pb = new ProcessBuilder(List.of("bash", "-lc", command));
        pb.directory(context.workspaceRoot().toFile());

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.error("bash command timed out after " + timeoutSeconds + "s");
            }

            int exit = process.exitValue();
            String stdout = read(process.getInputStream());
            String stderr = read(process.getErrorStream());
            String output = """
                    exit=%d
                    stdout:
                    %s
                    stderr:
                    %s
                    """.formatted(exit, stdout, stderr);
            return exit == 0 ? ToolResult.success(output) : ToolResult.error(output);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.error("bash execution failed: " + e.getMessage());
        }
    }

    private static int readInt(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String read(java.io.InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        stream.transferTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }
}
