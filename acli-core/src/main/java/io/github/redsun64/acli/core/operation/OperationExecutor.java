package io.github.redsun64.acli.core.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.redsun64.acli.api.ExecutionOptions;

import java.util.Map;

public interface OperationExecutor {
    JsonNode execute(OperationDefinition operation, Map<String, ?> arguments, ExecutionOptions options);
}
