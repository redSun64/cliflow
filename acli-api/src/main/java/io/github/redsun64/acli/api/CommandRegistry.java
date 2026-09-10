package io.github.redsun64.acli.api;

import picocli.CommandLine;

/**
 * The only command-tree seam exposed to a business {@link CliModule}.
 * The SDK owns the root command and rejects duplicate names.
 */
@FunctionalInterface
public interface CommandRegistry {
    void add(CommandLine command);
}
