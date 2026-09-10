package io.github.redsun64.acli.app;

import picocli.CommandLine.Command;

@Command(
        name = "cliflow",
        mixinStandardHelpOptions = true,
        version = "cliflow 0.1.0",
        description = "Business actions for AI agents."
)
public final class AcliRootCommand implements Runnable {
    @Override
    public void run() {
        // Picocli prints usage through Main when no subcommand is selected.
    }
}
