package io.github.redsun64.acli.app;

import io.github.redsun64.acli.api.CliException;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * Validates every service and OpenAPI resource that will be bundled into the CLI.
 * Invoked by Maven before packaging, including when tests are skipped.
 */
public final class OpenApiBuildValidator {
    private OpenApiBuildValidator() {
    }

    public static void main(String[] args) {
        try {
            RuntimeBootstrap.create(new PrintWriter(Writer.nullWriter()));
        } catch (CliException exception) {
            throw new IllegalStateException(
                    "Bundled OpenAPI validation failed [" + exception.code() + "]: " + exception.getMessage(),
                    exception
            );
        }
    }
}
