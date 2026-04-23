# Kotlin / Android: Error handling

How to raise, catch, propagate, and surface errors across coroutines,
UI state, and platform boundaries.

## Coroutine cancellation is sacred

`CancellationException` must propagate. Never swallow it:

```kotlin
try {
    repository.fetch()
} catch (e: CancellationException) {
    throw e  // Always rethrow first.
} catch (e: IOException) {
    emit(UiState.Error(e.message))
}
```

A bare `catch (e: Exception)` swallows cancellation and breaks structured
concurrency. Prefer narrow catches; when you must catch broadly, re-throw
`CancellationException` explicitly. `detekt` rule `SwallowedException`
catches most cases but not all.

## Scopes and supervision

- `viewModelScope` and `lifecycleScope` are the defaults. Don't create
  ad-hoc `CoroutineScope(Dispatchers.IO)` in ViewModels or UI code.
- `GlobalScope` is banned outside of `Application.onCreate` bootstrap
  code.
- Use `SupervisorJob` when a child failure should not cancel siblings
  (e.g., parallel independent fetches). Use a plain `Job` when any
  failure should fail the whole operation.
- Attach a `CoroutineExceptionHandler` at scope boundaries where you
  need to log unhandled failures.

## Domain errors as sealed types

Model expected failures as data, not exceptions. Unexpected failures
(bugs, OOM) may remain exceptions:

```kotlin
sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data object InvalidCredentials : LoginResult
    data class NetworkError(val cause: Throwable) : LoginResult
}
```

Repositories return `Result<T>` or a sealed type; they do not throw for
"expected" conditions like network errors or 404s. Throw only when a
precondition is violated (programmer error) or when the caller cannot
possibly recover (config missing, DB corrupt).

`kotlin.Result` is fine for return values but not for storage or IPC —
use a sealed class for persisted or serialized state.

## UI error surfaces

Errors reach the UI through `UiState`, never as thrown exceptions
crossing Composables:

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val items: List<Item>) : HomeUiState
    data class Error(val message: String, val retry: () -> Unit) : HomeUiState
}
```

One-shot messages (snackbars, toasts) go through a `Channel<UiEvent>`
collected with `collectLatest`, not through state.

## Platform boundaries

- Network: catch `IOException` and `HttpException` at the repository
  layer, convert to domain errors. Never let Retrofit/OkHttp exceptions
  leak into ViewModels.
- Room: `SQLiteConstraintException` and friends become domain errors at
  the DAO wrapper. Callers handle `DomainError`, not `SQLite*`.
- `Intent` extras: validate at the receiver. A malformed extra is an
  error surface, not a crash. Use `intent.getStringExtra("key") ?: return`
  plus logging for unexpected shapes.

## Logging

- Use Timber or a structured logger — never `android.util.Log.d(...)` in
  new code. `Log.e` is acceptable for explicit crash-path logging.
- Plant Timber trees only in `Application.onCreate`. One `DebugTree` for
  debug builds, a crash-reporter tree for release.
- Never log PII, auth tokens, or full request/response bodies in release
  builds. Gate verbose logs behind `BuildConfig.DEBUG`.
- Structured context goes in the message tags or via the reporter's
  custom keys — don't string-concat user IDs into log messages.

## Nullability

- `!!` is a code smell; use it only when you've just null-checked in the
  same scope, and even then prefer `requireNotNull(x) { "reason" }`.
- `lateinit var` is for dependencies injected after construction (Fragment
  arguments, DI fields). Don't use it for values that could legitimately
  be null — use `var foo: T? = null` instead.
- Platform types (from Java interop) must be annotated at the boundary.
  `@JvmField` Java strings come back as `String!` — cast them to
  `String?` or `String` at the first Kotlin touch point.

## Strict mode and crashes

Enable `StrictMode` in debug builds with `penaltyLog()` and
`penaltyDeathOnNetwork()` to surface main-thread I/O. Ship with the
debug-only `penaltyDeath` variant so regressions are loud.

`Thread.setDefaultUncaughtExceptionHandler` belongs to the crash reporter
(Crashlytics, Sentry). Don't install your own on top; wrap the existing
handler if you need extra logging.
