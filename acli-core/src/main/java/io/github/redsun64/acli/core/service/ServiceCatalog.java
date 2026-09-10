package io.github.redsun64.acli.core.service;

import java.util.List;

public record ServiceCatalog(List<ServiceDefinition> services) {
    public ServiceCatalog {
        services = services == null ? List.of() : List.copyOf(services);
    }
}
