# Plan: Address Step 7 PR Review Findings

## Task Description

Address all findings from the comprehensive PR review of the `feat/step7-habit-management` branch. The review was conducted by 5 specialized agents (code-reviewer, test-analyzer, silent-failure-hunter, type-analyzer, comment-analyzer) across 20+ files with 1,338+ lines of additions. Findings span critical bugs, architectural violations, missing invariants, error handling gaps, comment inaccuracies, and style inconsistencies.

## Objective

Fix all critical, important, and suggestion-level findings so the branch is ready for PR creation with clean code that follows established codebase conventions.

## Problem Statement

The PR review identified 18 distinct findings:

- **2 critical**: Navigation crash risk and non-atomic cascade delete
- **8 important**: Missing domain invariants, UDF violations, repository leaks, error handling gaps, inaccurate comments
- **8 suggestions**: Import consistency, hardcoded colors, redundant comments, missing test coverage

These findings, if unaddressed, would ship a crash-prone navigation layer, inconsistent error handling, and domain models that break established validation conventions.

## Solution Approach

Fixes are organized into 5 sequential/parallel phases:

1. **Domain layer** -- fix invariants, simplify cascade delete, add GetHabitUseCase, fix imports/KDocs
2. **Navigation + UI** -- guard UUID parsing, fix hardcoded colors, add clarifying comments, remove redundant comments
3. **ViewModel layer** -- fix UDF violation, replace repository injection, separate load/save errors, add TodayViewModel error handling
4. **Test updates** -- update tests for refactored code, add missing coverage for executeAction error paths
5. **Validation** -- quality-engineer verifies all fixes against acceptance criteria

## Relevant Files

### Domain Layer (Phase 1)

- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitDetail.kt` -- add init block with invariant validation
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/DeleteHabitUseCase.kt` -- simplify to rely on ON DELETE CASCADE
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/ArchiveHabitUseCase.kt` -- fix FQN UUID import
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/PauseHabitUseCase.kt` -- fix FQN UUID import
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/RestoreHabitUseCase.kt` -- fix FQN UUID import
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/ResumeHabitUseCase.kt` -- fix FQN UUID import
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/EditHabitUseCase.kt` -- fix KDoc accuracy
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/GetHabitDetailUseCase.kt` -- add off-by-one comment
- `core/src/main/kotlin/com/getaltair/kairos/core/di/UseCaseModule.kt` -- fix KDoc, register GetHabitUseCase

### New Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/GetHabitUseCase.kt` -- simple habit fetch use case (replaces direct HabitRepository injection in EditHabitViewModel)
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/GetHabitUseCaseTest.kt` -- tests for new use case

### Navigation + UI Layer (Phase 2)

- `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt` -- guard UUID.fromString with runCatching
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/components/CompletionCalendar.kt` -- replace Color.White with theme color, add off-by-one comments
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/HabitDetailScreen.kt` -- remove redundant section comments, observe isDeleted state
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/EditHabitScreen.kt` -- remove redundant section comments, use loadError field

### ViewModel Layer (Phase 3)

- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/HabitDetailViewModel.kt` -- replace onDeleteConfirmed callback with isDeleted state emission
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/HabitDetailUiState.kt` -- add isDeleted field
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/EditHabitViewModel.kt` -- replace HabitRepository with GetHabitUseCase
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/EditHabitUiState.kt` -- add loadError field
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayViewModel.kt` -- add ErrorMapper usage, add try-catch to onHabitComplete/onHabitSkip

### Test Layer (Phase 4)

- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/HabitDetailViewModelTest.kt` -- update for isDeleted pattern, add executeAction error path tests
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/EditHabitViewModelTest.kt` -- update for GetHabitUseCase injection

### Reference Files (read-only, for pattern matching)

- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitWithStatus.kt` -- init block pattern reference
- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/WeeklyStats.kt` -- init block pattern reference
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/ErrorMapper.kt` -- error mapping pattern reference
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt` -- UI state pattern reference

## Implementation Phases

### Phase 1: Domain Layer Foundation

Fix the domain model, use cases, and DI module. These are foundational changes that the presentation layer depends on.

**1a. HabitDetail init block** -- Add `require` checks matching the pattern set by `HabitWithStatus` and `WeeklyStats`:

```kotlin
data class HabitDetail(
    val habit: Habit,
    val recentCompletions: List<Completion>,
    val weeklyCompletionRate: Float
) {
    init {
        require(weeklyCompletionRate in 0f..1f) {
            "weeklyCompletionRate must be in 0.0..1.0"
        }
        require(recentCompletions.all { it.habitId == habit.id }) {
            "All recentCompletions must belong to the same habit"
        }
    }
}
```

**1b. DeleteHabitUseCase simplification** -- The Room schema already has `ON DELETE CASCADE` on the completions foreign key. Remove the explicit `completionRepository.deleteForHabit()` call and rely on the cascade. Also remove `CompletionRepository` from the constructor since it's no longer needed:

```kotlin
class DeleteHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: UUID): Result<Unit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            val habit = (result as Result.Success).value
            if (habit.status !is HabitStatus.Archived) {
                return Result.Error(
                    "Cannot delete habit: current status is ${habit.status.displayName}, expected Archived"
                )
            }
            habitRepository.delete(habitId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to delete habit: ${e.message}", cause = e)
        }
    }
}
```

Update `UseCaseModule.kt` registration: change `factory { DeleteHabitUseCase(get(), get()) }` to `factory { DeleteHabitUseCase(get()) }`. Update KDoc to note cascade behavior. Update `DeleteHabitUseCaseTest` accordingly (remove completion deletion mocking/verification, simplify to single delete call).

**1c. Create GetHabitUseCase** -- New use case that wraps `habitRepository.getById()` with proper error handling, following the established pattern:

```kotlin
class GetHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: UUID): Result<Habit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get habit: ${e.message}", cause = e)
        }
    }
}
```

Register in `UseCaseModule`: `factory { GetHabitUseCase(get()) }`. Write tests covering success, not-found, and exception cases.

**1d. Fix UUID FQN imports** -- In `ArchiveHabitUseCase.kt`, `PauseHabitUseCase.kt`, `RestoreHabitUseCase.kt`, `ResumeHabitUseCase.kt`: add `import java.util.UUID` and change `java.util.UUID` in the function signature to just `UUID`.

**1e. Fix KDoc comments**:

- `UseCaseModule.kt:20`: Change "Today screen and shared use cases" to "domain-layer use cases"
- `EditHabitUseCase.kt:12-14`: Change "Fetches the current habit, applies the caller-supplied [Habit]" to "Validates and persists an updated habit. Verifies the habit exists before applying changes."
- `GetHabitDetailUseCase.kt:34`: Add inline comment `// 30-day inclusive window (today minus 29 = 30 days)` above `val thirtyDaysAgo = today.minusDays(29)`
- `DeleteHabitUseCase.kt` KDoc: Update to note that completions are cascade-deleted by the database

### Phase 2: Navigation + UI Layer

**2a. Guard UUID.fromString in KairosNavGraph** -- Wrap both occurrences (lines 33 and 42) with `runCatching`:

```kotlin
composable("habitDetail/{habitId}") { backStackEntry ->
    val habitId = runCatching {
        UUID.fromString(backStackEntry.arguments?.getString("habitId"))
    }.getOrElse {
        navController.popBackStack()
        return@composable
    }
    HabitDetailScreen(...)
}
```

Same pattern for the `editHabit` route.

**2b. CompletionCalendar fixes**:

- Replace `Color.White` (line 86) with `MaterialTheme.colorScheme.onPrimary` for theme-aware text on colored cells
- Add inline comment above line 37: `// 30 days in chronological order (oldest first)`
- Add inline comment above line 42: `// 7-day backdate window (today minus 6 = 7 days inclusive)`

**2c. Remove redundant section comments** -- Remove `// Name section`, `// Anchor section`, `// Category section`, `// Options section`, `// Save button` from `EditHabitScreen.kt` and `// Habit info section`, `// Weekly completion rate`, `// Completion calendar`, `// Action buttons` from `HabitDetailScreen.kt`. Also remove `// Validate name`, `// Validate anchor`, `// Validate category` from `EditHabitViewModel.kt`.

### Phase 3: ViewModel Refactoring

**3a. HabitDetailViewModel UDF fix** -- Replace the `onDeleteConfirmed(onDeleted: () -> Unit)` callback pattern with state-based navigation:

Add `isDeleted: Boolean = false` to `HabitDetailUiState`.

In `HabitDetailViewModel.onDeleteConfirmed()`, remove the lambda parameter. On success, set `_uiState.update { it.copy(isDeleted = true) }`.

In `HabitDetailScreen`, observe `isDeleted`:

```kotlin
LaunchedEffect(uiState.isDeleted) {
    if (uiState.isDeleted) {
        onDeleted()
    }
}
```

