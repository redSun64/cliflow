package io.github.redsun64.acli.core.service;

import io.github.redsun64.acli.api.CliException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServiceRegistry {
    private final Map<String, ServiceDefinition> services = new LinkedHashMap<>();

    public void register(ServiceDefinition service) {
        ServiceDefinition existing = services.putIfAbsent(service.id(), service);
        if (existing != null) {
            throw new IllegalArgumentException("Duplicate service id: " + service.id());
        }
    }

    public ServiceDefinition get(String id) {
        ServiceDefinition service = services.get(id);
        if (service == null) {
            throw new CliException("SERVICE_NOT_FOUND", "Unknown service: " + id);
        }
        return service;
    }

    public Collection<ServiceDefinition> all() {
        return services.values();
    }
}
