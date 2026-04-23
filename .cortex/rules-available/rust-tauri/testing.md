# Rust / Tauri: Testing

Unit, integration, and end-to-end testing for Rust and Tauri apps.

## Unit tests

Co-located with the code under test, in a `#[cfg(test)] mod tests` block
at the bottom of the file:

```rust
pub fn normalize_username(s: &str) -> String {
    s.trim().to_lowercase()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn trims_and_lowercases() {
        assert_eq!(normalize_username("  Alice "), "alice");
    }

    #[test]
    fn empty_input() {
        assert_eq!(normalize_username(""), "");
    }
}
```

Keep unit tests focused on a single function. If a test needs a complex
setup, it probably belongs as an integration test.

## Integration tests

In the `tests/` directory at the crate root — separate binaries, only see
the crate's public API. This enforces the public/private boundary:

```
src-tauri/
  src/
    lib.rs
  tests/
    users.rs
    settings.rs
```

One file per feature area. Use `#[tokio::test]` for async tests:

```rust
#[tokio::test]
async fn create_user_persists_to_db() {
    let store = test_store().await;
    let user = store.create_user("alice").await.unwrap();
    let found = store.get_user(&user.id).await.unwrap();
    assert_eq!(found.username, "alice");
}
```

## Tauri command testing

Tauri commands that depend on `State<T>` or `AppHandle` are awkward to
test directly. The clean solution is to make commands thin:

```rust
// commands/users.rs — thin, hard to test in isolation
#[tauri::command]
async fn create_user(
    state: State<'_, AppState>,
    name: String,
) -> Result<User, AppError> {
    users::create(&state.db, &name).await
}

// users.rs — thick, fully testable
pub async fn create(db: &Pool, name: &str) -> Result<User, AppError> {
    // ...
}
```

Test `users::create` directly with a test database pool. Don't try to set
up a full Tauri app instance just to test business logic.

## E2E via WebDriver

Tauri supports WebDriver via `tauri-driver`. Use it for critical end-to-end
flows only — setup and teardown are slow, and any bugs these catch should
also be catchable cheaper. `playwright` or `webdriverio` work as the driver
client.

## Property tests

Use `proptest` for input-validation and parser code. One property test
catches a class of bugs that ten example-based tests miss:

```rust
use proptest::prelude::*;

proptest! {
    #[test]
    fn parse_roundtrip(s in "\\PC{0,100}") {
        if let Ok(parsed) = parse(&s) {
            prop_assert_eq!(serialize(&parsed), s);
        }
    }
}
```

Reach for `proptest` when the input space is large and structured;
example-based tests remain the default for most logic.

## Async test runtime

`#[tokio::test]` is the default. For tests that need a specific runtime
flavor (multi-thread, time-paused), use the explicit form:

```rust
#[tokio::test(flavor = "multi_thread", worker_threads = 4)]
async fn concurrent_load() { ... }
```

`start_paused = true` is invaluable for time-based tests — advances
`tokio::time` instantly instead of waiting in real time.

## Snapshot tests

`insta` is the standard for snapshot assertions. Use it sparingly, mostly
for serialization round-trips and formatted output — don't snapshot large
structured data that will churn.

## What not to test

- Don't test rustc — `assert_eq!(some_struct.field, value)` after an
  assignment adds no value.
- Don't test third-party crates through your code — mock at your boundary.
- Don't test `Debug`/`Display` impls unless format is load-bearing (logs,
  parsed by tooling).
