# Plan: Address PR Review Issues for feat/repo-use-cases

## Task Description

Address all Critical and Important issues identified by the 5-agent PR review of the `feat/repo-use-cases` branch. The review covered 13 modified files and ~22 new files across domain validators, data repositories, core use cases, tests, and DI modules.

The issues fall into these categories:

1. **Error handling foundation** - CancellationException swallowing, Result.Error losing context, no Timber logging, silent data corruption in mappers
2. **Architecture violation** - Use cases placed in `core/` instead of `domain/`
3. **Logic bugs** - UndoCompletionUseCase default parameter, GetWeeklyStatsUseCase hardcoded totalDays
4. **Data layer deficiencies** - getByStatus in-memory filtering, getLapsedHabits hardcoded threshold, duplicated isDueOnDate logic
5. **Type design gaps** - HabitWithStatus and WeeklyStats allow invalid state
6. **Missing tests** - GetWeeklyStatsUseCase and SkipHabitUseCase have zero coverage
7. **Comment/doc inaccuracies** - Misleading KDoc, over-commenting, missing documentation

## Objective

All Critical and Important issues from the PR review are resolved. The codebase has correct error handling with CancellationException propagation, Timber logging, and exception context preservation. Use cases live in the correct module. Logic bugs are fixed. Type invariants are enforced. Missing tests are added. Comments are accurate.

## Problem Statement

The PR review identified 6 Critical issues that could cause data corruption, broken coroutine structured concurrency, or incorrect business logic; 10 Important issues affecting code quality, test coverage, and architecture compliance; and 8 Suggestions for further improvement.

## Solution Approach

Work in 5 phases with parallel execution where file dependencies allow:

1. **Phase 1 (Foundation)**: Fix Result.Error, CancellationException, Timber logging, and mapper silent corruption -- these are cross-cutting and must come first since they touch nearly every file.
2. **Phase 2 (Parallel)**: Move use cases to domain/ (architecture) || Fix data layer issues (no file overlap).
3. **Phase 3**: Fix use case logic bugs, type invariants, and extract shared isDueOnDate -- depends on architecture move completing.
4. **Phase 4**: Add missing tests and fix comments/documentation.
5. **Phase 5**: Quality validation.

## Relevant Files

### Modified in this phase (existing files)

- `domain/src/main/kotlin/com/getaltair/kairos/domain/common/Result.kt` - Add `cause: Throwable?` field to Error
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt` - CancellationException, Timber, getByStatus fix, getLapsedHabits fix, extract isDueOnDate
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/CompletionRepositoryImpl.kt` - CancellationException, Timber, fix `?: "Full"` fallback
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/PreferencesRepositoryImpl.kt` - CancellationException, Timber, fix `?: "09:00"` and `?: "System"` fallbacks
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/HabitEntityMapper.kt` - Replace `?: "default"` fallbacks with throws
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/CompletionEntityMapper.kt` - Replace `?: "Full"` fallback with throw
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/UserPreferencesEntityMapper.kt` - Add Timber logging to catch blocks
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt` - Add getByStatus query
- `core/src/main/kotlin/com/getaltair/kairos/core/di/UseCaseModule.kt` - Update imports after use case move
- `core/build.gradle.kts` - Update after use case move
- `domain/build.gradle.kts` - Add mockk dependency for use case tests
- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitWithStatus.kt` - Add init block, KDoc
- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/WeeklyStats.kt` - Add init block, make completionRate computed, KDoc
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Completion.kt` - Add partialPercent range + energyLevel range to init block
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/HabitRepository.kt` - Fix getHabitsForDate KDoc
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/CompletionRepository.kt` - Fix insert KDoc
- `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt` - Update module imports if needed

