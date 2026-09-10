package io.github.redsun64.acli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.PrintWriter;

@Command(name = "skills", description = "Install the Cliflow agent Skill.")
public final class SkillsCommand implements Runnable {

    private SkillsCommand() {
    }

    public static void register(CommandLine root, ObjectMapper mapper, PrintWriter out) {
        CommandLine skills = new CommandLine(new SkillsCommand());
        skills.addSubcommand("install", new SkillInstallCommand(mapper, out));
        root.addSubcommand("skills", skills);
    }

    @Override
    public void run() {
        // Picocli prints usage when no skill subcommand is selected.
    }
}
