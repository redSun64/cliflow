package io.github.redsun64.acli.api;

public record ExecutionOptions(
        OutputFormat format,
        boolean dryRun,
        boolean confirmed
) {
}
