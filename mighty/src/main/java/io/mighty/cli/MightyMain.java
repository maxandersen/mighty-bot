package io.mighty.cli;

import io.mighty.agent.AgentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
        name = "mighty",
        mixinStandardHelpOptions = true,
        description = "Mighty coding agent (Phase 1 baseline).")
@ApplicationScoped
public class MightyMain implements Runnable {

    @Inject
    AgentService agentService;

    @Parameters(index = "0..*", description = "User prompt", arity = "0..*")
    String[] promptParts = new String[0];

    @Override
    public void run() {
        String prompt = String.join(" ", promptParts).trim();
        if (prompt.isEmpty()) {
            System.out.println("Provide a prompt, for example:");
            System.out.println("  ./mvnw quarkus:dev -Dquarkus.args='Explain this repository'");
            return;
        }

        String response = agentService.runTurn("default", prompt);
        System.out.println(response);
    }
}
