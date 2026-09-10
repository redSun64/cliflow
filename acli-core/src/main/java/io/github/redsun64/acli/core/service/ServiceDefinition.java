package io.github.redsun64.acli.core.service;

public record ServiceDefinition(
        String id,
        String name,
        String baseUrl,
        String baseUrlEnv,
        String openapi,
        AuthDefinition auth
) {
    public ServiceDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("service.id is required");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("service.baseUrl is required: " + id);
        }
        if (openapi == null || openapi.isBlank()) {
            throw new IllegalArgumentException("service.openapi is required: " + id);
        }
        auth = auth == null ? new AuthDefinition("none", null, null, null, false) : auth;
    }

    public String resolvedBaseUrl() {
        if (baseUrlEnv != null && !baseUrlEnv.isBlank()) {
            String value = System.getenv(baseUrlEnv);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return baseUrl;
    }
}