### New Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/CompleteHabitUseCase.kt` - Moved from core/
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/CreateHabitUseCase.kt` - Moved from core/
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/GetTodayHabitsUseCase.kt` - Moved from core/
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/GetWeeklyStatsUseCase.kt` - Moved from core/
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/SkipHabitUseCase.kt` - Moved from core/
- `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/UndoCompletionUseCase.kt` - Moved from core/
- `domain/src/main/kotlin/com/getaltair/kairos/domain/util/HabitScheduleUtil.kt` - Extracted shared isDueOnDate logic
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/GetWeeklyStatsUseCaseTest.kt` - New test file
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/SkipHabitUseCaseTest.kt` - New test file
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/CompleteHabitUseCaseTest.kt` - Moved from core/
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/CreateHabitUseCaseTest.kt` - Moved from core/
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/GetTodayHabitsUseCaseTest.kt` - Moved from core/
- `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/UndoCompletionUseCaseTest.kt` - Moved from core/

### Deleted Files

- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/` - Entire directory (moved to domain/)
- `core/src/test/kotlin/com/getaltair/kairos/core/usecase/` - Entire directory (moved to domain/)

## Implementation Phases

### Phase 1: Error Handling Foundation

All cross-cutting error handling fixes. Must complete before other phases since it touches every repository, use case, and mapper file.

**Changes:**

1. **Result.Error**: Add `cause: Throwable? = null` field. Update `getOrThrow()` to include cause. Update `onError` lambda to accept `(String, Throwable?)`.
2. **CancellationException**: In every `catch (e: Exception)` block across all repositories and use cases, add `if (e is kotlinx.coroutines.CancellationException) throw e` as the first line. This preserves structured concurrency.
3. **Timber logging**: Add `Timber.e(e, "...")` in every catch block before returning `Result.Error`. Include relevant context (IDs, operation name).
4. **Result.Error cause**: Update every `Result.Error("...: ${e.message}")` to `Result.Error("...: ${e.message}", cause = e)`.
5. **Mapper fallbacks**: In `HabitEntityMapper.toEntity()`, replace all `?: "default"` with `?: throw IllegalStateException("Failed to convert ...")`. Same for `CompletionEntityMapper.toEntity()` (`?: "Full"` on line 47).
6. **Repository update fallbacks**: In `HabitRepositoryImpl.update()`, replace `?: "AfterBehavior"`, `?: "Daily"`, `?: "ONBOARD"` with throws. In `CompletionRepositoryImpl.update()`, replace `?: "Full"`. In `PreferencesRepositoryImpl.update()`, replace `?: "09:00"` and `?: "System"`.
7. **UserPreferencesEntityMapper**: Add `Timber.e(e, ...)` to both catch blocks (lines 23 and 48). Keep the graceful degradation (emptyMap/null) but log the error.

### Phase 2: Architecture Move + Data Layer Fixes (Parallel)

Two independent work streams that can execute simultaneously.

**Stream A - Architecture Move:**

1. Create `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/` directory
2. Move all 6 use case files from `core/src/.../core/usecase/` to `domain/src/.../domain/usecase/`, updating package declarations from `com.getaltair.kairos.core.usecase` to `com.getaltair.kairos.domain.usecase`
3. Move all 4 test files from `core/src/test/.../core/usecase/` to `domain/src/test/.../domain/usecase/`, updating package declarations
4. Update `UseCaseModule.kt` imports to reference `com.getaltair.kairos.domain.usecase.*`
5. Remove empty `core/src/.../core/usecase/` and `core/src/test/.../core/usecase/` directories
6. Verify `core/build.gradle.kts` still needs `:domain` dependency (yes, for UseCaseModule imports)
7. Ensure `domain/build.gradle.kts` has `testImplementation(libs.mockk)` and `testImplementation(libs.kotlinx.coroutines.test)`

**Stream B - Data Layer Fixes:**

1. **HabitDao.getByStatus**: Add `@Query("SELECT * FROM habits WHERE status = :status ORDER BY created_at DESC") fun getByStatus(status: String): List<HabitEntity>` query
2. **HabitRepositoryImpl.getByStatus**: Replace in-memory filtering with `habitDao.getByStatus(habitStatusConverter.habitStatusToString(status))` and map the result
3. **HabitRepositoryImpl.getLapsedHabits**: Add a TODO comment documenting that this uses a hardcoded threshold and should use per-habit `lapse_threshold_days` in the future (the DAO query itself would need to change, which is out of scope for this fix)
4. **HabitRepository.getHabitsForDate KDoc**: Remove "and checks habit phase" since the implementation does not check phase
5. **CompletionRepository.insert KDoc**: Change to "Uses REPLACE conflict strategy; duplicate prevention is enforced at the use case layer"

