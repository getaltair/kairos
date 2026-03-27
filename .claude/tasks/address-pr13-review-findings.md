# Plan: Address All PR #13 Review Findings

## Task Description

Address all 15 findings from the comprehensive PR review of PR #13 (feat: set up Firebase project with Firestore sync module). This includes 3 Critical issues, 6 Important issues, and 6 Suggestions identified by 5 specialized review agents (code-reviewer, test-analyzer, error-hunter, comment-analyzer, type-analyzer).

## Objective

Fix all review findings across the sync module, Firestore rules, and tests so the PR is ready for merge with no outstanding issues. Every critical and important issue must be resolved; suggestions should be implemented where practical.

## Problem Statement

The PR review identified:

- Silent data loss (5 entities drop `createdAt`/`updatedAt`, timeWindow half-set bug)
- No schema validation in Firestore security rules
- Raw casts with no contextual error messages in all `fromMap` functions
- No input validation in `FirestoreCollections`
- Missing test coverage for 11 of 13 `fromTag` error paths
- Non-deterministic `System.currentTimeMillis()` embedded in mapping functions
- Inaccurate comments about sealed class naming conventions
- Stale `.claude/tasks/` files committed to the repo

## Solution Approach

1. Fix all data loss bugs in `FirestoreMapper.kt` (add missing fields, fix timeWindow logic)
2. Add contextual error handling to all `fromMap` functions with a `require` helper
3. Add input validation and value classes to `FirestoreCollections.kt`
4. Harden Firestore security rules with schema validation
5. Extract `version` timestamp from mapper into a parameter
6. Fix comment inaccuracies
7. Add comprehensive missing tests
8. Archive stale `.claude/tasks/` files

## Relevant Files

Use these files to complete the task:

**Production code (modify):**

