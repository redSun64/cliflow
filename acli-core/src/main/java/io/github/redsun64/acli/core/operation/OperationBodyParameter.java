package io.github.redsun64.acli.core.operation;

public record OperationBodyParameter(
        String name,
        String type,
        boolean required
) {
}