### Phase 3: Logic Fixes + Type Invariants

Depends on Phase 2 completing (files have been moved, data layer is stable).

**Use Case Logic Fixes:**

1. **UndoCompletionUseCase**: Remove default value from `completedAt` parameter. Make it `completedAt: Instant` (required). Update KDoc to clarify the caller must provide the original completion's createdAt timestamp.
2. **GetWeeklyStatsUseCase**: When `habitId != null`, retrieve the habit (already done), then calculate `totalDays` using `HabitScheduleUtil.countDueDays(habit, weekStart, today)` instead of hardcoded 7. When `habitId == null`, keep totalDays = 7 but add KDoc noting this is an approximation for aggregate mode.
3. **Extract HabitScheduleUtil**: Create `domain/src/main/kotlin/com/getaltair/kairos/domain/util/HabitScheduleUtil.kt` with shared `isDueOnDate(habit, dayOfWeek)`, `countDueDays(habit, start, end)`, `WEEKDAYS`, and `WEEKENDS`. Update `HabitRepositoryImpl` and `GetTodayHabitsUseCase` to delegate to it. Remove the duplicated private functions and companion objects.
4. **Use case comment cleanup**: Remove obvious inline comments (`// Validate habit exists`, `// Create the completion`, etc.) from all use cases. Keep business rule identifier comments (C-1, C-3, etc.).
5. **CompleteHabitUseCase KDoc**: Update to reference "all completion rules (C-1 through C-5)" instead of listing only C-1 and C-3.
6. **SkipHabitUseCase KDoc**: Same update.

**Type Invariant Fixes:**

1. **HabitWithStatus**: Expand to multi-line data class. Add init block: `require(todayCompletion == null || todayCompletion.habitId == habit.id)` and `require(weekCompletionRate in 0f..1f)`. Add KDoc documenting the type and `weekCompletionRate` semantics.
2. **WeeklyStats**: Remove `completionRate` from constructor. Make it a computed `val completionRate: Float get() = if (totalDays > 0) (completedCount + partialCount).toFloat() / totalDays else 0f`. Add init block for non-negative counts and `totalDays in 1..7`. Add KDoc documenting nullable `habitId` semantics. Update `GetWeeklyStatsUseCase` to stop passing `completionRate` to constructor.
3. **Completion.init**: Add `partialPercent` range check (`require(partialPercent == null || partialPercent in 1..99)`) and `energyLevel` range check (`require(energyLevel == null || energyLevel in 1..5)`).

### Phase 3: Integration & Polish

Testing and documentation to ensure all changes are correct.

**Missing Tests:**

1. **GetWeeklyStatsUseCaseTest**: 6-8 tests covering:
    - Happy path with habitId provided, various completion types
    - Happy path with habitId = null (aggregate)
    - Habit not found error propagation
    - Repository error propagation
    - Completion rate calculation for non-daily habit (uses due days, not 7)
    - Zero completions edge case
2. **SkipHabitUseCaseTest**: 5-6 tests covering:
    - Happy path: skip with reason
    - Happy path: skip without reason
    - Habit not found error
    - C-3 duplicate completion rejection
    - Repository error propagation
    - Exception caught and wrapped
3. **HabitValidatorTest additions**: 4 tests for H-6 timestamp sub-rules:
    - `pausedAt` before `createdAt` returns error
    - `archivedAt` before `createdAt` returns error
    - `pausedAt` after `archivedAt` returns error
    - Valid `createdAt < pausedAt < archivedAt` passes
4. **UndoCompletionUseCaseTest**: Fix boundary test flakiness by injecting a clock or using a wider tolerance. Update test for removed default parameter.
5. **Existing test updates**: Update all moved test file package declarations and imports.

**Documentation Fixes:**

