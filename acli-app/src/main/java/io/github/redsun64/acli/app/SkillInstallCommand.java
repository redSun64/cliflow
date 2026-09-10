package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.CliModule;
import io.github.redsun64.acli.api.CliSkill;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

@Command(name = "install", description = "Install the embedded Cliflow Skill for an agent.")
public final class SkillInstallCommand implements Callable<Integer> {
    enum Scope {
        user,
        repo
    }

    @Option(
            names = "--scope",
            defaultValue = "user",
            description = "Installation scope: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})."
    )
    private Scope scope;

    @Option(
            names = "--target",
            paramLabel = "SKILLS_DIR",
            description = "Override the scope and install below this skills directory."
    )
    private Path target;

    @Option(
            names = "--command",
            paramLabel = "COMMAND",
            description = "Command prefix written into the Skill. Defaults to the current packaged launcher, otherwise cliflow."
    )
    private String command;

    @Option(names = "--force", description = "Replace an existing installed Skill.")
    private boolean force;

    @Option(names = "--dry-run", description = "Show the installation plan without writing a file.")
    private boolean dryRun;

    private final ObjectMapper mapper;
    private final PrintWriter out;
    private final CliSkill skill;

    SkillInstallCommand(ObjectMapper mapper, PrintWriter out) {
        this(mapper, out, loadSkill());
    }

    SkillInstallCommand(ObjectMapper mapper, PrintWriter out, CliSkill skill) {
        this.mapper = mapper;
        this.out = out;
        this.skill = skill;
    }

    @Override
    public Integer call() throws Exception {
        String commandPrefix = resolveCommand(command);
        Path destination = resolveDestination();
        boolean exists = Files.exists(destination);
        if (exists && !force) {
            throw new CliException(
                    "SKILL_ALREADY_EXISTS",
                    "Skill already exists at " + destination + ". Re-run with --force to replace it.",
                    Map.of("path", destination.toString())
            );
        }

        if (!dryRun) {
            writeAtomically(destination, renderTemplate(commandPrefix));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skill", skill.name());
        data.put("path", destination.toString());
        data.put("scope", target == null ? scope.name() : "target");
        data.put("command", commandPrefix);
        data.put("overwritten", exists);
        data.put("dryRun", dryRun);
        out.println(mapper.writeValueAsString(Map.of("ok", true, "data", data)));
        out.flush();
        return 0;
    }

    private Path resolveDestination() {
        Path skillsDirectory;
        if (target != null) {
            skillsDirectory = target;
        } else if (scope == Scope.repo) {
            skillsDirectory = Path.of("").toAbsolutePath().resolve(".agents").resolve("skills");
        } else {
            skillsDirectory = Path.of(System.getProperty("user.home")).resolve(".agents").resolve("skills");
        }
        return skillsDirectory.toAbsolutePath().normalize().resolve(skill.name()).resolve("SKILL.md");
    }

    private static String validateCommand(String command) {
        if (command == null || command.isBlank() || command.contains("\n") || command.contains("\r") || command.contains("`")) {
            throw new CliException(
                    "SKILL_COMMAND_INVALID",
                    "--command must be a non-empty, single-line command prefix without backticks."
            );
        }
        return command.trim();
    }

    private static String resolveCommand(String configuredCommand) {
        if (configuredCommand != null) {
            return validateCommand(configuredCommand);
        }

        String packagedLauncher = System.getProperty("jpackage.app-path");
        if (packagedLauncher != null && !packagedLauncher.isBlank()) {
            try {
                Path launcher = Path.of(packagedLauncher).toAbsolutePath().normalize();
                if (Files.isRegularFile(launcher)) {
                    return validateCommand(launcher.toString());
                }
            } catch (InvalidPathException ignored) {
                // Fall back to the PATH command below.
            }
        }
        return "cliflow";
    }

    private String renderTemplate(String commandPrefix) {
            try (InputStream input = SkillInstallCommand.class.getResourceAsStream(skill.templateResource())) {
            if (input == null) {
                throw new CliException("SKILL_TEMPLATE_MISSING", "Embedded Skill template is missing.");
            }
            String template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{ACLI_COMMAND}}", commandPrefix);
        } catch (IOException e) {
            throw new CliException("SKILL_TEMPLATE_READ_FAILED", "Unable to read embedded Skill template: " + e.getMessage());
        }
    }

    private void writeAtomically(Path destination, String content) {
        Path parent = destination.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, skill.name() + "-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CliException(
                    "SKILL_INSTALL_FAILED",
                    "Unable to install Skill at " + destination + ": " + e.getMessage(),
                    Map.of("path", destination.toString())
            );
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The installation error above is more useful than a cleanup error.
                }
            }
        }
    }

    private static CliSkill loadSkill() {
        try {
            List<CliSkill> skills = ServiceLoader.load(CliModule.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .flatMap(module -> module.skill().stream())
                    .toList();
            if (skills.size() != 1) {
                throw new CliException(
                        "SKILL_DEFINITION_REQUIRED",
                        "A standalone business CLI must provide exactly one CliSkill through CliModule.",
                        Map.of("count", skills.size())
                );
            }
            return skills.getFirst();
        } catch (ServiceConfigurationError error) {
            throw new CliException("MODULE_LOAD_FAILED", "Unable to load a business CLI module: " + error.getMessage());
        }
    }
}
