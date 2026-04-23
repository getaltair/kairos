# Vue.js / TypeScript: Formatting and tooling

Toolchain, configuration, and non-negotiables.

## Required tools

| Tool | Purpose | Config location |
|---|---|---|
| `eslint` (flat config) | Lint | `eslint.config.ts` |
| `prettier` | Format | `.prettierrc` or `package.json` |
| `vue-tsc` | Vue-aware typecheck | invoked by `npm run typecheck` |
| `vitest` | Test runner | `vitest.config.ts` |

Use ESLint's flat config (`eslint.config.ts` / `.js`) — the legacy
`.eslintrc` format is on its way out.

## ESLint baseline

Minimum rule sets to enable:

- `@vue/eslint-config-typescript` — Vue + TS integration.
- `eslint-plugin-vue` with `vue3-recommended`.
- `eslint-plugin-jsx-a11y` if the project uses JSX/TSX.
- A formatter-conflict resolver like `eslint-config-prettier` (strips rules
  that fight with prettier).

## Prettier settings

Project-wide defaults to use unless a specific reason dictates otherwise:

```json
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "arrowParens": "always"
}
```

`semi: false` is a taste choice — pick one and enforce it. Flipping
mid-project generates churn.

## TypeScript strictness

`tsconfig.json` starting point:

```json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitOverride": true,
    "exactOptionalPropertyTypes": true,
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"]
  }
}
```

`strict: true` alone misses `noUncheckedIndexedAccess`, which catches a
whole class of `possibly undefined` bugs. Turn it on from day one.

## Non-negotiables

- **No `any`** in new code. If a third-party library forces your hand,
  `unknown` + a type guard is almost always the right answer. When you
  genuinely must use `any`, use `// @ts-expect-error <reason>` instead — it
  fails loudly when the underlying type gets fixed upstream.
- **No `@ts-ignore`** — always `@ts-expect-error` with a short reason. The
  latter fails when the ignore is no longer needed, which is what you want.
- **No `console.log`** in committed code. `console.error` is fine for
  genuine error paths; prefer a logger abstraction.
- `vue-tsc --noEmit` must pass before commit, not just `tsc --noEmit` — the
  latter ignores `.vue` files.

## Build tooling

- Vite is the default. Don't use webpack-based tooling for new projects
  unless there's a specific reason.
- Use `@vitejs/plugin-vue` for Vue SFCs; don't hand-roll.
- Tailwind via `@tailwindcss/vite` when used.

## Package manager

Pick one per project and commit the lockfile. `pnpm` is preferred for
workspaces; `bun` is preferred for speed; `npm` is acceptable as the
least-common-denominator. **Don't mix** — never commit both `pnpm-lock.yaml`
and `package-lock.json`.

## Node version

Declare the Node version in `package.json` `"engines"` and `.nvmrc`
(or `.tool-versions`). New projects should target the current LTS. Don't
support Node 16 or 18 in new code — 20 LTS or newer.