1. **UseCaseModule KDoc**: Replace "P0 use cases" with "MVP use cases"
2. **CompletionRepositoryImpl class KDoc**: Note that `update()` uses direct converters, not the mapper
3. **HabitRepositoryImpl class KDoc**: Same note about `update()`
4. **WeeklyStats and HabitWithStatus**: Add KDoc (covered in type invariant fixes)

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
    - Name: error-handling-builder
    - Role: Fix Result.Error, CancellationException, Timber logging, and mapper silent corruption across all repositories, use cases, and mappers
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: architecture-builder
    - Role: Move use cases from core/ to domain/, update package declarations, imports, and build files
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: data-layer-builder
    - Role: Fix data layer issues (getByStatus DAO query, getLapsedHabits docs, repository KDoc fixes)
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: logic-types-builder
    - Role: Fix use case logic bugs, extract shared HabitScheduleUtil, add type invariants, clean up comments
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: test-builder
    - Role: Add missing tests (GetWeeklyStatsUseCaseTest, SkipHabitUseCaseTest, HabitValidator timestamp tests), update existing tests
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

### 1. Fix Error Handling Foundation

- **Task ID**: error-handling-foundation
- **Depends On**: none
- **Assigned To**: error-handling-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Add `cause: Throwable? = null` to `Result.Error` data class in `domain/src/main/kotlin/com/getaltair/kairos/domain/common/Result.kt`. Update `getOrThrow()` to pass cause. Update `onError` signature.
- In every `catch (e: Exception)` block across ALL repository implementations (`HabitRepositoryImpl`, `CompletionRepositoryImpl`, `PreferencesRepositoryImpl`) and ALL use cases (`CompleteHabitUseCase`, `CreateHabitUseCase`, `GetTodayHabitsUseCase`, `GetWeeklyStatsUseCase`, `SkipHabitUseCase`, `UndoCompletionUseCase`):
    - Add `if (e is kotlinx.coroutines.CancellationException) throw e` as the first line
    - Add `Timber.e(e, "descriptive message with context")` before the return
    - Update `Result.Error(...)` to include `cause = e`
- In `HabitEntityMapper.toEntity()`: Replace all 5 `?: "default"` fallbacks with `?: throw IllegalStateException("Failed to convert: ...")`
- In `CompletionEntityMapper.toEntity()`: Replace `?: "Full"` on line 47 with throw
- In `HabitRepositoryImpl.update()`: Replace `?: "AfterBehavior"`, `?: "Daily"`, `?: "ONBOARD"` with throws
- In `CompletionRepositoryImpl.update()`: Replace `?: "Full"` with throw
- In `PreferencesRepositoryImpl.update()`: Replace `?: "09:00"` and `?: "System"` with throws
- In `UserPreferencesEntityMapper`: Add `Timber.e(e, ...)` to both catch blocks, keep the graceful degradation
- Add Timber import to all files that now use it
- Ensure all tests still compile (Result.Error constructor is backward-compatible due to default value on cause)

### 2. Move Use Cases to Domain Module

- **Task ID**: architecture-move
- **Depends On**: error-handling-foundation
- **Assigned To**: architecture-builder
- **Agent Type**: general-purpose
- **Parallel**: true (parallel with data-layer-fixes)
- Create directory `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/`
- Create directory `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/`
- Move all 6 use case source files from `core/src/main/kotlin/com/getaltair/kairos/core/usecase/` to `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/`
- Move all 4 test files from `core/src/test/kotlin/com/getaltair/kairos/core/usecase/` to `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/`
- Update `package` declaration in all moved files from `com.getaltair.kairos.core.usecase` to `com.getaltair.kairos.domain.usecase`
- Update `UseCaseModule.kt` imports from `com.getaltair.kairos.core.usecase.*` to `com.getaltair.kairos.domain.usecase.*`
- Remove empty directories `core/src/main/kotlin/com/getaltair/kairos/core/usecase/` and `core/src/test/kotlin/com/getaltair/kairos/core/usecase/`
- Verify `domain/build.gradle.kts` has `testImplementation(libs.mockk)` and `testImplementation(libs.kotlinx.coroutines.test)` (should already be there from this PR)
- Verify `core/build.gradle.kts` still has `implementation(project(":domain"))` (needed for UseCaseModule)
- Remove `testImplementation(libs.mockk)` and `testImplementation(libs.kotlinx.coroutines.test)` from `core/build.gradle.kts` since tests are moving out

