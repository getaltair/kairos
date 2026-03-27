# Plan: Fix PR#2 Critical Issues

## Task Description

Comprehensive fix implementation for PR#2 (feat/db-daos) addressing 35 issues found across code review, error handling, test coverage, type design, and comment analysis. Issues include compilation errors, silent data corruption patterns, missing test coverage, and code quality improvements.

## Objective

Fix all critical and important issues identified in PR#2 review, ensuring:

1. Code compiles without errors
2. Silent data corruption patterns are eliminated
3. Test coverage meets project standards
4. Type system invariants are properly enforced
5. Code quality and maintainability are improved

## Problem Statement

PR#2 introduces Room database DAOs, type converters, and entity mappers. The comprehensive review revealed:

- **Compilation failures**: Missing imports in test files
- **Silent data corruption**: JSON converters return null on parse failure without logging
- **Logic bugs**: CompletionEntity validation is backwards
- **Data loss risk**: Destructive migration enabled
- **Type mismatches**: Enum simpleName values don't match actual object names
- **Missing coverage**: Critical DAO methods and entity mappers are untested
- **Duplicate code**: Unused converter files duplicate RoomTypeConverters functionality

## Solution Approach

**Phase-based implementation**:

1. **Foundation fixes** (Phase 1) - Fix compilation errors and critical data corruption
2. **Test coverage** (Phase 2) - Add tests for untested methods and mappers
3. **Code cleanup** (Phase 3) - Remove duplicates, improve documentation
4. **Validation** (Phase 4) - Quality engineer validates all fixes

## Relevant Files

### Critical Files to Fix

**Compilation Errors**:

- `data/src/test/kotlin/com/getaltair/kairos/data/dao/CompletionDaoTest.kt:102` - Missing `assertTrue` import
- `data/src/test/kotlin/com/getaltair/kairos/data/database/DatabaseInitializationTest.kt:70,77` - Missing `assertTrue` import

**Variable Typos**:

- `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt:14,16,20,24,307,311,319,341` - `moshi` should be `moshi`
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/BlockerConverter.kt:14,15` - `moshi` should be `moshi`

**Logic Bugs**:

- `data/src/main/kotlin/com/getaltair/kairos/data/entity/CompletionEntity.kt:72-75` - Reversed validation logic
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/RecoverySessionEntity.kt:78-86` - Wrong condition for Completed status

**Data Corruption**:

