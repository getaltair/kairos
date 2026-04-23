# Rust / Tauri: Error handling

Error types, propagation, and the Tauri-specific serialization constraint.

## Domain error type

Every crate defines one top-level error enum using `thiserror`:

```rust
use thiserror::Error;

#[derive(Error, Debug)]
pub enum AppError {
    #[error("user {0} not found")]
    UserNotFound(String),

    #[error("database error")]
    Database(#[from] sqlx::Error),

    #[error("io error")]
    Io(#[from] std::io::Error),

    #[error("invalid input: {0}")]
    InvalidInput(String),
}
```

Don't use `Box<dyn Error>` in application code — it loses type information
and prevents the frontend from distinguishing error kinds. Reserve
`Box<dyn Error>` for genuine library boundaries where the caller shouldn't
care about the concrete error.

## `?` propagation

Default to `?`. Wrap with context when the chain loses meaning:

```rust
// `anyhow` for application-level with context
use anyhow::Context;

let config = std::fs::read_to_string(&path)
    .with_context(|| format!("reading config at {}", path.display()))?;
```

For library crates, prefer `thiserror` over `anyhow` — library consumers
need to match on error variants, which `anyhow` obstructs.

## `unwrap()` policy

- `unwrap()` in non-test code is a lint violation. Use `expect("reason")`
  when you've proven an invariant, with the reason stated.
- `unwrap()` in `#[cfg(test)]` or `#[test]` is fine — tests should panic
  on unexpected failure.
- `unwrap()` on `Mutex::lock()` should become `.lock().unwrap_or_else(|p|
  p.into_inner())` if recovery from a poisoned mutex is possible. If not,
  `expect("mutex poisoned")` makes the assumption explicit.

## `panic!` and `unreachable!`

Acceptable in:
- Tests.
- Code paths proven unreachable by the type system (`unreachable!()` with
  a comment explaining the proof).

Unacceptable in:
- Command handlers (crashes the backend from a frontend call).
- Library public API (unexpected by callers).

## Tauri command errors

Tauri serializes command errors to JSON via `serde::Serialize`. This means
your error type must be `Serialize`, but most error types aren't. Pattern:

```rust
impl serde::Serialize for AppError {
    fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        s.serialize_str(&self.to_string())
    }
}

#[tauri::command]
async fn get_user(id: String) -> Result<User, AppError> {
    users::find(&id).await
}
```

Alternatively, wrap in a specific command-facing error enum that implements
`Serialize` derive directly — useful when you want structured error data
(code, message, details) on the frontend.

## `unsafe` blocks

- `unsafe` requires a `// SAFETY:` comment explaining the invariants being
  upheld. No exceptions. The comment goes immediately before the block.
- If `unsafe` is needed in application code (not FFI or perf-critical
  library code), pause and reconsider — it's almost always avoidable.

## Async-specific

- Don't call blocking I/O from an `async` function — wrap with
  `tokio::task::spawn_blocking` if it's genuinely synchronous.
- `tokio::spawn` returns a `JoinHandle` that must be awaited or held. A
  dropped handle may or may not complete its future — be explicit.
- Cancellation: `tokio::select!` + `tokio::sync::oneshot` for cancel
  signals, not ad-hoc `AtomicBool` flags.

## Logging

Use `tracing` (not `log`) in new code. `tracing` gives structured spans that
survive async context switches, which `log` doesn't. Tauri ships with
`tauri-plugin-log` — that's fine for user-visible diagnostics, but pair it
with `tracing-subscriber` for development and debugging.
