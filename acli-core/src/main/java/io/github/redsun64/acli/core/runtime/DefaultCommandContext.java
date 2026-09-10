package io.github.redsun64.acli.core.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CommandContext;
import io.github.redsun64.acli.api.CommandOutput;
import io.github.redsun64.acli.api.ExecutionOptions;
import io.github.redsun64.acli.core.operation.OperationDefinition;
import io.github.redsun64.acli.core.operation.OperationExecutor;
import io.github.redsun64.acli.core.operation.OperationRegistry;

import java.util.Map;

public final class DefaultCommandContext implements CommandContext {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final OperationRegistry operations;
    private final OperationExecutor executor;
    private final ObjectMapper mapper;
    private final CommandOutput output;
    private final ExecutionOptions options;

    public DefaultCommandContext(
            OperationRegistry operations,
            OperationExecutor executor,
            ObjectMapper mapper,
            CommandOutput output,
            ExecutionOptions options) {
        this.operations = operations;
        this.executor = executor;
        this.mapper = mapper;
        this.output = output;
        this.options = options;
    }

    @Override
    public <T> T call(String operationId, Map<String, ?> arguments, Class<T> resultType) {
        OperationDefinition operation = operations.get(operationId);
        JsonNode result = executor.execute(operation, arguments, options);
        if (result == null || result.isNull()) {
            return null;
        }
        return mapper.convertValue(result, resultType);
    }

    @Override
    public Map<String, Object> call(String operationId, Map<String, ?> arguments) {
        OperationDefinition operation = operations.get(operationId);
        JsonNode result = executor.execute(operation, arguments, options);
        if (result == null || result.isNull()) {
            return Map.of();
        }
        return mapper.convertValue(result, MAP_TYPE);
    }

    @Override
    public CommandOutput output() {
        return output;
    }

    @Override
    public ExecutionOptions options() {
        return options;
    }
}
