# Kotlin / Android: Formatting and tooling

Toolchain, Gradle configuration, and non-negotiables.

## Required tools

| Tool | Purpose | Config location |
|---|---|---|
| `ktlint` | Format + lint (Kotlin style) | `.editorconfig` + `ktlint` plugin |
| `detekt` | Static analysis | `config/detekt/detekt.yml` |
| Android Lint | Android-specific checks | `lint.xml` + `build.gradle.kts` |
| Gradle version catalog | Dependency versions | `gradle/libs.versions.toml` |

Run via Gradle tasks: `./gradlew ktlintCheck detekt lint`. Wire all three
into CI; don't treat lint warnings as optional.

## ktlint

Use the official `org.jlleitschuh.gradle.ktlint` plugin and pin its
version in the version catalog. Configuration lives in `.editorconfig`:

```editorconfig
[*.{kt,kts}]
indent_size = 4
max_line_length = 120
ktlint_standard_function-naming = disabled  # Composables are PascalCase
ktlint_standard_filename = disabled          # @Composable files with multiple
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
```

Line length: 120. Narrower than FastAPI's 100 because Kotlin names and
generics chew column budget fast. Don't go past 140 — diff-unfriendly.

## detekt

```yaml
# config/detekt/detekt.yml (excerpt — full config inherits from default)
build:
  maxIssues: 0
  weights:
    complexity: 2
    LongParameterList: 1

style:
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2']
    ignoreAnnotation: true
  ReturnCount:
    active: true
    max: 3
  UnusedPrivateMember:
    active: true

potential-bugs:
  UnsafeCallOnNullableType:
    active: true  # Flags !! usage
  SwallowedException:
    active: true
    ignoredExceptionTypes: ['InterruptedException']
```

`maxIssues: 0` keeps the baseline at zero. If legacy code has
pre-existing violations, generate a baseline once
(`./gradlew detektBaseline`) and commit it, then treat any new issue as
a failure.

## Android Lint

Configure via `lint.xml` at module root:

```xml
<lint>
  <issue id="MissingPermission" severity="error" />
  <issue id="HardcodedText" severity="error" />
  <issue id="UnusedResources" severity="error" />
  <issue id="ObsoleteSdkInt" severity="error" />
  <!-- Baseline for legacy modules only -->
</lint>
```

Set `abortOnError = true` in `build.gradle.kts` so CI fails on new lint
issues. `baseline` files are acceptable for legacy code only — no new
baselines in green modules.

## Gradle version catalog

Use `gradle/libs.versions.toml`. Don't inline versions in module
`build.gradle.kts` files.

```toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.10.00"
coroutines = "1.9.0"
hilt = "2.52"

[libraries]
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

Convention plugins in `build-logic/` centralize common module
configuration (Android library setup, Compose enablement, Hilt wiring).
Apply them by plugin ID, not by copy-pasting `build.gradle.kts` blocks.

## SDK levels

- `compileSdk` and `targetSdk` track the latest stable API level. Update
  annually when the new API lands in stable.
- `minSdk` is a product decision, not a technical one — but default to
  **24 (Android 7.0)** for new projects in 2026. Below that, you lose
  modern security primitives and ART optimizations.
- Don't bump `targetSdk` without testing behavior changes; each version
  introduces intended incompatibilities.

## R8 / ProGuard

- R8 is on for release builds (`isMinifyEnabled = true`,
  `isShrinkResources = true`).
- Keep rules live in `proguard-rules.pro` at module root, never in
  `proguard-android-optimize.txt` (that's the platform file).
- Every library that uses reflection needs consumer rules — prefer
  libraries that ship their own (`-consumer-rules.pro` in the AAR).
- Test release builds in CI. A release-only crash from a missing keep
  rule is the classic Android production bug.

## Non-negotiables

- No `@Suppress("...")` without a comment explaining why.
- No `TODO()` in code that ships — convert to a tracked ticket or
  implement.
- Never commit with `ktlintCheck` or `detekt` failing. CI enforces this
  but local hooks (pre-commit) catch it earlier.
- Kotlin compiler warnings are errors in CI: `allWarningsAsErrors = true`
  in the `compilerOptions` block.

## Pre-commit (optional)

If used, run `./gradlew ktlintFormat` and `./gradlew detekt` on staged
`.kt` files only. Full `./gradlew check` at pre-commit time is too slow
for most projects — save it for pre-push or CI.
