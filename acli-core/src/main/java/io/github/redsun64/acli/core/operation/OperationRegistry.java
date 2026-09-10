package io.github.redsun64.acli.core.operation;

import io.github.redsun64.acli.api.CliException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OperationRegistry {
    private final Map<String, OperationDefinition> operations = new LinkedHashMap<>();

    public void register(OperationDefinition operation) {
        OperationDefinition previous = operations.putIfAbsent(operation.id(), operation);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate operation id: " + operation.id());
        }
    }

    public OperationDefinition get(String id) {
        OperationDefinition operation = operations.get(id);
        if (operation == null) {
            throw new CliException("OPERATION_NOT_FOUND", "Unknown operation: " + id);
        }
        return operation;
    }

    public Collection<OperationDefinition> all() {
        return operations.values();
    }
}
