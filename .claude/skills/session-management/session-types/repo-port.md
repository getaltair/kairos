# Session Type: Repo Port

Use this session type when building features based on an existing open-source repository (porting, rebuilding, implementing from source).

---

## When to Use

- User references a GitHub URL as the source for the work
- User says "port from", "rebuild", "based on", "inspired by" + repo reference
- User provides a repo link and asks to build something similar
- Task involves replicating or adapting functionality from an existing app

**Related**: If the work is refactoring or replacing your own existing code (not porting from an external repo), use `migration.md` instead. Migration focuses on feature inventory and parity within your own codebase; repo-port adds two-layer source analysis (data + UI/UX) of an external source.

---

## Plan Header

The Spec.md should declare the session type and source repo at the top:

```markdown
**Session Type**: Repo Port
**Source Repo**: [URL or path to source repository]
**Source Stack**: [framework/language of source app]
**Target Stack**: [framework/language Cortex is building in]
```

---

## Critical Rule

**The source code IS the design spec.**

The source repo defines BOTH the data/API patterns AND the UI/UX patterns. Planning must analyze both layers. Frontend agents must read source component files before building. Defaulting to generic patterns (DataTable, Sheet, basic layouts) when the source has superior UX is a failure.

---

## Modified Workflow

```
Standard:    Request -> Assess Complexity -> /blueprint -> /impl
Repo Port:   Request -> Source Repo Analysis -> /blueprint (with UX Reference) -> /impl (with UX injection)
```

The Source Analysis Phase happens before `/blueprint` is invoked. Without it, `/blueprint` Phase 1 (Spec.md) does not have the UX reference material it needs.

---

## Source Analysis Phase (Pre-Planning)

Before invoking `/blueprint`, analyze the source repo across TWO layers.

### Layer 1: Data/API Analysis (standard)

- API endpoints, route structure, data models
- Database schema, query patterns
- Authentication/authorization flows
- External service integrations
- Business logic and validation rules

### Layer 2: UI/UX Analysis (the layer that gets missed)

1. **Layout Patterns**: How pages are structured (panels, splits, stacking, responsive behavior)
2. **Interaction Flows**: What happens on click, select, navigate, search
3. **Component Patterns**: Reusable UI patterns (tabbed cards, inline panels, score badges, search history, filter panels, export dropdowns)
4. **Visual Design**: Distinctive styling choices, color use, typography, spacing
5. **State Management**: How UI state flows (selections updating multiple panels, localStorage persistence, URL state)

### How to Analyze

- Read the source repo's component files (not just API/data files)
- Focus on page-level components that define layouts
- Look for interaction handlers that reveal UX flows
- Identify patterns that differ from standard DataTable/Sheet/Drawer defaults
- Use the `deep-researcher` agent to scan a large source repo if needed

### Output

The analysis feeds directly into the "Source UI/UX Reference" section of `Context/Features/NNN-Name/Spec.md`. Document specific layout descriptions, interaction flows, component patterns, and list source files that frontend agents must read.

---

## Example: What Good UX Documentation Looks Like

For reference, here is what a Source UI/UX Reference section in Spec.md should look like. Specific layout descriptions, named patterns, and explicit source files for frontend agents to read.

```markdown
## Source UI/UX Reference

Source: https://github.com/example-org/source-app (Next.js + shadcn/ui)

### Layout Patterns

**Search Results Page (CRITICAL -- not a standard table page):**
Desktop uses a two-panel flex split (lg:flex-row). Left panel: search form +
result count + result list. Right panel: detail card for the selected result
with an inline 12-month trend chart. Clicking a list row highlights it
(bg-primary/5 border-l-2 border-l-primary) and updates the right panel.
There is NO modal or drawer -- the detail is always visible inline.
Mobile stacks vertically with the detail panel collapsed by default.

**Dashboard:**
Shows recent search history (localStorage, up to 20 items) when no active
search. After a search, primary metrics and breakdown table are in a
SINGLE card with tab switching at the top, not two separate scrolling
sections. Export dropdown lives in the card header. A "< Recent searches"
link returns to the history view without losing scroll position.

### Interaction Flows

- Result row click -> highlights row + updates detail panel + loads trend chart
- Search submit -> writes query to localStorage history -> shows results with back link
- Tab switch within result card -> swaps data view without remounting the card

### Component Patterns

- Circular score badges (not text badges) for quality scores: colored circle with number inside
- Tabbed cards: single Card component with `tabs-box` at top for switching data views
- Search history: localStorage with max 20 items, shown as clickable list with per-item remove buttons
- No skeleton loaders -- inline spinners on the affected panel only

### Source Files to Read

- src/app/search/SearchResults.tsx -- two-panel layout
- src/app/search/components/ResultCard.tsx -- selected-row detail card
- src/app/dashboard/page.tsx -- tabbed Keywords/Pages card and history
- src/lib/hooks/useSearchHistory.ts -- localStorage history pattern
```

The frontend specialist who builds this should read every file in the "Source Files to Read" list before writing any of their own components. Without this, agents default to standard DataTable + Sheet layouts and produce a worse UX than the source.

---

## Red Flags - Pause Repo Port

If you notice ANY of these, STOP and report:

- "Source UI/UX Reference" section is missing or empty
- Frontend agent prompts do not list source files to read
- Plan jumps directly to building without source analysis
- Output components default to DataTable/Sheet/Drawer when source uses richer patterns
- Quality engineer was not asked to compare output against the Source UI/UX Reference

---

## Quality Checklist

Before marking repo-port session complete:

- [ ] Source repo analyzed for BOTH data/API AND UI/UX patterns
- [ ] Spec.md contains "Source UI/UX Reference" section with specific patterns
- [ ] Frontend agents were given source file paths to read before building
- [ ] Output UI matches or exceeds source app's UX quality (not just functionality)
- [ ] No pages defaulted to generic DataTable/Sheet when source had better patterns
- [ ] Quality engineer compared output against Source UI/UX Reference
