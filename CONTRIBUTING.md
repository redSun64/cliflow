# Contributing to Cliflow

Thanks for considering a contribution.

## Before opening a pull request

1. Discuss material API or architecture changes in an issue first.
2. Keep a Shortcut's business failure semantics explicit: all-or-nothing, partial success, or unknown effect.
3. Never put credentials, real access tokens, or internal payloads in source, fixtures, documentation, or logs.
4. Add or update focused tests for behavior changes.
5. Run the full verification suite:

```bash
mvn verify
```

## Project boundaries

- `cliflow-api` is the public extension contract. Prefer compatible additions over breaking changes.
- `cliflow-core` owns OpenAPI, HTTP, authentication, and output behavior.
- Business teams implement `CliModule` through Java SPI; do not add application-specific commands to the SDK runtime.
- Transport-level OpenAPI commands remain under `operations`. The root `schema` command is reserved for agent-facing business actions.

## Pull requests

Keep pull requests focused. Explain user-facing behavior, tests run, and any compatibility or security consequence. For new write actions, document confirmation, dry-run, idempotency, and failure behavior.
