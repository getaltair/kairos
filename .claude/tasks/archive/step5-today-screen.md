# Plan: Step 5 -- Today Screen + Habit Completion UI

## Task Description

Implement the Today Screen, the primary daily interface users see in the Kairos habit tracker. This is a P0 feature from `docs/implementation-plan.md` Step 5. It includes the habit list grouped by category, a progress ring header, a completion bottom sheet (Done/Partial/Skip), undo support, empty/all-done states, haptic feedback, and navigation wiring.

**Critical prerequisite gap:** Step 4 (Repository Layer + Use Cases) is incomplete. The domain repository interfaces exist but have no implementations. No use cases exist. The `KairosDatabase` has no entities or DAOs registered. The `DataModule` DI is a stub. These must be built as foundational work before the UI layer.

Work is on a **new branch** off `main`.

## Objective

When complete, users can:

1. See today's active habits grouped by category (Morning, Afternoon, Evening, Anytime -- excluding Departure)
2. See a progress ring showing overall completion percentage
3. Tap a habit to open a bottom sheet with Done/Partial/Skip options
4. See visual feedback (checkmark, animation, haptics) on completion
5. Undo a completion within 30 seconds via snackbar
6. See an empty state when no habits exist
7. See a celebration state when all habits are completed
8. Navigate to the Today screen as the app's default destination

## Problem Statement

The app currently renders a placeholder "Hello Android!" screen. Steps 1-3 built the domain models, Room entities, DAOs, and mappers, but the repository implementations, use cases, ViewModel, and Compose UI for the Today screen do not exist. The `KairosDatabase` is an empty shell with no entities registered. The data DI module only provides the database instance -- no DAOs or repositories are wired.

## Solution Approach

Build in three phases:

1. **Foundation**: Fix KairosDatabase, implement repository layer, create use cases, wire DI
2. **Core UI**: Build TodayViewModel, TodayScreen, and all UI components
3. **Integration**: Wire navigation, add polish (haptics, animations), validate

All work targets a new feature branch `feature/step5-today-screen`.

## Relevant Files

### Existing Files to Modify

- `data/src/main/kotlin/com/getaltair/kairos/data/database/KairosDatabase.kt` -- Register all entities, DAOs, and TypeConverters (currently empty shell)
- `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt` -- Wire DAOs and repository implementations into Koin (currently only provides database)
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/di/TodayModule.kt` -- Wire ViewModel and use cases into Koin (currently empty)
- `app/src/main/java/com/getaltair/kairos/MainActivity.kt` -- Replace placeholder with NavHost routing to TodayScreen
- `core/build.gradle.kts` -- Add `:domain` and `:data` dependencies for use cases

### Existing Files to Reference (Read-Only)

- `docs/implementation-plan.md` -- Step 5 spec (screen layout, card states, completion sheet, done criteria)
- `docs/01-prd-core.md` -- UI Requirements, UC-2 (Complete Habit), UC-3 (Skip Habit)
- `docs/10-user-flows.md` -- Flow 3 (Daily Check-in), Flow 4 (Complete Habit)
- `docs/00-project-overview.md` -- Design Principles
- `docs/06-invariants.md` -- C-1 through C-5 completion invariants
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` -- Domain Habit entity
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Completion.kt` -- Domain Completion entity
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/HabitRepository.kt` -- Repository interface
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/CompletionRepository.kt` -- Repository interface
- `domain/src/main/kotlin/com/getaltair/kairos/domain/common/Result.kt` -- Result wrapper
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/` -- All sealed class enums (HabitCategory, CompletionType, SkipReason, HabitFrequency, HabitPhase, HabitStatus)
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt` -- DAO with `getTodayHabitsWithCompletions()` Flow query and `TodayHabitWithCompletion` result class
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/CompletionDao.kt` -- Completion DAO
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/HabitEntityMapper.kt` -- Bidirectional entity-domain mapper
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/CompletionEntityMapper.kt` -- Bidirectional entity-domain mapper
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/HabitEntity.kt` -- Room entity (all 9 entities follow this pattern)
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/CompletionEntity.kt` -- Room entity with foreign key to habits
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt` -- Consolidated type converters
- `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt` -- Application class with Koin setup (loads dataModule + todayModule)
- `app/src/main/java/com/getaltair/kairos/ui/theme/Theme.kt` -- KairosTheme with Material3 dynamic color
- `feature/today/build.gradle.kts` -- Today module dependencies (already has :domain, :data, :ui, :core, Compose, Koin, Navigation)
- `settings.gradle.kts` -- Module includes (`:feature:today` already included)

### New Files

**Data Layer (repository implementations):**

- `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt`
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/CompletionRepositoryImpl.kt`

