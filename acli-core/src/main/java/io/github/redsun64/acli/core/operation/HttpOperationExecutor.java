package io.github.redsun64.acli.core.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.Effect;
import io.github.redsun64.acli.api.ExecutionOptions;
import io.github.redsun64.acli.core.auth.AuthApplier;
import io.github.redsun64.acli.core.service.ServiceDefinition;
import io.github.redsun64.acli.core.service.ServiceRegistry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class HttpOperationExecutor implements OperationExecutor {
    private final ServiceRegistry services;
    private final ObjectMapper mapper;
    private final AuthApplier authApplier;

    public HttpOperationExecutor(ServiceRegistry services, ObjectMapper mapper) {
        this.services = services;
        this.mapper = mapper;
        this.authApplier = new AuthApplier();
    }

    @Override
    public JsonNode execute(
            OperationDefinition operation,
            Map<String, ?> arguments,
            ExecutionOptions options) {

        if (options.dryRun() && operation.effect() == Effect.WRITE) {
            throw new CliException(
                    "DRY_RUN_WRITE_BLOCKED",
                    "Dry-run blocked write operation: " + operation.id(),
                    Map.of("operation", operation.id())
            );
        }

        ServiceDefinition service = services.get(operation.serviceId());
        Map<String, Object> remaining = new LinkedHashMap<>();
        arguments.forEach((key, value) -> remaining.put(key, value));

        String path = operation.path();
        StringJoiner query = new StringJoiner("&");
        HttpRequest.Builder builder;

        Map<String, String> headers = new LinkedHashMap<>();
        for (OperationParameter parameter : operation.parameters()) {
            Object value = arguments.get(parameter.name());
            if (value == null) {
                if (parameter.required()) {
                    throw new CliException(
                            "OPERATION_ARGUMENT_REQUIRED",
                            "Missing argument '" + parameter.name() + "' for " + operation.id()
                    );
                }
                continue;
            }
            remaining.remove(parameter.name());
            switch (parameter.in()) {
                case "path" -> path = path.replace(
                        "{" + parameter.name() + "}",
                        encodePath(String.valueOf(value))
                );
                case "query" -> query.add(encodeQuery(parameter.name()) + "=" + encodeQuery(String.valueOf(value)));
                case "header" -> headers.put(parameter.name(), String.valueOf(value));
                default -> throw new CliException(
                        "OPENAPI_PARAMETER_LOCATION_UNSUPPORTED",
                        "Unsupported parameter location '" + parameter.in() + "' in " + operation.id()
                );
            }
        }

        String baseUrl = stripTrailingSlash(service.resolvedBaseUrl());
        String url = baseUrl + path + (query.length() == 0 ? "" : "?" + query);
        builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30));
        headers.forEach(builder::header);
        authApplier.apply(service.auth(), builder);

        String requestBody = null;
        if (operation.requestBody()) {
            Object body = arguments.containsKey("body") ? arguments.get("body") : remaining;
            try {
                requestBody = mapper.writeValueAsString(body);
                builder.header("Content-Type", "application/json");
            } catch (IOException e) {
                throw new CliException("REQUEST_SERIALIZATION_FAILED", e.getMessage());
            }
        }

        try {
            HttpRequest.BodyPublisher bodyPublisher = requestBody == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(requestBody);
            HttpRequest request = builder.method(operation.method(), bodyPublisher).build();

            // HttpClient starts a Windows NIO selector even for offline commands. The
            // blocking JDK transport keeps the packaged CLI usable in those environments.
            HttpURLConnection connection = (HttpURLConnection) request.uri().toURL().openConnection();
            connection.setRequestMethod(request.method());
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
            connection.setInstanceFollowRedirects(false);
            request.headers().map().forEach((name, values) ->
                    values.forEach(value -> connection.addRequestProperty(name, value))
            );
            if (requestBody != null) {
                connection.setDoOutput(true);
                try (var output = connection.getOutputStream()) {
                    output.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            }

            int statusCode = connection.getResponseCode();
            String responseBody;
            try (var input = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                responseBody = input == null ? "" : new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new CliException(
                        "HTTP_ERROR",
                        "Operation " + operation.id() + " returned HTTP " + statusCode,
                        Map.of(
                                "operation", operation.id(),
                                "status", statusCode,
                                "body", responseBody
                        )
                );
            }

            if (statusCode == 204 || responseBody.isBlank()) {
                return NullNode.getInstance();
            }

            String contentType = connection.getHeaderField("Content-Type");
            if ((contentType != null && contentType.contains("json")) || looksLikeJson(responseBody)) {
                return mapper.readTree(responseBody);
            }
            return TextNode.valueOf(responseBody);
        } catch (IOException e) {
            throw new CliException("HTTP_IO_ERROR", "HTTP request failed for " + operation.id() + ": " + e.getMessage());
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static boolean looksLikeJson(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[") || "null".equals(trimmed);
    }
}
