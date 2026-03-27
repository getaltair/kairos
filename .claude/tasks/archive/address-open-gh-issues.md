# Plan: Address Open GitHub Issues (#5, #6, #7, #8)

## Task Description

Address all four open GitHub issues identified during PR review of the Create Habit flow. These span bug fixes, a refactor, and a UI enhancement across the domain, data, and feature/habit modules.

| Issue | Type        | Title                                                          |
| ----- | ----------- | -------------------------------------------------------------- |
| #5    | bug         | `isDueToday()` returns false for Weekdays/Weekends frequencies |
| #6    | enhancement | Replace AtTime anchor text field with TimePicker               |
| #7    | refactor    | Extract `CreationStatus` sealed class from boolean flags       |
| #8    | bug         | Translate internal validator messages to user-friendly errors  |

## Objective

Close all four open issues with correct, tested implementations that follow existing architecture patterns (Clean Architecture, Koin DI, sealed classes for state).

## Problem Statement

1. **#5**: `Habit.isDueToday()` checks `activeDays` for Weekdays/Weekends frequencies, but creation never populates `activeDays` for non-Custom frequencies. The `?: false` fallback means Weekdays/Weekends habits never appear as "due today."
2. **#6**: The AtTime anchor uses a plain `OutlinedTextField` accepting arbitrary text (e.g., "banana") stored as `timeWindowStart` documented as ISO "HH:mm". Future parsing will throw `DateTimeParseException`.
3. **#7**: `CreateHabitUiState` uses three independent fields (`isCreating`, `creationError`, `isCreated`) allowing 8 boolean combinations, of which only 4 are valid. Impossible states are representable.
4. **#8**: `HabitValidator` produces developer-facing messages like `"anchorBehavior must not be blank"` that surface directly in the UI snackbar. `HabitRepositoryImpl` wraps raw exception messages similarly.

## Solution Approach

- **#5**: Replace `activeDays?.contains(today) ?: false` with hardcoded day sets for Weekdays (`MONDAY..FRIDAY`) and Weekends (`SATURDAY, SUNDAY`) in `isDueToday()`. Keep `activeDays` lookup only for Custom frequency.
- **#6**: Replace the `OutlinedTextField` in `AnchorStep.kt` with a Material3 `TimePicker` dialog. Change `onAnchorTimeChanged` to accept `hour: Int, minute: Int` and format to "HH:mm" in the ViewModel. The AnchorStep callback signature changes from `(String) -> Unit` to handle structured time data.
- **#7**: Introduce `CreationStatus` sealed interface (`Idle`, `Creating`, `Created`, `Failed(message)`) in `CreateHabitUiState.kt`. Replace the three boolean fields with `val creationStatus: CreationStatus`. Update ViewModel, Screen, and all tests.
- **#8**: Create a shared `ErrorMapper` object in the domain layer that maps known validator error patterns to user-friendly messages. The ViewModel catches `Result.Error` messages and maps them before setting `CreationStatus.Failed`. Raw messages are logged via Timber.

## Relevant Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` -- #5: `isDueToday()` fix
- `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/HabitValidator.kt` -- #8: source of developer-facing messages
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt` -- #8: wraps raw exception messages
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt` -- #7: extract `CreationStatus`
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt` -- #7, #8: update state management and error mapping
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt` -- #7: consume `CreationStatus` instead of boolean flags
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/AnchorStep.kt` -- #6: replace text field with TimePicker
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModelTest.kt` -- #7, #8: update test assertions

### New Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/common/ErrorMapper.kt` -- #8: shared error message translation utility

## Implementation Phases

### Phase 1: Foundation (Parallel)

- **#5** (domain bug fix): Fix `isDueToday()` with hardcoded day sets. Isolated to one method, no downstream impact.
- **#7** (refactor): Extract `CreationStatus` sealed interface, update UiState, ViewModel, Screen, and tests. This is foundational for #8.

### Phase 2: Core Implementation (Parallel, after Phase 1)

- **#8** (error mapping): Create `ErrorMapper`, wire into ViewModel's error handling path. Depends on #7's `CreationStatus.Failed` type.
- **#6** (TimePicker): Replace text field with Material3 TimePicker dialog, update ViewModel time handling. Independent of #8 but runs after #7 to avoid merge conflicts in the ViewModel.

### Phase 3: Validation

