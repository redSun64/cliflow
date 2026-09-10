package io.github.redsun64.acli.core.operation;

public record OperationParameter(
        String name,
        String in,
        boolean required
) {
}
