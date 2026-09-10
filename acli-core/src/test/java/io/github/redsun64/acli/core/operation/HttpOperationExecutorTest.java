package io.github.redsun64.acli.core.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.Effect;
import io.github.redsun64.acli.api.ExecutionOptions;
import io.github.redsun64.acli.api.OutputFormat;
import io.github.redsun64.acli.core.service.AuthDefinition;
import io.github.redsun64.acli.core.service.ServiceDefinition;
import io.github.redsun64.acli.core.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpOperationExecutorTest {

    @Test
    void executesHttpRequestWithoutNioHttpClient() throws Exception {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            AtomicReference<String> requestLine = new AtomicReference<>();
            AtomicReference<Throwable> serverFailure = new AtomicReference<>();
            Thread serverThread = Thread.ofVirtual().start(() -> serveOneRequest(server, requestLine, serverFailure));

            ServiceRegistry services = new ServiceRegistry();
            services.register(new ServiceDefinition(
                    "hr",
                    "HR",
                    "http://127.0.0.1:" + server.getLocalPort(),
                    null,
                    "classpath:openapi/hr.yaml",
                    new AuthDefinition("none", null, null, null, false)
            ));
            OperationDefinition operation = new OperationDefinition(
                    "hr.getEmployee",
                    "hr",
                    "getEmployee",
                    "GET",
                    "/employees/{id}",
                    List.of(
                            new OperationParameter("id", "path", true),
                            new OperationParameter("active", "query", false)
                    ),
                    List.of("employee"),
                    false,
                    false,
                    List.of(),
                    Effect.READ,
                    "Get employee"
            );

            var response = new HttpOperationExecutor(services, new ObjectMapper()).execute(
                    operation,
                    Map.of("id", "42", "active", true),
                    new ExecutionOptions(OutputFormat.JSON, false, false)
            );

            serverThread.join(2_000);
            assertFalse(serverThread.isAlive());
            assertNull(serverFailure.get());
            assertEquals("GET /employees/42?active=true HTTP/1.1", requestLine.get());
            assertTrue(response.isNull());
        }
    }

    private static void serveOneRequest(
            ServerSocket server,
            AtomicReference<String> requestLine,
            AtomicReference<Throwable> serverFailure) {
        try (Socket socket = server.accept()) {
            socket.setSoTimeout(2_000);
            requestLine.set(readHeaders(socket).split("\\r\\n", 2)[0]);
            String headers = "HTTP/1.1 204 No Content\\r\\nConnection: close\\r\\n\\r\\n";
            socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
        } catch (Throwable error) {
            serverFailure.set(error);
        }
    }

    private static String readHeaders(Socket socket) throws IOException {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = socket.getInputStream().read()) != -1) {
            captured.write(current);
            if (previous == '\r' && current == '\n' && captured.size() >= 4) {
                byte[] bytes = captured.toByteArray();
                int length = bytes.length;
                if (bytes[length - 4] == '\r' && bytes[length - 3] == '\n') {
                    break;
                }
            }
            previous = current;
        }
        return captured.toString(StandardCharsets.US_ASCII);
    }
}