- `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapper.kt` -- Fix data loss bugs (#1, #2), add error handling (#4), extract version (#7), fix comments (#8)
- `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollections.kt` -- Add input validation (#5), add value classes (#10)
- `firestore.rules` -- Add schema validation (#3)

**Test code (modify):**

- `sync/src/test/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapperTest.kt` -- Add missing tests (#6, #11, #12, #14, #15), update existing tests for API changes
- `sync/src/test/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollectionsTest.kt` -- Update tests for value class return types, add validation tests

**Reference files (read only):**

- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Completion.kt` -- Has `updatedAt` field (line 36) missing from mapper
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RoutineHabit.kt` -- Has `createdAt`/`updatedAt` (lines 28-29) missing from mapper
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RoutineVariant.kt` -- Has `createdAt`/`updatedAt` (lines 26-27) missing from mapper
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RoutineExecution.kt` -- Has `createdAt`/`updatedAt` (lines 33-34) missing from mapper
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RecoverySession.kt` -- Has `createdAt`/`updatedAt` (lines 36-37) missing from mapper; also has `require(blockers.isNotEmpty())` init check (line 40)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` -- Reference for timeWindow fields
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Routine.kt` -- Reference (already correctly mapped)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/UserPreferences.kt` -- Reference (already correctly mapped)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/HabitPhase.kt` -- Uses UPPER_SNAKE_CASE objects (not PascalCase), relevant to comment fix #8
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/RoomTypeConverters.kt` -- Parallel Room serialization for reference
- `docs/08-erd.md` -- ERD reference for Firestore schema

### New Files

- `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMappingException.kt` -- Custom exception for mapper errors
- `.claude/tasks/archive/` -- Directory for archived task files (create if not exists)

## Implementation Phases

### Phase 1: Foundation -- Error Handling & Data Integrity

Fix the core data loss bugs and add the error handling infrastructure that subsequent work depends on.

### Phase 2: Hardening -- Validation, Security Rules & API Improvements

Add input validation to FirestoreCollections, harden Firestore security rules, extract version parameter, and fix comments.

### Phase 3: Test Coverage & Cleanup

Add all missing tests, update existing tests for API changes, and archive stale task files.

## Team Orchestration

- You operate as the team lead and orchestrate the team to execute the plan.
- You NEVER operate directly on the codebase. You use `Task` and `Task*` tools.
- Communication is paramount. You'll use the Task\* Tools to communicate with the team members.
- Take note of the session id of each team member.

### Team Members

- Specialist
    - Name: `mapper-builder`
    - Role: Fix FirestoreMapper data loss bugs, add error handling infrastructure, extract version parameter, and fix comments
    - Agent Type: backend-engineer
    - Resume: true

- Specialist
    - Name: `collections-builder`
    - Role: Add input validation and value classes to FirestoreCollections, update all downstream references
    - Agent Type: backend-engineer
    - Resume: true

- Specialist
    - Name: `rules-builder`
    - Role: Harden Firestore security rules with schema validation and granular permissions
    - Agent Type: security-auditor
    - Resume: true

- Specialist
    - Name: `test-builder`
    - Role: Add all missing tests, update existing tests for API changes from Phase 1-2
    - Agent Type: quality-engineer
    - Resume: true

- Quality Engineer (Validator)
    - Name: `validator`
    - Role: Validate completed work against acceptance criteria (read-only inspection mode)
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

### 1. Fix FirestoreMapper Data Loss & Error Handling

- **Task ID**: fix-mapper
- **Depends On**: none
- **Assigned To**: `mapper-builder`
- **Agent Type**: backend-engineer
- **Parallel**: true (can run alongside tasks 2 and 3)

**Critical #1 -- Add missing `createdAt`/`updatedAt` to 5 entities:**

In `FirestoreMapper.kt`, add the missing timestamp fields to both serialization and deserialization:

- `Completion.toFirestoreMap()`: Add `"updatedAt" to updatedAt.toTimestamp()`
- `completionFromMap()`: Add `updatedAt = (map["updatedAt"] as Timestamp).toInstant()`
- `RoutineHabit.toFirestoreMap()`: Add `"createdAt" to createdAt.toTimestamp()` and `"updatedAt" to updatedAt.toTimestamp()`
- `routineHabitFromMap()`: Add `createdAt = (map["createdAt"] as Timestamp).toInstant()` and `updatedAt = (map["updatedAt"] as Timestamp).toInstant()`
- `RoutineVariant.toFirestoreMap()`: Add `"createdAt" to createdAt.toTimestamp()` and `"updatedAt" to updatedAt.toTimestamp()`
- `routineVariantFromMap()`: Add `createdAt = (map["createdAt"] as Timestamp).toInstant()` and `updatedAt = (map["updatedAt"] as Timestamp).toInstant()`
- `RoutineExecution.toFirestoreMap()`: Add `"createdAt" to createdAt.toTimestamp()` and `"updatedAt" to updatedAt.toTimestamp()`
- `routineExecutionFromMap()`: Add `createdAt = (map["createdAt"] as Timestamp).toInstant()` and `updatedAt = (map["updatedAt"] as Timestamp).toInstant()`
- `RecoverySession.toFirestoreMap()`: Add `"createdAt" to createdAt.toTimestamp()` and `"updatedAt" to updatedAt.toTimestamp()`
- `recoverySessionFromMap()`: Add `createdAt = (map["createdAt"] as Timestamp).toInstant()` and `updatedAt = (map["updatedAt"] as Timestamp).toInstant()`

**Critical #2 -- Fix timeWindow serialization:**

Replace the current timeWindow logic in `Habit.toFirestoreMap()`:

```kotlin
// BEFORE (buggy):
"timeWindow" to if (timeWindowStart != null && timeWindowEnd != null) {
    mapOf("start" to timeWindowStart, "end" to timeWindowEnd)
} else {
    null
},

// AFTER (preserves partial data):
"timeWindow" to run {
    val tw = mutableMapOf<String, String>()
    timeWindowStart?.let { tw["start"] = it }
    timeWindowEnd?.let { tw["end"] = it }
    tw.ifEmpty { null }
},
```

**Important #4 -- Add contextual error handling to all `fromMap` functions:**

Create `FirestoreMappingException.kt`:

```kotlin
package com.getaltair.kairos.sync.firestore

class FirestoreMappingException(
    entityType: String,
    field: String,
    cause: Throwable? = null
) : RuntimeException(
    "Failed to map Firestore document to $entityType: invalid or missing field '$field'",
    cause
)
```

Add a private helper inside `FirestoreMapper`:

```kotlin
private inline fun <reified T> Map<String, Any?>.requireField(
    entityType: String,
    key: String
): T {
    val value = this[key]
        ?: throw FirestoreMappingException(entityType, key)
    return (value as? T)
        ?: throw FirestoreMappingException(
            entityType, key,
            ClassCastException("Expected ${T::class.simpleName}, got ${value::class.simpleName}")
        )
}
```

Replace all raw `as` casts in `fromMap` functions with `requireField` calls. For example:

```kotlin
// BEFORE:
name = map["name"] as String,
// AFTER:
name = map.requireField("Habit", "name"),
```

For nullable fields, continue using `as?` since that is already safe.

**Important #7 -- Extract `version` from `toFirestoreMap`:**

Add a `version` parameter with default to all `toFirestoreMap()` extensions:

```kotlin
fun Habit.toFirestoreMap(version: Long = System.currentTimeMillis()): Map<String, Any?> = mapOf(
    ...
    "version" to version,
)
```

Apply to all 8 entity `toFirestoreMap()` functions.

**Important #8 -- Fix comment inaccuracy:**

Update the block comment at the top of `FirestoreMapper.kt` (lines 35-39):

```kotlin
// BEFORE:
// Every sealed class in the domain uses PascalCase data-object names
// (e.g. AfterBehavior, NotFeelingWell). Firestore stores them as
// UPPER_SNAKE_CASE strings (e.g. AFTER_BEHAVIOR, NOT_FEELING_WELL) to
// match the document schemas in docs/08-erd.md.

// AFTER:
// Most sealed classes in the domain use PascalCase data-object names
// (e.g. AfterBehavior, NotFeelingWell), while HabitPhase uses
// UPPER_SNAKE_CASE object names (ONBOARD, FORMING, etc.). Firestore
// stores all variants as UPPER_SNAKE_CASE strings to match the document
// schemas in docs/08-erd.md.
```

Also update the FirestoreMapper KDoc to note the version field exception:

```kotlin
// BEFORE:
// Each function mirrors the field layout produced by the corresponding
// `toFirestoreMap()` extension above.

// AFTER:
// Each function mirrors the field layout produced by the corresponding
// `toFirestoreMap()` extension above, except for the `version` field
// (used only for conflict resolution, not stored on domain entities).
```

### 2. Harden FirestoreCollections with Validation & Value Classes

- **Task ID**: fix-collections
- **Depends On**: none
- **Assigned To**: `collections-builder`
- **Agent Type**: backend-engineer
- **Parallel**: true (can run alongside tasks 1 and 3)

**Important #5 -- Add input validation:**

Add `require` guards to all functions:

```kotlin
fun habits(userId: String): CollectionPath {
    require(userId.isNotBlank()) { "userId must not be blank" }
    return CollectionPath("users/$userId/habits")
}
```

Apply to every function in `FirestoreCollections`.

**Suggestion #10 -- Introduce value classes:**

Add at the top of `FirestoreCollections.kt`:

```kotlin
@JvmInline
value class CollectionPath(val value: String)

@JvmInline
value class DocumentPath(val value: String)
```

Update all collection-returning functions to return `CollectionPath` and all document-returning functions to return `DocumentPath`.

Update `FirestoreCollectionsTest.kt`:

- Change all `assertEquals("users/...", FirestoreCollections.xxx())` to `assertEquals(CollectionPath("users/..."), FirestoreCollections.xxx())` or `assertEquals(DocumentPath("users/..."), FirestoreCollections.xxx())`
- Alternatively, compare `.value` strings if that keeps tests simpler
- Add tests for blank input validation (expect `IllegalArgumentException`)

### 3. Harden Firestore Security Rules

- **Task ID**: fix-rules
- **Depends On**: none
- **Assigned To**: `rules-builder`
- **Agent Type**: security-auditor
- **Parallel**: true (can run alongside tasks 1 and 2)

**Critical #3 -- Add schema validation and granular permissions:**

Replace the blanket `read, write` with granular `create`, `update`, `delete` rules per collection. Add field validation for required fields and type checks. Reference the ERD (`docs/08-erd.md`) for the schema.

Minimum requirements:

- Split `write` into `create`, `update`, `delete` for each subcollection
- Add `request.resource.data.keys().hasAll([...])` for required fields on `create`
- Prevent overwriting immutable fields (`id`, `createdAt`) on `update`
- Add type validation for key fields (e.g., `data.name is string`)
- Keep the owner-only auth check (`request.auth.uid == userId`)

Example structure:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }

    match /users/{userId} {
      allow read: if isOwner(userId);
      allow create: if isOwner(userId);
      allow update: if isOwner(userId);

      match /habits/{habitId} {
        allow read: if isOwner(userId);
        allow create: if isOwner(userId)
          && request.resource.data.keys().hasAll(['id', 'name', 'anchorBehavior', 'anchorType', 'category', 'frequency', 'status', 'phase', 'createdAt', 'updatedAt'])
          && request.resource.data.name is string
          && request.resource.data.id is string;
        allow update: if isOwner(userId)
          && !request.resource.data.diff(resource.data).affectedKeys().hasAny(['id', 'createdAt']);
        allow delete: if isOwner(userId);
      }

      // ... similar for completions, routines, etc.
    }
  }
}
```

Cover all subcollections: habits, completions, routines, routines/{id}/habits, routines/{id}/variants, routine_executions, recovery_sessions, preferences, deletions.

### 4. Add Missing Tests & Update Existing Tests

- **Task ID**: add-tests
- **Depends On**: fix-mapper, fix-collections
- **Assigned To**: `test-builder`
- **Agent Type**: quality-engineer
- **Parallel**: false (must wait for mapper and collections changes)

**Important #6 -- Add unknown-tag tests for remaining 11 `fromTag` functions:**

Add `@Test(expected = IllegalArgumentException::class)` tests for each:

```kotlin
@Test(expected = IllegalArgumentException::class)
fun `unknown HabitCategory tag throws`() { habitCategoryFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown HabitFrequency tag throws`() { habitFrequencyFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown HabitPhase tag throws`() { habitPhaseFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown HabitStatus tag throws`() { habitStatusFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown SkipReason tag throws`() { skipReasonFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown RecoveryType tag throws`() { recoveryTypeFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown SessionStatus tag throws`() { sessionStatusFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown RecoveryAction tag throws`() { recoveryActionFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown Blocker tag throws`() { blockerFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown RoutineStatus tag throws`() { routineStatusFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown ExecutionStatus tag throws`() { executionStatusFromTag("INVALID") }

@Test(expected = IllegalArgumentException::class)
fun `unknown Theme tag throws`() { themeFromTag("INVALID") }
```

**Suggestion #11 -- Test Habit with `subtasks = emptyList()`:**

```kotlin
@Test
fun `Habit with empty subtasks list serializes to empty list and deserializes to null`() {
    val habit = Habit(
        id = UUID.randomUUID(),
        name = "Test",
        anchorBehavior = "After X",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        subtasks = emptyList(),
        createdAt = now,
        updatedAt = now,
    )
    val map = habit.toFirestoreMap()
    assertEquals(emptyList<String>(), map["subtasks"])
    val restored = FirestoreMapper.habitFromMap(map)
    assertNull(restored.subtasks) // empty list normalizes to null
}
```

**Suggestion #12 -- Add map-structure assertion tests:**

Add at least one test per entity that asserts specific map keys and value types (not just round-trip). Example:

```kotlin
@Test
fun `Habit toFirestoreMap produces correct key set`() {
    val habit = createTestHabit()
    val map = habit.toFirestoreMap()
    val expectedKeys = setOf(
        "id", "name", "description", "icon", "color", "anchorBehavior",
        "anchorType", "timeWindow", "category", "frequency", "activeDays",
        "estimatedSeconds", "microVersion", "allowPartial", "subtasks",
        "lapseThresholdDays", "relapseThresholdDays", "phase", "status",
        "createdAt", "updatedAt", "pausedAt", "archivedAt", "version"
    )
    assertEquals(expectedKeys, map.keys)
    assertEquals("AFTER_BEHAVIOR", map["anchorType"]) // verify format
}
```

**Suggestion #14 -- Add reflection-based sealed subclass coverage test:**

```kotlin
@Test
fun `all AnchorType subclasses have tags`() {
    AnchorType::class.sealedSubclasses.forEach { subclass ->
        val instance = subclass.objectInstance!!
        val tag = (instance as AnchorType).toTag()
        assertEquals(instance, anchorTypeFromTag(tag))
    }
}
// Repeat for all 13 sealed classes
```

**Suggestion #15 -- Test RecoverySession boundary case:**

Note: `RecoverySession` has `require(blockers.isNotEmpty())` in its `init`, so empty blockers is invalid at the domain level. Instead, test that `fromMap` with empty blockers throws a meaningful error (now wrapped in `FirestoreMappingException` or propagating the `IllegalArgumentException` from the entity init).

**Update existing tests for API changes:**

- Update all `toFirestoreMap()` test calls if they need to pass `version` parameter (or rely on default)
- Update round-trip tests for the 5 entities with newly added `createdAt`/`updatedAt` to assert those fields
- Update `FirestoreCollectionsTest` for value class return types
- Add tests for blank-input validation in `FirestoreCollections`

### 5. Archive Stale Task Files

- **Task ID**: archive-tasks
- **Depends On**: none
- **Assigned To**: `collections-builder` (lightweight, piggyback on existing agent)
- **Agent Type**: backend-engineer
- **Parallel**: true

**Important #9 -- Move committed `.claude/tasks/` planning files to archive:**

Create `.claude/tasks/archive/` directory if it does not exist. Move these 7 files from the PR diff:

- `address-all-pr-review-findings.md`
- `address-open-gh-issues.md`
- `address-pr-review-findings.md`
- `address-step7-pr-review-findings.md`
- `step6-create-habit-flow.md`
- `step7-habit-management.md`
- `step8-firebase-project-setup.md`

Also move these older files already on the branch:

- `domain-models-enumerations.md`
- `pr2-fix-implementation-plan.md`
- `pr-review-fixes.md`
- `step-3-room-database-daos.md`
- `step-4-repo-use-cases.md`
- `step5-today-screen.md`

### 6. Final Validation

- **Task ID**: validate-all
- **Depends On**: fix-mapper, fix-collections, fix-rules, add-tests, archive-tasks
- **Assigned To**: `validator`
- **Agent Type**: quality-engineer
- **Parallel**: false

Validate all changes against acceptance criteria:

- Read all modified files and verify each issue is addressed
- Run `./gradlew :sync:test` to confirm all tests pass
- Run `./gradlew build` to confirm full project compiles
- Run `./gradlew ktlintFormat` to ensure code formatting is clean
- Verify no `.claude/tasks/*.md` files remain outside archive (except the current plan)
- Verify `firestore.rules` has per-collection validation
- Verify `FirestoreCollections` returns value classes and validates inputs
- Verify all 8 entity `toFirestoreMap()`/`fromMap()` pairs include `createdAt`/`updatedAt`
- Verify all `fromMap` functions use contextual error handling
- Verify the `version` parameter is extractable (has default value)
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

1. All 5 entities serialize and deserialize `createdAt`/`updatedAt` correctly
2. `Habit.toFirestoreMap()` preserves partial timeWindow data (one of start/end set)
3. Firestore security rules have per-collection `create`/`update`/`delete` with field validation
4. All `fromMap` functions throw `FirestoreMappingException` with entity type and field name on bad data
5. `FirestoreCollections` throws `IllegalArgumentException` on blank inputs
6. `FirestoreCollections` returns `CollectionPath`/`DocumentPath` value classes
7. All 13 `fromTag` functions have unknown-tag exception tests
8. `toFirestoreMap()` accepts a `version` parameter (with `System.currentTimeMillis()` default)
9. Block comment accurately describes HabitPhase naming convention
10. FirestoreMapper KDoc notes version field exception
11. Tests exist for empty subtasks, map structure assertions, and reflection-based sealed subclass coverage
12. All stale `.claude/tasks/` files moved to archive
13. `./gradlew :sync:test` passes
14. `./gradlew build` compiles without errors
15. `./gradlew ktlintFormat` produces no changes

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew :sync:test` -- Run sync module unit tests (all new and existing tests pass)
- `./gradlew build` -- Full project build with no errors
- `./gradlew ktlintFormat` -- Verify code formatting compliance
- `ls .claude/tasks/*.md` -- Only current plan file should remain (plus archive directory)
- `grep -c "FirestoreMappingException" sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapper.kt` -- Verify error handling is in place
- `grep -c "require(" sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollections.kt` -- Verify input validation present
- `grep -c "CollectionPath\|DocumentPath" sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollections.kt` -- Verify value classes used
- `grep -c "createdAt\|updatedAt" sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapper.kt` -- Verify timestamps added

## Notes

1. The `RecoverySession` entity has `require(blockers.isNotEmpty())` in its `init` block, so empty blockers list is invalid at the domain level. The mapper does not need to handle empty blockers specially -- the entity constructor will throw, which will now be wrapped in contextual error handling.

2. Suggestion #13 (unify Room and Firestore tag formats) is deferred -- it requires a Room database migration and is too large for this fix PR. It should be tracked as a separate issue.

3. The `SyncStatus` enum exists in domain but has no Firestore serialization yet. It is not part of this PR's scope.

4. When updating `FirestoreCollectionsTest.kt` for value classes, prefer comparing `.value` strings to keep assertions readable: `assertEquals("users/abc/habits", FirestoreCollections.habits("abc").value)`.

5. The `@Suppress("UNCHECKED_CAST")` annotations in `fromMap` functions for list/map casts are acceptable since the `requireField` helper handles the safety check. Keep the suppressions but move them to a single function-level annotation where possible.
