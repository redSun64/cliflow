package io.github.redsun64.acli.app;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCatalogTest {
    @Test
    void loadsBusinessSpiAndKeepsGeneratedOperationsUnderTheEscapeHatch() {
        CommandLine root = CommandCatalog.create(
                RuntimeBootstrap.create(new PrintWriter(new StringWriter(), true)),
                new PrintWriter(new StringWriter(), true)
        );

        assertTrue(root.getSubcommands().containsKey("employee"));
        assertTrue(root.getSubcommands().containsKey("operations"));
        assertFalse(root.getSubcommands().containsKey("hr"));

        CommandLine operations = root.getSubcommands().get("operations");
        assertTrue(operations.getSubcommands().containsKey("hr"));
    }
}
