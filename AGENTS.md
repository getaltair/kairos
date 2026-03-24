# Kairos Project Conventions

Instructions for AI agents working on the Kairos codebase.

---

## Code Exploration Policy

Always use jCodemunch-MCP tools — never fall back to Read, Grep, Glob, or Bash for code exploration.

- Before reading a file: use `get_file_outline` or `get_file_content`
- Before searching: use `search_symbols` or `search_text`
- Before exploring structure: use `get_file_tree` or `get_repo_outline`
- Call `list_repos` first; if the project is not indexed, call `index_folder` with the current directory.

---

## Project Overview

Kairos is an ADHD-optimized habit building and tracking system for Android and WearOS. The app serves as **permanent external scaffolding** for users whose brains operate on interest-based motivation rather than importance-based motivation.

### Core Philosophy

- **Shame-free**: No streaks, no gamification, no judgment
- **Flexible completion**: Partial credit always valid
- **Context over time**: Event-based triggers ("after brushing teeth") work better than time-based
- **Designed for return**: Expect cycling between engagement and disengagement

---

## Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material 3) |
| DI | Hilt |
| DB | Room |
| Sync | Firebase (Firestore + Auth) |
| Build | Gradle Kotlin DSL + Version Catalog |
| Target | Android (minSdk 31, targetSdk 36) |
| Future | WearOS, Compose Desktop (Pi Kiosk) |

---

## Architecture

Clean Architecture with clear layer separation:

```
app/
├── presentation/     # Compose UI, ViewModels
│   ├── screens/
│   ├── components/
│   └── theme/
├── domain/          # Use cases, domain models, repository interfaces
│   ├── model/
│   ├── usecase/
│   └── repository/
└── data/            # Repository implementations, Room DAOs, sync
    ├── local/
    │   ├── dao/
    │   └── entity/
    ├── remote/
    └── repository/
```

### Data Flow

- UI observes StateFlow from ViewModel
- ViewModel calls Use Cases
- Use Cases orchestrate Repository calls
- Repositories write to Room, then trigger async Firestore sync

---

## Kotlin/Android Conventions

### Gradle & Dependencies

- Use version catalog (`gradle/libs.versions.toml`) for all dependencies
- Reference via `libs.pluginName` or `libs.library.name`
- Keep `build.gradle.kts` files minimal

### Dependency Injection

- Use Hilt for all DI
- Constructor injection preferred
- Repository and Use Case lifetimes: `@Singleton`
- ViewModels: Hilt-managed via `@HiltViewModel`

### Null Safety

- Avoid `!!` operator
- Use `requireNotNull` with descriptive message when assertion needed
- Prefer `?.let` and early returns over nested null checks

### Coroutines

- ViewModels: Use `viewModelScope`
- Use Cases: Make suspend functions, don't create own scope
- Dispatchers: Inject `DispatcherProvider` for testability

```kotlin
class ExampleUseCase(
    private val repository: ExampleRepository,
    private val dispatchers: DispatcherProvider
) {
    suspend operator fun invoke(): Result<Data> = withContext(dispatchers.io) {
        repository.fetchData()
    }
}
```

---

## Jetpack Compose Guidelines

### State Management

- Unidirectional data flow: UI State → UI → Event → ViewModel → State
- Use `StateFlow<UiState>` for UI state
- Use `Channel<UiEvent>` for one-time events (navigation, toasts)

```kotlin
data class ExampleUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ExampleUiEvent {
    data class ShowToast(val message: String) : ExampleUiEvent
    data class NavigateToDetail(val id: String) : ExampleUiEvent
}
```

### Compose Best Practices

- Mark state types as `@Stable` or use immutable collections
- Use `remember` for expensive computations
- Use `derivedStateOf` for derived reactive state
- Hoist state to appropriate level (ViewModel for screen state)
- Avoid recomposition: use stable keys in LazyColumn

### Theming

- Material 3 components throughout
- Theme defined in `ui/theme/` (Color.kt, Theme.kt, Type.kt)
- Support for light/dark mode via `isSystemInDarkTheme()`

### Composable Naming

- Screen composables: `XxxScreen`
- Stateless UI components: `XxxContent` or descriptive name
- Preview functions: `XxxPreview`

---

## Domain Layer Patterns

### Use Cases

- Single public `invoke` function (operator)
- Return `Result<T>` sealed class for error handling
- Keep pure business logic, no Android framework

```kotlin
class CompleteHabitUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {
    suspend operator fun invoke(
        habitId: String,
        type: CompletionType
    ): Result<Completion> = runCatching {
        val habit = habitRepository.getById(habitId) ?: throw NotFoundException()
        val completion = Completion.create(habitId, type)
        completionRepository.insert(completion)
        completion
    }
}
```

### Repository Interfaces

- Define in `domain/repository/`
- Return domain models, not entities
- Suspend functions for all data access

---

## Testing Commands

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint

# Build debug APK
./gradlew assembleDebug

# Clean build
./gradlew clean build
```

### Test Location

- Unit tests: `src/test/java/`
- Instrumented tests: `src/androidTest/java/`

---

## UX Principles (ADHD-Optimized)

When implementing features, follow these principles from the project docs:

1. **Executive Function Externalization** - System does cognitive work, not user
2. **Sustainable Imperfection** - Design for return, not streak
3. **Immediate Dopamine** - Every interaction provides instant positive feedback
4. **Context Over Time** - Event-based triggers over time-based
5. **Flexible Structure** - Structured options with escape hatches
6. **Shame-Free Recovery** - Never judge, missed days are data
7. **Built-in Novelty** - Variation to combat boredom

### Never Implement

- Streak counters or longest streak tracking
- Gamification (points, badges, leaderboards)
- Shame-inducing copy ("You failed!", "X days since...")
- Rigid time-only triggers

---

## File Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Screen | `XxxScreen.kt` | `TodayScreen.kt` |
| ViewModel | `XxxViewModel.kt` | `TodayViewModel.kt` |
| Use Case | `XxxUseCase.kt` | `CompleteHabitUseCase.kt` |
| Repository | `XxxRepository.kt` / `XxxRepositoryImpl.kt` | `HabitRepository.kt` |
| Entity | `XxxEntity.kt` | `HabitEntity.kt` |
| DAO | `XxxDao.kt` | `HabitDao.kt` |
| Domain Model | `Xxx.kt` | `Habit.kt` |

---

## Error Handling

- Use sealed `Result<T>` class in Use Cases
- Map errors to UI-friendly messages in ViewModel
- Never expose raw exceptions to UI

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Exception) : Result<Nothing>
}
```

---

## Git Conventions

- Use conventional commits: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`
- Keep commits atomic and focused
- Reference issue numbers when applicable
