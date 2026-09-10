package io.github.redsun64.acli.core.operation;

import io.github.redsun64.acli.api.Effect;

import java.util.List;

public record OperationDefinition(
        String id,
        String serviceId,
        String operationId,
        String method,
        String path,
        List<OperationParameter> parameters,
        List<String> tags,
        boolean requestBody,
        boolean requestBodyRequired,
        List<OperationBodyParameter> bodyParameters,
        Effect effect,
        String summary
) {
    public OperationDefinition {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        tags = tags == null ? List.of() : List.copyOf(tags);
        bodyParameters = bodyParameters == null ? List.of() : List.copyOf(bodyParameters);
        summary = summary == null ? "" : summary;
    }
}
