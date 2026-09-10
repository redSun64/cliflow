package io.github.redsun64.acli.api;

import java.util.Optional;

/**
 * A business CLI contribution discovered by the SDK with {@link java.util.ServiceLoader}.
 * Implementations belong to the business CLI JAR, not to the SDK.
 */
public interface CliModule {
    void register(CommandRegistry commands, CommandContextFactory contextFactory);

    /** A standalone business CLI may publish one static Skill for agents. */
    default Optional<CliSkill> skill() {
        return Optional.empty();
    }
}
