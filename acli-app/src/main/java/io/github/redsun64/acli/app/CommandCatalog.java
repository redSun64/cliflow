package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.CliModule;
import io.github.redsun64.acli.api.CommandRegistry;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/** Builds the command tree shared by the executable CLI and developer tooling. */
final class CommandCatalog {
    private CommandCatalog() {
    }

    static CommandLine create(RuntimeBootstrap runtime, PrintWriter out) {
        ObjectMapper mapper = runtime.mapper();
        CommandLine root = new CommandLine(new AcliRootCommand());
        registerBusinessModules(root, runtime);
        CommandLine operations = new CommandLine(new OperationsCommand(runtime.operations(), mapper, out));
        GeneratedOperationCommands.register(operations, runtime.operations().all(), runtime.contextFactory(), mapper);
        root.addSubcommand("operations", operations);
        root.addSubcommand("schema", new SchemaCommand(root, mapper, out));
        SkillsCommand.register(root, mapper, out);
        return root;
    }

    private static void registerBusinessModules(CommandLine root, RuntimeBootstrap runtime) {
        CommandRegistry registry = command -> {
            String name = command.getCommandSpec().name();
            if (root.getSubcommands().containsKey(name)) {
                throw new CliException("COMMAND_DUPLICATE", "Duplicate top-level business command: " + name);
            }
            root.addSubcommand(name, command);
        };
        try {
            List<CliModule> modules = ServiceLoader.load(CliModule.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(module -> module.getClass().getName()))
                    .toList();
            modules.forEach(module -> module.register(registry, runtime.contextFactory()));
        } catch (ServiceConfigurationError error) {
            throw new CliException("MODULE_LOAD_FAILED", "Unable to load a business CLI module: " + error.getMessage());
        }
    }
}
