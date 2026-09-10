package io.github.redsun64.acli.core.operation;

import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.Effect;
import io.github.redsun64.acli.core.service.ServiceDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OpenApiOperationLoader {

    public List<OperationDefinition> load(ServiceDefinition service, String openApiText) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(false);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(openApiText, null, options);
        OpenAPI api = result.getOpenAPI();
        if (api == null) {
            throw new CliException(
                    "OPENAPI_INVALID",
                    "Unable to parse OpenAPI for service " + service.id() + ": " + String.join("; ", result.getMessages())
            );
        }

        List<OperationDefinition> definitions = new ArrayList<>();
        if (api.getPaths() == null) {
            return definitions;
        }
        Map<String, String> routesByOperationId = new java.util.LinkedHashMap<>();

        api.getPaths().forEach((path, pathItem) -> {
            Map<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operations = pathItem.readOperationsMap();
            operations.forEach((method, operation) -> {
                String operationId = resolveOperationId(operation.getOperationId(), method, path);
                String route = method + " " + path;
                String previousRoute = routesByOperationId.putIfAbsent(operationId, route);
                if (previousRoute != null) {
                    throw new CliException(
                            "OPENAPI_OPERATION_ID_DUPLICATE",
                            "Duplicate resolved operationId '" + operationId + "' for service " + service.id()
                                    + ": " + previousRoute + " and " + route
                    );
                }

                List<OperationParameter> parameters = new ArrayList<>();
                addParameters(parameters, pathItem.getParameters());
                addParameters(parameters, operation.getParameters());
                List<OperationBodyParameter> bodyParameters = bodyParameters(api, operation);

                String httpMethod = method.name().toUpperCase(Locale.ROOT);
                definitions.add(new OperationDefinition(
                        service.id() + "." + operationId,
                        service.id(),
                        operationId,
                        httpMethod,
                        path,
                        parameters,
                        operation.getTags(),
                        operation.getRequestBody() != null,
                        operation.getRequestBody() != null && Boolean.TRUE.equals(operation.getRequestBody().getRequired()),
                        bodyParameters,
                        effectOf(httpMethod),
                        operation.getSummary()
                ));
            });
        });
        return definitions;
    }

    private static String resolveOperationId(String explicitOperationId, PathItem.HttpMethod method, String path) {
        if (explicitOperationId != null && !explicitOperationId.isBlank()) {
            return explicitOperationId.trim();
        }

        StringBuilder generated = new StringBuilder(method.name().toLowerCase(Locale.ROOT));
        for (String segment : path.split("[^A-Za-z0-9]+")) {
            if (!segment.isBlank()) {
                generated.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
            }
        }
        return generated.toString();
    }

    private static List<OperationBodyParameter> bodyParameters(
            OpenAPI api,
            io.swagger.v3.oas.models.Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return List.of();
        }

        Schema<?> schema = resolveSchema(api, jsonSchema(operation.getRequestBody().getContent()));
        if (schema == null || schema.getProperties() == null) {
            return List.of();
        }

        List<String> required = schema.getRequired() == null ? List.of() : schema.getRequired();
        List<OperationBodyParameter> parameters = new ArrayList<>();
        schema.getProperties().forEach((name, property) -> {
            Schema<?> propertySchema = resolveSchema(api, property);
            if (propertySchema != null && isScalarType(propertySchema.getType())) {
                parameters.add(new OperationBodyParameter(name, propertySchema.getType(), required.contains(name)));
            }
        });
        return parameters;
    }

    private static Schema<?> jsonSchema(Content content) {
        MediaType json = content.get("application/json");
        if (json != null) {
            return json.getSchema();
        }
        return content.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).startsWith("application/json"))
                .map(Map.Entry::getValue)
                .map(MediaType::getSchema)
                .findFirst()
                .orElse(null);
    }

    private static Schema<?> resolveSchema(OpenAPI api, Schema<?> schema) {
        if (schema == null || schema.get$ref() == null || api.getComponents() == null || api.getComponents().getSchemas() == null) {
            return schema;
        }
        String prefix = "#/components/schemas/";
        if (!schema.get$ref().startsWith(prefix)) {
            return schema;
        }
        return api.getComponents().getSchemas().get(schema.get$ref().substring(prefix.length()));
    }

    private static boolean isScalarType(String type) {
        return "string".equals(type) || "integer".equals(type) || "number".equals(type) || "boolean".equals(type);
    }

    private static void addParameters(List<OperationParameter> target, List<Parameter> parameters) {
        if (parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            target.add(new OperationParameter(
                    parameter.getName(),
                    parameter.getIn(),
                    Boolean.TRUE.equals(parameter.getRequired())
            ));
        }
    }

    private static Effect effectOf(String method) {
        return switch (method) {
            case "GET", "HEAD", "OPTIONS" -> Effect.READ;
            default -> Effect.WRITE;
        };
    }
}
