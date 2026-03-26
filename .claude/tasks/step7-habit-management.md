# Plan: Step 7 -- Habit Management (Edit, Pause, Archive, Delete, Backdate, Detail)

## Task Description

Implement the full Habit Management feature set from the implementation plan Step 7. This includes a Habit Detail screen, Edit Habit form, status transitions (pause/resume/archive/restore/delete), backdate completions, and viewing archived habits. All work happens on a new `feat/step7-habit-management` branch off `main`.

## Objective

When complete, users can tap a habit on the Today screen to view its detail page (with completion calendar and weekly rate), edit any habit field, pause/resume/archive/restore habits, permanently delete archived habits (with confirmation), and backdate completions for the past 7 days. Archived habits are viewable from a separate list.

## Problem Statement

After creating habits (Step 6) and tracking them on the Today screen (Step 5), users have no way to manage existing habits. They cannot edit fields, pause tracking, archive completed habits, view habit history, or correct missed completions from recent days. This step closes that gap.

## Solution Approach

Follow the existing Clean Architecture layering:

1. **Domain layer**: Add `HabitDetail` model, 8 new use cases with validation, extend `CompletionRepository` with `deleteForHabit`
2. **Data layer**: Implement repository changes
3. **Core layer**: Wire new use cases in DI module
4. **Feature layer**: Build `HabitDetailScreen`, `EditHabitScreen`, ViewModels, update navigation
5. **Branch**: All work on `feat/step7-habit-management` created from `main`

## Relevant Files

Use these files to complete the task:

**Domain -- entities and enums (read, do not modify):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` -- Habit data class with copy(), status, pausedAt, archivedAt
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Completion.kt` -- Completion data class with copy()
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/HabitStatus.kt` -- sealed class: Active, Paused, Archived
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/CompletionType.kt` -- sealed class: Full, Partial, Skipped, Missed
- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/WeeklyStats.kt` -- existing stats model with completionRate

**Domain -- repositories (modify CompletionRepository only):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/HabitRepository.kt` -- interface with getById, update, delete, getByStatus
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/CompletionRepository.kt` -- needs `deleteForHabit(habitId)` added

**Domain -- existing validators (read for pattern, do not modify):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/HabitValidator.kt` -- validates H-1, H-4, H-5, H-6
- `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/CompletionValidator.kt` -- validates C-2, C-4, C-5 (backdate rules already here)

**Domain -- existing use cases (read for pattern):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/CreateHabitUseCase.kt` -- pattern: constructor inject repo, suspend invoke, validate, return Result
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/CompleteHabitUseCase.kt` -- pattern: multi-repo use case with validation

**Data layer (modify):**

- `data/src/main/kotlin/com/getaltair/kairos/data/repository/CompletionRepositoryImpl.kt` -- add deleteForHabit implementation
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/CompletionDao.kt` -- already has `deleteForHabit(habitId: UUID)` at line 124
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt` -- read for update/delete patterns

**DI (modify):**

- `core/src/main/kotlin/com/getaltair/kairos/core/di/UseCaseModule.kt` -- add new use case factories
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/di/HabitModule.kt` -- add new ViewModels

**Feature layer (read for pattern, add new files):**

- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt` -- Compose screen pattern
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt` -- ViewModel pattern with StateFlow
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt` -- UiState data class pattern
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayScreen.kt` -- has HabitCard that needs onHabitClick
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/components/HabitCard.kt` -- needs click handler

**Navigation (modify):**

- `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt` -- add habitDetail/{habitId} and editHabit/{habitId} routes

**Build files (read for dependency pattern):**

- `feature/habit/build.gradle.kts` -- module dependencies pattern
- `feature/settings/build.gradle.kts` -- may need archived habits list

**Spec documents (reference):**

- `docs/01-prd-core.md` -- UC-4 (Edit Habit), FR-4 (Habit Management requirements)
- `docs/09-state-machines.md` -- Habit Status State Machine (lines 100-147)
- `docs/06-invariants.md` -- H-6 (Timestamp Consistency), C-4/C-5 (Backdate rules)

### New Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitDetail.kt` -- new model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/EditHabitUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/PauseHabitUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/ResumeHabitUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/ArchiveHabitUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/RestoreHabitUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/DeleteHabitUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/BackdateCompletionUseCase.kt`
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/GetHabitDetailUseCase.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/EditHabitUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/PauseHabitUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/ResumeHabitUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/ArchiveHabitUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/RestoreHabitUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/DeleteHabitUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/BackdateCompletionUseCaseTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/GetHabitDetailUseCaseTest.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/HabitDetailScreen.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/HabitDetailViewModel.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/HabitDetailUiState.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/EditHabitScreen.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/EditHabitViewModel.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/EditHabitUiState.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/components/CompletionCalendar.kt`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/components/HabitActionButtons.kt`
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/HabitDetailViewModelTest.kt`
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/EditHabitViewModelTest.kt`

