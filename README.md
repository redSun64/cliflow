<p align="center">
  <img src="docs/assets/cliflow-wordmark.svg" alt="Cliflow" width="720" />
</p>

<p align="center">
  <a href="https://github.com/redSun64/cliflow/actions/workflows/ci.yml"><img src="https://github.com/redSun64/cliflow/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/redSun64/cliflow" alt="License" /></a>
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21" /></a>
</p>

**Cliflow is a Java framework for turning APIs into composable, AI-friendly CLI commands.**

Cliflow keeps HTTP transport details in OpenAPI, puts business workflows in Java, and exposes a stable command surface to both people and agents.

> **Cliflow** is the project name and `cliflow` is its command.

[简体中文](README_CN.md) · [Architecture](docs/architecture.md) · [Business CLI guide](docs/business-onboarding.md) · [Contributing](CONTRIBUTING.md)

## Why Cliflow?

An agent needs more than a list of HTTP endpoints. It needs business intent, risk metadata, confirmation rules, predictable JSON, and a clear home for multi-step workflows.

<p align="center">
  <img src="docs/assets/flow-cliflow-hub.svg" alt="Cliflow as the stable command boundary" width="100%" />
</p>

- **OpenAPI stays transport-level.** Imported operations live below `operations` for development and incident troubleshooting.
- **Java owns business semantics.** A `+subcommand` can compose calls, validate input, compensate effects, or return a partial result.
- **Agents discover the safe surface.** `schema` exposes business commands and their metadata while hiding transport commands by default.
- **Safety is built in.** Read/write risk, explicit `--yes`, dry runs, environment-only credentials, structured errors, and UTF-8 JSON are runtime concerns.
- **One business, one CLI.** A team depends on the SDK, contributes one SPI module, and packages an independent CLI.

## Built for agents, not just terminals

Cliflow gives an agent a small, progressive protocol instead of forcing it to ingest a full OpenAPI document or guess a shell command.

<p align="center">
  <img src="docs/assets/flow-progressive-protocol.svg" alt="Cliflow progressive agent protocol" width="100%" />
</p>

`schema` only shows business-facing Shortcuts. The generated OpenAPI transport tree stays under `operations`, available for development or incident work without becoming the agent's default tool surface.

For example, an agent can inspect a single action without loading unrelated commands:

```bash
cliflow schema employee +onboard
```

It then receives intent, parameter, risk, confirmation, and example metadata—the information needed to decide whether the action fits the request.

## Quick start

Prerequisites: **JDK 21** and **Maven 3.9+**.

```bash
git clone https://github.com/redSun64/cliflow.git
cd cliflow
mvn verify
java -jar acli-app/target/cliflow-sdk-0.1.0-SNAPSHOT-all.jar schema
```

The repository includes an example business module. Inspect its agent-facing contract:

```bash
java -jar acli-app/target/cliflow-sdk-0.1.0-SNAPSHOT-all.jar schema employee +onboard
```

Generated OpenAPI commands are intentionally an internal escape hatch:

```bash
java -jar acli-app/target/cliflow-sdk-0.1.0-SNAPSHOT-all.jar operations
```

## Build a self-contained CLI

`jpackage` produces an application image with its own JRE, so users do not need Java installed. Native launchers must be built on their target OS.

```powershell
# Windows PowerShell, with a full JDK that contains jpackage
.\scripts\package.ps1
.\dist\cliflow\cliflow.exe --version
```

```bash
# Linux or macOS
./scripts/package.sh
./dist/cliflow/bin/cliflow --version
```

The Windows launcher writes UTF-8 to both stdout and stderr, including JSON containing Chinese text.

## Core concepts

| Concept | Responsibility |
| --- | --- |
| Service | A remote system with base URL, OpenAPI document, and environment-backed authentication. |
| Operation | An atomic OpenAPI capability, such as `crm.createCustomer`. |
| Shortcut | A Java `+subcommand` that implements a meaningful business action. |
| `CliModule` | The SPI boundary through which a business JAR registers its command tree and optional agent Skill. |
| `schema` | Progressive agent discovery for business commands, their intent, options, risk, and confirmation rules. |

A Shortcut invokes an imported operation through `CommandContext`:

```java
Customer customer = ctx.call(
    "crm.createCustomer",
    Map.of("name", name),
    Customer.class
);
```

Business code never builds HTTP requests or reads secrets itself.

## Create a business CLI

Each business owns its own repository and ships its own CLI. It uses the SDK at build time and Java SPI at runtime—there is no plugin-specific command registration model.

<p align="center">
  <img src="docs/assets/flow-business-packaging.svg" alt="From business APIs to a self-contained CLI" width="100%" />
