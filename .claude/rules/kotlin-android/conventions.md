# Kotlin / Android: Conventions

Module layout, naming, and Android-specific structural patterns.

## Gradle module layout

Single-module apps use `:app`. Once the app exceeds ~50 files, split by
feature, not by layer:

```
project/
  app/                      # Assembles the final binary; thin
  core/
    data/                   # Repositories, DTOs, DB
    domain/                 # Use cases, domain models
    ui/                     # Shared Composables, theme
  feature/
    onboarding/
    home/
    settings/
  build-logic/              # Convention plugins
  gradle/libs.versions.toml # Version catalog
```

Feature modules depend on `core:*` modules but never on each other. When
two features need to share code, hoist it into a `core:` module instead of
adding a feature-to-feature dependency.

## Package naming

- Reverse-DNS root (`com.company.app`) matching `applicationId`.
- Feature packages mirror module names: `com.company.app.feature.home`.
- No `util` or `helpers` packages — put the code next to its owner. If it
  truly has no owner, it's probably a `core:` module concern.

## File and type naming

- One top-level class per file; file name matches the class.
- `PascalCase` for classes, `camelCase` for functions and properties,
  `SCREAMING_SNAKE_CASE` for `const val`.
- Composables are `PascalCase` and named after what they render:
  `UserAvatar`, `HomeScreen`. Not `ShowUser` or `RenderAvatar`.
- `ViewModel` suffix on ViewModels, `Repository` on repositories,
  `UseCase` on use cases. Don't suffix plain domain classes.

## Resource naming

Use `type_feature_description` for resource IDs so they group alphabetically:

- `btn_login_submit`, `tv_profile_username`, `ic_arrow_back_24`.
- Colors: semantic names (`color_surface_primary`) over literal
  (`color_blue_500`). Literal names couple design to implementation.
- Strings: `screen_action` — `home_welcome_title`, `login_error_invalid`.
- Never reference layout XML IDs from cross-feature code; feature UI IDs
  are private.

## Source sets

- `src/main/` — production code.
- `src/test/` — JVM unit tests (no Android framework).
- `src/androidTest/` — instrumented tests that run on a device/emulator.
- Keep `src/debug/` and `src/release/` lean. Flavor-specific code is a
  smell past a few files; prefer runtime configuration.

## Architecture

Default to unidirectional data flow: **UI → ViewModel → UseCase →
Repository → DataSource**. State flows back up as immutable `StateFlow`.

- `ViewModel` owns `StateFlow<UiState>` and exposes it as read-only.
- `UiState` is a sealed class or data class — no mutable fields.
- UI observes via `collectAsStateWithLifecycle()` in Compose or
  `repeatOnLifecycle(STARTED)` in Views.
- Side effects (navigation, snackbars) go through a separate `Channel`
  or `SharedFlow`, not through state.

## Dependency injection

Hilt is the default for app DI. Koin is acceptable for KMP-shared modules.
Don't mix them in the same module.

- Annotate Activities, Fragments, and ViewModels with `@AndroidEntryPoint`
  / `@HiltViewModel`.
- Bind interfaces with `@Binds` in an `@Module`, not `@Provides`.
- Scope deliberately — `@Singleton` for stateless services,
  `@ViewModelScoped` for per-screen caches, nothing global with mutable
  state.

## Compose

- Hoist state: Composables take `state: T` and `onEvent: (Event) -> Unit`
  rather than holding their own ViewModels.
- `Modifier` is always the first optional parameter, defaults to
  `Modifier`, and is passed through to the root layout.
- Previews live in the same file as the Composable, annotated
  `@Preview` + `@Composable`, named `<Composable>Preview`.
- No `LaunchedEffect(Unit) { ... }` for one-shot work tied to lifecycle —
  use `ViewModel.init` or a `LifecycleEventObserver`.