## Implementation Phases

### Phase 1: Foundation (Domain + Data)

Create the `HabitDetail` model, extend `CompletionRepository` with `deleteForHabit`, and implement all 8 use cases with unit tests. This phase has no UI dependencies and produces the full business logic layer.

**Key decisions:**

- `HabitDetail` model: `data class HabitDetail(val habit: Habit, val recentCompletions: List<Completion>, val weeklyCompletionRate: Float)`
- Use cases follow existing pattern: constructor-inject repositories, `suspend operator fun invoke`, return `Result<T>`, validate with existing validators, catch exceptions (rethrow CancellationException)
- Status transition use cases enforce the state machine from `docs/09-state-machines.md`:
    - Pause: ACTIVE -> PAUSED (set pausedAt)
    - Resume: PAUSED -> ACTIVE (clear pausedAt)
    - Archive: ACTIVE|PAUSED -> ARCHIVED (set archivedAt)
    - Restore: ARCHIVED -> ACTIVE (clear archivedAt)
    - Delete: ARCHIVED only -> cascade delete completions then habit
- `BackdateCompletionUseCase` reuses `CompletionValidator` (already has C-4, C-5 rules) and checks C-3 (no duplicate)
- `GetHabitDetailUseCase` fetches habit, last 30 days of completions, and calculates weekly rate

### Phase 2: Core Implementation (UI + Navigation)

Build the Compose screens, ViewModels, and wire navigation. Follow existing patterns from `CreateHabitScreen`/`CreateHabitViewModel`.

**Key decisions:**

- `HabitDetailScreen`: Scaffold with TopAppBar, habit info section, completion calendar (last 30 days grid), weekly rate display, action buttons (Edit/Pause/Archive). Receives `habitId` via navigation argument
- `EditHabitScreen`: Pre-filled form mirroring Create flow fields (name, anchor, category, options). Uses same step components where possible
- Navigation: `habitDetail/{habitId}` and `editHabit/{habitId}` routes in `KairosNavGraph`
- Today screen: Add `onHabitClick` callback to `HabitCard` component to navigate to detail
- Delete confirmation: AlertDialog in HabitDetailScreen (only shown for archived habits)
- Backdate: Calendar cells in detail view are tappable for past 7 days, opens completion bottom sheet

### Phase 3: Integration and Polish

Wire DI, update navigation, connect Today screen to detail screen, and run validation.

**Key decisions:**

- Register all 8 use cases as factories in `useCaseModule`
- Register `HabitDetailViewModel` and `EditHabitViewModel` in `habitModule`
- Verify build compiles, tests pass, lint passes

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
    - Name: builder-domain
    - Role: Implement domain model, all 8 use cases, repository interface changes, data layer changes, DI wiring, and unit tests for all use cases
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-ui
    - Role: Implement HabitDetailScreen, EditHabitScreen, ViewModels, UiState classes, shared components (CompletionCalendar, HabitActionButtons), navigation updates, TodayScreen click handler, ViewModel tests
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
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: false
- Run `git checkout -b feat/step7-habit-management main`

### 2. Domain Model and Repository Extension