**Domain Layer (composite model):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitWithStatus.kt`

**Core Layer (use cases):**

- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/GetTodayHabitsUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/CompleteHabitUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/SkipHabitUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/UndoCompletionUseCase.kt`

**Feature Layer (UI):**

- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayViewModel.kt`
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayUiState.kt`
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayScreen.kt`
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/HabitCard.kt`
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/CompletionBottomSheet.kt`
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/ProgressRing.kt`
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/EmptyState.kt`

**Navigation:**

- `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt`

## Implementation Phases

### Phase 1: Foundation

Fix the incomplete Step 4 prerequisites:

1. **KairosDatabase** -- Register all 9 entity classes, all DAO abstract methods, and `@TypeConverters(RoomTypeConverters::class)`. Currently an empty abstract class.
2. **Repository Implementations** -- `HabitRepositoryImpl` wraps `HabitDao` with `HabitEntityMapper` conversions, returns `Result<T>`. `CompletionRepositoryImpl` wraps `CompletionDao` with `CompletionEntityMapper`. Both use `Dispatchers.IO` for DAO calls.
3. **HabitWithStatus model** -- Composite domain model combining `Habit`, nullable `Completion` (today's), and `weekCompletionRate: Float`.
4. **Use Cases** -- Pure Kotlin classes in `core/` that orchestrate repositories:
    - `GetTodayHabitsUseCase`: Query active habits for today, join with completions, filter by frequency and exclude DEPARTURE category, compute weekly completion rates
    - `CompleteHabitUseCase`: Create FULL completion with invariant validation (C-1 through C-5)
    - `SkipHabitUseCase`: Create SKIPPED completion with optional reason
    - `UndoCompletionUseCase`: Delete completion within 30-second window
5. **DI Wiring** -- DataModule provides all DAOs (via database instance) and binds repository interfaces to implementations. Core module updated with `:domain` + `:data` dependencies.

### Phase 2: Core Implementation

Build the Today screen UI:

1. **TodayUiState** -- Data class with `habits: Map<HabitCategory, List<HabitWithStatus>>`, `progress: Float`, `date: LocalDate`, `isLoading: Boolean`, `undoState: UndoState?`, `isAllDone: Boolean`
2. **TodayViewModel** -- Collects `GetTodayHabitsUseCase` flow, groups by category in display order (Morning -> Afternoon -> Evening -> Anytime), handles complete/skip/undo actions, manages 30-second undo timer
3. **ProgressRing** -- Circular progress indicator showing completion percentage with animated fill
4. **HabitCard** -- Card showing habit name, anchor text, category emoji, with visual states for Pending/Completed/Partial/Skipped. Touch target >= 48dp
5. **CompletionBottomSheet** -- Modal bottom sheet with three options: Done (immediate), Partial (slider 1-99), Skip (optional reason picker)
6. **EmptyState** -- Illustration + "Add your first habit" CTA when no habits exist
7. **TodayScreen** -- Scaffold composing header (date + progress ring), LazyColumn of categorized habit cards, FAB placeholder, undo snackbar

### Phase 3: Integration & Polish

1. **Navigation** -- Create `KairosNavGraph.kt` with `today` as start destination. Update `MainActivity` to use NavHost instead of placeholder.
2. **TodayModule DI** -- Wire ViewModel and use cases in Koin module
3. **Haptics** -- Trigger haptic feedback on completion tap
4. **Animations** -- Checkmark animation on completion, subtle celebration on all-done
5. **Process Death** -- Ensure ViewModel state survives via `SavedStateHandle` for undo timer

## Team Orchestration

- You operate as the team lead and orchestrate the team to execute the plan.
- You're responsible for deploying the right team members with the right context to execute the plan.
- IMPORTANT: You NEVER operate directly on the codebase. You use `Task` and `Task*` tools to deploy team members to the building, validating, testing, deploying, and other tasks.
    - This is critical. Your job is to act as a high level director of the team, not a builder.
    - Your role is to validate all work is going well and make sure the team is on track to complete the plan.
    - You'll orchestrate this by using the Task\* Tools to manage coordination between the team members.
    - Communication is paramount. You'll use the Task\* Tools to communicate with the team members and ensure they're on track to complete the plan.
- Take note of the session id of each team member. This is how you'll reference them.

### Team Members

- Specialist
    - Name: builder-data
    - Role: Build data layer foundation -- fix KairosDatabase, implement repository impls, create HabitWithStatus model, wire DataModule DI
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-core
    - Role: Build use cases in core module, update core build.gradle.kts dependencies
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-ui
    - Role: Build TodayViewModel, TodayScreen, all UI components, CompletionBottomSheet, wire TodayModule DI, update MainActivity navigation
    - Agent Type: general-purpose
    - Resume: true

- Quality Engineer (Validator)
    - Name: validator
    - Role: Validate completed work against acceptance criteria (read-only inspection mode)
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

- IMPORTANT: Execute every step in order, top to bottom. Each task maps directly to a `TaskCreate` call.
- Before you start, run `TaskCreate` to create the initial task list that all team members can see and execute.

### 1. Create Feature Branch

- **Task ID**: create-branch
- **Depends On**: none
- **Assigned To**: orchestrator (self)
- **Agent Type**: N/A (orchestrator runs directly)
- **Parallel**: false
- Run `git checkout -b feature/step5-today-screen` from main

### 2. Fix KairosDatabase and Wire DataModule

- **Task ID**: fix-database-and-di
- **Depends On**: create-branch
- **Assigned To**: builder-data
- **Agent Type**: general-purpose
- **Parallel**: false
- Read all 9 entity files in `data/src/main/kotlin/com/getaltair/kairos/data/entity/` to get the class names
- Read all 9 DAO files in `data/src/main/kotlin/com/getaltair/kairos/data/dao/` to get the interface names
- Read `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt` for the TypeConverters class
- Update `KairosDatabase.kt` to:
    - Add `@Database(entities = [HabitEntity::class, CompletionEntity::class, RoutineEntity::class, RoutineHabitEntity::class, RoutineVariantEntity::class, RoutineExecutionEntity::class, RecoverySessionEntity::class, HabitNotificationEntity::class, UserPreferencesEntity::class], version = 1, exportSchema = true)`
    - Add `@TypeConverters(RoomTypeConverters::class)`
    - Add abstract DAO accessor methods for all 9 DAOs
- Update `DataModule.kt` to provide all DAOs via `get<KairosDatabase>().habitDao()` etc.

### 3. Implement Repository Implementations

- **Task ID**: implement-repositories
- **Depends On**: fix-database-and-di
- **Assigned To**: builder-data
- **Agent Type**: general-purpose
- **Parallel**: false
- Read the repository interfaces: `HabitRepository.kt`, `CompletionRepository.kt`
- Read the mappers: `HabitEntityMapper.kt`, `CompletionEntityMapper.kt`
- Read the DAOs: `HabitDao.kt`, `CompletionDao.kt`
- Create `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt`:
    - Implements `HabitRepository` interface
    - Constructor injection of `HabitDao`
    - Uses `HabitEntityMapper` for domain/entity conversions
    - All methods wrap DAO calls in try/catch returning `Result.Success` or `Result.Error`
    - Uses `withContext(Dispatchers.IO)` for DAO calls
- Create `data/src/main/kotlin/com/getaltair/kairos/data/repository/CompletionRepositoryImpl.kt`:
    - Implements `CompletionRepository` interface
    - Constructor injection of `CompletionDao`
    - Uses `CompletionEntityMapper` for conversions
    - Same error handling pattern
- Update `DataModule.kt` to bind `HabitRepository` to `HabitRepositoryImpl` and `CompletionRepository` to `CompletionRepositoryImpl`

### 4. Create HabitWithStatus Model and Use Cases

- **Task ID**: create-model-and-usecases
- **Depends On**: implement-repositories
- **Assigned To**: builder-core
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitWithStatus.kt`:
    ```kotlin
    data class HabitWithStatus(
        val habit: Habit,
        val todayCompletion: Completion?,  // null = pending
        val weekCompletionRate: Float      // 0.0 - 1.0
    )
    ```
