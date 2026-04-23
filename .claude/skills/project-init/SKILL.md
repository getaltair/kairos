# Skill: Project Initialization Templates

## Purpose

Provides the templates that `/setup` uses when initializing a project for
the Cortex workflow. This skill is not invoked directly — it is a reference
library of template content consumed at step 10 of `/setup` (create-or-
update `CLAUDE.md`) and wherever else scaffolding is needed.

## What this skill owns vs. what lives elsewhere

This skill is deliberately scoped. It owns **universal** templates — the
content that every Cortex project gets regardless of stack. It does **not**
own stack-specific rule content. That lives in `core/rules/<stack>/` and is
promoted per-project by `/setup` based on detected stacks.

| Concern | Location |
|---|---|
| Shared conventions (git, code org, docs) | This skill (`shared-conventions.md`) |
| CLAUDE.md template for new projects | This skill |
| Backlog scaffolds (Ideas.md, Bugs.md) | This skill |
| Post-edit lint hook template | This skill |
| Settings.json template | This skill |
| `.cortex/config.json` template | This skill |
| **Stack-specific rules (Python/Vue/Rust/etc.)** | **`core/rules/<stack>/` — NOT here** |

Earlier versions of this skill had inline stack rule templates
(python-fastapi, vue-typescript, rust-tauri, embedded-c, cpp-qt, kotlin-kmp,
kotlin-android).
Those were removed when stack detection shipped (see ADR-0001). `/setup`
now promotes rules from `.cortex/rules-available/<stack>/` (staged by
`install.sh` from `core/rules/<stack>/`) directly. Stack-specific rule
content should be added to `core/rules/<stack>/`, not here.

## Templates

### shared-conventions.md (universal, always created)

Written to `.claude/rules/shared-conventions.md` during project init.
Applies regardless of stack — no `paths:` frontmatter.

```markdown
# Shared Conventions

These conventions apply to all code regardless of stack.

## Git
- Conventional commits: `type(scope): description`
- Types: feat, fix, refactor, test, docs, chore, build, ci
- Commit messages reference task numbers: `Tasks: S001, S002`
- Commit messages reference ADRs when applicable: `ADR-0003`
- One logical change per commit — don't mix unrelated changes
- Planning artifacts (Context/) committed separately from source code

## Code Organization
- One responsibility per file — if a file does two unrelated things, split it
- Public API surface should be minimal — don't export internals
- Dead code gets deleted, not commented out (git has history)
- No secrets in source code — use environment variables or secret managers

## Documentation
- Public functions/methods get documentation comments
- Complex algorithms get inline comments explaining *why*, not *what*
- README.md at project root covers setup, build, and run instructions
- Architecture decisions go in Context/Decisions/ as ADRs

## Planning Workflow
- Features use the 4-phase workflow: Spec → Tech → Steps → Implementation
- Small tasks use quick plans
- Every deviation from spec gets an ADR
- Milestone checkpoints verify alignment with Testable Assertions
- Test and documentation tasks are planned, not afterthoughts

## Error Messages
- Error messages should help the user fix the problem
- Include what went wrong, what was expected, and what to try
- Never expose internal stack traces to end users
- Log the full error context for debugging
```

---

### CLAUDE.md template

Created when `CLAUDE.md` does not already exist. Substitute bracketed
placeholders during init.

When `CLAUDE.md` already exists, `/setup` should check for a Cortex
workflow section and append it if missing — do not overwrite user content.

```markdown
# [Project Name]

## Overview

[Fill in: what does this project do?]

## Architecture

[Fill in: high-level architecture description]

## Tech Stack

This project's stacks are recorded in `Context/stack.md`, written by
`/setup`. Re-run `/setup --re-detect` to refresh.

[Optionally embed the detected stacks list here as a quick reference, but
Context/stack.md is the source of truth.]

## Key Directories

- `Context/Features/` — Feature specifications, tech plans, and implementation steps
- `Context/Decisions/` — Architecture Decision Records (ADRs)
- `Context/Reviews/` — Review capture files
- `Context/Backlog/` — Ideas and bugs for future work
- `Context/stack.md` — Detected project stacks
- `.claude/rules/` — Stack-specific coding conventions (populated by `/setup`
  from `.cortex/rules-available/<stack>/` based on detected stacks — do not
  edit rules under stack-key directories by hand; edit in the Cortex repo
  and re-deploy)
- `.cortex/rules-available/` — All stack rules staged during install; `/setup`
  promotes matching stacks into `.claude/rules/`

## Development Workflow

This project uses Cortex, a 4-phase planning + agent execution workflow.

### Planning

1. **Spec** — Define what and why (Spec.md with testable assertions)
2. **Tech Research** — Decide how to build (Tech.md with architecture decisions)
3. **Steps** — Break into tasks with team orchestration (Steps.md with milestones)
4. **Implementation** — Execute via `/impl` (sub-agents) or `/team-impl` (Agent Teams)

Quick tasks skip to a single-file plan via `/quick`.

### Complexity Routing

- **Trivial** (single file, obvious fix) → Execute directly
- **Moderate** (2-5 files, clear scope) → `/quick` → execute
- **Complex** (multi-phase, 5+ files) → `/blueprint` → `/impl`
- **Collaborative** (cross-domain integration) → `/blueprint` → `/team-impl`

### Post-Implementation

- Run `/qa` for parallel stack-specific quality checks (reads
  `Context/stack.md` to decide which QA agents to invoke)
- Run `/review-capture` to capture findings (4-category triage)
- Run `/review-resolve` in a fresh session to work through findings
- Run `/commit` with conventional format

### Context Management

- Central AI conserves context — delegates to sub-agents
- Sub-agents maximize context — read all relevant files before executing
- Steps.md is the planning source of truth
- `.cortex/session.md` tracks ephemeral execution state
- `Context/` persists planning artifacts across sessions

## Conventions

- Conventional commits with task and ADR references
- ADRs required for any deviation from spec
- Milestone checkpoints verify spec alignment during implementation
- Test and documentation tasks are planned alongside implementation
- See `.claude/rules/` for stack-specific coding conventions

## Coding Guidelines

Guidelines to reduce common LLM coding mistakes. Biased toward caution over speed.

1. **Think Before Coding** — State assumptions explicitly. If uncertain, ask.
   Surface tradeoffs. If multiple valid approaches exist, present them as
   options — don't silently pick one.
2. **Read Before You Write** — Read the full file before modifying. Check for
   existing implementations. Follow existing patterns.
3. **Simplicity First** — Minimum code that solves the problem. No speculative
   features, abstractions, or error handling for impossible scenarios.
4. **Surgical Changes** — Touch only what you must. Don't improve adjacent
   code. Match existing style. Every changed line traces to the request.
5. **Goal-Driven Execution** — Define success criteria, loop until verified.
   For `/impl`, milestones handle this. For `/quick` and direct tasks, state
   a brief plan with verification at each step.

## Setup

[Fill in: how to install dependencies, build, and run]

## Active Work

- **Feature:** [NNN] — [Name]
- **Status:** [Phase]
```