- **Task ID**: domain-foundation
- **Depends On**: create-branch
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitDetail.kt`:
    ```kotlin
    data class HabitDetail(
        val habit: Habit,
        val recentCompletions: List<Completion>,
        val weeklyCompletionRate: Float
    )
    ```
- Add `suspend fun deleteForHabit(habitId: UUID): Result<Unit>` to `CompletionRepository` interface
- Implement `deleteForHabit` in `CompletionRepositoryImpl` using existing `completionDao.deleteForHabit()`
- Follow the exact error handling pattern from other methods in `CompletionRepositoryImpl`

### 3. Implement Status Transition Use Cases

- **Task ID**: status-use-cases
- **Depends On**: domain-foundation
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `EditHabitUseCase`: fetch habit by ID, apply updates via `habit.copy(...)`, validate with `HabitValidator.validate()`, call `habitRepository.update()`
- Create `PauseHabitUseCase`: fetch habit, guard `status == Active`, set `status = Paused, pausedAt = Instant.now()`, update
- Create `ResumeHabitUseCase`: fetch habit, guard `status == Paused`, set `status = Active, pausedAt = null`, update
- Create `ArchiveHabitUseCase`: fetch habit, guard `status == Active || status == Paused`, set `status = Archived, archivedAt = Instant.now()`, update
- Create `RestoreHabitUseCase`: fetch habit, guard `status == Archived`, set `status = Active, archivedAt = null`, update
- Create `DeleteHabitUseCase`: fetch habit, guard `status == Archived`, call `completionRepository.deleteForHabit(habitId)`, then `habitRepository.delete(habitId)`
- All use cases follow the pattern in `CreateHabitUseCase`: constructor-inject repos, `suspend operator fun invoke`, return `Result<T>`, catch exceptions (rethrow `CancellationException`)

### 4. Implement Backdate and Detail Use Cases

- **Task ID**: backdate-detail-use-cases
- **Depends On**: domain-foundation
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: true (can run alongside status-use-cases)
- Create `BackdateCompletionUseCase(habitRepository, completionRepository)`:
    - Validate habit exists (fetch by ID)
    - Guard: habit status must be Active
    - Guard: cannot manually create MISSED completions (C-1)
    - Create `Completion` with the given date, type, and optional partialPercent
    - Validate with `CompletionValidator.validate(completion, today)` -- this enforces C-4 (no future) and C-5 (7 day window)
    - Check C-3: `completionRepository.getForHabitOnDate(habitId, date)` must return null
    - Insert via `completionRepository.insert(completion)`
- Create `GetHabitDetailUseCase(habitRepository, completionRepository)`:
    - Fetch habit by ID
    - Fetch completions for last 30 days via `completionRepository.getForHabitInDateRange(habitId, today.minusDays(29), today)`
    - Calculate weekly completion rate: count completions in last 7 days where type is Full or Partial, divide by days habit was active in the last week
    - Return `Result<HabitDetail>`

### 5. Use Case Unit Tests

- **Task ID**: use-case-tests
- **Depends On**: status-use-cases, backdate-detail-use-cases
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: false
- Write tests following existing patterns in `domain/src/test/kotlin/.../usecase/`:
    - `EditHabitUseCaseTest`: test successful edit, test validation failure, test habit not found
    - `PauseHabitUseCaseTest`: test pause from Active, reject pause from Paused/Archived
    - `ResumeHabitUseCaseTest`: test resume from Paused, reject from Active/Archived
    - `ArchiveHabitUseCaseTest`: test archive from Active, test archive from Paused, reject from Archived
    - `RestoreHabitUseCaseTest`: test restore from Archived, reject from Active/Paused
    - `DeleteHabitUseCaseTest`: test delete from Archived (verify cascade), reject from Active/Paused
    - `BackdateCompletionUseCaseTest`: test valid backdate, reject future date, reject >7 days, reject duplicate, reject non-Active habit
    - `GetHabitDetailUseCaseTest`: test returns habit with completions and rate, test habit not found
- Use MockK for mocking repositories (matches existing test dependencies)
- Use `runTest` coroutine test runner

### 6. DI Wiring for Use Cases

- **Task ID**: di-use-cases
- **Depends On**: use-case-tests
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: false
- Add all 8 use cases as factories in `core/src/main/kotlin/.../core/di/UseCaseModule.kt`:
    ```kotlin
    factory { EditHabitUseCase(get()) }
    factory { PauseHabitUseCase(get()) }
    factory { ResumeHabitUseCase(get()) }
    factory { ArchiveHabitUseCase(get()) }
    factory { RestoreHabitUseCase(get()) }
    factory { DeleteHabitUseCase(get(), get()) }
    factory { BackdateCompletionUseCase(get(), get()) }
    factory { GetHabitDetailUseCase(get(), get()) }
    ```
- Add corresponding imports
- Commit domain + data + core changes: "feat: add habit management use cases and domain model"

### 7. Habit Detail Screen and ViewModel

- **Task ID**: habit-detail-ui
- **Depends On**: di-use-cases
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Before writing code: read `DESIGN.md` for design system values, read existing `CreateHabitScreen.kt` and `TodayScreen.kt` for Compose patterns, read `CreateHabitViewModel.kt` for ViewModel/StateFlow pattern
- Create `HabitDetailUiState`:
    ```kotlin
    data class HabitDetailUiState(
        val habit: Habit? = null,
        val recentCompletions: List<Completion> = emptyList(),
        val weeklyCompletionRate: Float = 0f,
        val isLoading: Boolean = true,
        val error: String? = null,
        val showDeleteConfirmation: Boolean = false,
        val actionResult: String? = null
    )
    ```
- Create `HabitDetailViewModel(getHabitDetailUseCase, pauseHabitUseCase, resumeHabitUseCase, archiveHabitUseCase, restoreHabitUseCase, deleteHabitUseCase, backdateCompletionUseCase)`:
    - `init` block or `loadHabit(habitId: UUID)` method to fetch detail
    - Action methods: `onPause()`, `onResume()`, `onArchive()`, `onRestore()`, `onDeleteRequested()`, `onDeleteConfirmed()`, `onDismissDeleteDialog()`, `onBackdate(date, type, partialPercent?)`
    - Each action calls the use case, updates state, reloads habit detail on success
- Create `HabitDetailScreen`:
    - Scaffold with TopAppBar showing habit name + Edit button
    - Habit info section: name, anchor, category, estimated duration
    - `CompletionCalendar` component: 30-day grid, colored by completion type
    - Weekly completion rate display (progress indicator or text)
    - Action buttons section (HabitActionButtons component):
        - Active habits: Pause, Archive
        - Paused habits: Resume, Archive
        - Archived habits: Restore, Delete
    - Delete confirmation AlertDialog
    - Backdate: tappable calendar cells for past 7 days, opens completion bottom sheet
- Create `CompletionCalendar` composable in `feature/habit/.../components/`:
    - LazyVerticalGrid showing last 30 days
    - Each cell shows date, colored by completion type (Full/Partial/Skipped/Missed/none)
    - Past 7 days are tappable (if no completion exists)
- Create `HabitActionButtons` composable in `feature/habit/.../components/`:
    - Renders appropriate buttons based on habit status

### 8. Edit Habit Screen and ViewModel

- **Task ID**: edit-habit-ui
- **Depends On**: di-use-cases
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: true (can run alongside habit-detail-ui)
- Before writing code: read `DESIGN.md`, read `CreateHabitScreen.kt` and step composables for form patterns
- Create `EditHabitUiState`:
    ```kotlin
    data class EditHabitUiState(
        val habitId: UUID? = null,
        val name: String = "",
        val nameError: String? = null,
        val anchorType: AnchorType = AnchorType.AfterBehavior,
        val anchorBehavior: String = "",
        val anchorError: String? = null,
        val anchorTime: String? = null,
        val category: HabitCategory? = null,
        val categoryError: String? = null,
        val estimatedSeconds: Int = 300,
        val microVersion: String = "",
        val icon: String? = null,
        val color: String? = null,
        val frequency: HabitFrequency = HabitFrequency.Daily,
        val activeDays: Set<DayOfWeek> = emptySet(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isSaved: Boolean = false
    )
    ```
- Create `EditHabitViewModel(editHabitUseCase, habitRepository)`:
    - `loadHabit(habitId)` -- fetches habit and pre-fills all UiState fields
    - Same field update methods as `CreateHabitViewModel` (onNameChanged, onAnchorTypeSelected, etc.)
    - `saveHabit()` -- calls `editHabitUseCase` with updated Habit built from current state
- Create `EditHabitScreen`:
    - Reuse existing step composables (`NameStep`, `AnchorStep`, `CategoryStep`, `OptionsStep`) where possible
    - Single scrollable form (not wizard steps) with all fields visible at once -- editing is different from creation
    - Save button at bottom, loading indicator while saving
    - Top bar with "Edit Habit" title and back button

### 9. Navigation and Today Screen Integration

- **Task ID**: navigation-integration
- **Depends On**: habit-detail-ui, edit-habit-ui
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Update `KairosNavGraph.kt`:
    - Add `composable("habitDetail/{habitId}")` route that extracts habitId argument and renders HabitDetailScreen
    - Add `composable("editHabit/{habitId}")` route for EditHabitScreen
    - HabitDetailScreen receives `onBack`, `onEdit(habitId)`, `onDeleted` callbacks
    - EditHabitScreen receives `onBack`, `onSaved` callbacks
- Update TodayScreen to pass `onHabitClick: (UUID) -> Unit` callback
- Update HabitCard to be clickable, navigating to `habitDetail/{habitId}`
- Register ViewModels in `habitModule`:
    ```kotlin
    val habitModule = module {
        viewModelOf(::CreateHabitViewModel)
        viewModelOf(::HabitDetailViewModel)
        viewModelOf(::EditHabitViewModel)
    }
    ```
- Commit UI + navigation changes: "feat: add habit detail and edit screens with navigation"

### 10. ViewModel Tests

- **Task ID**: viewmodel-tests
- **Depends On**: navigation-integration
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Write `HabitDetailViewModelTest`:
    - Test initial load populates state
    - Test pause action updates status
    - Test resume action updates status
    - Test archive action updates status
    - Test restore action updates status
    - Test delete flow (confirm dialog, then delete)
    - Test backdate completion
    - Test error handling
- Write `EditHabitViewModelTest`:
    - Test loadHabit pre-fills all fields
    - Test saveHabit calls EditHabitUseCase with correct values
    - Test validation errors display
    - Test save error handling
- Use MockK for mocking use cases, `runTest` for coroutines

### 11. Build Verification

- **Task ID**: build-verify
- **Depends On**: viewmodel-tests
- **Assigned To**: builder-ui
- **Agent Type**: general-purpose
- **Parallel**: false
- Run `./gradlew build` to verify full compilation
- Run `./gradlew ktlintFormat` to fix formatting
- Run `./gradlew :domain:test :core:test :feature:habit:test` to verify all tests pass
- Fix any compilation or test failures
- Commit any fixes: "fix: resolve build and test issues"

### 12. Final Validation

- **Task ID**: validate-all
- **Depends On**: build-verify
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run all validation commands
- Verify acceptance criteria met
- Operate in validation mode: inspect and report only, do not modify files
- Check all status transitions enforce correct guards (from -> to state machine)
- Check backdate rules match invariants (C-4, C-5, C-3)
- Check HabitValidator is called in EditHabitUseCase
- Check cascade delete removes completions before habit
- Check navigation routes are correctly wired
- Check DI registrations match constructor parameters
- Check no circular dependencies introduced

## Acceptance Criteria

- [ ] Habit detail screen shows habit info (name, anchor, category, duration)
- [ ] Habit detail screen shows completion calendar for last 30 days
- [ ] Habit detail screen shows weekly completion rate
- [ ] Edit form pre-fills all current values, saves correctly
- [ ] Pause transition: ACTIVE -> PAUSED, sets pausedAt, rejects from other states
- [ ] Resume transition: PAUSED -> ACTIVE, clears pausedAt, rejects from other states
- [ ] Archive transition: ACTIVE|PAUSED -> ARCHIVED, sets archivedAt, rejects from ARCHIVED
- [ ] Restore transition: ARCHIVED -> ACTIVE, clears archivedAt, rejects from other states
- [ ] Delete requires confirmation dialog, only works on ARCHIVED habits
- [ ] Delete cascades: removes completions first, then habit
- [ ] Backdate completion works for past 7 days
- [ ] Backdate rejects future dates, >7 days, and duplicate completions
- [ ] Today screen habit cards are tappable, navigate to detail
- [ ] All new use cases have unit tests with mock repositories
- [ ] Both ViewModels have unit tests
- [ ] Project builds without errors (`./gradlew build`)
- [ ] All tests pass (`./gradlew test`)
- [ ] Code formatted (`./gradlew ktlintFormat`)
- [ ] All work is on `feat/step7-habit-management` branch

## Validation Commands

Execute these commands to validate the task is complete:

- `git branch --show-current` -- verify on `feat/step7-habit-management`
- `./gradlew build` -- verify project builds without errors
- `./gradlew :domain:test` -- verify domain use case tests pass
- `./gradlew :core:test` -- verify core use case tests pass
- `./gradlew :feature:habit:test` -- verify ViewModel tests pass
- `./gradlew ktlintFormat` -- verify code formatting
- `git log --oneline main..HEAD` -- verify commits on feature branch

## Notes

- The `CompletionDao` already has `deleteForHabit(habitId: UUID)` at line 124, but the `CompletionRepository` interface does not expose it. This gap must be filled for cascade deletes.
- There is no existing `HabitDetail` model -- it must be created in the domain model package.
- Use cases exist in both `domain/usecase/` and `core/usecase/`. New use cases that only need `HabitRepository` go in `domain/usecase/`. Those needing multiple repositories (like `DeleteHabitUseCase` and `BackdateCompletionUseCase`) go in `domain/usecase/` as well (matching `CreateHabitUseCase` pattern), with DI wiring in `core/di/UseCaseModule.kt`.
- The existing `CreateHabitViewModel` uses Koin `koinViewModel()` injection in composables -- follow the same pattern for new ViewModels.
- `HabitStatus` is a sealed class (Active, Paused, Archived), not an enum. Use `is` checks for state guards.
- The Habit entity has a custom `copy()` method (not Kotlin data class copy) that auto-sets `updatedAt = Instant.now()`. Use it for all status transitions to keep timestamps correct.
- Existing step composables (`NameStep`, `AnchorStep`, etc.) can potentially be reused in the Edit screen, but the Edit screen should show all fields at once (not wizard steps).
- Today screen's `onAddHabit` callback pattern should be mirrored for `onHabitClick` navigation.