- Update `core/build.gradle.kts` to add `implementation(project(":domain"))` and `implementation(project(":data"))` dependencies
- Create use cases in `core/src/main/kotlin/com/getaltair/kairos/core/usecase/`:
    - `GetTodayHabitsUseCase` -- Takes `HabitRepository` and `CompletionRepository`. Returns `Flow<Result<List<HabitWithStatus>>>`. Queries active habits, filters out DEPARTURE category, filters by frequency for current day, joins with today's completions, computes 7-day completion rate per habit.
    - `CompleteHabitUseCase` -- Takes `CompletionRepository`. Input: habitId, CompletionType, partialPercent?. Validates: C-2 (partial range 1-99), C-4 (date <= today). Creates Completion and inserts. Returns `Result<Completion>`.
    - `SkipHabitUseCase` -- Takes `CompletionRepository`. Input: habitId, SkipReason?. Creates SKIPPED completion. Returns `Result<Completion>`.
    - `UndoCompletionUseCase` -- Takes `CompletionRepository`. Input: completionId. Deletes completion. Returns `Result<Unit>`. (30-second window enforced in ViewModel, not use case.)
- All use cases are classes with `suspend operator fun invoke(...)` pattern
- All return `Result<T>` from `com.getaltair.kairos.domain.common.Result`

