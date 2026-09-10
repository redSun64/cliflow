package io.github.redsun64.acli.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.redsun64.acli.api.CliException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class ServiceConfigLoader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public ServiceCatalog loadClasspath(String resourcePath) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new CliException("CONFIG_NOT_FOUND", "Classpath resource not found: " + resourcePath);
            }
            return yamlMapper.readValue(input, ServiceCatalog.class);
        } catch (IOException e) {
            throw new CliException("CONFIG_INVALID", "Failed to load service config: " + e.getMessage());
        }
    }

    /** Loads every module-owned service descriptor at the same classpath location. */
    public List<ServiceCatalog> loadClasspathAll(String resourcePath) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            List<ServiceCatalog> catalogs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream input = resource.openStream()) {
                    catalogs.add(yamlMapper.readValue(input, ServiceCatalog.class));
                }
            }
            return List.copyOf(catalogs);
        } catch (IOException e) {
            throw new CliException("CONFIG_INVALID", "Failed to load service config: " + e.getMessage());
        }
    }
}
