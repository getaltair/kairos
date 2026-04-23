# Vue.js / TypeScript: Error handling

How to handle errors in components, composables, and stores.

## Where errors surface

Vue components have three distinct error surfaces:

1. **Synchronous render errors** — caught by `app.config.errorHandler`.
2. **Lifecycle hook and event handler errors** — also caught by the global
   error handler, but also catchable with `onErrorCaptured` in ancestors.
3. **Unhandled promise rejections** — NOT caught by Vue's error handler.
   Use `window.addEventListener('unhandledrejection', ...)` or wrap async
   work yourself.

## Global handler

Every app registers a global error handler in `main.ts`:

```ts
app.config.errorHandler = (err, instance, info) => {
  // Report to telemetry, log to console in dev, show user-facing error
  logger.error("vue error", { err, info });
};
```

Without this, errors in render functions and lifecycle hooks disappear
silently in production builds.

## Async handler errors

A component event handler that's `async` can throw — but the event system
won't await it. Handle errors locally:

```vue
<script setup lang="ts">
async function onSubmit() {
  try {
    await api.createUser(form.value);
  } catch (err) {
    error.value = formatError(err);
  }
}
</script>
```

Or wrap once with a `useAsyncHandler` composable if the pattern repeats.
Don't let `async` event handlers throw — you'll get unhandled rejection
warnings and no user feedback.

## Fetch / network errors

Don't `alert()` or `console.log()` errors in production paths. Surface them
in component state:

```ts
const { data, error, isLoading } = useFetch('/api/users');
```

Every composable that performs I/O returns `{ data, error, isLoading }` or
equivalent. The caller decides how to render each state — don't bake error
UI into the composable.

## Error types

Prefer typed error results over throwing when a failure is expected:

```ts
type Result<T, E = Error> = { ok: true; value: T } | { ok: false; error: E };

async function fetchUser(id: string): Promise<Result<User, ApiError>> {
  try {
    const res = await api.get(`/users/${id}`);
    return { ok: true, value: res.data };
  } catch (err) {
    return { ok: false, error: normalizeError(err) };
  }
}
```

Throw when something is genuinely exceptional (bug, unexpected state). Return
`Result` when the failure is part of the domain (404, validation error,
optimistic update conflict).

## Pinia stores

Don't `throw` from store actions without also updating store state. The
component calling the action has no way to know the store is now in a
partial state unless it handles the thrown error AND inspects the store.
Prefer setting an `error` field in the store and letting subscribers react.

## Never silently swallow

No empty `catch {}` blocks. At minimum, log. If you're intentionally
ignoring a specific failure mode, narrow the catch and comment why:

```ts
try {
  await optionalTelemetry.track(event);
} catch {
  // Telemetry failures must not break user flow — ignore deliberately.
}
```
