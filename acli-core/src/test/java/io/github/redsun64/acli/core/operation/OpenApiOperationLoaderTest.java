package io.github.redsun64.acli.core.operation;

import io.github.redsun64.acli.api.Effect;
import io.github.redsun64.acli.core.service.AuthDefinition;
import io.github.redsun64.acli.core.service.ServiceDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiOperationLoaderTest {

    @Test
    void loadsOperationsWithServicePrefix() {
        String spec = """
                openapi: 3.0.3
                info:
                  title: demo
                  version: 1.0.0
                paths:
                  /users/{id}:
                    get:
                      operationId: getUser
                      tags: [user]
                      parameters:
                        - in: path
                          name: id
                          required: true
                          schema: { type: string }
                      responses:
                        '200':
                          description: ok
                """;

        ServiceDefinition service = new ServiceDefinition(
                "hr",
                "HR",
                "http://localhost",
                null,
                "classpath:openapi/hr.yaml",
                new AuthDefinition("none", null, null, null, false)
        );

        List<OperationDefinition> operations = new OpenApiOperationLoader().load(service, spec);

        assertEquals(1, operations.size());
        assertEquals("hr.getUser", operations.getFirst().id());
        assertEquals(Effect.READ, operations.getFirst().effect());
        assertEquals(List.of("user"), operations.getFirst().tags());
        assertFalse(operations.getFirst().requestBodyRequired());
        assertTrue(operations.getFirst().parameters().getFirst().required());
    }

    @Test
    void derivesOperationIdFromMethodAndPathWhenOpenApiDoesNotProvideOne() {
        String spec = """
                openapi: 3.0.3
                info: { title: demo, version: 1.0.0 }
                paths:
                  /airag/knowledge/doc/progress:
                    get:
                      responses:
                        '200': { description: ok }
                """;

        List<OperationDefinition> operations = new OpenApiOperationLoader().load(service(), spec);

        assertEquals(1, operations.size());
        assertEquals("getAiragKnowledgeDocProgress", operations.getFirst().operationId());
        assertEquals("hr.getAiragKnowledgeDocProgress", operations.getFirst().id());
    }

    @Test
    void rejectsDuplicateResolvedOperationIds() {
        String spec = """
                openapi: 3.0.3
                info: { title: demo, version: 1.0.0 }
                paths:
                  /users:
                    get:
                      operationId: getUsers
                      responses:
                        '200': { description: ok }
                  /users/:
                    get:
                      responses:
                        '200': { description: ok }
                """;

        assertEquals("OPENAPI_OPERATION_ID_DUPLICATE", assertThrows(
                io.github.redsun64.acli.api.CliException.class,
                () -> new OpenApiOperationLoader().load(service(), spec)
        ).code());
    }

    @Test
    void exposesTopLevelScalarRequestBodyFields() {
        String spec = """
                openapi: 3.0.3
                info: { title: demo, version: 1.0.0 }
                paths:
                  /knowledge/page:
                    post:
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              type: object
                              required: [page]
                              properties:
                                page: { type: integer }
                                keyword: { type: string }
                                labels: { type: array, items: { type: string } }
                      responses:
                        '200': { description: ok }
                """;

        List<OperationBodyParameter> parameters = new OpenApiOperationLoader().load(service(), spec)
                .getFirst()
                .bodyParameters();

        assertEquals(List.of(
                new OperationBodyParameter("page", "integer", true),
                new OperationBodyParameter("keyword", "string", false)
        ), parameters);
    }

    private static ServiceDefinition service() {
        return new ServiceDefinition(
                "hr",
                "HR",
                "http://localhost",
                null,
                "classpath:openapi/hr.yaml",
                new AuthDefinition("none", null, null, null, false)
        );
    }
}
