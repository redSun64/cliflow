package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.CommandContext;
import io.github.redsun64.acli.api.CommandContextFactory;
import io.github.redsun64.acli.api.CommonOptions;
import io.github.redsun64.acli.api.Effect;
import io.github.redsun64.acli.core.operation.OperationDefinition;
import io.github.redsun64.acli.core.operation.OperationBodyParameter;
import io.github.redsun64.acli.core.operation.OperationParameter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

final class GeneratedOperationCommands {
    private GeneratedOperationCommands() {
    }

    static void register(
            CommandLine root,
            Collection<OperationDefinition> operations,
            CommandContextFactory contextFactory,
            ObjectMapper mapper) {
        Map<String, List<OperationDefinition>> byService = new TreeMap<>();
        operations.forEach(operation -> byService
                .computeIfAbsent(operation.serviceId(), ignored -> new java.util.ArrayList<>())
                .add(operation));

        byService.forEach((serviceId, serviceOperations) -> {
            CommandLine service = root.getSubcommands().get(serviceId);
            if (service == null) {
                service = commandLine(new GeneratedServiceCommand(serviceId), serviceId,
                        "Generated OpenAPI commands for " + serviceId + ".");
                root.addSubcommand(serviceId, service);
            }
            CommandLine serviceCommand = service;

            Map<String, List<OperationDefinition>> byTag = new TreeMap<>();
            serviceOperations.forEach(operation -> byTag
                    .computeIfAbsent(groupName(operation), ignored -> new java.util.ArrayList<>())
                    .add(operation));
            byTag.forEach((tag, tagOperations) -> {
                if (serviceCommand.getSubcommands().containsKey(tag)) {
                    throw new IllegalArgumentException("Duplicate generated command group: " + serviceId + " " + tag);
                }
                CommandLine group = commandLine(new GeneratedTagCommand(serviceId, tag, tagOperations.size()), tag,
                        "Generated OpenAPI commands tagged " + tag + ".");
                tagOperations.stream()
                        .sorted(java.util.Comparator.comparing(OperationDefinition::operationId))
                        .forEach(operation -> addOperation(group, serviceId, tag, operation, contextFactory, mapper));
                serviceCommand.addSubcommand(tag, group);
            });
        });
    }

    private static void addOperation(
            CommandLine group,
            String serviceId,
            String tag,
            OperationDefinition operation,
            CommandContextFactory contextFactory,
            ObjectMapper mapper) {
        if (group.getSubcommands().containsKey(operation.operationId())) {
            throw new IllegalArgumentException("Duplicate generated command: "
                    + serviceId + " " + tag + " " + operation.operationId());
        }

        GeneratedOperationCommand command = new GeneratedOperationCommand(operation, contextFactory, mapper);
        CommandLine commandLine = commandLine(command, operation.operationId(), operation.summary());
        operation.parameters().forEach(parameter -> commandLine.getCommandSpec().addOption(option(parameter, command)));
        HashSet<String> reservedOptions = new HashSet<>(Set.of("body", "body-file", "dry-run", "format", "help", "version", "yes", "y"));
        operation.parameters().forEach(parameter -> reservedOptions.add(parameter.name()));
        operation.bodyParameters().stream()
                .filter(parameter -> !reservedOptions.contains(parameter.name()))
                .forEach(parameter -> commandLine.getCommandSpec().addOption(bodyOption(parameter, command)));
        if (operation.requestBody()) {
            commandLine.getCommandSpec().addOption(CommandLine.Model.OptionSpec.builder("--body")
                    .paramLabel("JSON")
                    .description("Request body as JSON.")
                    .type(String.class)
                    .setter(setter(value -> {
                        if (value != null) {
                            command.setBody(String.valueOf(value));
                        }
                    }))
                    .build());
            commandLine.getCommandSpec().addOption(CommandLine.Model.OptionSpec.builder("--body-file")
                    .paramLabel("FILE")
                    .description("Read the request body as JSON from FILE.")
                    .type(Path.class)
                    .setter(setter(value -> {
                        if (value != null) {
                            command.setBodyFile((Path) value);
                        }
                    }))
                    .build());
        }
        group.addSubcommand(operation.operationId(), commandLine);
    }

