# Rust / Tauri: Formatting and tooling

Toolchain, configuration, and non-negotiables.

## Required tools

| Tool | Purpose | Config location |
|---|---|---|
| `rustfmt` | Format | `rustfmt.toml` |
| `clippy` | Lint | `Cargo.toml` `[lints]` or `clippy.toml` |
| `cargo-audit` | Dep vulnerabilities | `deny.toml` (optional via cargo-deny) |
| `cargo-deny` | Licenses + sources + advisories | `deny.toml` |

All four come from standard rustup components or `cargo install`. Pin the
MSRV (minimum supported Rust version) in `Cargo.toml`:

```toml
[package]
rust-version = "1.79"
```

Don't chase nightly unless a specific feature demands it.

## Rustfmt

Minimum `rustfmt.toml`:

```toml
edition = "2021"
max_width = 100
use_small_heuristics = "Max"
imports_granularity = "Crate"
group_imports = "StdExternalCrate"
```

`imports_granularity = "Crate"` collapses repeated `use` lines from the
same crate. `group_imports` enforces std/external/crate ordering. Don't
bikeshed these — set once and move on.

## Clippy configuration

Enable the extra-pedantic lints project-wide in `Cargo.toml`:

```toml
[lints.clippy]
pedantic = { level = "warn", priority = -1 }
nursery = { level = "warn", priority = -1 }
unwrap_used = "deny"
expect_used = "warn"
panic = "deny"
unimplemented = "deny"
todo = "warn"

[lints.rust]
unsafe_code = "deny"  # remove if crate genuinely needs unsafe
missing_docs = "warn"
```

`unwrap_used = "deny"` is the single most valuable lint — it catches the
most common class of production crash before it ships.

## Non-negotiables

- **`cargo clippy --all-targets --all-features -- -D warnings` must pass**
  before commit. `-D warnings` promotes warnings to errors locally.
- **`cargo fmt --check` must pass** before commit.
- **`cargo test`** must pass. Integration tests that hit a real service go
  behind a feature flag or the `--ignored` flag so they don't run by
  default in CI.
- **`cargo audit`** runs weekly in CI at minimum. Vulnerable transitive
  dependencies are not a someday-problem.

## Cargo-deny

For any crate that ships as a binary to end users (Tauri apps qualify),
configure `cargo-deny` to enforce:

- License allowlist (MIT/Apache-2.0/BSD/ISC — GPL inclusion is a legal
  question, not a convenience one).
- No duplicate dependency versions (`[bans] multiple-versions = "warn"`).
- Advisory database checks (subsumes `cargo audit` if configured).

## MSRV policy

- New projects target a recent stable (2 versions behind current is a
  reasonable floor).
- MSRV bumps require updating `rust-version` in `Cargo.toml` AND CI.
- Don't bump MSRV without a concrete need (a new stdlib API, a dep that
  dropped older versions).

## Feature flags

- `default` features should be the minimum that works for the most common
  use case.
- Heavy optional dependencies go behind feature flags (`tokio-full`,
  `serde_json` vs. `simd-json`).
- Don't use features as a way to bikeshed implementation choices — pick one
  and ship.

## Tauri-specific

- Run `tauri info` in CI to verify the Tauri toolchain matches across
  dev/build environments.
- `tauri build` for release, `tauri dev` for development. Don't hand-roll
  bundling.
- Commit `tauri.conf.json` with `bundle.active = true` and inspect the
  bundle identifier, icons, and signing keys before the first release.

## Package manager

Cargo is the only package manager. The frontend side uses whatever the
frontend stack uses (pnpm, bun, npm) — don't let the two lockfiles drift.
