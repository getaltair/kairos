# Rust / Tauri: Conventions

Module layout, naming, and Tauri-specific structural patterns.

## Workspace layout

Tauri projects default to a two-crate layout:

```
project/
  src/                # Frontend (Vue, React, Svelte, ...)
  src-tauri/          # Rust backend
    src/
      main.rs
      commands/
      state.rs
      errors.rs
    Cargo.toml
    tauri.conf.json
    capabilities/
  package.json
```

Keep the frontend and backend in lockstep on major version bumps. The
`tauri` crate version in `src-tauri/Cargo.toml` and the `@tauri-apps/api`
version in `package.json` must match minor versions (both 2.x, both 1.x).

## Module naming

- snake_case for files and modules: `user_store.rs`, not `UserStore.rs`.
- CamelCase for types, traits, and enums: `UserStore`, `AuthError`.
- SCREAMING_SNAKE_CASE for consts and statics.
- `mod.rs` is dying — prefer `foo.rs` + `foo/` sibling directory.

## Module organization

- `lib.rs` or `main.rs` wires things together; it should be small.
- `commands/` holds `#[tauri::command]` functions. One file per feature
  area (`commands/users.rs`, `commands/settings.rs`).
- `state.rs` defines the application state type and its `State<...>`
  implementation.
- `errors.rs` defines the domain error enum.
- Keep business logic out of command handlers — handlers are thin adapters
  that call into regular functions.

## Commands

- `#[tauri::command]` on every frontend-invokable function.
- Command functions return `Result<T, E>` where `E` implements
  `serde::Serialize` so the frontend gets a useful error payload.
- Validate inputs at the command boundary. Don't assume the frontend
  sanitized anything — commands are a security boundary.
- Don't pass raw paths from the frontend without scoping them to an
  expected base directory. Use `tauri::path::BaseDirectory` resolution.

## State management

Use `tauri::State<T>` with `Mutex<T>` or `RwLock<T>` around mutable
state. Avoid static mutables (`static mut`) — they're unsound and unwise.
For async-heavy state, use `tokio::sync::Mutex` instead of `std::sync::Mutex`.

## Capabilities (Tauri 2)

- Start with the narrowest capability set and widen only when needed.
- One capability file per window or permission boundary: `default.json`,
  `main-window.json`.
- `fs:default` is too broad for most apps — use specific scoped permissions
  like `fs:allow-read-app-data` instead.
- Review capabilities during `/qa` runs, not just at initial setup.

## Visibility

- `pub` is the most common mistake in Rust code. Prefer `pub(crate)` or
  `pub(super)` to keep the public surface intentional.
- Top-level `pub` is only for items that cross the crate boundary (FFI,
  library consumers, macros).

## Clippy-driven refinement

`cargo clippy -- -D warnings` must pass. Common offenders to anticipate in
new code:
- `clippy::unnecessary_wraps` — functions that always return `Ok(...)`.
- `clippy::too_many_arguments` — break into a struct if >6 params.
- `clippy::needless_pass_by_value` — prefer `&T` over `T` when not consumed.