    private static CommandLine.Model.OptionSpec option(OperationParameter parameter, GeneratedOperationCommand command) {
        return CommandLine.Model.OptionSpec.builder("--" + parameter.name())
                .paramLabel(parameter.name().toUpperCase(Locale.ROOT))
                .description(parameter.in() + " parameter.")
                .required(parameter.required())
                .type(String.class)
                .setter(setter(value -> {
                    if (value != null) {
                        command.setArgument(parameter.name(), String.valueOf(value));
                    }
                }))
                .build();
    }

    private static CommandLine.Model.OptionSpec bodyOption(
            OperationBodyParameter parameter,
            GeneratedOperationCommand command) {
        return CommandLine.Model.OptionSpec.builder("--" + parameter.name())
                .paramLabel(parameter.name().toUpperCase(Locale.ROOT))
                .description("request body " + parameter.type() + " field"
                        + (parameter.required() ? " (required when using body fields)." : "."))
                .required(false)
                .type(bodyParameterType(parameter.type()))
                .setter(setter(value -> {
                    if (value != null) {
                        command.setBodyField(parameter.name(), value);
                    }
                }))
                .build();
    }

    private static Class<?> bodyParameterType(String type) {
        return switch (type) {
            case "integer" -> Long.class;
            case "number" -> Double.class;
            case "boolean" -> Boolean.class;
            default -> String.class;
        };
    }

    private static CommandLine.Model.ISetter setter(Consumer<Object> consumer) {
        return new CommandLine.Model.ISetter() {
            @Override
            public <T> T set(T value) {
                consumer.accept(value);
                return value;
            }
        };
    }