- `data/src/main/kotlin/com/getaltair/kairos/data/converter/JsonListConverter.kt:40-46,59-63` - Silent JSON parse failures
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/JsonMapConverter.kt:37-42` - Silent JSON parse failures
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/DayOfWeekListConverter.kt:26-35` - Silent invalid day filtering
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt` - Multiple silent catch blocks

**Data Loss Risk**:

- `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt:28-33` - `fallbackToDestructiveMigration()`

**Type Mismatches**:

- `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt` - 12 enum simpleName mismatches

**Unused Files to Remove**:

- `data/src/main/kotlin/com/getaltair/kairos/data/converter/InstantConverter.kt` - Duplicate
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/LocalDateConverter.kt` - Duplicate
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/LocalTimeConverter.kt` - Duplicate

### Files to Add Tests To

**Untested DAO Methods**:

- `data/src/test/kotlin/com/getaltair/kairos/data/dao/HabitDaoTest.kt` - Add tests for update(), insertAll(), getLapsedHabits(), getTodayHabitsWithCompletions(), getByUserId(), getActiveHabitsFlow()
- `data/src/test/kotlin/com/getaltair/kairos/data/dao/CompletionDaoTest.kt` - Add tests for update(), insertAll(), getForHabit(), deleteForDateRange(), getAllFlow()

**Untested Mappers** (new test files needed):

- All files in `data/src/main/kotlin/com/getaltair/kairos/data/mapper/`

**Untested Converters**:

- `data/src/test/kotlin/com/getaltair/kairos/data/converter/ConvertersTest.kt` - Add JsonListConverter and JsonMapConverter tests

### Files for Documentation Improvements

**Complex Query Documentation**:

- `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt:145-161` - Document lapse detection logic

**SQL Fix**:

- `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineVariantDao.kt:86` - Fix missing space in SQL

## Implementation Phases

### Phase 1: Foundation Fixes (Critical - Blockers)

Fix compilation errors and silent data corruption that prevents the code from working correctly.

**Tasks**:

1. Add missing imports to test files
2. Fix `moshi` → `moshi` typos in RoomTypeConverters.kt and BlockerConverter.kt
3. Fix CompletionEntity init block validation logic
4. Fix RecoverySessionEntity validation condition
5. Add Timber logging to all silent catch blocks in converters
6. Remove `fallbackToDestructiveMigration()` from DataModule

### Phase 2: Test Coverage (Important)

Add comprehensive test coverage for untested methods and new test files for mappers.

**Tasks**:

1. Add tests for HabitDao.update(), insertAll(), getLapsedHabits(), getTodayHabitsWithCompletions(), getByUserId(), getActiveHabitsFlow()
2. Add tests for CompletionDao.update(), insertAll(), getForHabit(), deleteForDateRange(), getAllFlow()
3. Create test files for all entity mappers (7 new test files)
4. Add tests for JsonListConverter and JsonMapConverter

### Phase 3: Code Cleanup & Quality (Suggestions)

Remove duplicates and improve code quality.

**Tasks**:

1. Remove unused duplicate converter files (InstantConverter.kt, LocalDateConverter.kt, LocalTimeConverter.kt)
2. Fix RoutineVariantDao SQL syntax error
3. Add documentation for complex queries
4. Fix enum display name typos (optional, lower priority)
5. Remove trivial KDoc comments

### Phase 4: Validation

Quality engineer validates all fixes against acceptance criteria.

## Team Orchestration

- You operate as a team lead and orchestrate team to execute this plan.
- You're responsible for deploying the right team members with the right context to execute the plan.
- IMPORTANT: You NEVER operate directly on the codebase. You use `Task` and `Task*` tools to deploy team members to building, validating, testing, deploying, and other tasks.
- This is critical. Your job is to act as a high level director of team, not a builder.
- Your role is to validate all work is going well and make sure the team is on track to complete the plan.
- You'll orchestrate this by using `Task*` Tools to manage coordination between team members.
- Communication is paramount. You'll use `Task*` Tools to communicate with team members and ensure they're on track to complete the plan.
- Take note of the session id of each team member. This is how you'll reference them.

### Team Members

- Builder
    - Name: database-fixer
    - Role: Fix database layer issues - variable typos, logic bugs, converter errors
    - Agent Type: general-purpose
    - Resume: true
    - Notes: Focus on RoomTypeConverters.kt, CompletionEntity, RecoverySessionEntity, all converters

- Test Builder
    - Name: test-coverer
    - Role: Add comprehensive test coverage for DAOs, mappers, and converters
    - Agent Type: general-purpose
    - Resume: true
    - Notes: Create new test files for mappers, add tests to existing DAO test files

- Code Cleaner
    - Name: code-simplifier
    - Role: Remove duplicate files, fix documentation, improve code quality
    - Agent Type: code-simplifier
    - Resume: false
    - Notes: Remove unused converter files, fix SQL syntax, improve KDoc

- Quality Engineer (Validator)
    - Name: validator
    - Role: Validate completed work against acceptance criteria (read-only inspection mode)
    - Agent Type: quality-engineer
    - Resume: false
    - Notes: Operate in validation mode - inspect and report only, do not modify files

## Step by Step Tasks

### 1. Fix compilation errors in test files

- **Task ID**: fix-compilation-errors
- **Depends On**: none
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- Add `import org.junit.Assert.assertTrue` to `data/src/test/kotlin/com/getaltair/kairos/data/dao/CompletionDaoTest.kt` (around line 14)
- Add `import org.junit.Assert.assertTrue` to `data/src/test/kotlin/com/getaltair/kairos/data/database/DatabaseInitializationTest.kt` (around line 14)
- Verify tests compile by running `./gradlew :data:test --tests "*CompletionDaoTest" --tests "*DatabaseInitializationTest"`

### 2. Fix moshi typo in RoomTypeConverters

- **Task ID**: fix-roomtypeconverters-moshi
- **Depends On**: fix-compilation-errors
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt`:
    - Replace `moshi` with `moshi` at lines 14, 16, 20, 24, 307, 311, 319, 341
- Verify converter registration in KairosDatabase still works after variable rename

### 3. Fix moshi typo in BlockerConverter

- **Task ID**: fix-blockerconverter-moshi
- **Depends On**: fix-roomtypeconverters-moshi
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/converter/BlockerConverter.kt`:
    - Replace `moshi` with `moshi` at lines 14, 15
- Fix lambda parameter bug at line 26: change `blockers?.let { blockerListAdapter.toJson(it) }` to use proper parameter

### 4. Fix CompletionEntity init block logic

- **Task ID**: fix-completion-entity-validation
- **Depends On**: fix-blockerconverter-moshi
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/entity/CompletionEntity.kt:72-78`:
    - Fix line 72: `require(type != "Partial" || partialPercent != null)` (was `== null`)
    - Fix line 73: Add `require(type == "Partial" || partialPercent == null)` for clarity
    - Fix line 74: `require(type != "Skipped" || skipReason != null)` (was `== null`)
    - Add line 75: `require(type == "Skipped" || skipReason == null)` for clarity
    - Remove redundant line 76-77 about Full type
