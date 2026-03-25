# Kotlin/Android Compose Conventions

paths:
  - app/
  - ui/
  - domain/
  - data/
  - core/

## Code Style

- Follow Kotlin coding conventions (Kotlin Style Guide)
- Use 4-space indentation
- Prefer expression bodies for simple functions
- Use `val` over `var` by default
- Use `when` expressions for branching when appropriate
- Prefer data classes for simple value holders

## Compose Guidelines

- Follow declarative UI patterns
- Keep composables focused and reusable
- Use state hoisting for shared state
- Prefer `remember` for derived values
- Use `derivedStateOf` for derived state
- Avoid side effects in composables
- Keep parameter lists reasonable (use data objects if needed)

## Architecture

### Clean Architecture Layers

```
app/      # Presentation layer (UI, ViewModels)
domain/   # Business logic (Use cases, Entities)
data/     # Data layer (Repositories, Data sources)
core/     # Shared utilities, ViewModels
ui/       # Shared UI components and theming
```

### Dependency Flow

- app → domain → data
- app → core
- app → ui
- No circular dependencies
- Domain layer is framework-agnostic (pure Kotlin)
- data → core for utilities

## Hilt/DI

- Use Hilt for dependency injection
- Inject dependencies via constructor
- Use `@Singleton` for thread-safe singletons
- Use `@ViewModel` for ViewModels
- Qualify ambiguous types with `@Named` or custom qualifiers
- Application class in `app/` annotated with `@HiltAndroidApp`

## Coroutines

- Use structured concurrency
- Prefer coroutineScope over GlobalScope
- Use `viewModelScope` in ViewModels
- Use appropriate dispatchers (`Dispatchers.IO`, `Dispatchers.Main`)
- Handle coroutine cancellations properly

## Room Database

- Use entities with primary keys
- Define relationships with foreign keys
- Use DAOs for database operations
- Migrations should handle data preservation
- Consider using `@Transaction` for multi-step operations

## Firebase

- Use Firebase Auth for authentication
- Use Firestore for cloud storage
- Handle offline scenarios gracefully
- Use snapshot listeners for real-time updates
- Clean up listeners in `onDispose`

## Logging

- Use Timber for logging (configured, not yet wired)
- Tag can be omitted—Timber uses the class name
- Use `Timber.d()` for debug logs
- Use `Timber.e()` for errors
- Use `Timber.w()` for warnings

## Testing

- Unit tests for pure functions
- Integration tests for data layer
- UI tests with Compose Testing
- Mock dependencies appropriately
- Use `runTest` coroutine test runner

## Naming Conventions

- Classes: PascalCase (e.g., `HabitTracker`)
- Functions: camelCase (e.g., `trackCompletion`)
- Constants: UPPER_SNAKE_CASE or camelCase (module-level)
- Composables: PascalCase starting with verb (e.g., `HabitListScreen`)
- Private properties: camelCase with underscore prefix if needed

## Linting

- ktlint: Code formatting and style
- detekt: Code smells and complexity
- Fix lint issues before committing
- Run `./gradlew ktlintFormat` to auto-fix
