package io.github.redsun64.acli.api;

public interface CommandContextFactory {
    CommandContext create(Class<?> commandType, ExecutionOptions options);
}