- Test that invalid combinations now throw as expected

### 5. Fix RecoverySessionEntity validation

- **Task ID**: fix-recovery-session-validation
- **Depends On**: fix-completion-entity-validation
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/entity/RecoverySessionEntity.kt:78-86`:
    - Fix condition: Completed sessions MUST have action != null
    - Correct logic: `status == SessionStatus.Completed && action != null` (check action is not null)

### 6. Add Timber logging to silent catch blocks

- **Task ID**: add-logging-to-converters
- **Depends On**: fix-recovery-session-validation
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/converter/JsonListConverter.kt`:
    - Add `Timber.e(e, "Failed to parse UUID list from JSON: $json")` in catch block at line 43
    - Add same logging at line 63 for stringToStringList
- In `data/src/main/kotlin/com/getaltair/kairos/data/converter/JsonMapConverter.kt`:
    - Add `Timber.e(e, "Failed to parse map from JSON: $json")` in catch block at line 42
- In `data/src/main/kotlin/com/getaltair/kairos/data/converter/DayOfWeekListConverter.kt`:
    - Add `Timber.w("Invalid day name: $dayName, skipping")` in catch block at line 33
- In `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt`:
    - Add Timber logging to all silent catch blocks (lines 95, 108, 124, 141, and others)
- Run tests to verify logging doesn't break functionality

### 7. Remove destructive migration

- **Task ID**: remove-destructive-migration
- **Depends On**: add-logging-to-converters
- **Assigned To**: database-fixer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt:28-33`:
    - Remove `.fallbackToDestructiveMigration()` call
    - Add comment: "TODO: Implement proper Room migrations for schema changes"
    - Verify database provider compiles

### 8. Add HabitDao tests for untested methods

- **Task ID**: add-habitdao-tests
- **Depends On**: remove-destructive-migration
- **Assigned To**: test-coverer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/test/kotlin/com/getaltair/kairos/data/dao/HabitDaoTest.kt`:
    - Add test for `update()` method (20 params)
    - Add test for `insertAll()` bulk operation
    - Add test for `getLapsedHabits()` complex JOIN/HAVING query
    - Add test for `getTodayHabitsWithCompletions()` with Flow return
    - Add test for `getByUserId()` for user-specific data
    - Add test for `getActiveHabitsFlow()` reactive method
    - Fix existing test for archived habit (line 106): use `archivedAt` instead of `pausedAt`
    - Run all HabitDao tests

### 9. Add CompletionDao tests for untested methods

- **Task ID**: add-completiondao-tests
- **Depends On**: add-habitdao-tests
- **Assigned To**: test-coverer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/test/kotlin/com/getaltair/kairos/data/dao/CompletionDaoTest.kt`:
    - Add test for `update()` method
    - Add test for `insertAll()` bulk operation
    - Add test for `getForHabit()` for habit history
    - Add test for `deleteForDateRange()` for data cleanup
    - Add test for `getAllFlow()` reactive method
    - Update `createTestCompletion` helper to support partialPercent parameter
    - Run all CompletionDao tests

### 10. Create entity mapper test files

- **Task ID**: create-mapper-tests
- **Depends On**: add-completiondao-tests
- **Assigned To**: test-coverer
- **Agent Type**: general-purpose
- **Parallel**: false
- Create test files for all mappers in `data/src/test/kotlin/com/getaltair/kairos/data/mapper/`:
    - `HabitEntityMapperTest.kt`
    - `CompletionEntityMapperTest.kt`
    - `RoutineEntityMapperTest.kt`
    - `RoutineExecutionEntityMapperTest.kt`
    - `RecoverySessionEntityMapperTest.kt`
    - `RoutineHabitEntityMapperTest.kt`
    - `RoutineVariantEntityMapperTest.kt`
    - `UserPreferencesEntityMapperTest.kt`
- Each test file should:
    - Test toEntity() conversion
    - Test toDomain() conversion
    - Test null handling
    - Test invalid data throws appropriate exceptions
    - Run all mapper tests

### 11. Add converter tests for JSON converters

- **Task ID**: add-json-converter-tests
- **Depends On**: create-mapper-tests
- **Assigned To**: test-coverer
- **Agent Type**: general-purpose
- **Parallel**: false
- In `data/src/test/kotlin/com/getaltair/kairos/data/converter/ConvertersTest.kt`:
    - Add tests for `JsonListConverter` (UUID list and String list variants)
    - Add tests for `JsonMapConverter`
    - Test valid JSON parsing
    - Test invalid JSON handling (now with logging)
    - Test null handling
    - Run all converter tests

### 12. Remove duplicate converter files

- **Task ID**: remove-duplicate-converters
- **Depends On**: add-json-converter-tests
- **Assigned To**: code-simplifier
- **Agent Type**: code-simplifier
- **Parallel**: false
- Remove `data/src/main/kotlin/com/getaltair/kairos/data/converter/InstantConverter.kt`
- Remove `data/src/main/kotlin/com/getaltair/kairos/data/converter/LocalDateConverter.kt`
- Remove `data/src/main/kotlin/com/getaltair/kairos/data/converter/LocalTimeConverter.kt`
- Verify RoomTypeConverters.kt still has these converters
- Run tests to ensure nothing breaks

### 13. Fix RoutineVariantDao SQL syntax

- **Task ID**: fix-routinevariantdao-sql
- **Depends On**: remove-duplicate-converters
- **Assigned To**: code-simplifier
- **Agent Type**: code-simplifier
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineVariantDao.kt:86`:
    - Fix missing space: change `is_default =1` to `is_default = 1`
    - Consider using @Transaction annotation for atomic operation
    - Test the setAsDefault method

