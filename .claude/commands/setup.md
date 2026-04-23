---
description: Initialize project for Cortex workflow (detects stacks, scaffolds Context/, wires hooks)
model: sonnet
---

# Setup Cortex

Initialize this project for the Cortex workflow. Detects stacks, writes
`Context/stack.md`, promotes matching rules, creates scaffolding, and
configures the project.

## When to use

- First time using Cortex in a project.
- User says "set up cortex", "initialize cortex", "cortex setup".
- After running `bun run deploy` / `scripts/install.sh` to install Cortex files.
- With `--re-detect` to re-scan stacks and update `Context/stack.md` + promoted
  rules.

## Flags

- `--re-detect` — force re-scan even if `Context/stack.md` already exists;
  show a diff of changes and prompt before overwriting.
- No flag (default) — if `Context/stack.md` already exists, offer to re-use
  it (additive) rather than re-scan from scratch.

## Workflow

### Step 1: Load stacks manifest

Read `.cortex/stacks.json` (installed alongside the rest of Cortex by
`install.sh`). This file is the source of truth for detection signals. Do
**not** hardcode stack detection rules in this command — always consult the
manifest.

If `.cortex/stacks.json` is missing, inform the user to run
`scripts/install.sh` first, and stop.

### Step 2: Load existing stack record (if any)

Read `Context/stack.md` if it exists. Parse the bullet list under the
`## Detected stacks` heading to extract previously-recorded stack keys.

Behavior:
- `Context/stack.md` missing → proceed to Step 3 (full scan).
- `Context/stack.md` present, no `--re-detect` → ask the user:
  "Existing stacks on record: [list]. Re-scan the project or keep as-is?"
  If keep: skip to Step 7.
  If re-scan: proceed to Step 3 (additive — new stacks add, existing stacks
  retained unless user explicitly removes in Step 5).
- `Context/stack.md` present, `--re-detect` flag set → proceed to Step 3
  (full re-scan, will replace the file).

### Step 3: Detect stacks

For each entry in `stacks.json.stacks`:

1. Check `detect.manifestFiles` — does any exist in the project root or
   common subdirectories (`src/`, `app/`, `src-tauri/`)?
2. Check `detect.configFiles` — does any exist?
3. Check `detect.directories` — does any exist?
4. Check `detect.extensions` — is there at least one file with one of these
   extensions in a source directory (not in `node_modules/`, `target/`,
   `dist/`, `.git/`, `venv/`, `.venv/`)?
5. Check `detect.depsInManifest` — if the manifest file exists, does it
   contain the listed dependency strings? (Case-sensitive; look inside
   `[project.dependencies]`, `dependencies`, `devDependencies` etc.)
6. Check `detect.contentPatterns` — if the file exists, does the listed
   regex pattern match its contents?
7. Check `detect.envPatterns` — scan `.env`, `.env.example`, `.env.local`
   for these variable names.

A stack **matches** if ANY of its detection criteria succeed. Different
stacks may match independently.

### Step 4: Apply precedence

For each matched stack, check its `overriddenBy` field in the manifest. If
any stack listed in `overriddenBy` also matched, the current stack is
**suppressed**. Example: a project matching both `python` and
`python-fastapi` keeps only `python-fastapi`.

### Step 5: Confirm with user

Present the final stack list to the user with:

- Stack key
- Display name (from `stacks.json`)
- What signal matched (e.g., "detected via `pyproject.toml` + `fastapi` in
  deps")
- Source directories (where the matching files live)

Ask the user:

1. "Is this correct?"
2. "Any stacks I missed?" (user can list additional stack keys or
   display names)
3. "Any misdetections to remove?"

If the user adds a stack not in `stacks.json`, ask whether to:
- Add it verbatim to `Context/stack.md` under an `## Unrecognized` section
  (so the user can populate rules manually), OR
- Pick the closest matching stack from `stacks.json` (show the options).

### Step 6: Greenfield fallback

If **no** stacks were detected AND the user does not add any in Step 5:

- Display the prompt from `stacks.json.greenfieldFallback.promptCopy`.
- Collect a free-text comma-separated answer.
- Fuzzy-match each entry against `displayName` and the stack key (case
  insensitive). For any entry that doesn't match cleanly, ask the user to
  pick from a list or add as unrecognized.

If the user skips greenfield (empty answer), proceed without any stack
record — but warn that no stack-specific rules will be promoted and
stack-filtered skills/agents will be unavailable.

### Step 7: Write Context/stack.md

Write `Context/stack.md` with this exact structure (required for the
parser in `skill-activation.ts`):

```markdown
# Project Stack

_Maintained by `/setup`. Re-run `/setup --re-detect` to refresh._

## Detected stacks

- `<stack-key>` — <Display Name> (<source dirs>)
- `<stack-key>` — <Display Name> (<source dirs>)

## Unrecognized stacks

- <user-supplied name> — no `core/rules/` slot; populate manually if needed.

## Detection metadata

- Detected: <ISO 8601 timestamp>
- User-confirmed: yes
- Greenfield fallback: <yes|no>
```

The `## Detected stacks` section is parsed by other Cortex components. Each
bullet begins with `` ` `` (backtick), contains the stack key wrapped in
backticks, then an em dash, then the display name. Keep this format stable.

Omit the `## Unrecognized stacks` section entirely if there are none (do
not write an empty section).

### Step 8: Promote rules

For each stack key recorded in `Context/stack.md`:

1. Source: `.cortex/rules-available/<stack-key>/`
2. Destination: `.claude/rules/<stack-key>/`

For each file in the source directory (recursively), copy it to the
destination. Skip placeholder `README.md` files — they only live in the
source as documentation for the repo maintainer; no value in promoting them
to the target project's rules.

On `--re-detect`, **remove** previously-promoted rule directories that are
no longer in the detected stacks list. Use this algorithm:

1. List existing directories under `.claude/rules/`.
2. For each directory name that matches a known stack key (per
   `stacks.json`), check if that stack is in the new `Context/stack.md`.
3. If not, ask the user: "Remove `.claude/rules/<stack-key>/` (N files)?
   This stack is no longer detected." Default to yes.

Do not touch directories under `.claude/rules/` whose names aren't known
stack keys — those are user-authored rules, not Cortex-managed.

### Step 9: Scaffold Context/

Ensure this directory tree exists (create any missing, don't touch
existing):

```
Context/
  Features/
  Decisions/
  Reviews/
  Backlog/
    Ideas.md
    Bugs.md
```

Also ensure `.cortex/archive/` exists.

### Step 10: Create or update CLAUDE.md

If `CLAUDE.md` does not exist, create it using the Cortex template from
`core/skills/project-init/`. Include a section listing detected stacks
referencing `Context/stack.md`.

If `CLAUDE.md` exists, check for a Cortex workflow section. If missing,
append it. If present, leave it alone. Don't clobber user additions.

### Step 11: Verify hooks

Check `.claude/settings.json` for the required Cortex hooks:

- `UserPromptSubmit` — skill-activation hook
- `PreCompact` — context-recovery hook
- `PostToolUse` — formatter hook

If any are missing, inform the user and suggest running
`scripts/install.sh` (which calls `merge-settings.ts`) to wire them.

### Step 12: Detect issue tracker

1. Check `gh auth status` — is GitHub CLI installed and authenticated?
2. Check `glab auth status` — is GitLab CLI installed and authenticated?
3. Resolve:
   - Both found & authenticated → ask user which they prefer.
   - One found & authenticated → confirm with user.
   - Neither found → inform user; deferred findings will use local backlog.
4. Write to `.cortex/config.json` (merge into existing JSON without
   overwriting other keys):
   - GitHub: `{ "issueTracker": { "type": "github", "cli": "gh" } }`
   - GitLab: `{ "issueTracker": { "type": "gitlab", "cli": "glab" } }`
   - Local: `{ "issueTracker": { "type": "backlog" } }`

### Step 13: Summary

Present to the user:

- Stacks detected (with count)
- Unrecognized stacks (if any)
- Rules promoted (count, per stack)
- Rules removed on re-detect (if applicable)
- Context/ directories created / already existed
- CLAUDE.md status (created / appended to / left alone)
- Hooks status (all wired / missing)
- Issue tracker configured

Suggest next steps:
1. Review `Context/stack.md` and `CLAUDE.md`.
2. Start planning with `/blueprint "feature description"`.
3. Use `/quick` for smaller tasks.
4. Use `/qa` to run quality checks against detected stacks.