    private static CommandLine commandLine(Object command, String name, String description) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.getCommandSpec().name(name);
        commandLine.getCommandSpec().usageMessage().description(description == null || description.isBlank()
                ? new String[]{"Generated from OpenAPI."}
                : new String[]{description});
        return commandLine;
    }

    private static String groupName(OperationDefinition operation) {
        String tag = operation.tags().isEmpty() ? "operations" : operation.tags().getFirst();
        String normalized = tag.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "operations" : normalized;
    }

    @Command(mixinStandardHelpOptions = true)
    private static final class GeneratedServiceCommand implements Runnable, SchemaMetadataProvider {
        private final String serviceId;

        private GeneratedServiceCommand(String serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public void run() {
            // Groups only organize generated subcommands.
        }

        @Override
        public Map<String, Object> schemaMetadata() {
            return Map.of("generated", true, "kind", "openapi-service", "service", serviceId);
        }
    }

    @Command(mixinStandardHelpOptions = true)
    private static final class GeneratedTagCommand implements Runnable, SchemaMetadataProvider {
        private final String serviceId;
        private final String tag;
        private final int operationCount;

        private GeneratedTagCommand(String serviceId, String tag, int operationCount) {
            this.serviceId = serviceId;
            this.tag = tag;
            this.operationCount = operationCount;
        }

        @Override
        public void run() {
            // Groups only organize generated subcommands.
        }

        @Override
        public Map<String, Object> schemaMetadata() {
            return Map.of(
                    "generated", true,
                    "kind", "openapi-tag",
                    "service", serviceId,
                    "tag", tag,
                    "operationCount", operationCount
            );
        }
    }

    @Command(mixinStandardHelpOptions = true)
    private static final class GeneratedOperationCommand implements Callable<Integer>, SchemaMetadataProvider {
        @Mixin
        private CommonOptions commonOptions = new CommonOptions();

        private final OperationDefinition operation;
        private final CommandContextFactory contextFactory;
        private final ObjectMapper mapper;
        private final Map<String, Object> arguments = new LinkedHashMap<>();
        private final Map<String, Object> bodyFields = new LinkedHashMap<>();
        private String body;
        private Path bodyFile;

        private GeneratedOperationCommand(
                OperationDefinition operation,
                CommandContextFactory contextFactory,
                ObjectMapper mapper) {
            this.operation = operation;
            this.contextFactory = contextFactory;
            this.mapper = mapper;
        }

        private void setArgument(String name, String value) {
            arguments.put(name, value);
        }

        private void setBody(String body) {
            this.body = body;
        }

        private void setBodyFile(Path bodyFile) {
            this.bodyFile = bodyFile;
        }

        private void setBodyField(String name, Object value) {
            bodyFields.put(name, value);
        }

        @Override
        public Integer call() throws Exception {
            try {
                return execute();
            } finally {
                arguments.clear();
                bodyFields.clear();
                body = null;
                bodyFile = null;
            }
        }

        private Integer execute() throws Exception {
            if ((body != null && bodyFile != null)
                    || ((body != null || bodyFile != null) && !bodyFields.isEmpty())) {
                throw new CliException(
                        "REQUEST_BODY_AMBIGUOUS",
                        "Use either --body, --body-file, or generated request body fields, not a combination."
                );
            }
            if (operation.requestBodyRequired() && body == null && bodyFile == null && bodyFields.isEmpty()) {
                throw new CliException("REQUEST_BODY_REQUIRED", "A JSON request body is required for " + operation.id() + ".");
            }
            if (body == null && bodyFile == null) {
                operation.bodyParameters().stream()
                        .filter(OperationBodyParameter::required)
                        .filter(parameter -> !bodyFields.containsKey(parameter.name()))
                        .findFirst()
                        .ifPresent(parameter -> {
                            throw new CliException(
                                    "REQUEST_BODY_PARAMETER_REQUIRED",
                                    "Missing required request body field: --" + parameter.name()
                            );
                        });
            }
            if (operation.effect() == Effect.WRITE && !commonOptions.toExecutionOptions().confirmed()
                    && !commonOptions.toExecutionOptions().dryRun()) {
                throw new CliException(
                        "CONFIRMATION_REQUIRED",
                        "This OpenAPI operation performs a write action. Re-run with --yes after user approval.",
                        Map.of("operation", operation.id())
                );
            }

            Map<String, Object> requestArguments = new LinkedHashMap<>(arguments);
            if (body != null || bodyFile != null || !bodyFields.isEmpty()) {
                requestArguments.put("body", body == null && bodyFile == null ? mapper.valueToTree(bodyFields) : readBody());
            }

            CommandContext context = contextFactory.create(getClass(), commonOptions.toExecutionOptions());
            if (context.options().dryRun()) {
                context.output().success(Map.of(
                        "dryRun", true,
                        "operation", operation.id(),
                        "method", operation.method(),
                        "arguments", requestArguments
                ));
                return 0;
            }

            Object result = context.call(operation.id(), requestArguments, Object.class);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("operation", operation.id());
            response.put("result", result);
            context.output().success(response);
            return 0;
        }

        private JsonNode readBody() throws IOException {
            String text = body != null ? body : Files.readString(bodyFile);
            try {
                return mapper.readTree(text);
            } catch (IOException error) {
                throw new CliException("REQUEST_BODY_INVALID", "Invalid JSON request body: " + error.getMessage());
            }
        }

        @Override
        public Map<String, Object> schemaMetadata() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("generated", true);
            metadata.put("kind", "openapi-operation");
            metadata.put("operation", operation.id());
            metadata.put("method", operation.method());
            metadata.put("risk", Map.of(
                    "effect", operation.effect().name(),
                    "confirmation", operation.effect() == Effect.WRITE ? "REQUIRED" : "NONE"
            ));
            return metadata;
        }
    }
}
