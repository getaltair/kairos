# Plan: Address All PR Review Findings

## Task Description

Address all findings from the comprehensive PR review of the `fix/address-pr-review-findings` branch. This includes 4 Critical issues, 6 Important issues, and 6 Suggestions identified by the code-reviewer, test-analyzer, error-hunter, type-analyzer, and comment-analyzer agents.

## Objective

Fix all 16 review findings across domain and feature/habit modules so the branch is ready for merge with no outstanding issues.

## Problem Statement

The PR review identified bugs (TimePicker state mismatch, dropped Timber cause), missing test coverage (exception paths), architectural concerns (ErrorMapper in domain layer), stale comments/test names, and opportunities for improved type safety and documentation.

## Solution Approach

1. Move `ErrorMapper` from domain to feature/habit (fixes architecture concern, enables Timber logging)
2. Fix production code bugs and improvements across AnchorStep, CreateHabitViewModel, CreateHabitScreen, CreateHabitUiState, and Habit.kt
3. Add missing tests and fix stale test names/comments
4. Validate all changes compile and pass tests

## Relevant Files

Use these files to complete the task:

**Production code (modify):**

- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/AnchorStep.kt` -- TimePicker state init bug (Critical #1)
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt` -- Timber.e cause (Critical #2), validation logging (#9), clearCreationError guard (#11), ErrorMapper import update (#7)
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt` -- Replace `else -> Unit` with explicit branches (#5)
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt` -- Add KDoc to CreationStatus (#12)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` -- isDueToday KDoc (#10), copy function KDoc (#16)

**Move (delete old, create new):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/common/ErrorMapper.kt` -- DELETE from domain
- `domain/src/test/kotlin/com/getaltair/kairos/domain/common/ErrorMapperTest.kt` -- DELETE from domain

### New Files

- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/ErrorMapper.kt` -- Moved from domain, add Timber.w for unmapped errors (#7, #8, #15)
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/ErrorMapperTest.kt` -- Moved from domain, update package

**Test files (modify):**

- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModelTest.kt` -- Fix stale names (#6), add exception tests (#3, #4), add ErrorMapper integration test (#13), assert validation messages (#14)

**Reference (read-only for context):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/common/Result.kt` -- Has `cause: Throwable?` field on Error
- `feature/habit/build.gradle.kts` -- Confirms Timber dependency available
- `domain/build.gradle.kts` -- Confirms pure JVM library (no Timber)

## Implementation Phases

### Phase 1: Production Code Fixes

All production code changes across domain and feature/habit modules. Includes moving ErrorMapper, fixing bugs, improving documentation, and hardening state transitions.

### Phase 2: Test Fixes and Additions

Fix stale test names, add missing exception path tests, add ErrorMapper integration test, and assert specific validation messages.

### Phase 3: Validation

Compile, run all tests, run ktlint to verify no regressions.

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
    - Name: builder-production
    - Role: Fix all production code -- move ErrorMapper, fix AnchorStep TimePicker bug, restore Timber.e cause, replace else -> Unit, add clearCreationError guard, add validation logging, update KDocs
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-tests
    - Role: Fix stale test names/section headers, add missing exception path tests, add ErrorMapper integration test, assert validation messages, move ErrorMapperTest
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

### 1. Move ErrorMapper from domain to feature/habit

- **Task ID**: move-error-mapper
- **Depends On**: none
- **Assigned To**: builder-production
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/ErrorMapper.kt` with:
    - Package: `com.getaltair.kairos.feature.habit`
    - Add `import timber.log.Timber`
    - Add Timber.w logging in the `else` branch: `Timber.w("ErrorMapper: unmapped error: %s", technicalMessage)`
    - Add KDoc documenting the matching strategy: substring `contains()` for domain validation errors, prefix `startsWith()` for repository-layer errors
    - All existing mapping logic preserved
- Update `CreateHabitViewModel.kt` import from `com.getaltair.kairos.domain.common.ErrorMapper` to `com.getaltair.kairos.feature.habit.ErrorMapper`
- Delete `domain/src/main/kotlin/com/getaltair/kairos/domain/common/ErrorMapper.kt`

### 2. Fix AnchorStep TimePicker state initialization

- **Task ID**: fix-timepicker
- **Depends On**: none
- **Assigned To**: builder-production
- **Agent Type**: general-purpose
- **Parallel**: true (with task 1)
- In `AnchorStep.kt`, fix the `is AnchorType.AtTime` branch:
    - Parse `anchorTime` string ("HH:mm" format) to derive `initialHour` and `initialMinute`:
        ```kotlin
        val parsedHour = anchorTime?.substringBefore(":")?.toIntOrNull() ?: 7
        val parsedMinute = anchorTime?.substringAfter(":")?.toIntOrNull() ?: 0
        ```
    - Pass parsed values to `rememberTimePickerState(initialHour = parsedHour, initialMinute = parsedMinute, is24Hour = false)`
    - Update button display text to use `anchorTime` for deriving display (not `timePickerState`):
        ```kotlin
        text = if (anchorTime.isNullOrBlank()) {
            "Select a time"
        } else {
            formatTime(parsedHour, parsedMinute)
        }
        ```

### 3. Fix ViewModel error handling and state management

- **Task ID**: fix-viewmodel
- **Depends On**: move-error-mapper
- **Assigned To**: builder-production
- **Agent Type**: general-purpose
- **Parallel**: false
- In `CreateHabitViewModel.kt`:
    - **Restore Timber.e cause** (line 202): Change `Timber.e("createHabit failed: ${result.message}")` to `Timber.e(result.cause, "createHabit failed: %s", result.message)`
    - **Add logging for custom frequency validation** (line 166-171): Add `Timber.w("createHabit called with Custom frequency but no active days selected")` before the Failed status update
    - **Guard clearCreationError** (line 223-225): Change to only transition from Failed:
        ```kotlin
        fun clearCreationError() {
            _uiState.update {
                if (it.creationStatus is CreationStatus.Failed) {
                    it.copy(creationStatus = CreationStatus.Idle)
                } else {
                    it
                }
            }
        }
        ```

### 4. Fix CreateHabitScreen exhaustive when

- **Task ID**: fix-screen-when
- **Depends On**: none
- **Assigned To**: builder-production
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 1-3)
- In `CreateHabitScreen.kt`, replace `else -> Unit` (line 54) with explicit branches:
    ```kotlin
    is CreationStatus.Idle -> { /* No action needed */ }
    is CreationStatus.Creating -> { /* Progress bar handled in composition */ }
    ```

### 5. Add KDoc to CreationStatus

- **Task ID**: add-creation-status-kdoc
- **Depends On**: none
- **Assigned To**: builder-production
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 1-4)
- In `CreateHabitUiState.kt`, add KDoc above the `sealed interface CreationStatus`:
    ```kotlin
    /**
     * Represents the lifecycle of a habit creation attempt.
     *
     * Transitions: Idle -> Creating -> Created (terminal) or
     * Creating -> Failed -> Idle (via [CreateHabitViewModel.clearCreationError]).
     * Weekdays and Weekends use fixed day sets; only Custom frequency consults activeDays.
     */
    ```
    (Note: Remove the last line about Weekdays -- that belongs in Habit.kt, not here. Keep only the creation lifecycle documentation.)

### 6. Update Habit.kt KDocs

- **Task ID**: fix-habit-kdocs
- **Depends On**: none
- **Assigned To**: builder-production
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 1-5)
- **isDueToday KDoc** (line 143): Change first line from "Checks if this habit is due today based on its frequency." to "Checks if this habit is due on the given date based on its frequency." Add note: "Weekdays and Weekends use fixed day sets; only Custom frequency consults [activeDays]."
- **copy function KDoc** (line 74-76): Change from "Creates a copy of this habit with the specified changes. Use for updating habits immutably." to "Creates an updated copy preserving the original [id] and [createdAt]. The [updatedAt] timestamp defaults to now. Excludes [id] and [createdAt] from parameters to prevent accidental identity changes."

### 7. Fix stale test names and section headers

- **Task ID**: fix-test-names
- **Depends On**: fix-viewmodel
- **Assigned To**: builder-tests
- **Agent Type**: general-purpose
- **Parallel**: false
- In `CreateHabitViewModelTest.kt`:
    - Line 639: Change section header from `// 11. createHabit with null category sets creationError` to `// 11. createHabit with null category sets Failed status`
    - Line 643: Rename test from `createHabit with null category sets creationError` to `createHabit with null category sets Failed status`
    - Line 694: Change section header from `// 14. clearCreationError clears creationError` to `// 14. clearCreationError resets Failed status to Idle`
    - Line 732: Change section header from `// 16. Custom frequency with empty activeDays sets creationError` to `// 16. Custom frequency with empty activeDays sets Failed status`
    - Line 736: Rename test from `createHabit with Custom frequency and empty activeDays sets creationError` to `createHabit with Custom frequency and empty activeDays sets Failed status`

### 8. Add missing exception path tests

- **Task ID**: add-exception-tests
- **Depends On**: fix-viewmodel
- **Assigned To**: builder-tests
- **Agent Type**: general-purpose
- **Parallel**: true (with task 7)
- In `CreateHabitViewModelTest.kt`, add after the section 8 (createHabit failure) tests:
    - **Unexpected exception test**: Add a new section "8b. createHabit unexpected exception":

        ```kotlin
        @Test
        fun `createHabit unexpected exception sets Failed with generic message`() = runTest {
            setupForCreate()
            coEvery { createHabitUseCase(any()) } throws RuntimeException("Unexpected DB error")

            viewModel.createHabit()
            advanceUntilIdle()

            val status = viewModel.uiState.value.creationStatus
            assertTrue(status is CreationStatus.Failed)
            assertEquals("Something went wrong. Please try again.", (status as CreationStatus.Failed).message)
        }
        ```

    - **CancellationException rethrow test**: In same section:

        ```kotlin
        @Test
        fun `createHabit rethrows CancellationException without setting Failed`() = runTest {
            setupForCreate()
            coEvery { createHabitUseCase(any()) } throws kotlin.coroutines.cancellation.CancellationException("Cancelled")

            viewModel.createHabit()
            advanceUntilIdle()

            // CancellationException is rethrown, status should not be Failed
            assertFalse(viewModel.uiState.value.creationStatus is CreationStatus.Failed)
        }
        ```

### 9. Add ErrorMapper integration test and validation message assertions

- **Task ID**: add-integration-tests
- **Depends On**: move-error-mapper, fix-viewmodel
- **Assigned To**: builder-tests
- **Agent Type**: general-purpose
- **Parallel**: true (with tasks 7-8)
- Move `ErrorMapperTest.kt` from `domain/src/test/kotlin/com/getaltair/kairos/domain/common/` to `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/`. Update package to `com.getaltair.kairos.feature.habit`.
- Delete the old test file at `domain/src/test/kotlin/com/getaltair/kairos/domain/common/ErrorMapperTest.kt`
- In `CreateHabitViewModelTest.kt`:
    - **ErrorMapper integration test** (add in section 8):

        ```kotlin
        @Test
        fun `createHabit failure with known error maps through ErrorMapper`() = runTest {
            setupForCreate()
            coEvery { createHabitUseCase(any()) } returns Result.Error("anchorBehavior must not be blank")

            viewModel.createHabit()
            advanceUntilIdle()

            val status = viewModel.uiState.value.creationStatus
            assertTrue(status is CreationStatus.Failed)
            assertEquals("Please describe when you'll do this habit.", (status as CreationStatus.Failed).message)
        }
        ```

    - **Null category message assertion** (update test at line 643): Add assertion on the specific message:
        ```kotlin
        val status = freshViewModel.uiState.value.creationStatus as CreationStatus.Failed
        assertEquals("Please select a category before creating your habit.", status.message)
        ```
    - **Empty activeDays message assertion** (update test at line 736): Add assertion on the specific message:
        ```kotlin
        val status = viewModel.uiState.value.creationStatus as CreationStatus.Failed
        assertEquals("Please select at least one day for custom frequency", status.message)
        ```

- Also clean up the `domain/src/test/kotlin/com/getaltair/kairos/domain/common/` directory if it becomes empty after moving the test file. Check if `domain/src/test/kotlin/com/getaltair/kairos/domain/entity/` has `HabitIsDueTodayTest.kt` (it does, leave that directory alone).

### 10. Final Validation

- **Task ID**: validate-all
- **Depends On**: fix-timepicker, fix-viewmodel, fix-screen-when, add-creation-status-kdoc, fix-habit-kdocs, fix-test-names, add-exception-tests, add-integration-tests
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run `./gradlew ktlintFormat` to auto-fix any formatting issues
- Run `./gradlew :domain:test :feature:habit:test` to verify all tests pass
- Run `./gradlew build` to verify full project compiles
- Verify all 16 review findings are addressed:
    - [Critical #1] TimePicker state initializes from anchorTime, not hardcoded 7:00
    - [Critical #2] Timber.e includes result.cause throwable
    - [Critical #3] Unexpected exception path test exists
    - [Critical #4] CancellationException rethrow test exists
    - [Important #5] No `else -> Unit` in CreateHabitScreen LaunchedEffect
    - [Important #6] No stale test names referencing `creationError`
    - [Important #7] ErrorMapper lives in feature/habit, not domain
    - [Important #8] ErrorMapper else branch logs with Timber.w
    - [Important #9] Custom frequency validation guard has Timber.w
    - [Important #10] isDueToday KDoc says "on the given date"
    - [Suggestion #11] clearCreationError guards on Failed state
    - [Suggestion #12] CreationStatus has KDoc with state machine documentation
    - [Suggestion #13] ErrorMapper integration test exists in ViewModel tests
    - [Suggestion #14] Validation error tests assert specific messages
    - [Suggestion #15] ErrorMapper has matching strategy documentation
    - [Suggestion #16] Habit.copy KDoc explains preserved id/createdAt
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

1. `ErrorMapper.kt` exists in `feature/habit/` with Timber.w logging and matching strategy KDoc; deleted from `domain/`
2. `AnchorStep.kt` TimePicker initializes from `anchorTime` parameter, not hardcoded values
3. `CreateHabitViewModel.kt` Timber.e call includes `result.cause` throwable
4. `CreateHabitScreen.kt` LaunchedEffect uses explicit `when` branches (no `else`)
5. `clearCreationError()` only transitions from Failed to Idle
6. Custom frequency validation guard logs with Timber.w
7. `CreationStatus` has KDoc documenting state machine transitions
8. `isDueToday` KDoc describes checking "on the given date" and notes Weekdays/Weekends use fixed day sets
9. `Habit.copy` KDoc explains preserved id/createdAt
10. All stale test names and section headers updated to reference `CreationStatus.Failed` instead of `creationError`
11. New tests: unexpected exception path, CancellationException rethrow, ErrorMapper integration, specific validation messages
12. `./gradlew build` succeeds with no errors
13. `./gradlew :domain:test :feature:habit:test` passes all tests
14. `./gradlew ktlintFormat` reports no changes needed

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew ktlintFormat` -- Auto-fix formatting and verify no changes needed
- `./gradlew :domain:test` -- Run domain module tests (HabitIsDueTodayTest still in domain)
- `./gradlew :feature:habit:test` -- Run feature/habit module tests (ErrorMapperTest moved here, CreateHabitViewModelTest updated)
- `./gradlew build` -- Full project build to verify no compilation errors
- `grep -r "ErrorMapper" domain/src/` -- Should return nothing (ErrorMapper removed from domain)
- `grep -r "ErrorMapper" feature/habit/src/main/` -- Should find the new ErrorMapper.kt and its import in CreateHabitViewModel.kt
- `grep "else -> Unit" feature/habit/src/main/` -- Should return nothing
- `grep "creationError" feature/habit/src/test/` -- Should return nothing (all references updated)

## Notes

- The domain module is a pure JVM library (`kairos.jvm.library`) with no Timber dependency. This is why ErrorMapper must move to feature/habit to enable Timber logging.
- `ErrorMapper` has exactly one consumer (`CreateHabitViewModel`), confirmed by grep, so moving it to feature/habit is clean with no cross-module impact.
- The `ErrorMapperTest` must also move since it tests a class that will now live in feature/habit.
- `HabitIsDueTodayTest.kt` stays in `domain/src/test/` since `Habit` remains in domain.
- `domain/src/test/kotlin/com/getaltair/kairos/domain/common/` directory may become empty after removing ErrorMapperTest -- delete if empty.
- Tasks 1, 2, 4, 5, 6 have no dependencies and can run in parallel. Tasks 3, 7, 8, 9 depend on earlier tasks. Task 10 (validation) depends on all others.
