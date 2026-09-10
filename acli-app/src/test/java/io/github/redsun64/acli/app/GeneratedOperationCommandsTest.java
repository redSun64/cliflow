package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CommandContext;
import io.github.redsun64.acli.api.CommandContextFactory;
import io.github.redsun64.acli.api.CommandOutput;
import io.github.redsun64.acli.api.Effect;
import io.github.redsun64.acli.api.ExecutionOptions;
import io.github.redsun64.acli.core.operation.OperationDefinition;
import io.github.redsun64.acli.core.operation.OperationBodyParameter;
import io.github.redsun64.acli.core.operation.OperationParameter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedOperationCommandsTest {

    @Test
    void exposesOpenApiCommandsProgressivelyAndDispatchesReadOperations() {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter schemaOutput = new StringWriter();
        CapturingContextFactory factory = new CapturingContextFactory();
        CommandLine root = new CommandLine(new AcliRootCommand());
        CommandLine operations = new CommandLine(new OperationsCommand(
                new io.github.redsun64.acli.core.operation.OperationRegistry(), mapper, new PrintWriter(new StringWriter())));
        GeneratedOperationCommands.register(
                operations,
                List.of(operation("getCustomer", "GET", false), operation("createCustomer", "POST", true)),
                factory,
                mapper
        );
        root.addSubcommand("operations", operations);
        root.addSubcommand("schema", new SchemaCommand(root, mapper, new PrintWriter(schemaOutput, true)));

        assertEquals(0, root.execute("schema"));
        assertFalse(schemaOutput.toString().contains("operations"));

        schemaOutput.getBuffer().setLength(0);
        assertEquals(0, root.execute("schema", "operations", "crm"));
        assertTrue(schemaOutput.toString().contains("customer"));
        assertFalse(schemaOutput.toString().contains("getCustomer"));

        schemaOutput.getBuffer().setLength(0);
        assertEquals(0, root.execute("schema", "operations", "crm", "customer", "getCustomer"));
        assertTrue(schemaOutput.toString().contains("--id"));
        assertTrue(schemaOutput.toString().contains("crm.getCustomer"));

        assertEquals(0, root.execute("operations", "crm", "customer", "getCustomer", "--id", "42", "--format", "json"));
        assertEquals("crm.getCustomer", factory.operationId);
        assertEquals("42", factory.arguments.get("id"));

        assertEquals(0, root.execute("operations", "crm", "customer", "createCustomer", "--body", "{}", "--dry-run"));
        assertEquals("crm.getCustomer", factory.operationId);

        assertEquals(0, root.execute("operations", "crm", "customer", "createCustomer", "--name", "Ada", "--page", "2", "--yes"));
        assertEquals("crm.createCustomer", factory.operationId);
        com.fasterxml.jackson.databind.JsonNode body = (com.fasterxml.jackson.databind.JsonNode) factory.arguments.get("body");
        assertEquals("Ada", body.get("name").asText());
        assertEquals(2, body.get("page").asInt());
    }

    private static OperationDefinition operation(String operationId, String method, boolean requestBody) {
        return new OperationDefinition(
                "crm." + operationId,
                "crm",
                operationId,
                method,
                requestBody ? "/customers" : "/customers/{id}",
                requestBody ? List.of() : List.of(new OperationParameter("id", "path", true)),
                List.of("customer"),
                requestBody,
                requestBody,
                requestBody ? List.of(
                        new OperationBodyParameter("name", "string", true),
                        new OperationBodyParameter("page", "integer", false)
                ) : List.of(),
                "GET".equals(method) ? Effect.READ : Effect.WRITE,
                operationId
        );
    }

    private static final class CapturingContextFactory implements CommandContextFactory {
        private String operationId;
        private Map<String, ?> arguments = Map.of();

        @Override
        public CommandContext create(Class<?> commandType, ExecutionOptions options) {
            return new CommandContext() {
                @Override
                public <T> T call(String id, Map<String, ?> values, Class<T> resultType) {
                    operationId = id;
                    arguments = new LinkedHashMap<>(values);
                    return resultType.cast(Map.of());
                }

                @Override
                public Map<String, Object> call(String id, Map<String, ?> values) {
                    operationId = id;
                    arguments = new LinkedHashMap<>(values);
                    return Map.of();
                }

                @Override
                public CommandOutput output() {
                    return ignored -> { };
                }

                @Override
                public ExecutionOptions options() {
                    return options;
                }
            };
        }
    }
}
