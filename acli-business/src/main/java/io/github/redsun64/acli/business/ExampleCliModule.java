package io.github.redsun64.acli.business;

import io.github.redsun64.acli.api.CliModule;
import io.github.redsun64.acli.api.CliSkill;
import io.github.redsun64.acli.api.CommandContextFactory;
import io.github.redsun64.acli.api.CommandRegistry;
import io.github.redsun64.acli.business.employee.EmployeeCommand;
import io.github.redsun64.acli.business.employee.EmployeeOnboardShortcut;
import picocli.CommandLine;

import java.util.Optional;

/** Example of the one class a business CLI publishes to the SDK. */
public final class ExampleCliModule implements CliModule {
    @Override
    public void register(CommandRegistry commands, CommandContextFactory contextFactory) {
        CommandLine employee = new CommandLine(new EmployeeCommand());
        employee.addSubcommand("+onboard", new EmployeeOnboardShortcut(contextFactory));
        commands.add(employee);

    }

    @Override
    public Optional<CliSkill> skill() {
        return Optional.of(new CliSkill("example-hr", "/skills/example-hr/SKILL.md"));
    }
}