### 5. Build TodayViewModel and UI State

- **Task ID**: build-viewmodel
- **Depends On**: create-model-and-usecases
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Read `docs/implementation-plan.md` Step 5 section for ViewModel state spec
- Read `docs/01-prd-core.md` for UI requirements and habit card states
- Read `docs/10-user-flows.md` for Flow 3 (Daily Check-in) and Flow 4 (Complete Habit)
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayUiState.kt`:
    - `TodayUiState` data class with: `habitsByCategory: Map<HabitCategory, List<HabitWithStatus>>`, `progress: Float`, `date: LocalDate`, `isLoading: Boolean`, `undoState: UndoState?`, `isEmpty: Boolean`, `isAllDone: Boolean`, `error: String?`
    - `UndoState` data class with: `completionId: UUID`, `habitName: String`, `remainingSeconds: Int`
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayViewModel.kt`:
    - Inject `GetTodayHabitsUseCase`, `CompleteHabitUseCase`, `SkipHabitUseCase`, `UndoCompletionUseCase`
    - Expose `uiState: StateFlow<TodayUiState>`
    - `onHabitComplete(habitId, CompletionType, partialPercent?)` -- calls use case, starts 30s undo timer
    - `onHabitSkip(habitId, SkipReason?)` -- calls use case, starts 30s undo timer
    - `onUndoCompletion()` -- calls undo use case, clears undo state
    - `onDismissUndo()` -- clears undo state
    - Group habits by category in display order: Morning, Afternoon, Evening, Anytime
    - Compute progress as completedCount / totalCount
    - Use `viewModelScope` for coroutines

### 6. Build Today Screen Composables

- **Task ID**: build-composables
- **Depends On**: build-viewmodel
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Read `docs/implementation-plan.md` Step 5 for screen layout wireframe and card states
- Read `docs/00-project-overview.md` for design principles
- Read `app/src/main/java/com/getaltair/kairos/ui/theme/Theme.kt` to understand theme setup
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/ProgressRing.kt`:
    - Circular progress indicator with animated arc
    - Shows percentage text in center
    - Uses MaterialTheme colors (primary for filled, surfaceVariant for track)
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/HabitCard.kt`:
    - Card composable showing habit name, anchor text ("After brushing teeth"), category emoji
    - Visual states: Pending (unfilled circle), Completed (checkmark, muted), Partial (half-fill), Skipped (skip icon)
    - Touch target >= 48dp
    - `onClick` callback for opening completion sheet
    - Color contrast >= 4.5:1
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/CompletionBottomSheet.kt`:
    - Material3 ModalBottomSheet
    - Three action rows: Done (checkmark icon), Partial (slider 1-99), Skip (skip icon)
    - "Done" triggers immediate completion with haptic feedback
    - "Partial" expands slider, confirm button
    - "Skip" shows optional SkipReason picker (6 options from SkipReason sealed class), confirm button
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/EmptyState.kt`:
    - Centered layout with illustration placeholder and "Add your first habit" text
    - Secondary text explaining what habits are
    - CTA button (non-functional until Step 6 -- just visual for now)
- Create `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayScreen.kt`:
    - Scaffold with top bar showing formatted date
    - Header section with ProgressRing
    - LazyColumn with category section headers (emoji + category name)
    - HabitCard items under each category
    - Empty state when no habits
    - "All done" celebration text when all completed
    - SnackbarHost for undo snackbar (30-second countdown)
    - FAB placeholder (for Step 6 create habit flow)
    - Haptic feedback via `LocalHapticFeedback`

### 7. Wire Navigation and DI

- **Task ID**: wire-navigation-and-di
- **Depends On**: build-composables
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Update `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/di/TodayModule.kt`:
    - Register `GetTodayHabitsUseCase`, `CompleteHabitUseCase`, `SkipHabitUseCase`, `UndoCompletionUseCase` as factory
    - Register `TodayViewModel` via `viewModelOf(::TodayViewModel)`
- Create `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt`:
    - NavHost with `today` as startDestination
    - `composable("today") { TodayScreen() }` route
    - Placeholder routes for `habit` and `settings` (empty screens)
