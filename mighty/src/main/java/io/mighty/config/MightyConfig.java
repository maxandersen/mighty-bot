package io.mighty.config;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MightyConfig {

    @ConfigProperty(name = "mighty.sessions-dir", defaultValue = ".mighty/sessions")
    String sessionsDir;

    @ConfigProperty(name = "mighty.workdir", defaultValue = ".")
    String workdir;

    @ConfigProperty(name = "mighty.max-turns", defaultValue = "6")
    int maxTurns;

    public Path sessionsDir() {
        return Path.of(sessionsDir).toAbsolutePath().normalize();
    }

    public Path workdir() {
        return Path.of(workdir).toAbsolutePath().normalize();
    }

    public int maxTurns() {
        return maxTurns;
    }
}
