package io.github.redsun64.acli.api;

import java.util.Map;

public interface CommandContext {

    <T> T call(String operationId, Map<String, ?> arguments, Class<T> resultType);

    Map<String, Object> call(String operationId, Map<String, ?> arguments);

    CommandOutput output();

    ExecutionOptions options();
}
