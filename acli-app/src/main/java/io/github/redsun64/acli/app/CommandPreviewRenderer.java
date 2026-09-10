package io.github.redsun64.acli.app;

import io.github.redsun64.acli.api.AgentCommand;
import io.github.redsun64.acli.api.CliRisk;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Renders a deterministic, human-readable view of the configured Picocli tree. */
final class CommandPreviewRenderer {
    String render(CommandLine root) {
        List<Entry> entries = new ArrayList<>();
        collect(root, List.of("cliflow"), entries);

        StringBuilder markdown = new StringBuilder("# Cliflow Command Preview\n\n");
        markdown.append("This file is generated from the same Picocli command tree used by the CLI. Do not edit it manually.\n\n");
        markdown.append("## Command tree\n\n```text\n");
        for (Entry entry : entries) {
            markdown.append("  ".repeat(Math.max(0, entry.path().size() - 1)))
                    .append(entry.path().get(entry.path().size() - 1))
                    .append('\n');
        }
        markdown.append("```\n\n## Command contracts\n");

        entries.stream()
                .filter(entry -> visibleSubcommands(entry.commandLine()).isEmpty())
                .forEach(entry -> appendContract(markdown, entry));
        return markdown.toString();
    }

    private void collect(CommandLine commandLine, List<String> path, List<Entry> entries) {
        entries.add(new Entry(path, commandLine));
        visibleSubcommands(commandLine).forEach((name, child) -> {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(name);
            collect(child, List.copyOf(childPath), entries);
        });
    }

    private Map<String, CommandLine> visibleSubcommands(CommandLine commandLine) {
        return commandLine.getSubcommands().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("help"))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ));
    }

    private void appendContract(StringBuilder markdown, Entry entry) {
        CommandLine.Model.CommandSpec spec = entry.commandLine().getCommandSpec();
        markdown.append("\n### `").append(String.join(" ", entry.path())).append("`\n\n");
        appendDescription(markdown, spec);
        markdown.append("- Syntax: `").append(String.join(" ", entry.path())).append(" [options]`").append('\n');
        appendMetadata(markdown, spec.userObject());
        appendOptions(markdown, spec);
        appendParameters(markdown, spec);
    }

    private void appendDescription(StringBuilder markdown, CommandLine.Model.CommandSpec spec) {
        String description = String.join(" ", spec.usageMessage().description());
        if (!description.isBlank()) {
            markdown.append(escape(description)).append("\n\n");
        }
    }

    private void appendMetadata(StringBuilder markdown, Object userObject) {
        if (userObject instanceof SchemaMetadataProvider provider) {
            Map<String, Object> metadata = provider.schemaMetadata();
            appendMetadataValue(markdown, "Operation", metadata.get("operation"));
            appendMetadataValue(markdown, "HTTP method", metadata.get("method"));
            appendRisk(markdown, metadata.get("risk"));
        }
        if (userObject == null) {
            return;
        }
        Class<?> type = userObject.getClass();
        AgentCommand agent = type.getAnnotation(AgentCommand.class);
        if (agent != null) {
            appendMetadataValue(markdown, "Agent summary", agent.summary());
        }
        CliRisk risk = type.getAnnotation(CliRisk.class);
        if (risk != null) {
            markdown.append("- Risk: `").append(risk.effect()).append("`; confirmation: `")
                    .append(risk.confirmation()).append("`\n");
        }
    }

    private void appendMetadataValue(StringBuilder markdown, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            markdown.append("- ").append(label).append(": `").append(escape(value.toString())).append("`\n");
        }
    }

    private void appendRisk(StringBuilder markdown, Object value) {
        if (value instanceof Map<?, ?> risk) {
            Object effect = risk.get("effect");
            Object confirmation = risk.get("confirmation");
            if (effect != null || confirmation != null) {
                markdown.append("- Risk: `").append(escape(String.valueOf(effect)))
                        .append("`; confirmation: `").append(escape(String.valueOf(confirmation))).append("`\n");
            }
        }
    }

    private void appendOptions(StringBuilder markdown, CommandLine.Model.CommandSpec spec) {
        List<CommandLine.Model.OptionSpec> options = spec.options().stream()
                .filter(option -> !option.hidden())
                .toList();
        if (options.isEmpty()) {
            return;
        }
        markdown.append("\n| Option | Type | Required | Description |\n| --- | --- | --- | --- |\n");
        for (CommandLine.Model.OptionSpec option : options) {
            markdown.append("| `").append(escape(String.join(", ", option.names()))).append("` | ")
                    .append(escape(option.type().getSimpleName())).append(" | ")
                    .append(option.required() ? "yes" : "no").append(" | ")
                    .append(escape(String.join(" ", option.description()))).append(" |\n");
        }
    }

    private void appendParameters(StringBuilder markdown, CommandLine.Model.CommandSpec spec) {
        List<CommandLine.Model.PositionalParamSpec> parameters = spec.positionalParameters().stream()
                .filter(parameter -> !parameter.hidden())
                .toList();
        if (parameters.isEmpty()) {
            return;
        }
        markdown.append("\n| Parameter | Type | Required | Description |\n| --- | --- | --- | --- |\n");
        for (CommandLine.Model.PositionalParamSpec parameter : parameters) {
            markdown.append("| `").append(escape(parameter.paramLabel())).append("` | ")
                    .append(escape(parameter.type().getSimpleName())).append(" | ")
                    .append(parameter.required() ? "yes" : "no").append(" | ")
                    .append(escape(String.join(" ", parameter.description()))).append(" |\n");
        }
    }

    private String escape(String value) {
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private record Entry(List<String> path, CommandLine commandLine) {
    }
}