- Quality engineer validates all changes against acceptance criteria, runs tests, verifies build.

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
    - Role: Fix `isDueToday()` bug (#5) in the domain entity layer
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-refactor
    - Role: Extract `CreationStatus` sealed class (#7) across UiState, ViewModel, Screen, and tests
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-errors
    - Role: Create `ErrorMapper` and wire user-friendly error messages (#8) across domain, data, and feature layers
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-timepicker
    - Role: Replace AtTime text field with Material3 TimePicker (#6) in AnchorStep and ViewModel
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

### 1. Fix isDueToday() for Weekdays/Weekends (#5)

- **Task ID**: fix-is-due-today
- **Depends On**: none
- **Assigned To**: builder-domain
- **Agent Type**: general-purpose
- **Parallel**: true (runs alongside task 2)
- In `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt`, modify `isDueToday()` (line 139):
    - For `HabitFrequency.Weekdays`: return `today in setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)` instead of checking `activeDays`
    - For `HabitFrequency.Weekends`: return `today in setOf(SATURDAY, SUNDAY)` instead of checking `activeDays`
    - Keep `HabitFrequency.Custom` branch using `activeDays?.contains(today) ?: false`
    - Keep `HabitFrequency.Daily` returning `true`
- Add unit tests in `domain/src/test/` for `isDueToday()` covering all four frequency types across relevant days of the week
- Import `java.time.DayOfWeek.*` constants as needed
- Run `./gradlew :domain:test` to verify

### 2. Extract CreationStatus Sealed Class (#7)

- **Task ID**: extract-creation-status
- **Depends On**: none
- **Assigned To**: builder-refactor
- **Agent Type**: general-purpose
- **Parallel**: true (runs alongside task 1)
- In `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt`:
    - Add `CreationStatus` sealed interface with: `Idle`, `Creating`, `Created`, `Failed(val message: String)`
    - Replace `isCreating: Boolean`, `creationError: String?`, `isCreated: Boolean` with `val creationStatus: CreationStatus = CreationStatus.Idle`
- In `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt`:
    - Replace all `.copy(isCreating = true, creationError = null)` with `.copy(creationStatus = CreationStatus.Creating)`
    - Replace all `.copy(isCreating = false, isCreated = true)` with `.copy(creationStatus = CreationStatus.Created)`
    - Replace all `.copy(isCreating = false, creationError = msg)` with `.copy(creationStatus = CreationStatus.Failed(msg))`
    - Replace `.copy(creationError = msg)` (without isCreating) with `.copy(creationStatus = CreationStatus.Failed(msg))`
    - Update `clearCreationError()` to set `creationStatus = CreationStatus.Idle`
    - Update the `if (_uiState.value.isCreating) return` guard to `if (_uiState.value.creationStatus is CreationStatus.Creating) return`
- In `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt`:
    - Replace `LaunchedEffect(uiState.isCreated)` with `LaunchedEffect(uiState.creationStatus)` checking for `is CreationStatus.Created`
    - Replace `LaunchedEffect(uiState.creationError)` with a branch in the same effect checking for `is CreationStatus.Failed`
    - Replace `uiState.isCreating` references with `uiState.creationStatus is CreationStatus.Creating`
    - Update `OptionsStep` `isCreating` parameter to derive from `creationStatus`
- In `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModelTest.kt`:
    - Update all assertions referencing `isCreating`, `isCreated`, `creationError` to use `creationStatus`
    - e.g., `assertEquals(true, state.isCreated)` becomes `assertEquals(CreationStatus.Created, state.creationStatus)`
    - e.g., `assertNotNull(state.creationError)` becomes `assertTrue(state.creationStatus is CreationStatus.Failed)`
- Run `./gradlew :feature:habit:test` to verify all 63 tests pass

### 3. Create ErrorMapper and Wire User-Friendly Errors (#8)

- **Task ID**: wire-error-mapper
- **Depends On**: extract-creation-status
- **Assigned To**: builder-errors
- **Agent Type**: general-purpose
- **Parallel**: true (runs alongside task 4)
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/common/ErrorMapper.kt`:
    - `object ErrorMapper` with `fun toUserMessage(technicalMessage: String): String`
    - Map known patterns to friendly messages:
        - `"anchorBehavior must not be blank"` -> `"Please describe when you'll do this habit."`
        - `"allowPartialCompletion must be true"` -> `"Something went wrong. Please try again."`
        - Messages containing `"relapseThresholdDays"` and `"lapseThresholdDays"` -> `"Something went wrong with the habit settings. Please try again."`
        - Messages containing `"createdAt"` or `"pausedAt"` or `"archivedAt"` -> `"Something went wrong. Please try again."`
        - Messages starting with `"Failed to insert habit"` or `"Failed to update habit"` or `"Failed to delete habit"` -> `"Could not save your habit. Please try again."`
        - Default fallback: `"Something went wrong. Please try again."`
- In `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt`:
    - In `createHabit()`, where `Result.Error` is handled, log the raw `result.message` with Timber, then pass `ErrorMapper.toUserMessage(result.message)` to `CreationStatus.Failed`
- Add unit tests for `ErrorMapper` in `domain/src/test/` verifying all patterns map correctly
- Run `./gradlew :domain:test :feature:habit:test` to verify

### 4. Replace AtTime Text Field with TimePicker (#6)

- **Task ID**: add-timepicker
- **Depends On**: extract-creation-status
- **Assigned To**: builder-timepicker
- **Agent Type**: general-purpose
- **Parallel**: true (runs alongside task 3)
- In `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/AnchorStep.kt`:
    - Replace the `OutlinedTextField` in the `AnchorType.AtTime` branch (lines 126-134) with:
        - A read-only display showing the selected time (or "Select a time" placeholder)
        - A button/clickable to open a `TimePicker` dialog
        - A Material3 `TimePickerDialog` (or `AlertDialog` wrapping `TimePicker`) with confirm/dismiss actions
    - Use `rememberTimePickerState()` to manage hour/minute state
    - On confirm, format as "HH:mm" and call `onAnchorTimeChanged` with the formatted string
    - Display the selected time in a user-friendly format (e.g., "7:00 AM")
- In `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt`:
    - The `onAnchorTimeChanged(time: String)` method already accepts a String -- the TimePicker will now guarantee valid "HH:mm" format, so no signature change needed
    - Verify the `anchorBehavior` is set to `"At $time"` as the current code does
- Verify the `isContinueEnabled` logic in AnchorStep still works (checks `!anchorTime.isNullOrBlank()`)
- Run `./gradlew :feature:habit:test` and verify build with `./gradlew :feature:habit:assembleDebug`

### 5. Final Validation

- **Task ID**: validate-all
- **Depends On**: fix-is-due-today, wire-error-mapper, add-timepicker
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run `./gradlew test` to verify all unit tests pass across all modules
- Run `./gradlew build` to verify the project compiles without errors
- Run `./gradlew ktlintCheck` to verify code formatting
- Verify each issue's acceptance criteria:
    - **#5**: `isDueToday()` returns true for Weekdays habits on Monday-Friday, true for Weekends on Saturday-Sunday
    - **#6**: AtTime anchor uses a TimePicker dialog, no free-text time input, stored format is valid "HH:mm"
    - **#7**: `CreateHabitUiState` has no `isCreating`/`isCreated`/`creationError` fields, uses `CreationStatus` sealed interface
    - **#8**: Snackbar shows user-friendly messages, technical messages are only in Timber logs
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

- [ ] **#5**: Habits with `Weekdays` frequency show as due on Monday through Friday; `Weekends` habits show as due on Saturday and Sunday; behavior unchanged for `Daily` and `Custom`
- [ ] **#5**: Unit tests cover `isDueToday()` for all four frequency types
- [ ] **#6**: AtTime anchor step shows a Material3 TimePicker instead of a free-text field
- [ ] **#6**: Selected time is stored as valid "HH:mm" format in `anchorTime`
- [ ] **#7**: `CreateHabitUiState` uses `CreationStatus` sealed interface; no `isCreating`, `isCreated`, or `creationError` fields remain
- [ ] **#7**: All 63+ existing ViewModel tests updated and passing
- [ ] **#8**: `ErrorMapper` translates all known validator messages to user-friendly text
- [ ] **#8**: Raw technical messages logged via Timber, not shown in UI
- [ ] **#8**: Unit tests verify all `ErrorMapper` patterns
- [ ] All modules compile without errors (`./gradlew build`)
- [ ] All tests pass (`./gradlew test`)
- [ ] Code formatting passes (`./gradlew ktlintCheck`)

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew build` -- Verify the project builds without errors
- `./gradlew test` -- Run all unit tests across all modules
- `./gradlew ktlintCheck` -- Verify Kotlin code formatting
- `./gradlew :domain:test` -- Verify domain layer tests (isDueToday, ErrorMapper)
- `./gradlew :feature:habit:test` -- Verify feature/habit tests (CreationStatus, error mapping)

## Notes

- The project uses **Koin** for DI (not Hilt) -- agents must not introduce Hilt annotations.
- The `Result` type is a custom sealed class at `domain/.../common/Result.kt`, not `kotlin.Result`.
- Material3 does not include a built-in `TimePickerDialog` composable -- agents will need to wrap `TimePicker` in an `AlertDialog` or `DatePickerDialog`-style custom dialog.
- The `onAnchorTimeChanged` in the ViewModel currently sets both `anchorTime` and `anchorBehavior` (to `"At $time"`), which satisfies the validator's `anchorBehavior must not be blank` check. This pattern must be preserved.
- All commits should reference the issue number (e.g., `fix: ... (#5)`). Do NOT use `--no-verify` on commits.