Update the delete confirmation dialog to call `viewModel.onDeleteConfirmed()` (no lambda).

**3b. EditHabitViewModel repository replacement** -- Replace `HabitRepository` injection with `GetHabitUseCase`:

```kotlin
class EditHabitViewModel(
    private val editHabitUseCase: EditHabitUseCase,
    private val getHabitUseCase: GetHabitUseCase
) : ViewModel()
```

Replace both `habitRepository.getById(habitId)` calls (in `loadHabit` and `saveHabit`) with `getHabitUseCase(habitId)`.

**3c. EditHabitUiState load error separation** -- Add `loadError: String? = null` field. In `EditHabitViewModel.loadHabit()`, set `loadError` on failure instead of `saveError`. In `EditHabitScreen`, check `uiState.loadError != null && uiState.habitId == null` for the error state. Keep the snackbar `LaunchedEffect` only for `saveError`.

**3d. TodayViewModel error handling** -- Two fixes:

Fix 1: Add `ErrorMapper` usage. The `ErrorMapper` class is in the `feature/habit` module, so it cannot be directly imported from `feature/today`. The cleanest approach is to add a similar inline mapping in `TodayViewModel` or a generic fallback message. Since `TodayViewModel` errors are currently only from `GetTodayHabitsUseCase`, `CompleteHabitUseCase`, `SkipHabitUseCase`, and `UndoCompletionUseCase`, replace raw `result.message` with a generic user-friendly message:

```kotlin
is Result.Error -> {
    Timber.e(result.cause, "Failed to complete habit: %s", result.message)
    _uiState.update { it.copy(error = "Something went wrong. Please try again.") }
}
```

Apply this pattern to all 4 error branches in `onHabitComplete` (line 103), `onHabitSkip` (line 121), `onUndoCompletion` (line 140), and `loadTodayHabits` (line 65).

Fix 2: Add outer try-catch to `onHabitComplete` and `onHabitSkip`, matching the pattern in `loadTodayHabits`:

```kotlin
fun onHabitComplete(habitId: UUID, completionType: CompletionType, partialPercent: Int? = null) {
    viewModelScope.launch {
        try {
            val result = completeHabitUseCase(habitId, completionType, partialPercent)
            when (result) {
                is Result.Success -> { ... }
                is Result.Error -> { ... }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error completing habit")
            _uiState.update { it.copy(error = "Something went wrong. Please try again.") }
        }
    }
}
```

### Phase 4: Test Updates

**4a. Update DeleteHabitUseCaseTest** -- Remove completion deletion mocking (`coEvery { completionRepository.deleteForHabit(...) }`), remove `coVerifyOrder` for cascade ordering, remove "completion deletion failure" test case. Add test verifying single `habitRepository.delete()` call.

**4b. Create GetHabitUseCaseTest** -- Tests for success, not-found error, and unexpected exception.

**4c. Update EditHabitViewModelTest** -- Replace `HabitRepository` mock with `GetHabitUseCase` mock. Update `coEvery` calls from `habitRepository.getById(...)` to `getHabitUseCase(...)`.

**4d. Update HabitDetailViewModelTest** --

- Update `onDeleteConfirmed` tests: verify `isDeleted = true` state instead of callback invocation
- Add `executeAction` error path tests for pause/resume/archive/restore (most significant test gap from review)

**4e. Remove redundant validation comments from EditHabitViewModel** -- Already covered in Phase 2c.

## Deferred Findings (with rationale)

The following suggestions were reviewed and intentionally deferred:

