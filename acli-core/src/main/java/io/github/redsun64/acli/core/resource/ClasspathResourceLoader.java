package io.github.redsun64.acli.core.resource;

import io.github.redsun64.acli.api.CliException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ClasspathResourceLoader {

    public String read(String location) {
        if (!location.startsWith("classpath:")) {
            throw new CliException(
                    "RESOURCE_SCHEME_UNSUPPORTED",
                    "Only classpath: resources are supported in the MVP: " + location
            );
        }

        String path = location.substring("classpath:".length());
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path)) {
            if (input == null) {
                throw new CliException("RESOURCE_NOT_FOUND", "Resource not found: " + location);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CliException("RESOURCE_READ_FAILED", "Failed to read " + location + ": " + e.getMessage());
        }
    }
}
