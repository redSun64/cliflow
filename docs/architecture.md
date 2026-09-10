# Architecture

## 1. Core abstractions

### Service

A remote enterprise system and its connection/authentication configuration.

Examples: `hr`, `iam`, `crm`.

### Operation

An atomic capability imported from an OpenAPI `operationId`.

```text
service.id + "." + operationId
```

Example:

```text
hr.searchEmployee
iam.createAccount
iam.bindRole
```

Operations are runtime capabilities. Cliflow generates transport-level commands under the internal `operations` escape hatch, but does **not** expose them through the agent-facing root `schema` surface.

### Shortcut

A deterministic Java business action. A Shortcut may call one or more Operations through `CommandContext`.

```text
employee +onboard
  ├─ hr.searchEmployee
  ├─ iam.createAccount
  └─ iam.bindRole
```

### Command Catalog

Picocli is the stable command surface. Humans use `--help`; agents use `cliflow schema` for progressive discovery.

## 2. Runtime call path

```text
argv
  ↓
Picocli
  ↓
Shortcut.call()
  ↓
CommandContext.call(operationId, args)
  ↓
OperationRegistry
  ↓
HttpOperationExecutor
  ├─ resolve Service
  ├─ map path/query/header/body from OpenAPI
  ├─ apply auth
  ├─ execute HTTP
  └─ parse JSON
  ↓
Shortcut continues deterministic Java orchestration
```

## 3. Why OpenAPI is not the CLI model

OpenAPI describes transport-level operations. User intent is often higher level and may require multiple operations, ID resolution, validation, fallback or aggregation.

Therefore:

```text
OpenAPI -> Operation Registry
Java    -> Shortcut
```

instead of:

```text
OpenAPI operation -> CLI command
```

## 4. Safety boundary

- `@CliRisk` declares READ/WRITE and confirmation policy at command level.
- `DefaultCommandContextFactory` blocks a write Shortcut without explicit confirmation.
- `HttpOperationExecutor` blocks WRITE Operations while `--dry-run` is active.
- Auth credentials are resolved by Runtime from environment/config, not handled by Shortcut code.

## 5. AI friendliness

`@AgentCommand` stores semantic routing metadata:

- summary
- useWhen
- avoidWhen
- examples

`cliflow schema` exposes this progressively instead of forcing an agent to load the whole command tree/OpenAPI catalog into context.
