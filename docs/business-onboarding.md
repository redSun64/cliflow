# Business CLI onboarding

Use one repository and one independently packaged CLI per business domain. The business project depends on Cliflow, implements Java SPI, and can bundle one agent Skill.

## Add a remote service

1. Add `src/main/resources/META-INF/cliflow/services.yaml` to the business project.
2. Put the OpenAPI document under `src/main/resources/META-INF/cliflow/openapi/`.
3. Give operations stable `operationId` values when possible. Missing IDs are derived from method and path.
4. Build the JAR, then inspect `cliflow operations` as a development-only transport escape hatch.

## Add a business action

Implement `CliModule` and declare it in `META-INF/services/io.github.redsun64.acli.api.CliModule`.

Rules:

- Do not construct HTTP requests in business commands.
- Do not read tokens or secrets in business commands.
- Call remote capabilities only through `CommandContext.call(...)`.
- Use `@CliRisk` for every action's effect and confirmation policy.
- Add `@AgentCommand` so an agent can choose the action through `schema`.
- Return stable structured data through `ctx.output()`.
- State the shortcut's failure model explicitly: all-or-nothing, partial success, or unknown effect.

## Add an Agent Skill

Return one `CliSkill` from the business `CliModule` and keep Markdown guidance focused on workflow, progressive discovery, dry-run, and confirmation. The skill must never contain credentials or claim authority to execute a write action.

## Native operations

Cliflow currently executes OpenAPI/HTTP operations. Add a native operation extension only when OpenAPI cannot describe the transport cleanly, for example custom cryptographic signing, streaming, gRPC, WebSocket, or a proprietary SDK.