- **EditHabitUiState sealed save status** (type-analyzer suggestion #12): `CreateHabitUiState` uses the same boolean pattern (`isCreating`/`creationError`/`isCreated`). The current approach IS consistent with the codebase. Introducing a sealed class here but not in `CreateHabitUiState` would create a new inconsistency. If sealed status types are desired, both should be refactored together in a separate PR.

- **HabitDetailUiState sealed ActionResult** (type-analyzer suggestion #13): `TodayUiState` uses raw `String?` for errors. The pattern is consistent across the codebase. A sealed interface would be a codebase-wide enhancement, not specific to this PR.

- **HabitDetail/CreateHabitUiState form field deduplication** (type-analyzer suggestion): Extracting shared `HabitFormFields` data class is premature at 2 consumers. Revisit if a third consumer emerges.

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
    - Name: domain-fixer
    - Role: Fix all domain layer issues (model invariants, use case simplification, new GetHabitUseCase, imports, KDocs) and update domain tests
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: presentation-fixer
    - Role: Fix navigation safety, ViewModel refactoring, UI component issues, error handling, and update presentation tests
    - Agent Type: general-purpose
    - Resume: true

- Quality Engineer (Validator)
    - Name: validator
    - Role: Validate completed work against acceptance criteria (read-only inspection mode)
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

### 1. Fix HabitDetail init block

- **Task ID**: fix-habit-detail-init
- **Depends On**: none
- **Assigned To**: domain-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with task 2)
- Add `init` block to `HabitDetail` with `require(weeklyCompletionRate in 0f..1f)` and `require(recentCompletions.all { it.habitId == habit.id })`
- Follow the exact pattern from `HabitWithStatus.kt` and `WeeklyStats.kt`

### 2. Simplify DeleteHabitUseCase

- **Task ID**: simplify-delete-usecase
- **Depends On**: none
- **Assigned To**: domain-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with task 1)
- Remove `CompletionRepository` dependency from constructor
- Remove explicit `completionRepository.deleteForHabit()` call -- Room's ON DELETE CASCADE handles this
- Update KDoc to note cascade behavior
- Update `UseCaseModule.kt`: change `factory { DeleteHabitUseCase(get(), get()) }` to `factory { DeleteHabitUseCase(get()) }`
- Update `DeleteHabitUseCaseTest`: remove completion deletion mocking and cascade ordering tests, add test for single delete call

### 3. Create GetHabitUseCase

- **Task ID**: create-get-habit-usecase
- **Depends On**: none
- **Assigned To**: domain-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 1, 2)
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/GetHabitUseCase.kt`
- Follow the exact error handling pattern from other use cases (try-catch with CancellationException rethrow)
- Register in `UseCaseModule.kt`: `factory { GetHabitUseCase(get()) }`
- Create `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/GetHabitUseCaseTest.kt` with success, not-found, and exception tests

### 4. Fix UUID imports and KDocs

- **Task ID**: fix-imports-kdocs
- **Depends On**: none
- **Assigned To**: domain-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 1, 2, 3)
- Add `import java.util.UUID` to `ArchiveHabitUseCase.kt`, `PauseHabitUseCase.kt`, `RestoreHabitUseCase.kt`, `ResumeHabitUseCase.kt`
- Change `java.util.UUID` in function signatures to short `UUID`
- Fix `UseCaseModule.kt` KDoc: "Today screen and shared use cases" -> "domain-layer use cases"
- Fix `EditHabitUseCase.kt` KDoc: reword "Fetches...applies" to "Validates and persists an updated habit. Verifies the habit exists before applying changes."
- Add inline comment in `GetHabitDetailUseCase.kt:34`: `// 30-day inclusive window (today minus 29 = 30 days)`

### 5. Fix navigation UUID safety

- **Task ID**: fix-nav-uuid-safety
- **Depends On**: none
- **Assigned To**: presentation-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with domain tasks)
- Wrap `UUID.fromString()` calls in `KairosNavGraph.kt` (lines 33 and 42) with `runCatching { ... }.getOrElse { navController.popBackStack(); return@composable }`
- Apply to both `habitDetail/{habitId}` and `editHabit/{habitId}` routes

### 6. Fix HabitDetailViewModel UDF violation

- **Task ID**: fix-detail-vm-udf
- **Depends On**: none
- **Assigned To**: presentation-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with task 5)
- Add `isDeleted: Boolean = false` to `HabitDetailUiState`
- Change `onDeleteConfirmed(onDeleted: () -> Unit)` to `onDeleteConfirmed()` (no lambda)
- On delete success, set `_uiState.update { it.copy(isDeleted = true) }`
- In `HabitDetailScreen`, add `LaunchedEffect(uiState.isDeleted)` that calls `onDeleted()`
- Update dialog confirm button: `onClick = { viewModel.onDeleteConfirmed() }`

### 7. Fix EditHabitViewModel repository leak and load error separation

- **Task ID**: fix-edit-vm-refactor
- **Depends On**: create-get-habit-usecase
- **Assigned To**: presentation-fixer
- **Agent Type**: general-purpose
- **Parallel**: false (depends on task 3)
- Replace `HabitRepository` constructor parameter with `GetHabitUseCase`
- Replace both `habitRepository.getById(habitId)` calls with `getHabitUseCase(habitId)`
- Add `loadError: String? = null` field to `EditHabitUiState`
- In `loadHabit()`, set `loadError` on failure instead of `saveError`
- In `EditHabitScreen`, check `uiState.loadError != null && uiState.habitId == null` for error state
- Keep snackbar `LaunchedEffect` only for `saveError`

