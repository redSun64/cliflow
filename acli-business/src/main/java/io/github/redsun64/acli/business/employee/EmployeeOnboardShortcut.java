package io.github.redsun64.acli.business.employee;

import io.github.redsun64.acli.api.AgentCommand;
import io.github.redsun64.acli.api.CliRisk;
import io.github.redsun64.acli.api.CommandContext;
import io.github.redsun64.acli.api.CommandContextFactory;
import io.github.redsun64.acli.api.CommonOptions;
import io.github.redsun64.acli.api.Confirmation;
import io.github.redsun64.acli.api.Effect;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
        name = "+onboard",
        description = "Provision an enterprise account and default role for an employee."
)
@AgentCommand(
        summary = "为企业员工初始化账号和默认角色。",
        useWhen = {
                "用户要求给新员工开通系统账号",
                "用户要求初始化员工权限"
        },
        avoidWhen = {
                "用户只是查询员工信息",
                "用户只想修改已有账号的单个角色"
        },
        examples = {
                "给张三开通系统账号",
                "初始化李四的 developer 权限"
        }
)
@CliRisk(effect = Effect.WRITE, confirmation = Confirmation.REQUIRED)
public final class EmployeeOnboardShortcut implements Callable<Integer> {

    @Option(names = "--name", required = true, description = "Employee name.")
    private String name;

    @Option(names = "--role", defaultValue = "employee", description = "Default role.")
    private String role;

    @Mixin
    private CommonOptions commonOptions = new CommonOptions();

    private final CommandContextFactory contextFactory;

    public EmployeeOnboardShortcut(CommandContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public Integer call() {
        CommandContext ctx = contextFactory.create(getClass(), commonOptions.toExecutionOptions());

        if (ctx.options().dryRun()) {
            ctx.output().success(Map.of(
                    "dryRun", true,
                    "command", "employee +onboard",
                    "arguments", Map.of("name", name, "role", role),
                    "operations", List.of(
                            Map.of("operation", "hr.searchEmployee", "effect", "READ"),
                            Map.of("operation", "iam.createAccount", "effect", "WRITE"),
                            Map.of("operation", "iam.bindRole", "effect", "WRITE")
                    )
            ));
            return 0;
        }

        Employee employee = ctx.call(
                "hr.searchEmployee",
                Map.of("keyword", name),
                Employee.class
        );

        Account account = ctx.call(
                "iam.createAccount",
                Map.of(
                        "employeeId", employee.id(),
                        "name", employee.name()
                ),
                Account.class
        );

        ctx.call(
                "iam.bindRole",
                Map.of(
                        "accountId", account.id(),
                        "role", role
                )
        );

        ctx.output().success(Map.of(
                "employee", employee,
                "account", account,
                "role", role
        ));
        return 0;
    }
}
