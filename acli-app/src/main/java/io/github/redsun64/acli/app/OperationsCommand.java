package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.core.operation.OperationDefinition;
import io.github.redsun64.acli.core.operation.OperationRegistry;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "operations", description = "List internal OpenAPI operations.")
public final class OperationsCommand implements Callable<Integer> {
    private final OperationRegistry operations;
    private final ObjectMapper mapper;
    private final PrintWriter out;

    public OperationsCommand(OperationRegistry operations, ObjectMapper mapper, PrintWriter out) {
        this.operations = operations;
        this.mapper = mapper;
        this.out = out;
    }

    @Override
    public Integer call() throws Exception {
        List<Map<String, Object>> rows = operations.all().stream()
                .map(this::toMap)
                .toList();
        out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows));
        out.flush();
        return 0;
    }

    private Map<String, Object> toMap(OperationDefinition operation) {
        return Map.of(
                "id", operation.id(),
                "method", operation.method(),
                "path", operation.path(),
                "effect", operation.effect().name(),
                "summary", operation.summary()
        );
    }
}
