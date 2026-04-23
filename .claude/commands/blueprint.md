---
description: Full 4-phase planning: Spec, Tech, Steps with team orchestration
model: sonnet
---

# Full Planning Workflow

Generate a complete 4-phase plan for major features or complex tasks. Creates Spec.md, Tech.md, and Steps.md sequentially in `Context/Features/NNN-FeatureName/`.

## When to use
- Major features requiring architectural decisions
- Tasks estimated at multiple hours/days of work
- User says "plan this feature", "full workflow", "4-phase planning"
- Tasks that will touch multiple components or stacks
- Central AI auto-invokes this for complex/collaborative work

## Workflow

**CRITICAL:** If $ARGUMENTS is empty, STOP and ask: "What feature would you like to plan?" Do NOT proceed with file operations or project exploration until a description is provided.

### Step 1: Understand the feature
1. If $ARGUMENTS is NOT empty, user has described the feature
2. If $ARGUMENTS is empty, ask for a feature description
3. Clarify scope: major feature, multi-component work, complex refactor
4. Confirm task qualifies for full 4-phase workflow (suggest `/quick` if too small)
5. Identify which stacks and domains this feature will touch

### Step 2: Detect session type
1. Read `core/skills/session-management/SKILL.md` Session Type Detection table
2. Match the feature description against the Detection Triggers column
3. Pick the matching type (default to Development if nothing matches; pick the more restrictive type if multiple match)
4. Read the matching protocol file from `core/skills/session-management/session-types/<type>.md`
5. Note the session type in the plan -- it determines whether special plan sections are required (e.g., Migration plans must include a Feature Inventory; Debugging plans must document a root-cause hypothesis; Repo Port plans must include a Source UI/UX Reference)
6. Apply the protocol's rules to subsequent phases. For example: a Debugging session's Spec.md must capture the failing scenario as a testable assertion; a Migration session's Steps.md must include a feature-parity verification milestone; a Repo Port session must complete the Source Analysis Phase below before Step 3
7. **Repo Port branch (pre-Phase-1):** if the session type is Repo Port, run the Source Analysis Phase from `session-types/repo-port.md` before continuing to Step 3. Analyze the source repo across both layers (Data/API and UI/UX), reading actual component files and not just data files. The output of this analysis becomes the "Source UI/UX Reference" section of Spec.md. Skipping this analysis is the failure mode the Repo Port type exists to prevent.

### Step 3: Create Spec.md (Phase 1)
1. Determine feature number (next available NNN in Context/Features/)
2. Use the `plan-spec` skill to create `Context/Features/NNN-FeatureName/Spec.md`
3. Include: Overview, Problem Statement, User Stories, Requirements (Must/Should/Won't), Testable Assertions, Open Questions, Dependencies
4. Apply session-type-specific Spec sections per the protocol loaded in Step 2
5. Present to user for review and approval before proceeding

### Step 4: Create Tech.md (Phase 2)
1. Use the `plan-research` skill to create `Context/Features/NNN-FeatureName/Tech.md`
2. Research and document: Architecture overview, key decisions with options considered, stack-specific details, integration points, risks and unknowns
3. Create ADRs for significant architectural decisions. For each significant decision, consult the Advisor tool before finalising the ADR:

   ```bash
   cat > /tmp/cortex-adr-<short-name>.md <<'EOF'
   Context (from Spec.md and research so far):
   [paste the relevant requirement, constraint, or trade-off]

   Options considered:
   [enumerate the options and their observable consequences]

   Question: which option should the ADR accept, and what are the two
   most load-bearing consequences the Consequences section must name?
   Respond as enumerated bullets.
   EOF

   bun run .claude/hooks/advisor/advisor-cli.ts --question-file /tmp/cortex-adr-<short-name>.md
   ```

   - Exit 0: fold the Advisor's enumerated response into the ADR draft.
   - Exit 2: Advisor unavailable; draft the ADR in-thread without the consult.
4. Present to user for review and approval before proceeding

### Step 5: Create Steps.md (Phase 3)
1. Use the `plan-steps` skill to create `Context/Features/NNN-FeatureName/Steps.md`
2. Break implementation into tasks with:
   - Agent assignments from available specialists
   - Dependency declarations between tasks
   - Parallel execution flags
   - Test tasks (S###-T) and documentation tasks (S###-D)
   - Milestone markers with testable assertion references
   - Contract declarations at milestone boundaries
3. Define team composition based on stacks involved
4. Present to user for review and approval

### Step 6: Present plan summary
Present all three documents for review:
- Spec.md summary: key requirements and assertions
- Tech.md summary: architecture decisions and ADRs created
- Steps.md summary: task count, team composition, wave structure

Confirm or adjust before proceeding to implementation.

### Step 7: Suggest execution command
ALWAYS end by recommending the most appropriate execution command with the feature number:
- `/impl NNN` -- default for most features (hub-and-spoke sub-agent orchestration)
- `/team-impl NNN` -- when cross-domain coordination is needed (peer-to-peer Agent Teams)

Choose based on the Steps.md content: if tasks span multiple stacks with tight integration points, recommend `/team-impl`. Otherwise recommend `/impl`.

## Rules
- Each phase requires user approval before proceeding to the next
- Do NOT skip phases or combine them
- If the feature is too small for 4-phase planning, suggest `/quick` instead
- ADRs are created during Phase 2 for any significant architectural decision
- Phase 3 must produce a Steps.md with team orchestration metadata
- Note any ADRs created during planning in the final summary
