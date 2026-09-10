package io.github.redsun64.acli.core.service;

public record AuthDefinition(
        String type,
        String env,
        String header,
        String prefix,
        boolean required
) {
    public AuthDefinition {
        type = type == null ? "none" : type;
        header = header == null ? "Authorization" : header;
        prefix = prefix == null ? "" : prefix;
    }
}