### 3. Fix Data Layer Issues

- **Task ID**: data-layer-fixes
- **Depends On**: error-handling-foundation
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true (parallel with architecture-move)
- Add `@Query("SELECT * FROM habits WHERE status = :status ORDER BY created_at DESC") fun getByStatus(status: String): List<HabitEntity>` to `HabitDao`
- Update `HabitRepositoryImpl.getByStatus()` to use the new DAO query: convert `HabitStatus` to string via `habitStatusConverter`, call `habitDao.getByStatus(statusString)`, map results. Remove the `getAll()` + filter pattern.
- Add a TODO comment on `HabitRepositoryImpl.getLapsedHabits()` documenting the hardcoded threshold limitation: `// TODO: Use per-habit lapse_threshold_days instead of hardcoded default`
- Fix `HabitRepository.getHabitsForDate` KDoc: Remove "and checks habit phase" (implementation does not check phase)
- Fix `CompletionRepository.insert` KDoc: Change to "Uses REPLACE conflict strategy; duplicate prevention is enforced at the use case layer"
- Fix `CompletionRepositoryImpl` class KDoc: Note that `update()` uses direct converters, not the mapper
- Fix `HabitRepositoryImpl` class KDoc: Same note about `update()`

### 4. Fix Use Case Logic and Type Invariants

- **Task ID**: logic-types-fixes
- **Depends On**: architecture-move, data-layer-fixes
- **Assigned To**: logic-types-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- **Extract HabitScheduleUtil**: Create `domain/src/main/kotlin/com/getaltair/kairos/domain/util/HabitScheduleUtil.kt` as an `object` with:
    - `fun isDueOnDate(habit: Habit, dayOfWeek: DayOfWeek): Boolean` (same logic currently in HabitRepositoryImpl and GetTodayHabitsUseCase)
    - `fun countDueDays(habit: Habit, start: LocalDate, end: LocalDate): Int` (same logic currently in GetTodayHabitsUseCase)
    - `val WEEKDAYS` and `val WEEKENDS` sets
- Update `HabitRepositoryImpl.isDueOnDate()` to delegate to `HabitScheduleUtil.isDueOnDate()`. Remove private `isDueOnDate`, `WEEKDAYS`, `WEEKENDS` from companion object.
- Update `GetTodayHabitsUseCase` to use `HabitScheduleUtil.isDueOnDate()` and `HabitScheduleUtil.countDueDays()`. Remove private functions and companion object. Clean up fully qualified imports.
- **Fix UndoCompletionUseCase**: Remove `= Instant.now()` default from `completedAt` parameter. Update KDoc to clarify caller must provide the original completion timestamp.
- **Fix GetWeeklyStatsUseCase**: When `habitId != null`, after fetching the habit, call `HabitScheduleUtil.countDueDays(habit, weekStart, today)` to get the correct `totalDays`. When `habitId == null`, keep `totalDays = 7` but add KDoc clarifying this is an approximation.
- **Fix WeeklyStats**: Remove `completionRate` from constructor parameters. Add it as a computed property: `val completionRate: Float get() = if (totalDays > 0) (completedCount + partialCount).toFloat() / totalDays else 0f`. Add init block:
    ```kotlin
    init {
        require(totalDays in 1..7) { "totalDays must be in 1..7" }
        require(completedCount >= 0) { "completedCount must be non-negative" }
        require(partialCount >= 0) { "partialCount must be non-negative" }
        require(skippedCount >= 0) { "skippedCount must be non-negative" }
        require(missedCount >= 0) { "missedCount must be non-negative" }
    }
    ```
    Add KDoc documenting `habitId` null semantics and completionRate formula.
