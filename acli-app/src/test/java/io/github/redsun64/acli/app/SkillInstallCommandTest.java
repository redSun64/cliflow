package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillInstallCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void installsStaticSkillToTargetDirectoryAndProtectsExistingFile() throws Exception {
        StringWriter output = new StringWriter();
        CommandLine root = root(output);

        assertEquals(0, root.execute("skills", "install", "--target", temporaryDirectory.toString(), "--command", "my-acli"));

        Path installed = temporaryDirectory.resolve("example-hr").resolve("SKILL.md");
        assertTrue(Files.exists(installed));
        String content = Files.readString(installed);
        assertTrue(content.contains("name: example-hr"));
        assertTrue(content.contains("Command prefix: `my-acli`."));
        assertTrue(content.contains("--dry-run"));
        assertTrue(content.contains("employee +onboard"));
        assertTrue(content.contains("--yes"));
        assertFalse(content.contains("{{ACLI_COMMAND}}"));
        assertTrue(output.toString().contains("\"scope\":\"target\""));

        assertEquals(2, root.execute("skills", "install", "--target", temporaryDirectory.toString()));
        assertEquals(0, root.execute("skills", "install", "--target", temporaryDirectory.toString(), "--force"));
    }

    @Test
    void dryRunDoesNotWriteSkill() {
        StringWriter output = new StringWriter();
        CommandLine root = root(output);

        assertEquals(0, root.execute("skills", "install", "--target", temporaryDirectory.toString(), "--dry-run"));

        assertFalse(Files.exists(temporaryDirectory.resolve("example-hr").resolve("SKILL.md")));
        assertTrue(output.toString().contains("\"dryRun\":true"));
    }

    @Test
    void defaultsToThePackagedLauncherWhenAvailable() throws Exception {
        Path launcher = Files.createFile(temporaryDirectory.resolve("acli.exe"));
        String previous = System.getProperty("jpackage.app-path");
        try {
            System.setProperty("jpackage.app-path", launcher.toString());
            StringWriter output = new StringWriter();

            assertEquals(0, root(output).execute("skills", "install", "--target", temporaryDirectory.resolve("skills").toString()));

            String expected = launcher.toAbsolutePath().normalize().toString();
            Path installed = temporaryDirectory.resolve("skills").resolve("example-hr").resolve("SKILL.md");
            assertTrue(output.toString().contains("\"command\":\"" + expected.replace("\\", "\\\\") + "\""));
            assertTrue(Files.readString(installed).contains("Command prefix: `" + expected + "`."));
        } finally {
            if (previous == null) {
                System.clearProperty("jpackage.app-path");
            } else {
                System.setProperty("jpackage.app-path", previous);
            }
        }
    }

    @Test
    void rejectsACommandThatWouldBreakTheSkillMarkdown() {
        CommandLine root = root(new StringWriter());

        assertEquals(2, root.execute("skills", "install", "--target", temporaryDirectory.toString(), "--command", "`unsafe`"));
        assertFalse(Files.exists(temporaryDirectory.resolve("openaction-cli").resolve("SKILL.md")));
    }

    private static CommandLine root(StringWriter output) {
        CommandLine root = new CommandLine(new AcliRootCommand());
        SkillsCommand.register(root, new ObjectMapper(), new PrintWriter(output, true));
        root.setExecutionExceptionHandler((exception, commandLine, parseResult) ->
                exception instanceof io.github.redsun64.acli.api.CliException ? 2 : 1
        );
        return root;
    }
}
