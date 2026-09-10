package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.AgentCommand;
import io.github.redsun64.acli.api.CliRisk;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "schema", description = "Progressively inspect the CLI command catalog for agents.")
public final class SchemaCommand implements Callable<Integer> {
    private static final Set<String> INTERNAL_ROOT_COMMANDS = Set.of("operations", "skills");

    @Parameters(arity = "0..*", paramLabel = "COMMAND", description = "Command path, e.g. employee +onboard")
    private List<String> path = new ArrayList<>();

    private final CommandLine root;
    private final ObjectMapper mapper;
    private final PrintWriter out;

    public SchemaCommand(CommandLine root, ObjectMapper mapper, PrintWriter out) {
        this.root = root;
        this.mapper = mapper;
        this.out = out;
    }

    @Override
    public Integer call() throws Exception {
        CommandLine target = root;
        for (String part : path) {
            CommandLine next = target.getSubcommands().get(part);
            if (next == null) {
                throw new CommandLine.ParameterException(
                        root,
                        "Unknown command path: " + String.join(" ", path)
                );
            }
            target = next;
        }

        out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(describe(target)));
        out.flush();
        return 0;
    }

    private Map<String, Object> describe(CommandLine commandLine) {
        CommandLine.Model.CommandSpec spec = commandLine.getCommandSpec();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", spec.name());
        result.put("description", String.join(" ", spec.usageMessage().description()));

        if (!commandLine.getSubcommands().isEmpty()) {
            result.put("subcommands", commandLine.getSubcommands().entrySet().stream()
                    .filter(entry -> !entry.getKey().startsWith("help"))
                    .filter(entry -> commandLine != root || !INTERNAL_ROOT_COMMANDS.contains(entry.getKey()))
                    .map(entry -> describeSummary(entry.getKey(), entry.getValue()))
                    .toList());
        }

        List<Map<String, Object>> options = spec.options().stream()
                .filter(option -> !option.hidden())
                .map(option -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", option.longestName());
                    item.put("required", option.required());
                    item.put("type", option.type().getSimpleName());
                    item.put("description", String.join(" ", option.description()));
                    return item;
                })
                .toList();
        if (!options.isEmpty()) {
            result.put("options", options);
        }

        Object userObject = spec.userObject();
        if (userObject instanceof SchemaMetadataProvider provider) {
            result.putAll(provider.schemaMetadata());
        }
        if (userObject != null) {
            Class<?> type = userObject.getClass();
            AgentCommand agent = type.getAnnotation(AgentCommand.class);
            if (agent != null) {
                result.put("agent", Map.of(
                        "summary", agent.summary(),
                        "useWhen", Arrays.asList(agent.useWhen()),
                        "avoidWhen", Arrays.asList(agent.avoidWhen()),
                        "examples", Arrays.asList(agent.examples())
                ));
            }

            CliRisk risk = type.getAnnotation(CliRisk.class);
            if (risk != null) {
                result.put("risk", Map.of(
                        "effect", risk.effect().name(),
                        "confirmation", risk.confirmation().name()
                ));
            }
        }

        return result;
    }

    private Map<String, Object> describeSummary(String name, CommandLine commandLine) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", name);
        summary.put("description", String.join(" ", commandLine.getCommandSpec().usageMessage().description()));
        Object userObject = commandLine.getCommandSpec().userObject();
        if (userObject instanceof SchemaMetadataProvider provider) {
            summary.putAll(provider.schemaMetadata());
        }
        return summary;
    }
}
