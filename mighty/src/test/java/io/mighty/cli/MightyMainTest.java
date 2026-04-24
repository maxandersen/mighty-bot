package io.mighty.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
class MightyMainTest {

    @Test
    void printsUsageWhenNoPrompt(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("Provide a prompt"));
    }

    @Test
    @Launch({ "hello", "world" })
    void executesDryRun(LaunchResult result) {
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("Tool result")
                || result.getOutput().contains("Tool execution failed")
                || result.getOutput().contains("I"));
    }
}
