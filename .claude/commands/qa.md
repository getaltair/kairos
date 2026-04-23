---
description: Run stack-specific QA agents in parallel against detected stacks
model: sonnet
---

# Parallel Quality Assurance

Run stack-specific QA agents in parallel, one per detected project stack.

## Workflow

### Step 1: Read detected stacks

Read `Context/stack.md`. Parse the bullet list under the `## Detected stacks`
heading. Each bullet is of the form:

```
- `<stack-key>` — <Display Name> (<source dirs>)
```

Extract the stack keys.

If `Context/stack.md` is missing, tell the user to run `/setup` first and
stop. Don't fall back to re-detecting — that's `/setup`'s job, and running it
here hides configuration drift.

### Step 2: Map stacks to QA agents

Cortex ships the following stack → agent mapping. Only agents whose frontmatter
declares a `stacks:` list matching one of the detected stacks are eligible:

| Stack key | QA agent |
|---|---|
| `python-fastapi`, `python` | `qa-python` |
| `vue-typescript` | `qa-vue` |
| `rust-tauri`, `rust` | `qa-rust` |
| `cpp-qt` | `qa-cpp-qt` |
| `embedded-c` | `qa-embedded` |
| `kotlin-kmp`, `kotlin-android` | `qa-kotlin` |

This mapping is derived from the `stacks:` frontmatter on each `qa-*` agent
file under `.claude/agents/` — if in doubt, read those files directly rather
than relying on the table above.

Stacks without a matching QA agent (e.g., `supabase`, `svelte`, `react`) are
**skipped** for `/qa` — they may have specialist agents for other purposes,
but those aren't invoked here.

If no QA agents match any detected stack, inform the user:
- "No QA agents match your detected stacks ([list]). To run checks, add a
  stack-specific QA agent or run general checks via `/review-verify`."
- Stop.

### Step 3: Spawn agents in parallel

For each matched QA agent, invoke it as a background sub-agent. Pass
`$ARGUMENTS` (if any) as additional scope context — e.g., "only check the
`src/auth/` directory."

### Step 4: Collect and summarize

Wait for all agents to complete. Aggregate findings into a structured report:

- Stack name
- Issues found (errors, warnings, suggestions) with file:line
- Summary counts

Present as a single consolidated report rather than N separate agent reports.

### Step 5: Offer next actions

- **Fix issues** — create tasks via `/impl` or add to `Context/Backlog/Bugs.md`.
- **Mark as reviewed** — no blocking issues found.
- **Re-run** — after applying fixes.

## Notes

- `/qa` reads detected stacks but doesn't modify them. To change which stacks
  are on record, run `/setup` or `/setup --re-detect`.
- Agents with `stacks:` frontmatter that doesn't match any detected stack are
  not available for invocation — this is intentional, to prevent running
  irrelevant checks.
- The old behavior of re-detecting stacks inside `/qa` has been removed. If
  you need a quick sanity check without running `/setup`, read
  `Context/stack.md` yourself.
