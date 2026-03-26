# Repository Primer

## What This Repo Is

A habit tracking Android application that helps users build and maintain daily habits. The app provides a focused interface for tracking habits, viewing daily progress, and managing settings. Built with modern Android development practices using Jetpack Compose and Clean Architecture.

## Product / Feature Structure

| Feature / Module | Entry Point                 | Description                                           |
| ---------------- | --------------------------- | ----------------------------------------------------- |
| **Today**        | Navigation route `today`    | Daily habit tracking view for today's progress        |
| **Habit**        | Navigation route `habit`    | Habit creation, editing, and individual habit details |
| **Settings**     | Navigation route `settings` | App configuration and user preferences                |

## Tech Stack

### Core Framework

- **Android Gradle Plugin 9.0.1** with Kotlin DSL
- **Kotlin 2.3.20** with Kotlin Compose Compiler plugin
- **Jetpack Compose 2026.03.00** for declarative UI
- **Target SDK 36** (Android 16), Min SDK 31 (Android 12)
- **Java 21** compatibility for compilation

### Project Structure

Multi-module project following Clean Architecture:

```
app/           # Presentation layer (MainActivity, Application class, Navigation)
domain/        # Business logic layer (pure Kotlin, no Android dependencies)
data/          # Data layer (repositories, data sources, Room database)
core/          # Shared utilities and ViewModels
ui/            # Shared UI components, theming, and design system

feature/
  habit/        # Habit-specific feature module
  settings/      # Settings feature module
  today/         # Today view feature module
```

### Database

- **Room 2.8.4** for local data persistence
- Entities and DAOs in `data/` module
- **Firestore** (BOM included, ready for cloud sync)

### Authentication

- **Firebase Auth** (BOM included, ready for implementation)
- Currently scaffolded but not implemented

### Payments / Billing

None - not applicable to this project

### Analytics / Monitoring

- **Timber 5.0.1** for logging
- No analytics currently configured

### Styling

- **Jetpack Compose Material3** for UI components
- **Compose Navigation 2.9.7** for screen navigation
- Custom theme in `ui/src/main/kotlin/com/getaltair/kairos/ui/theme/`
- Light/dark color schemes with Material3 design tokens

### Key Architectural Decisions

1. **Module dependency flow**: `app → core/ui → domain → data`. No circular dependencies. Domain layer is framework-agnostic pure Kotlin.
2. **Version catalog**: All dependencies managed centrally in `gradle/libs.versions.toml`. Never add versions directly to build files.
3. **Compose-first**: All UI built with Jetpack Compose. No XML layouts except AndroidManifest and resources.
4. **Kotlin code style**: Set to "official" in `gradle.properties`. Follow Kotlin Style Guide.

## Important Paths

| Path                                                                    | Purpose                                                       |
| ----------------------------------------------------------------------- | ------------------------------------------------------------- |
| `gradle/libs.versions.toml`                                             | Central dependency version catalog - update here for new libs |
| `settings.gradle.kts`                                                   | Module definitions - add new modules here                     |
| `app/src/main/kotlin/com/getaltair/kairos/MainActivity.kt`              | Entry point activity                                          |
| `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt` | Navigation graph                                              |
| `ui/src/main/kotlin/com/getaltair/kairos/ui/theme/`                     | App theming (Color, Type, Theme)                              |
| `app/build.gradle.kts`                                                  | App module dependencies and configuration                     |
| `build.gradle.kts`                                                      | Root project - plugin aliases for all modules                 |

## Build Commands

```bash
# Development
./gradlew build                 # Full build of all modules
./gradlew :app:installDebug    # Install debug APK to connected device
./gradlew :app:assembleDebug   # Build debug APK only

# Code Quality
./gradlew ktlintFormat         # Auto-format Kotlin code
./gradlew detekt              # Run code smell analysis

# Testing
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests on device/emulator

# Clean
./gradlew clean                # Clean all build artifacts
```

## Environment Variables

**Required for full functionality:**

None currently - app runs without required environment variables.

**Optional:**

- Firebase configuration (google-services.json) - needed for Auth/Firestore features when implemented
- Keystore properties - for release builds with signing

## Common Gotchas

1. **Version catalog** - Always use `libs.xxx` syntax from `gradle/libs.versions.toml`. Never hardcode versions in module build files.
2. **Module includes** - New feature modules must be added to `settings.gradle.kts` `include()` list.
3. **Compose compiler** - Kotlin version must match between `kotlin-android` plugin and `kotlin-compose` plugin (both 2.3.20).
4. **Clean build after deps** - After changing versions in `libs.versions.toml`, run `./gradlew clean` first.
5. **Namespace changes** - When adding new modules, ensure `namespace` in `android {}` block matches the package structure.
6. **CRITICAL: Never bypass git hooks** - NEVER use `git commit --no-verify`. If pre-commit hooks fail, fix the underlying issues (ktlint formatting, tests, etc.) before committing.

## Workflows

1. **Feature module creation**: Add module directory, `build.gradle.kts`, include in `settings.gradle.kts`, update dependencies.
2. **Dependency updates**: Edit `gradle/libs.versions.toml`, run clean build, verify no API breakage.
3. **Screen additions**: Add composable to navigation graph in `KairosNavGraph.kt`, add route definition.

## Notes

1. "Kairos" (Καιρός) is Ancient Greek for "the right, critical, or opportune moment" - fitting for a habit tracking app focused on timing and consistency.
2. Feature modules (`habit/`, `settings/`, `today/`) are scaffolded but not yet implemented in build files.
3. Room and Firebase BOM are included but schema/entities are not yet defined - database layer is a work in progress.
4. Project uses Kotlin official code style - 4-space indentation, expression bodies, immutability preference.