- Update `GetWeeklyStatsUseCase` to stop passing `completionRate` to the WeeklyStats constructor.
- **Fix HabitWithStatus**: Expand to multi-line. Add init block:
    ```kotlin
    init {
        require(todayCompletion == null || todayCompletion.habitId == habit.id) {
            "todayCompletion.habitId must match habit.id"
        }
        require(weekCompletionRate in 0f..1f) {
            "weekCompletionRate must be in 0.0..1.0"
        }
    }
    ```
    Add KDoc.
- **Fix Completion.init**: Add `partialPercent` range check and `energyLevel` range check:
    ```kotlin
    if (partialPercent != null) {
        require(partialPercent in 1..99) { "partialPercent must be in 1..99" }
    }
    if (energyLevel != null) {
        require(energyLevel in 1..5) { "energyLevel must be in 1..5" }
    }
    ```
- **Clean up use case comments**: Remove obvious inline comments (`// Validate habit exists`, `// Create the completion`, etc.). Keep business rule references (C-1, C-3).
- Update `CompleteHabitUseCase` and `SkipHabitUseCase` KDoc to reference "all completion rules (C-1 through C-5)".
- Replace "P0 use cases" with "MVP use cases" in `UseCaseModule.kt` KDoc.

### 5. Add Missing Tests and Fix Documentation

- **Task ID**: tests-and-docs
- **Depends On**: logic-types-fixes
- **Assigned To**: test-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- **GetWeeklyStatsUseCaseTest** (`domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/GetWeeklyStatsUseCaseTest.kt`):
    - Test happy path with habitId, various completion types counted correctly
    - Test happy path with habitId = null (aggregate stats)
    - Test habit not found returns error
    - Test repository error propagation
    - Test completion rate for non-daily habit (uses actual due days, not 7)
    - Test zero completions returns all zeros and 0f rate
    - Test CancellationException is rethrown (not caught)
- **SkipHabitUseCaseTest** (`domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/SkipHabitUseCaseTest.kt`):
    - Test skip with reason succeeds
    - Test skip without reason succeeds
    - Test habit not found returns error
    - Test C-3 duplicate rejection
    - Test repository error propagation
    - Test exception caught and wrapped
- **HabitValidatorTest additions** (add to existing `domain/src/test/kotlin/com/getaltair/kairos/domain/validator/HabitValidatorTest.kt`):
    - Test `pausedAt` before `createdAt` returns error
    - Test `archivedAt` before `createdAt` returns error
    - Test `pausedAt` after `archivedAt` returns error (when both set)
    - Test valid `createdAt < pausedAt < archivedAt` passes
- **UndoCompletionUseCaseTest updates**:
    - Update all test calls to pass `completedAt` explicitly (no more default)
    - Fix boundary test flakiness: use `Instant.now().minusSeconds(30)` with the understanding that the test clock and production `Instant.now()` may differ by milliseconds. Consider using `minusSeconds(29)` for the "at boundary" test to avoid flakiness.
- **CompletionValidatorTest**: Verify existing tests still pass with new `partialPercent` range enforcement in init block. The test `C-2 FULL with non-null partialPercent rejects at init block` already exercises this path.
- Verify all test imports updated to `com.getaltair.kairos.domain.usecase` package.

### 6. Final Validation

- **Task ID**: validate-all
- **Depends On**: error-handling-foundation, architecture-move, data-layer-fixes, logic-types-fixes, tests-and-docs
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run `./gradlew :domain:test` to verify all domain tests pass
- Run `./gradlew :core:test` to verify core tests pass (should have no tests left after move)
- Run `./gradlew build` to verify full project compiles
- Verify no files remain in `core/src/main/kotlin/com/getaltair/kairos/core/usecase/`
- Verify no files remain in `core/src/test/kotlin/com/getaltair/kairos/core/usecase/`
- Verify all use cases are in `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/`
- Spot-check that every `catch (e: Exception)` block in repos/use cases has CancellationException rethrow
- Spot-check that no `?: "default"` fallbacks remain in mapper toEntity methods
- Spot-check that `Result.Error` calls include `cause = e` in catch blocks
- Verify `WeeklyStats.completionRate` is a computed property (not in constructor)
- Verify `HabitWithStatus` has init block with cross-field validation
- Verify `GetWeeklyStatsUseCase` uses `HabitScheduleUtil.countDueDays` for per-habit mode
- Verify `UndoCompletionUseCase.invoke` has no default for `completedAt`
- Operate in validation mode: inspect and report only, do not modify files
- Report pass/fail for each criterion

