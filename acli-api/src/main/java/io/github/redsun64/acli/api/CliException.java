package io.github.redsun64.acli.api;

import java.util.Map;

public final class CliException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;

    public CliException(String code, String message) {
        this(code, message, Map.of());
    }

    public CliException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