### 8. Fix TodayViewModel error handling

- **Task ID**: fix-today-vm-errors
- **Depends On**: none
- **Assigned To**: presentation-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 5, 6)
- Replace raw `result.message` with generic user-friendly message in all error branches: `loadTodayHabits` (line 65), `onHabitComplete` (line 103), `onHabitSkip` (line 121), `onUndoCompletion` (line 140)
- Add outer try-catch with CancellationException rethrow to `onHabitComplete` and `onHabitSkip`

### 9. Fix UI component issues

- **Task ID**: fix-ui-components
- **Depends On**: none
- **Assigned To**: presentation-fixer
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 5-8)
- In `CompletionCalendar.kt`: replace `Color.White` with `MaterialTheme.colorScheme.onPrimary`
- Add inline comment above line 37: `// 30 days in chronological order (oldest first)`
- Add inline comment above line 42: `// 7-day backdate window (today minus 6 = 7 days inclusive)`
- Remove redundant section comments from `EditHabitScreen.kt` (lines 135, 145, 159, 169, 189)
- Remove redundant section comments from `HabitDetailScreen.kt` (lines 160, 205, 230, 240)
- Remove redundant validation comments from `EditHabitViewModel.kt` (lines 144, 157)

### 10. Update presentation tests

- **Task ID**: update-presentation-tests
- **Depends On**: fix-detail-vm-udf, fix-edit-vm-refactor
- **Assigned To**: presentation-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- Update `HabitDetailViewModelTest`: change delete tests to verify `isDeleted = true` state instead of callback invocation
- Add `executeAction` error path tests: test that pause/resume/archive/restore set `actionResult` when use case returns `Result.Error`
- Update `EditHabitViewModelTest`: replace `HabitRepository` mock with `GetHabitUseCase` mock, update all `coEvery` calls

### 11. Validate all fixes

- **Task ID**: validate-all
- **Depends On**: fix-imports-kdocs, update-presentation-tests
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run `./gradlew test` to verify all tests pass
- Run `./gradlew ktlintFormat` to verify formatting
- Inspect each finding from the review and verify it has been addressed
- Verify no regressions in existing functionality
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

1. `./gradlew test` passes with zero failures
2. `./gradlew ktlintFormat` produces no changes (code is already formatted)
3. `HabitDetail` has init block matching `HabitWithStatus`/`WeeklyStats` pattern
4. `DeleteHabitUseCase` no longer depends on `CompletionRepository`
5. `GetHabitUseCase` exists with tests
6. All 4 use cases use short `UUID` import instead of FQN
7. `KairosNavGraph` safely handles malformed UUID navigation arguments
8. `HabitDetailViewModel.onDeleteConfirmed` has no lambda parameter; uses `isDeleted` state
9. `EditHabitViewModel` injects `GetHabitUseCase`, not `HabitRepository`
10. `EditHabitUiState` has separate `loadError` field
11. `TodayViewModel` uses generic user-friendly error messages and has try-catch on all coroutine launches
12. `CompletionCalendar` uses theme-aware color instead of `Color.White`
13. All inaccurate KDocs are corrected
14. Redundant section comments removed
15. `HabitDetailViewModelTest` covers executeAction error paths
16. All existing tests updated to reflect refactored code

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew test` -- Run all unit tests, expect zero failures
- `./gradlew ktlintFormat` -- Auto-format code, expect no changes
- `./gradlew detekt` -- Run code smell analysis, expect clean
- `./gradlew :app:assembleDebug` -- Verify project compiles without errors

## Notes

- The `ErrorMapper` class lives in `feature/habit/` module. Since `feature/today/` cannot import from `feature/habit/`, the TodayViewModel fix uses inline generic messages rather than the `ErrorMapper` class. If error mapping is needed across modules in the future, `ErrorMapper` should move to a shared module.
- The `DeleteHabitUseCase` simplification relies on Room's `ON DELETE CASCADE` foreign key constraint on the completions table, confirmed in the database schema at `data/schemas/com.getaltair.kairos.data.database.KairosDatabase/1.json`.
- Three type-design suggestions (sealed SaveStatus, sealed ActionResult, shared HabitFormFields) are intentionally deferred because the current patterns are consistent with the rest of the codebase. These would be codebase-wide enhancements for a separate PR.