## Acceptance Criteria

1. **CancellationException**: Every `catch (e: Exception)` block in repositories and use cases rethrows `CancellationException`
2. **Result.Error cause**: `Result.Error` has a `cause: Throwable? = null` field; all catch blocks pass `cause = e`
3. **Timber logging**: Every catch block in repositories and use cases has a `Timber.e(e, ...)` call
4. **No silent corruption**: No `?: "default"` fallbacks remain in mapper `toEntity` methods or repository `update` methods
5. **Architecture**: All use cases live in `domain/src/.../domain/usecase/`; none remain in `core/src/.../core/usecase/`
6. **UndoCompletionUseCase**: `completedAt` parameter has no default value
7. **GetWeeklyStatsUseCase**: Uses frequency-aware due day calculation for per-habit mode
8. **HabitScheduleUtil**: Shared utility exists in domain; no duplicated isDueOnDate logic in HabitRepositoryImpl or GetTodayHabitsUseCase
9. **getByStatus**: Uses DAO SQL query, not in-memory filtering
10. **WeeklyStats**: `completionRate` is a computed property; init block validates count invariants
11. **HabitWithStatus**: Init block validates `todayCompletion.habitId == habit.id` and `weekCompletionRate` range
12. **Completion.init**: Validates `partialPercent in 1..99` and `energyLevel in 1..5` when non-null
13. **Tests**: GetWeeklyStatsUseCaseTest (6+ tests), SkipHabitUseCaseTest (5+ tests), HabitValidatorTest timestamp tests (4 tests) all pass
14. **Build**: `./gradlew build` succeeds with no errors
15. **Domain tests**: `./gradlew :domain:test` passes all tests

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew :domain:test` - Run all domain module tests (validators + use cases)
- `./gradlew :core:test` - Verify core module has no remaining use case tests
- `./gradlew :data:test` - Run data module tests
- `./gradlew build` - Full project build succeeds
- `grep -r "CancellationException" data/src/main/kotlin/ core/src/main/kotlin/ domain/src/main/kotlin/ --include="*.kt" | wc -l` - Verify CancellationException handling exists in catch blocks
- `grep -rn '?: "' data/src/main/kotlin/com/getaltair/kairos/data/mapper/ --include="*.kt"` - Verify no silent fallbacks remain in mappers
- `find core/src/main/kotlin/com/getaltair/kairos/core/usecase -name "*.kt" 2>/dev/null | wc -l` - Should be 0 (all moved)
- `find domain/src/main/kotlin/com/getaltair/kairos/domain/usecase -name "*.kt" 2>/dev/null | wc -l` - Should be 6
- `find domain/src/test/kotlin/com/getaltair/kairos/domain/usecase -name "*.kt" 2>/dev/null | wc -l` - Should be 6 (4 moved + 2 new)
- `./gradlew ktlintFormat` - Auto-fix any formatting issues

## Notes

1. The `UserPreferencesEntityMapper` catch blocks are kept as graceful degradation (emptyMap/null) because notification channel data is non-critical and the app should still function. But Timber logging is mandatory so the issue is observable.
2. The `getLapsedHabits` hardcoded threshold is documented as a TODO rather than fixed because the DAO query change requires a more complex SQL modification (joining against per-habit thresholds) that is out of scope for this PR fix pass.
3. `WeeklyStats` removing `completionRate` from the constructor is a breaking change for any existing callers. Since this is new, unreleased code, this is safe. The computed property provides the same API surface for reads.
4. The `domain` module uses `kairos.jvm.library` convention plugin (pure Kotlin, no Android). Use cases are pure Kotlin with constructor injection and do not depend on Android or Koin, so they belong in domain. The Koin wiring (`UseCaseModule.kt`) correctly stays in `core/` since it depends on the Koin framework.
5. Agent model preference: All agents should use **Opus** model for this work given the cross-cutting nature of changes and need for precise code modifications.