- Update `app/src/main/java/com/getaltair/kairos/MainActivity.kt`:
    - Replace `Greeting()` with `KairosNavGraph()` inside KairosTheme + Scaffold
    - Remove `Greeting` and `GreetingPreview` composables

### 8. Validate All Work

- **Task ID**: validate-all
- **Depends On**: wire-navigation-and-di
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run `./gradlew build` to verify the project compiles without errors
- Run `./gradlew ktlintFormat` to fix formatting
- Verify all acceptance criteria from the spec (listed below)
- Check that no DEPARTURE category habits appear in TodayScreen
- Check that undo state management is correct (30-second window)
- Check touch targets >= 48dp in HabitCard
- Check that all new files follow project naming conventions (PascalCase composables, camelCase functions)
- Verify DI wiring: DataModule provides DAOs + repos, TodayModule provides use cases + ViewModel
- Verify HabitWithStatus, use cases, and repositories match their interface contracts
- Report pass/fail without modifying files

## Acceptance Criteria

From `docs/implementation-plan.md` Step 5 "Done when":

- [ ] Today screen loads and displays habits grouped by category (MORNING -> AFTERNOON -> EVENING -> ANYTIME)
- [ ] DEPARTURE category habits excluded from Today screen
- [ ] Progress ring shows accurate completion percentage
- [ ] Tapping habit opens completion bottom sheet with Done/Partial/Skip
- [ ] "Done" creates FULL completion with checkmark animation + haptics
- [ ] "Partial" shows slider, creates PARTIAL completion with percentage
- [ ] "Skip" optionally captures reason, creates SKIPPED completion
- [ ] Completed habits remain visible with visual distinction (not hidden)
- [ ] Undo snackbar appears for 30 seconds after completion
- [ ] Empty state shows illustration + "Add your first habit" when no habits exist
- [ ] "All done" state shows subtle celebration when all habits completed
- [ ] Screen survives rotation and process death (SavedStateHandle)

Additional technical criteria:

- [ ] `KairosDatabase` registers all 9 entities, all DAOs, and TypeConverters
- [ ] `HabitRepositoryImpl` and `CompletionRepositoryImpl` implement their interfaces
- [ ] All 4 use cases return `Result<T>` and validate invariants
- [ ] DI modules wire correctly (app starts without Koin errors)
- [ ] All code compiles with `./gradlew build`
- [ ] Code passes `./gradlew ktlintFormat` with no remaining issues

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew build` -- Verify the project builds without errors across all modules
- `./gradlew :feature:today:build` -- Verify the today feature module specifically
- `./gradlew :data:build` -- Verify data module with new repository implementations
- `./gradlew :core:build` -- Verify core module with new use cases
- `./gradlew ktlintFormat` -- Auto-fix Kotlin formatting issues
- `./gradlew detekt` -- Run code smell analysis (if configured)

## Notes

1. **Koin, not Hilt**: The project uses Koin for DI (not Hilt as stated in some rule files). Use `module {}`, `single {}`, `factory {}`, `viewModelOf()` patterns. The `KairosApp.kt` already loads `dataModule` and `todayModule`.
2. **Sealed class enums**: All enums (HabitCategory, CompletionType, SkipReason, etc.) are Kotlin sealed classes with `data object` variants, not Java-style enums. Pattern match with `is` checks.
3. **Entity column types**: Room entities store enums as Strings, timestamps as Longs (epoch millis), dates as Strings (ISO format). The converters and mappers handle translation.
4. **TodayHabitWithCompletion**: The `HabitDao` already defines this JOIN query result class and a `getTodayHabitsWithCompletions(): Flow<List<TodayHabitWithCompletion>>` method. The repository implementation should leverage this for the Today screen flow.
5. **Result type**: Use `com.getaltair.kairos.domain.common.Result` (project's own sealed class), not Kotlin stdlib Result.
6. **core module sub-modules**: `core/common/`, `core/data/`, `core/domain/`, `core/ui/` directories exist but are not included in `settings.gradle.kts`. Place use cases directly in `:core` module at `core/src/main/kotlin/com/getaltair/kairos/core/usecase/`.
7. **HabitDao.update() signature**: The update method takes individual fields, not an entity object. Repository impl needs to destructure the domain Habit when calling update.
8. **CompletionEntity init block**: Has validation in `init {}` that checks type/partialPercent/skipReason consistency. The current logic is inverted (allows partial percent for non-Partial types). The repository should validate before constructing entities to avoid init block issues.
9. **Process death**: Use `SavedStateHandle` in TodayViewModel for undo timer state. The habit list itself is re-fetched from Room on recreation.
10. **Feature branch**: All work on `feature/step5-today-screen` branch, do not merge to main.
