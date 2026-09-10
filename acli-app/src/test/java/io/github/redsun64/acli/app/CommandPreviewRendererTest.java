package io.github.redsun64.acli.app;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPreviewRendererTest {
    @Test
    void rendersGeneratedOperationMetadataAndOptions() {
        CommandLine root = new CommandLine(new RootCommand());
        CommandLine service = new CommandLine(new ServiceCommand());
        CommandLine operations = new CommandLine(new Operations());
        operations.addSubcommand("postKnowledgePage", new KnowledgePageCommand());
        service.addSubcommand("operations", operations);
        root.addSubcommand("knowledge", service);

        String markdown = new CommandPreviewRenderer().render(root);

        assertTrue(markdown.contains("# Cliflow Command Preview"));
        assertTrue(markdown.contains("      postKnowledgePage"));
        assertTrue(markdown.contains("### `cliflow knowledge operations postKnowledgePage`"));
        assertTrue(markdown.contains("- Operation: `knowledge.postKnowledgePage`"));
        assertTrue(markdown.contains("- HTTP method: `POST`"));
        assertTrue(markdown.contains("- Risk: `WRITE`; confirmation: `REQUIRED`"));
        assertTrue(markdown.contains("| `--page` | Long | yes | Page number. |"));
    }

    @Command(name = "cliflow")
    static class RootCommand {
    }

    @Command(name = "knowledge")
    static class ServiceCommand {
    }

    @Command(name = "operations")
    static class Operations {
    }

    @Command(name = "postKnowledgePage")
    static class KnowledgePageCommand implements Runnable, SchemaMetadataProvider {
        @Option(names = "--page", required = true, description = "Page number.")
        private Long page;

        @Override
        public void run() {
        }

        @Override
        public Map<String, Object> schemaMetadata() {
            return Map.of(
                    "operation", "knowledge.postKnowledgePage",
                    "method", "POST",
                    "risk", Map.of("effect", "WRITE", "confirmation", "REQUIRED")
            );
        }
    }
}
