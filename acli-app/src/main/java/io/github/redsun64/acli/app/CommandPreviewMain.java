package io.github.redsun64.acli.app;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Entry point used by the build script to generate the command catalog preview. */
public final class CommandPreviewMain {
    private CommandPreviewMain() {
    }

    public static void main(String[] args) throws Exception {
        Path output = outputPath(args);
        try (PrintWriter quiet = new PrintWriter(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8)) {
            RuntimeBootstrap runtime = RuntimeBootstrap.create(quiet);
            String markdown = new CommandPreviewRenderer().render(CommandCatalog.create(runtime, quiet));
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, markdown, StandardCharsets.UTF_8);
        }
        System.out.println("Generated CLI command preview: " + output);
    }

    private static Path outputPath(String[] args) {
        if (args.length == 0) {
            return Path.of("acli-app", "target", "command-preview.md").toAbsolutePath().normalize();
        }
        if (args.length == 2 && "--output".equals(args[0]) && !args[1].isBlank()) {
            return Path.of(args[1]).toAbsolutePath().normalize();
        }
        throw new IllegalArgumentException("Usage: CommandPreviewMain [--output <file>]");
    }
}