---

### Backlog scaffolds

Created during `Context/Backlog/` scaffolding if not already present.

**Ideas.md:**

```markdown
# Ideas Backlog

Ideas and enhancements for future consideration. Add entries via the
`backlog-add` skill. Prioritize via the `backlog-prioritize` skill.

<!-- Add new ideas below this line -->
```

**Bugs.md:**

```markdown
# Bugs Backlog

Known bugs not blocking current work. Add entries via the `backlog-add`
skill. High-priority bugs should be addressed before starting new features.

<!-- Add new bugs below this line -->
```

---

### post-edit-lint.sh template

Generated with cases only for the detected stacks (from `Context/stack.md`).
This is file-extension-driven — it runs the formatter for the file type you
just edited, not project-wide. Complementary to stack detection: stack
detection decides which agents/rules apply; this hook decides which
formatter to run per file save.

```bash
#!/usr/bin/env bash
# Post-edit linting hook — auto-format on file save.
# Only includes formatters for stacks detected during init.
# Regenerated by /setup --re-detect when detected stacks change.

FILEPATH="$1"
[ -z "$FILEPATH" ] && exit 0
EXTENSION="${FILEPATH##*.}"

case "$EXTENSION" in
    # === Python (if detected) ===
    py)
        command -v ruff &>/dev/null && ruff format "$FILEPATH" 2>/dev/null \
            && ruff check --fix "$FILEPATH" 2>/dev/null
        ;;
    # === Vue/TypeScript (if detected) ===
    vue|ts|tsx|js|jsx)
        command -v npx &>/dev/null \
            && npx prettier --write "$FILEPATH" 2>/dev/null \
            && npx eslint --fix "$FILEPATH" 2>/dev/null
        ;;
    # === Rust (if detected) ===
    rs)
        command -v rustfmt &>/dev/null && rustfmt "$FILEPATH" 2>/dev/null
        ;;
    # === Embedded C/C++ (if detected) / C++/Qt (if detected) ===
    c|h|cpp|hpp)
        command -v clang-format &>/dev/null \
            && clang-format -i "$FILEPATH" 2>/dev/null
        ;;
    # === C++/Qt QML (if detected) ===
    qml)
        command -v qmlformat &>/dev/null \
            && qmlformat -i "$FILEPATH" 2>/dev/null
        ;;
    # === Kotlin (if detected) ===
    kt|kts)
        command -v ktlint &>/dev/null && ktlint -F "$FILEPATH" 2>/dev/null
        ;;
esac

exit 0
```

---

### settings.json template

Base `.claude/settings.json` when none exists. Merged into any existing
settings.json by `scripts/merge-settings.ts` — never overwritten.

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": ".claude/hooks/post-edit-lint.sh $FILEPATH",
            "timeout": 30
          }
        ]
      }
    ]
  }
}
```

---

### .cortex/config.json template

Created during scaffolding if it does not already exist. Stores project-level
Cortex configuration. `/setup` step 12 populates the `issueTracker` key.

```json
{
  "issueTracker": {
    "type": "backlog"
  }
}
```

## Migration notes

If you're looking for the old inline stack rule templates (python-fastapi.md,
vue-typescript.md, rust-tauri.md, embedded-c.md, cpp-qt.md,
kotlin-multiplatform.md), they have moved:

- **Content lives at** `core/rules/<stack>/` in the Cortex repo.
- **Distribution** is via `install.sh` → `.cortex/rules-available/<stack>/`
  in target projects, then promotion by `/setup` into `.claude/rules/<stack>/`.
- **Applicability** is declared by directory location (no frontmatter needed).

See `ADR-0001` (`Context/Decisions/0001-stack-detection-system.md`) for
design rationale, including why stack rule content lives alongside the
Cortex repo rather than inside this skill.

Previous versions of the rule templates used `paths:` frontmatter to scope
rules to specific file globs (e.g., `**/*.py`). That is a valid Claude Code
feature and can be used inside individual `core/rules/<stack>/*.md` files
when finer-grained scoping than the whole-directory-per-stack default is
warranted. The current starter rules do not use it; the directory-based
default has proven sufficient so far.
