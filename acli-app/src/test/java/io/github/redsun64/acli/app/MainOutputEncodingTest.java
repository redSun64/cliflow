package io.github.redsun64.acli.app;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MainOutputEncodingTest {

    @Test
    void writesUtf8BytesEvenWhenTheUnderlyingConsoleUsesGbk() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream windowsConsole = new PrintStream(bytes, true, Charset.forName("GBK"))) {
            PrintWriter out = Main.utf8Writer(windowsConsole);
            out.print("项目三同时");
            out.flush();
        }

        assertArrayEquals("项目三同时".getBytes(StandardCharsets.UTF_8), bytes.toByteArray());
    }
}
