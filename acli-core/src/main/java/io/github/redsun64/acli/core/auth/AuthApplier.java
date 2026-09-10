package io.github.redsun64.acli.core.auth;

import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.core.service.AuthDefinition;

import java.net.http.HttpRequest;
import java.util.Locale;

public final class AuthApplier {

    public void apply(AuthDefinition auth, HttpRequest.Builder request) {
        String type = auth.type().toLowerCase(Locale.ROOT);
        if ("none".equals(type)) {
            return;
        }

        if (auth.env() == null || auth.env().isBlank()) {
            throw new CliException("AUTH_CONFIG_INVALID", "auth.env is required for auth type: " + type);
        }

        String secret = System.getenv(auth.env());
        if (secret == null || secret.isBlank()) {
            if (auth.required()) {
                throw new CliException(
                        "AUTH_CREDENTIAL_MISSING",
                        "Missing required credential environment variable: " + auth.env()
                );
            }
            return;
        }

        switch (type) {
            case "bearer-env" -> request.header(
                    auth.header(),
                    auth.prefix().isBlank() ? "Bearer " + secret : auth.prefix() + secret
            );
            case "api-key-env" -> request.header(auth.header(), auth.prefix() + secret);
            default -> throw new CliException("AUTH_TYPE_UNSUPPORTED", "Unsupported auth type: " + auth.type());
        }
    }
}
