package io.github.redsun64.acli.api;

/** A module-owned, static Skill template installed by the SDK. */
public record CliSkill(String name, String templateResource) {
    public CliSkill {
        if (name == null || !name.matches("[a-z0-9][a-z0-9-]*")) {
            throw new IllegalArgumentException("skill name must use lowercase letters, digits, and hyphens");
        }
        if (templateResource == null || templateResource.isBlank()) {
            throw new IllegalArgumentException("skill template resource is required");
        }
    }
}
