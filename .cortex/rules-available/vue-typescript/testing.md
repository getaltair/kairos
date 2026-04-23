# Vue.js / TypeScript: Testing

Test framework, structure, and component testing patterns.

## Framework stack

- **Unit / component:** `vitest` + `@vue/test-utils` + `@testing-library/vue`.
- **E2E:** `playwright`. Don't use Cypress for new projects — Playwright has
  better cross-browser support and a cleaner API.
- Jest is acceptable in older codebases but don't introduce it for new Vite
  projects. Vitest has nearly the same API and is vastly faster.

## Structure

Tests live beside their source files OR in a parallel `tests/unit/`
directory. Pick one convention per project and stick to it. Co-location is
usually better for components; parallel directory is fine for pure
logic/composables.

```
src/components/UserCard.vue
src/components/UserCard.test.ts    # co-located
```

E2E tests always live in `tests/e2e/` at the project root.

## What to test on components

**Test behavior from the user's perspective** — clicks, typing, rendered
text. Don't test implementation details (internal state, method calls that
aren't exposed).

`@testing-library/vue` encourages this because its API is built around user
interactions:

```ts
import { render, screen } from "@testing-library/vue";
import { userEvent } from "@testing-library/user-event";

test("shows error when password is too short", async () => {
  const user = userEvent.setup();
  render(LoginForm);
  await user.type(screen.getByLabelText(/password/i), "abc");
  await user.click(screen.getByRole("button", { name: /log in/i }));
  expect(screen.getByText(/must be at least 8 characters/i)).toBeVisible();
});
```

Reach for `@vue/test-utils` directly only when you need something Testing
Library explicitly discourages — inspecting emitted events, for example.

## Composables

Composables are plain functions and should be tested as such. Mount a host
component only if the composable depends on lifecycle hooks:

```ts
import { withSetup } from "../test-utils";

test("useCounter increments", () => {
  const [result, app] = withSetup(() => useCounter(0));
  result.increment();
  expect(result.count.value).toBe(1);
  app.unmount();
});
```

## Mocking

- Mock modules with `vi.mock('./api')`. Prefer full-module mocks at the top
  of the test file over partial runtime mocks inside individual tests.
- For global composables that rely on an injected app instance (Pinia, i18n,
  router), use `createTestingPinia()` etc. rather than mocking at the module
  level.

## Async

- `await nextTick()` after state changes to let the DOM update.
- `await flushPromises()` for fetch/axios chains. Don't `setTimeout(() => ...
  done())` — vitest/jest will falsely pass the test if the timer fires after
  the assertion.

## Snapshots

Use sparingly. Inline snapshots (`toMatchInlineSnapshot()`) for small,
deliberately-stable outputs are fine. Full component HTML snapshots rot
quickly and produce low-signal diffs — prefer explicit assertions on the
parts that matter.

## E2E scope

Cover critical user flows — signup, login, checkout, the one or two features
that matter most to the product. Don't try to achieve unit-level coverage
via E2E; it's slow and flaky.