### 14. Improve complex query documentation

- **Task ID**: improve-query-documentation
- **Depends On**: fix-routinevariantdao-sql
- **Assigned To**: code-simplifier
- **Parallel**: false
- In `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt:145-161`:
    - Add @param documentation for thresholdDays explaining business meaning
    - Add detailed comment explaining lapse detection algorithm
    - Document what happens for habits with no completions (NULL case)
- In `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineVariantDao.kt:82-92`:
    - Document atomic nature of setAsDefault operation
    - Document @Transaction requirement

### 15. Run validation and acceptance criteria

- **Task ID**: validate-all
- **Depends On**: improve-query-documentation
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run `./gradlew clean` to ensure clean build
- Run `./gradlew build` to verify compilation
- Run `./gradlew test` to verify all tests pass
- Run `./gradlew ktlintCheck` to verify code formatting
- Verify no Timber import issues in test files
- Verify no compilation errors related to variable names
- Verify CompletionEntity validation works correctly
- Verify destructive migration is removed
- Verify all new tests exist and pass
- Verify duplicate files are removed
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

1. **Compilation**: All code compiles without errors (`./gradlew build` succeeds)
2. **Tests Pass**: All tests pass (`./gradlew test` succeeds), including new mapper tests
3. **Code Formatting**: ktlint passes (`./gradlew ktlintCheck` succeeds)
4. **No Silent Failures**: All converters have Timber logging in catch blocks
5. **No Data Loss**: Destructive migration removed from DataModule
6. **Validation Works**: CompletionEntity and RecoverySessionEntity invariants enforce correctly
7. **Coverage**: All critical DAO methods have tests (update, insertAll, complex queries)
8. **Mapper Tests**: All 7 entity mappers have test files
9. **No Duplicates**: Unused converter files removed
10. **Documentation**: Complex queries documented

## Validation Commands

```bash
# Build verification
./gradlew clean
./gradlew build

# Test execution
./gradlew test
./gradlew :data:test
./gradlew :data:test --tests "*HabitDaoTest"
./gradlew :data:test --tests "*CompletionDaoTest"
./gradlew :data:test --tests "*Mapper*"

# Code quality
./gradlew ktlintCheck
./gradlew detekt
```

## Notes

1. **Order matters**: Tasks must execute in order - foundation fixes before tests, tests before cleanup
2. **Timber**: Ensure Timber is properly imported in converter files before adding logging
3. **KairosDatabase**: Consider adding entities and DAOs to @Database annotation if not already done
4. **Test helpers**: Leverage existing helper methods in test files (createTestHabit, createTestCompletion)
5. **Mapper tests**: Follow pattern of existing converter tests (null handling, valid cases, error cases)
6. **Flow tests**: Use `runTest` coroutine test runner for Flow-based methods
7. **Enum typos**: RoomTypeConverters has 12 enum simpleName mismatches - decide whether to fix domain enums or converters
8. **Git commit**: After validation passes, commit with conventional commit message format

### Decision Point: Enum simpleName Mismatches

The RoomTypeConverters.kt uses hardcoded strings that don't match domain enum simpleName values (due to typos in domain enums like `Evensing`, `Deparure`, etc.). Two options:

**Option A**: Fix domain enum simpleNames to standard English spelling

- Pros: Corrects root cause, cleaner code
- Cons: Requires changes to domain layer files

**Option B**: Fix RoomTypeConverters to match existing domain enum typos

- Pros: Smaller change, keeps domain as-is
- Cons: Perpetuates typos throughout codebase

**Recommendation**: Option A - fix domain enums to use correct spelling

### Additional Considerations

1. **Indexes**: Consider adding indexes for common query patterns (paused_at, archived_at, completed_at)
2. **Validation functions**: Consider adding companion object validation methods to domain entities
3. **Error messages**: Improve exception messages to include valid values
4. **Transaction annotations**: Review DAO methods that should use @Transaction
