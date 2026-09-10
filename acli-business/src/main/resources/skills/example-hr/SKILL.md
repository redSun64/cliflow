---
name: example-hr
description: When a user explicitly asks to provision an example employee account, use this example Cliflow business CLI.
---

# Example HR CLI

Command prefix: `{{ACLI_COMMAND}}`.

## Discovery

Run `{{ACLI_COMMAND}} schema`, then inspect only the relevant business command path. This example exposes `employee +onboard`; transport-level OpenAPI commands under `operations` are for development and troubleshooting, not normal agent discovery.

## Provision an employee account

First inspect the plan without sending requests:

```powershell
{{ACLI_COMMAND}} employee +onboard --name "Example User" --role employee --dry-run --format json
```

This is a WRITE action. Explain the planned account and role changes, obtain explicit user confirmation, then execute with `--yes`:

```powershell
{{ACLI_COMMAND}} employee +onboard --name "Example User" --role employee --yes --format json
```

Handle JSON errors structurally through `error.code`, `error.message`, and `error.details`. Do not put credentials in command arguments, Skills, or output. If a multi-step action reports partial success or an unknown effect, preserve that state and follow its recovery guidance rather than retrying blindly.