</p>

This division keeps each concern in one place:

| The business team supplies | Cliflow supplies |
| --- | --- |
| OpenAPI, service configuration, Java Shortcuts, one SPI descriptor, and an optional Skill | Launcher, command tree, OpenAPI loading, HTTP mapping, authentication, confirmation, dry-run, JSON output, and command discovery |

```xml
<parent>
  <groupId>io.github.redsun64.cliflow</groupId>
  <artifactId>cliflow-parent</artifactId>
  <version>0.1.0</version>
</parent>

<artifactId>order-cli</artifactId>

<dependencies>
  <dependency>
    <groupId>io.github.redsun64.cliflow</groupId>
    <artifactId>cliflow-sdk</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

> `0.1.0` is the intended first release coordinate. Until it is published, build this repository locally with the `0.1.0-SNAPSHOT` version in the root `pom.xml`.

### 1. Describe the remote service

Create `src/main/resources/META-INF/cliflow/services.yaml`:

```yaml
- id: crm
  name: CRM
  baseUrl: https://crm.example.com
  baseUrlEnv: CRM_BASE_URL
  openapi: classpath:META-INF/cliflow/openapi/crm.yaml
  auth:
    type: bearer-env
    env: CRM_API_TOKEN
    required: true
```

Place the OpenAPI file at `src/main/resources/META-INF/cliflow/openapi/crm.yaml`. `operationId` is recommended; when an export omits it, Cliflow derives a lower-camel-case name from the method and path.

### 2. Register a business action with SPI

Implement `CliModule`, then expose it through Java's standard `ServiceLoader` descriptor at `META-INF/services/io.github.redsun64.acli.api.CliModule`:

```java
public final class OrderCliModule implements CliModule {
    @Override
    public void register(CommandRegistry commands, CommandContextFactory contextFactory) {
        CommandLine order = new CommandLine(new OrderCommand());
        order.addSubcommand("+create", new CreateOrderShortcut(contextFactory));
        commands.add(order);
    }
}
```

The descriptor contains one class name:

```text
com.example.order.OrderCliModule
```

`mvn package` produces `target/order-cli-<version>-all.jar`. The parent POM merges SPI descriptors into the shaded JAR and configures Cliflow's main class.

### From API to an agent-ready action

<p align="center">
  <img src="docs/assets/flow-agent-pipeline.svg" alt="Cliflow agent execution flow" width="100%" />
</p>

The Shortcut, not the framework, owns business failure semantics. Keep compensation, partial-success reporting, and unknown-effect handling explicit in the Java workflow.

## Agent Skills

A business module can provide one static `CliSkill`. The packaged CLI installs it where compatible agents can find it:

```powershell
cliflow skills install
cliflow skills install --scope repo
cliflow skills install --dry-run
```

The installer writes the current packaged executable path into the Skill. Skills explain workflow and discovery; they never grant permission for a write action.

## Safety model

Write actions require explicit confirmation. Use `--dry-run` to inspect a deterministic plan before execution.

```bash
cliflow order +create --customer-id C-42 --dry-run --format json
cliflow order +create --customer-id C-42 --yes --format json
```

Each composed Shortcut must declare its failure semantics:

- **All-or-nothing:** compensate completed effects and verify compensation.
- **Partial success:** retain successful items and return a failure ledger.
- **Unknown effect:** never retry blindly after a timeout; query or use an idempotency key first.

## Developer tooling

Generate a reviewable Markdown rendering of the actual Picocli command tree:

```powershell
.\scripts\preview-commands.ps1
```

It writes `acli-app/target/command-preview.md` by default and includes both Shortcuts and the internal OpenAPI escape hatch.

## Project layout

```text
cliflow-api       Public extension contracts and annotations
cliflow-core      OpenAPI loader, HTTP runtime, auth, output, service registry
cliflow-sdk       Runnable launcher, schema, Skills, generated operations
cliflow-parent    Parent POM for a standalone business CLI
cliflow-example   Example SPI business module and Skill
```

The source directories retain their original `acli-*` names for now; Maven artifact names are the public contract.

## Roadmap

- [ ] Publish SDK artifacts and a signed first release.
- [ ] Validate external-business OpenAPI during the consumer build.
- [ ] Add audit and trace propagation hooks.
- [ ] Add an optional Maven plugin for one-command validation and native packaging.
- [ ] Evaluate GraalVM Native Image only when a single binary is needed.

## Contributing

Issues and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md), keep credentials out of fixtures and logs, and run `mvn verify` before opening a pull request.

## License

[MIT](LICENSE) © 2026 redSun64
