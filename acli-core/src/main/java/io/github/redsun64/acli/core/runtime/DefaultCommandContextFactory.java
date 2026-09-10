package io.github.redsun64.acli.core.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.CliRisk;
import io.github.redsun64.acli.api.CommandContext;
import io.github.redsun64.acli.api.CommandContextFactory;
import io.github.redsun64.acli.api.Confirmation;
import io.github.redsun64.acli.api.ExecutionOptions;
import io.github.redsun64.acli.core.operation.OperationExecutor;
import io.github.redsun64.acli.core.operation.OperationRegistry;

import java.io.PrintWriter;
import java.util.Map;

public final class DefaultCommandContextFactory implements CommandContextFactory {
    private final OperationRegistry operations;
    private final OperationExecutor executor;
    private final ObjectMapper mapper;
    private final PrintWriter out;

    public DefaultCommandContextFactory(
            OperationRegistry operations,
            OperationExecutor executor,
            ObjectMapper mapper,
            PrintWriter out) {
        this.operations = operations;
        this.executor = executor;
        this.mapper = mapper;
        this.out = out;
    }

    @Override
    public CommandContext create(Class<?> commandType, ExecutionOptions options) {
        CliRisk risk = commandType.getAnnotation(CliRisk.class);
        if (risk != null
                && risk.confirmation() == Confirmation.REQUIRED
                && !options.confirmed()
                && !options.dryRun()) {
            throw new CliException(
                    "CONFIRMATION_REQUIRED",
                    "This command performs a write action and requires explicit confirmation. Re-run with --yes after user approval.",
                    Map.of("commandType", commandType.getName())
            );
        }

        return new DefaultCommandContext(
                operations,
                executor,
                mapper,
                new DefaultCommandOutput(mapper, out, options.format()),
                options
        );
    }
}
