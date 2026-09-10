package io.github.redsun64.acli.api;

import picocli.CommandLine.Option;

public final class CommonOptions {

    @Option(
            names = "--format",
            description = "Output format: ${COMPLETION-CANDIDATES}",
            defaultValue = "TEXT",
            converter = OutputFormatConverter.class
    )
    private OutputFormat format = OutputFormat.TEXT;

    @Option(
            names = "--dry-run",
            description = "Show the planned business action without sending write operations."
    )
    private boolean dryRun;

    @Option(
            names = "-y",
            description = "Alias for --yes."
    )
    private boolean shortYes;

    @Option(
            names = "--yes",
            description = "Confirm a command that requires explicit user approval."
    )
    private boolean yes;

    public ExecutionOptions toExecutionOptions() {
        return new ExecutionOptions(format, dryRun, yes || shortYes);
    }
}
