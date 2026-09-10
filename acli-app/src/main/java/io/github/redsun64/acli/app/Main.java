package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CliException;
import picocli.CommandLine;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        PrintWriter out = utf8Writer(System.out);
        PrintWriter err = utf8Writer(System.err);

        try {
            RuntimeBootstrap runtime = RuntimeBootstrap.create(out);
            ObjectMapper mapper = runtime.mapper();

            CommandLine root = CommandCatalog.create(runtime, out);

            root.setExecutionExceptionHandler((exception, commandLine, parseResult) -> {
                if (exception instanceof CliException cliException) {
                    writeError(mapper, err, cliException);
                    return 2;
                }
                err.println(exception.getMessage());
                return 1;
            });

            if (args.length == 0) {
                root.usage(out);
                return 0;
            }
            return root.execute(args);
        } catch (CliException e) {
            writeError(new ObjectMapper(), err, e);
            return 2;
        } catch (Exception e) {
            err.println("Fatal: " + e.getMessage());
            return 1;
        }
    }

    static PrintWriter utf8Writer(OutputStream stream) {
        return new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8), true);
    }

    private static void writeError(ObjectMapper mapper, PrintWriter err, CliException exception) {
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", exception.code());
            error.put("message", exception.getMessage());
            error.put("details", exception.details());
            err.println(mapper.writeValueAsString(Map.of("ok", false, "error", error)));
        } catch (Exception serializationError) {
            err.println(exception.code() + ": " + exception.getMessage());
        }
    }
}
