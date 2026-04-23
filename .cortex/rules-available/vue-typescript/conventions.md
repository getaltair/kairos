# Vue.js / TypeScript: Conventions

File naming, SFC layout, component patterns, and accessibility basics.

## File naming

- Components: `PascalCase.vue` — always. Never `kebab-case.vue` or
  `camelCase.vue`. `MyComponent.vue`, not `myComponent.vue` or
  `my-component.vue`.
- Composables: `useXxx.ts` — camelCase with `use` prefix. `useFetch.ts`,
  `useAuthUser.ts`.
- Stores (Pinia): `useXxxStore.ts` — camelCase with `use` prefix and `Store`
  suffix. `useUserStore.ts`.
- Types/interfaces: either co-located with their consumer (`foo.ts` contains
  both `interface Foo` and the code using it) or in a dedicated `types/`
  directory. Don't scatter `types.ts` files next to every component.

## SFC block order

Fixed order: `<script setup>`, then `<template>`, then `<style>`. Rationale:
the script declares the state and props that the template consumes, so
putting it first mirrors the mental model and matches Vue 3's documentation
convention. Legacy `<template>`-first ordering is acceptable in older
codebases; don't mix styles within one project.

## Composition API only

In new code, use `<script setup lang="ts">`. Don't reach for the Options API
or defineComponent({...}) unless extending an old codebase. Mixing Options
and Composition within a component is a bug in waiting.

## Props and emits

- Always declare with generics: `defineProps<{ count: number; label?: string }>()`
  and `defineEmits<{ change: [value: number] }>()`.
- Never use the object-runtime form (`defineProps({ count: Number })`) in
  new code — it loses type information and duplicates the type as runtime
  validators.
- Prop names are camelCase in the script, kebab-case in templates. Vue
  handles the conversion; don't fight it.

## Reactivity

- `ref()` for primitive-shaped state (even if the value is an object, use
  `ref` when you'll reassign the whole thing).
- `reactive()` for shaped objects you'll mutate in place.
- `computed()` for derived values — never compute in template expressions
  beyond trivial conditionals.
- `shallowRef` / `shallowReactive` for large objects where deep reactivity
  costs more than it's worth (big data tables, external library instances).
- Don't destructure a `reactive` — it breaks reactivity. Use `toRefs()` if
  you need destructured refs.

## Template rules

- Every `v-for` has a `:key`. The key is a stable unique identifier, not the
  array index unless the list is append-only and never reordered.
- `v-if` and `v-for` never on the same element — split into a wrapper with
  `v-if` and a child with `v-for`.
- No `v-html` with user-sourced content. If you must use `v-html`, the input
  must be sanitized (DOMPurify) and the sanitization must be visible in the
  diff.

## Accessibility (non-negotiable)

- `<img>` requires `alt` — empty `alt=""` for decorative images, descriptive
  text otherwise. Missing `alt` is an error, not a warning.
- `<button>` and `<a>` must contain visible text or an `aria-label`. Icon-only
  buttons require `aria-label`.
- Click handlers on `<div>` or `<span>` require `role="button"`, `tabindex="0"`,
  and keyboard handlers (`@keydown.enter`, `@keydown.space`). Preferable: use
  a real `<button>` and style it.
- Color is never the only indicator — pair it with an icon, label, or
  pattern for colorblind users.

## Global registration

Avoid `app.component('MyComponent', MyComponent)` except for layout primitives
used on nearly every page. Local imports are explicit, tree-shakeable, and
survive refactors.
