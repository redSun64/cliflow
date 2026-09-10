package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CommandContextFactory;
import io.github.redsun64.acli.core.operation.HttpOperationExecutor;
import io.github.redsun64.acli.core.operation.OpenApiOperationLoader;
import io.github.redsun64.acli.core.operation.OperationRegistry;
import io.github.redsun64.acli.core.resource.ClasspathResourceLoader;
import io.github.redsun64.acli.core.runtime.DefaultCommandContextFactory;
import io.github.redsun64.acli.core.service.ServiceCatalog;
import io.github.redsun64.acli.core.service.ServiceConfigLoader;
import io.github.redsun64.acli.core.service.ServiceDefinition;
import io.github.redsun64.acli.core.service.ServiceRegistry;

import java.io.PrintWriter;
import java.util.List;

public final class RuntimeBootstrap {
    private static final String MODULE_SERVICES_RESOURCE = "META-INF/cliflow/services.yaml";
    private static final String DEMO_SERVICES_RESOURCE = "services/services.yaml";
    private final ObjectMapper mapper;
    private final ServiceRegistry services;
    private final OperationRegistry operations;
    private final CommandContextFactory contextFactory;

    private RuntimeBootstrap(
            ObjectMapper mapper,
            ServiceRegistry services,
            OperationRegistry operations,
            CommandContextFactory contextFactory) {
        this.mapper = mapper;
        this.services = services;
        this.operations = operations;
        this.contextFactory = contextFactory;
    }

    public static RuntimeBootstrap create(PrintWriter out) {
        ObjectMapper mapper = new ObjectMapper();
        ServiceRegistry services = new ServiceRegistry();
        OperationRegistry operations = new OperationRegistry();

        ServiceConfigLoader serviceConfigLoader = new ServiceConfigLoader();
        List<ServiceCatalog> catalogs = serviceConfigLoader.loadClasspathAll(MODULE_SERVICES_RESOURCE);
        if (catalogs.isEmpty()) {
            // Retain the repository's bundled example, while published business CLIs use only their module descriptor.
            catalogs = List.of(serviceConfigLoader.loadClasspath(DEMO_SERVICES_RESOURCE));
        }
        ClasspathResourceLoader resources = new ClasspathResourceLoader();
        OpenApiOperationLoader operationLoader = new OpenApiOperationLoader();

        for (ServiceCatalog catalog : catalogs) {
            for (ServiceDefinition service : catalog.services()) {
                services.register(service);
                String openApiText = resources.read(service.openapi());
                operationLoader.load(service, openApiText).forEach(operations::register);
            }
        }

        HttpOperationExecutor executor = new HttpOperationExecutor(services, mapper);
        CommandContextFactory contextFactory = new DefaultCommandContextFactory(
                operations,
                executor,
                mapper,
                out
        );

        return new RuntimeBootstrap(mapper, services, operations, contextFactory);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public ServiceRegistry services() {
        return services;
    }

    public OperationRegistry operations() {
        return operations;
    }

    public CommandContextFactory contextFactory() {
        return contextFactory;
    }
}
