# Plan: Address PR Review Findings for Create Habit Flow

## Task Description

Address all critical, high, and important issues identified during the comprehensive PR review of the `feat/step6-create-habit-flow` branch. The review was conducted by 6 specialized agents (code-reviewer, test-analyzer, comment-analyzer, silent-failure-hunter, type-analyzer, code-simplifier) and identified 2 critical bugs, 5 high-severity issues, 8 important issues, and 5 test coverage gaps.

## Objective

Fix all critical and high-severity bugs, clean up important code quality issues, and fill the top test coverage gaps so the Create Habit feature is merge-ready.

## Problem Statement

The Create Habit wizard has a runtime crash (LazyVerticalGrid inside verticalScroll), silent failure paths in the ViewModel, missing input validation, dead code, theme inconsistencies, and insufficient test coverage for key behaviors.

## Solution Approach

Three-phase sequential fix: (1) fix all critical/high bugs in ViewModel and UI, (2) clean up important code quality issues, (3) add missing test coverage. A final validation pass ensures everything compiles, passes lint, and tests pass.

## Relevant Files

Use these files to complete the task:

- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt` -- silent returns, missing guards, coroutine error handling, validation gaps
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt` -- default anchor type change
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt` -- verticalScroll crash trigger, hardcoded step count, onSkip wiring
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/CategoryStep.kt` -- LazyVerticalGrid crash, Color.Red
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/AnchorStep.kt` -- unused imports, Color.Red, null anchor display
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/NameStep.kt` -- Color.Red
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/OptionsStep.kt` -- dead onSkip param, no-op .map
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayScreen.kt` -- redundant categoryEmoji/categoryName functions
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModelTest.kt` -- test coverage gaps

## Implementation Phases

### Phase 1: Critical and High-Severity Fixes

Fix the 2 critical bugs and 5 high-severity issues that cause crashes, silent failures, or data corruption.

**1a. Fix LazyVerticalGrid crash (CRITICAL)**

- In `CategoryStep.kt`: Replace `LazyVerticalGrid` with a regular `Column` containing two `Row`s (only 5 items, lazy provides no benefit). Remove the hardcoded `.height(320.dp)`.
- Remove `LazyVerticalGrid` imports, add `Row` import if needed.
- This eliminates the same-axis nested scrollable crash without needing to change `CreateHabitScreen.kt`'s `verticalScroll`.

**1b. Fix silent `?: return` in createHabit() (CRITICAL)**

- In `CreateHabitViewModel.kt` lines 155-156: Replace `val anchorType = state.anchorType ?: return` and `val category = state.category ?: return` with explicit null checks that log the error via Timber and set `creationError` with a user-friendly message.

**1c. Add double-submission guard (HIGH)**

- In `CreateHabitViewModel.kt`: Add `if (_uiState.value.isCreating) return` at the top of `createHabit()`.
- In `OptionsStep.kt`: Accept `isCreating: Boolean` parameter and disable both Create and Skip buttons when `isCreating` is true.
- In `CreateHabitScreen.kt`: Pass `uiState.isCreating` to `OptionsStep`.

**1d. Add coroutine exception handler (HIGH)**

- In `CreateHabitViewModel.kt`: Wrap the `viewModelScope.launch` body in a try-catch that catches `Exception`, re-throws `CancellationException`, logs unexpected errors, and resets `isCreating = false` with a user-friendly `creationError`.

**1e. Add Custom frequency empty activeDays validation (HIGH)**

- In `CreateHabitViewModel.kt` `createHabit()`: After computing `activeDays`, check if `frequency is HabitFrequency.Custom && activeDays.isNullOrEmpty()`. If so, set `creationError = "Please select at least one day for custom frequency"` and return.

**1f. Pre-select AfterBehavior as default anchor type (HIGH -- fixes UX confusion)**

- In `CreateHabitUiState.kt`: Change `val anchorType: AnchorType? = null` to `val anchorType: AnchorType = AnchorType.AfterBehavior`.
- In `CreateHabitViewModel.kt`: Update `createHabit()` to remove the `?: return` for `anchorType` since it can no longer be null. Remove the null anchor validation in `goToNextStep()` ANCHOR case (no longer possible). Update `onAnchorTypeSelected` if needed.
- In `AnchorStep.kt`: Remove the `null -> 0` branch from `selectedTabIndex` and `anchorType ?: AnchorType.AfterBehavior` fallback since `anchorType` is no longer nullable.
- **Note**: Making `anchorType` non-nullable has cascading effects. The `AnchorStep` composable parameter changes from `AnchorType?` to `AnchorType`. Multiple tests reference null anchor type. Update accordingly.

### Phase 2: Important Code Quality Fixes

Clean up theme inconsistencies, dead code, and redundant patterns.

**2a. Replace Color.Red with MaterialTheme.colorScheme.error**

- `NameStep.kt:78`: Change `color = Color.Red` to `color = MaterialTheme.colorScheme.error`. Remove `import androidx.compose.ui.graphics.Color` if unused elsewhere.
- `AnchorStep.kt:142`: Same change. Remove Color import if unused.
- `CategoryStep.kt:91`: Same change. Remove Color import if unused.

**2b. Remove unused imports in AnchorStep.kt**

- Remove `import androidx.compose.foundation.layout.Row` (line 6)
- Remove `import androidx.compose.material3.TextButton` (line 19)

**2c. Fix dead onSkip parameter in OptionsStep**

- In `OptionsStep.kt`: Remove the `onSkip: () -> Unit` parameter entirely. The skip button on line 319 already calls `onCreateHabit`, which is the correct behavior.
- In `CreateHabitScreen.kt`: Remove `onSkip = viewModel::createHabit` from the `OptionsStep` call.

**2d. Remove no-op .map on habitColors**

- In `OptionsStep.kt` lines 66-75: Remove `.map { (hex, color) -> hex to color }` from the end of the `habitColors` list.

**2e. Fix hardcoded step count**

- In `CreateHabitScreen.kt:87`: Change `"$stepTitle ($stepNumber/4)"` to `"$stepTitle ($stepNumber/${WizardStep.entries.size})"`.

**2f. Remove redundant categoryEmoji/categoryName in TodayScreen**

- In `TodayScreen.kt`: Replace `"${categoryEmoji(category)} ${categoryName(category)}"` (line 187) with `"${category.emoji} ${category.displayName}"`.
- Delete the `categoryEmoji()` function (lines 231-237) and `categoryName()` function (lines 239-245).

### Phase 3: Test Coverage

Add the top 5 missing test cases.

**3a. Test createHabit() with null category (silent return guard)**

- Test that calling `createHabit()` before selecting a category does not invoke the use case and sets a `creationError`. (Note: with Phase 1f making anchorType non-null, only category null guard remains.)

**3b. Test onAnchorTimeChanged dual-write**

- Test that calling `onAnchorTimeChanged("07:30")` sets both `anchorTime = "07:30"` AND `anchorBehavior = "07:30"`.

**3c. Test goToNextStep from OPTIONS is a no-op**

- Set up state to OPTIONS step, call `goToNextStep()`, verify step remains OPTIONS.

**3d. Test clearCreationError**

- Set up a state with `creationError`, call `clearCreationError()`, verify `creationError` is null.

**3e. Test double-submission guard**

- With `isCreating = true`, call `createHabit()`, verify use case is not invoked.

**3f. Test Custom frequency with empty activeDays validation**

- Set up for create, select Custom frequency but do not select any days. Call `createHabit()`. Verify use case is not invoked and `creationError` is set.

**3g. Test initial isCreating is false**

- Verify `viewModel.uiState.value.isCreating` is `false` on init.

**3h. Update existing tests for non-nullable anchorType**

- Tests that check `initial state - anchorType is null` need updating to check `anchorType is AfterBehavior`.
- Tests that validate null anchor type error ("Please select an anchor type") should be removed since the state is no longer possible.

## Team Orchestration

- You operate as the team lead and orchestrate the team to execute the plan.
- You're responsible for deploying the right team members with the right context to execute the plan.
- IMPORTANT: You NEVER operate directly on the codebase. You use `Task` and `Task*` tools to deploy team members to the building, validating, testing, deploying, and other tasks.

### Team Members

- Specialist
    - Name: builder-core
    - Role: Fix all critical, high, and important code issues (Phases 1 and 2)
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-tests
    - Role: Add missing test coverage and update existing tests (Phase 3)
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

### 1. Fix Critical Bugs (LazyVerticalGrid crash + silent returns)

- **Task ID**: fix-critical-bugs
- **Depends On**: none
- **Assigned To**: builder-core
- **Agent Type**: general-purpose
- **Parallel**: false
- Replace `LazyVerticalGrid` in `CategoryStep.kt` with a non-lazy `Column`+`Row` layout. Remove `.height(320.dp)`. Remove lazy grid imports.
- In `CreateHabitViewModel.kt`: Replace `?: return` on lines 155-156 with explicit null checks that set `creationError` and log via `Timber.e()`.
- Read existing code before modifying. Match existing code style.

### 2. Fix High-Severity ViewModel Issues

- **Task ID**: fix-high-severity
- **Depends On**: fix-critical-bugs
- **Assigned To**: builder-core
- **Agent Type**: general-purpose
- **Parallel**: false
- Add `if (_uiState.value.isCreating) return` guard at top of `createHabit()`.
- Wrap `viewModelScope.launch` body in try-catch. Re-throw `CancellationException`. Log and set `creationError` for unexpected exceptions. Always reset `isCreating = false`.
- Add Custom frequency empty activeDays validation before habit construction.
- Make `anchorType` non-nullable in `CreateHabitUiState` (default to `AnchorType.AfterBehavior`). Update ViewModel accordingly: remove null checks for anchorType, simplify `goToNextStep()` ANCHOR validation.
- Update `AnchorStep.kt`: change parameter from `AnchorType?` to `AnchorType`, remove null branches.
- Pass `isCreating` to `OptionsStep` and disable Create/Skip buttons when true.

### 3. Fix Important UI/Code Quality Issues

- **Task ID**: fix-ui-cleanup
- **Depends On**: fix-high-severity
- **Assigned To**: builder-core
- **Agent Type**: general-purpose
- **Parallel**: false
- Replace `Color.Red` with `MaterialTheme.colorScheme.error` in `NameStep.kt`, `AnchorStep.kt`, `CategoryStep.kt`. Remove unused `Color` imports.
- Remove unused `Row` and `TextButton` imports in `AnchorStep.kt`.
- Remove `onSkip` parameter from `OptionsStep.kt` and its usage in `CreateHabitScreen.kt`.
- Remove no-op `.map { (hex, color) -> hex to color }` from `habitColors` in `OptionsStep.kt`.
- Change hardcoded `"4"` to `WizardStep.entries.size` in `CreateHabitScreen.kt`.
- Replace `categoryEmoji()`/`categoryName()` with direct `category.emoji`/`category.displayName` in `TodayScreen.kt`. Delete the two functions.

### 4. Add Missing Test Coverage

- **Task ID**: add-test-coverage
- **Depends On**: fix-ui-cleanup
- **Assigned To**: builder-tests
- **Agent Type**: general-purpose
- **Parallel**: false
- Read the updated `CreateHabitViewModel.kt` and `CreateHabitUiState.kt` before writing tests. Match existing test style and patterns.
- Add test: `createHabit with null category sets creationError and does not invoke use case`
- Add test: `onAnchorTimeChanged sets both anchorTime and anchorBehavior`
- Add test: `goToNextStep from OPTIONS stays on OPTIONS`
- Add test: `clearCreationError clears creationError`
- Add test: `createHabit while isCreating does not invoke use case`
- Add test: `createHabit with Custom frequency and empty activeDays sets creationError`
- Add test: `initial state isCreating is false`
- Update existing test: change `initial state - anchorType is null` to assert `AnchorType.AfterBehavior`
- Remove or update test: `goToNextStep from ANCHOR with no anchorType sets anchorError` (no longer possible since anchorType is non-nullable)

### 5. Run Build and Tests

- **Task ID**: run-build-tests
- **Depends On**: add-test-coverage
- **Assigned To**: builder-tests
- **Agent Type**: general-purpose
- **Parallel**: false
- Run `./gradlew :feature:habit:test` to verify all tests pass.
- Run `./gradlew ktlintFormat` to auto-fix any formatting issues.
- Run `./gradlew :feature:habit:build :feature:today:build :app:build` to verify compilation.
- Fix any failures before marking complete.

### 6. Final Validation

- **Task ID**: validate-all
- **Depends On**: run-build-tests
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Verify all critical issues are fixed: no LazyVerticalGrid in scrollable parent, no silent `?: return` in createHabit()
- Verify all high issues are fixed: double-submission guard exists, coroutine has try-catch, Custom+empty days validated, anchorType non-nullable
- Verify all important issues are fixed: no Color.Red, no unused imports, no dead onSkip, no no-op .map, no hardcoded "4", no redundant functions in TodayScreen
- Verify test file has new tests for: null category guard, dual-write, OPTIONS no-op, clearCreationError, double-submission, Custom empty days, initial isCreating
- Verify existing tests updated for non-nullable anchorType
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

- [ ] App does not crash when navigating to the Category step (LazyVerticalGrid replaced)
- [ ] `createHabit()` shows user-friendly error instead of silently returning when category is null
- [ ] `createHabit()` is guarded against double-submission (`isCreating` check)
- [ ] Coroutine in `createHabit()` has try-catch that always resets `isCreating`
- [ ] Custom frequency with empty activeDays is rejected with an error message
- [ ] `anchorType` defaults to `AfterBehavior` (non-nullable in UI state)
- [ ] Error text uses `MaterialTheme.colorScheme.error` (not `Color.Red`) in all 3 step files
- [ ] No unused imports in `AnchorStep.kt`
- [ ] `onSkip` parameter removed from `OptionsStep`
- [ ] No-op `.map` removed from `habitColors`
- [ ] Step counter uses `WizardStep.entries.size` instead of hardcoded "4"
- [ ] Redundant `categoryEmoji()`/`categoryName()` removed from TodayScreen
- [ ] All new tests pass
- [ ] `./gradlew :feature:habit:test` passes
- [ ] `./gradlew :feature:habit:build :feature:today:build :app:build` succeeds
- [ ] ktlint reports no errors

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew :feature:habit:test` -- Run habit module unit tests
- `./gradlew ktlintFormat` -- Auto-fix and verify formatting
- `./gradlew :feature:habit:build` -- Verify habit module compiles
- `./gradlew :feature:today:build` -- Verify today module compiles (TodayScreen changes)
- `./gradlew :app:build` -- Verify app module compiles (navigation wiring)

## Notes

- The `isDueToday()` bug for Weekdays/Weekends in `Habit.kt` was flagged by the type-analyzer but is outside the scope of this PR's changes (it's in the domain entity, not in the create flow). Track separately.
- Time format validation for the `AtTime` anchor (replacing text field with TimePicker) is a UX enhancement that should be its own PR.
- Type design improvements (CreationStatus sealed class, Custom carrying days data) are valuable refactors but should be done as follow-up work to keep this PR focused on bug fixes and code quality.
- Internal validator messages leaking to users (Issue 11 from silent-failure-hunter) is a cross-cutting concern affecting all use cases, not just create. Track separately.
