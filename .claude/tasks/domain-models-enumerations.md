# Plan: Domain Models + Enumerations

## Task Description

Implement all core domain models, enumerations, value objects, and repository interfaces for the Kairos habit tracking app. This is STEP 2 of the implementation plan, creating the pure Kotlin foundation in the `domain` module that will be used by the data layer (Room) and core layer (use cases). The domain module must have zero Android framework dependencies — pure Kotlin with sealed classes, enums, and data classes.

## Objective

When this plan is complete:

- All entity data classes defined in `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/`
- All enumeration sealed classes defined in `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/`
- All value objects defined in `domain/src/main/kotlin/com/getaltair/kairos/domain/model/`
- Repository interfaces defined in `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/`
- State machine validation methods implemented for `HabitPhase` and `HabitStatus`
- Domain module has zero Android framework dependencies (verified by build)
- Unit tests cover all invariants that can be validated at model level

## Problem Statement

The domain module currently exists as an empty Kotlin-only module. Without proper domain models, the data layer (Room) has no schema to map, and the core layer (use cases) has no interfaces to implement. Domain models must be pure Kotlin to enable future Kotlin Multiplatform extraction for the Pi dashboard and to keep business logic independent of Android framework.

## Solution Approach

1. **Pure Kotlin First**: Define all entities as `data class` with immutable properties. Use sealed classes for enums to enable exhaustiveness checking.
2. **Type Safety**: Use `java.time.*` types (`LocalDate`, `LocalTime`, `Instant`) for temporal fields rather than Long wrappers.
3. **State Machine Validation**: Implement `canTransitionTo()` methods directly on sealed classes to enforce valid state transitions.
4. **Repository Pattern**: Define interfaces only in domain layer; implementations will live in `data` module.
5. **Validation at Model Level**: Implement invariants that can be checked without external dependencies (H-1 through H-6, C-1 through C-6).

## Relevant Files

### New Files to Create

```
domain/src/main/kotlin/com/getaltair/kairos/domain/
  entity/
    Habit.kt
    Completion.kt
    Routine.kt
    RoutineHabit.kt
    RoutineVariant.kt
    RoutineExecution.kt
    RecoverySession.kt
    UserPreferences.kt
  enums/
    AnchorType.kt
    HabitCategory.kt
    HabitFrequency.kt
    HabitPhase.kt
    HabitStatus.kt
    CompletionType.kt
    SkipReason.kt
    RecoveryType.kt
    SessionStatus.kt
    RecoveryAction.kt
    Blocker.kt
    SyncStatus.kt
    ExecutionStatus.kt
    RoutineStatus.kt
    Theme.kt
  model/
    TimeWindow.kt
    GeoPoint.kt
    Version.kt
    DateRange.kt
  repository/
    HabitRepository.kt
    CompletionRepository.kt
    RoutineRepository.kt
    RecoveryRepository.kt
    PreferencesRepository.kt
```

### Existing Files to Reference

| Path                        | Purpose                                                    |
| --------------------------- | ---------------------------------------------------------- |
| `domain/build.gradle.kts`   | Current module uses `java-library` plugin only             |
| `gradle/libs.versions.toml` | Version catalog — verify no Android deps needed for domain |

## Implementation Phases

### Phase 1: Core Enumerations

Implement all sealed class enumerations first. These have no dependencies and establish the type system for the domain.

1. `AnchorType` — AFTER_BEHAVIOR, BEFORE_BEHAVIOR, AT_LOCATION, AT_TIME
2. `HabitCategory` — MORNING, AFTERNOON, EVENING, ANYTIME, DEPARTURE
3. `HabitFrequency` — DAILY, WEEKDAYS, WEEKENDS, CUSTOM
4. `HabitStatus` — ACTIVE, PAUSED, ARCHIVED
5. `Theme` — SYSTEM, LIGHT, DARK
6. `CompletionType` — FULL, PARTIAL, SKIPPED, MISSED
7. `SkipReason` — TOO_TIRED, NO_TIME, NOT_FEELING_WELL, TRAVELING, TOOK_DAY_OFF, OTHER
8. `RecoveryType` — LAPSE, RELAPSE
9. `SessionStatus` — PENDING, COMPLETED, ABANDONED
10. `RecoveryAction` — RESUME, SIMPLIFY, PAUSE, ARCHIVE, FRESH_START
11. `Blocker` — All 10 options from `02-prd-recovery.md`
12. `SyncStatus` — LOCAL_ONLY, SYNCED, PENDING_SYNC, PENDING_DELETE, CONFLICT
13. `ExecutionStatus` — NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, ABANDONED
14. `RoutineStatus` — ACTIVE, PAUSED, ARCHIVED

### Phase 2: State Machine Enums

Implement `HabitPhase` with state machine validation. This is the most complex enum due to transition logic.

1. Define sealed class `HabitPhase` with objects: ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED
2. Implement `canTransitionTo(target: HabitPhase): Boolean` method
3. Unit test all valid and invalid transitions from `09-state-machines.md`

### Phase 3: Value Objects

Implement immutable value objects that represent domain concepts without identity.

1. `TimeWindow` — start: LocalTime, end: LocalTime, contains() method
2. `GeoPoint` — latitude: Double, longitude: Double, radiusMeters: Int
3. `Version` — value: Long, increment(), isNewerThan() methods
4. `DateRange` — start: LocalDate, end: LocalDate, contains(), dayCount() methods

### Phase 4: Entity Data Classes

Implement all core entities as `data class` with `val` properties.

1. `Habit` — All fields from `05-domain-model.md` §Habit, with `java.time.*` types for timestamps
2. `Completion` — All fields from `05-domain-model.md` §Completion
3. `Routine` — All fields from `05-domain-model.md` §Routine
4. `RoutineHabit` — All fields from `05-domain-model.md` §RoutineHabit
5. `RoutineVariant` — All fields from `05-domain-model.md` §RoutineVariant
6. `RoutineExecution` — All fields from `05-domain-model.md` §RoutineExecution
7. `RecoverySession` — All fields from `05-domain-model.md` §RecoverySession (blockers as List<Blocker>)
8. `UserPreferences` — All fields from `05-domain-model.md` §UserPreferences

### Phase 5: Repository Interfaces

Define interfaces only — no implementations. These will be implemented in the `data` module during STEP 3.

1. `HabitRepository` — CRUD methods, query methods for active habits, habits by date/status/category
2. `CompletionRepository` — CRUD, query by habit/date, date range methods
3. `RoutineRepository` — CRUD, get active routines, get routine with habits
4. `RecoveryRepository` — Get pending sessions, insert, update
5. `PreferencesRepository` — Get and update preferences

## Team Orchestration

- You operate as team lead and orchestrate team to execute the plan.
- You're responsible for deploying the right team members with the right context to execute the plan.
- IMPORTANT: You NEVER operate directly on the codebase. You use `Task` and `Task*` tools to deploy team members to building, validating, testing, deploying, and other tasks.
- Your role is to validate all work is going well and make sure the team is on track to complete the plan.
- You'll orchestrate this by using Task\* Tools to manage coordination between team members.
- Communication is paramount. You'll use Task\* Tools to communicate with team members and ensure they're on track to complete the plan.

### Team Members

- Specialist
    - Name: domain-model-builder
    - Role: Domain model and enum implementation
    - Agent Type: general-purpose
    - Resume: true. This lets agent continue working with the same context for sequential phases.
- Quality Engineer (Validator)
    - Name: domain-validator
    - Role: Validate completed domain models against spec and invariants
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

- IMPORTANT: Execute every step in order, top to bottom. Each task maps directly to a `TaskCreate` call.
- Before you start, run `TaskCreate` to create the initial task list that all team members can see and execute.

### 1. Implement Core Enumerations

- **Task ID**: enums-core
- **Depends On**: none
- **Assigned To**: domain-model-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create all sealed class enumerations in `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/`
- Implement these enums: AnchorType, HabitCategory, HabitFrequency, HabitStatus, Theme, CompletionType, SkipReason, RecoveryType, SessionStatus, RecoveryAction, Blocker, SyncStatus, ExecutionStatus, RoutineStatus
- Follow sealed class pattern: `sealed class EnumName { object VALUE1 : EnumName() ... }`
- Each enum should have `displayName` property for UI (can be derived from enum name or customized)

### 2. Implement HabitPhase with State Machine Validation

- **Task ID**: habit-phase-state-machine
- **Depends On**: enums-core
- **Assigned To**: domain-model-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `HabitPhase.kt` with objects: ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED
- Implement `canTransitionTo(target: HabitPhase): Boolean` method with logic from `09-state-machines.md`
- Valid transitions:
    - ONBOARD → FORMING
    - FORMING → MAINTAINING or LAPSED
    - MAINTAINING → LAPSED
    - LAPSED → FORMING or RELAPSED
    - RELAPSED → FORMING
- Add companion object with all valid transitions map for easy reference

### 3. Implement Value Objects

- **Task ID**: value-objects
- **Depends On**: enums-core
- **Assigned To**: domain-model-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `TimeWindow.kt` with start: LocalTime, end: LocalTime, contains() method
- Create `GeoPoint.kt` with lat: Double, lon: Double, radiusMeters: Int
- Create `Version.kt` with value: Long, increment(), isNewerThan() methods
- Create `DateRange.kt` with start: LocalDate, end: LocalDate, contains(), dayCount() methods
- Ensure all properties are `val` (immutable)

### 4. Implement Entity Data Classes

- **Task ID**: entities-core
- **Depends On**: enums-core, value-objects
- **Assigned To**: domain-model-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `Habit.kt` entity with all fields from spec
- Create `Completion.kt` entity with all fields from spec
- Create `Routine.kt` entity with all fields from spec
- Create `RoutineHabit.kt` entity with all fields from spec
- Create `RoutineVariant.kt` entity with all fields from spec
- Create `RoutineExecution.kt` entity with all fields from spec
- Create `RecoverySession.kt` entity with all fields from spec (blockers as List<Blocker>)
- Create `UserPreferences.kt` entity with all fields from spec
- Use `java.time.Instant` for timestamps, `java.time.LocalDate` for dates, `java.time.LocalTime` for times
- Add default values to data class constructor where specified in docs

### 5. Implement Repository Interfaces

- **Task ID**: repository-interfaces
- **Depends On**: entities-core
- **Assigned To**: domain-model-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `HabitRepository.kt` interface with methods: getById, getActiveHabits, getHabitsForDate, insert, update, delete, getByStatus, getByCategory, getLapsedHabits
- Create `CompletionRepository.kt` interface with methods: getForHabitOnDate, getForDate, getForDateRange, insert, update, delete, getForHabitInDateRange
- Create `RoutineRepository.kt` interface with methods: getById, getActiveRoutines, getRoutineWithHabits, insert, update, delete
- Create `RecoveryRepository.kt` interface with methods: getPendingForHabit, insert, update, delete
- Create `PreferencesRepository.kt` interface with methods: get, update
- All methods should return `Result<T>` type (not throwing)

### 6. Write Unit Tests for State Machine Validation

- **Task ID**: state-machine-tests
- **Depends On**: habit-phase-state-machine
- **Assigned To**: domain-model-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create test file for `HabitPhase.canTransitionTo()`
- Test all valid transitions from `09-state-machines.md` should return true
- Test all invalid transitions should return false
- Test edge cases (same state, null state)

### 7. Validate Domain Models

- **Task ID**: validate-all
- **Depends On**: enums-core, habit-phase-state-machine, value-objects, entities-core, repository-interfaces, state-machine-tests
- **Assigned To**: domain-validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Verify all enums exist and are sealed classes
- Verify `HabitPhase.canTransitionTo()` passes all valid/invalid transitions from spec
- Verify all entity data classes have correct fields and types
- Verify repository interfaces are defined (no implementations needed yet)
- Verify domain module has zero Android framework dependencies
- Verify unit tests pass

## Acceptance Criteria

- [ ] All 14 enumerations defined as sealed classes in correct package structure
- [ ] `HabitPhase.canTransitionTo()` correctly validates all transitions from `09-state-machines.md`
- [ ] All 4 value objects implemented as immutable data classes
- [ ] All 8 entity data classes implemented with correct fields and types
- [ ] All 5 repository interfaces defined with correct method signatures
- [ ] Domain module builds successfully with no Android framework dependencies
- [ ] Unit tests for `HabitPhase` state machine pass all test cases
- [ ] All code follows Kotlin coding conventions (4-space indentation, expression bodies, `val` over `var`)

## Validation Commands

```bash
# Verify domain module compiles with zero Android dependencies
./gradlew :domain:build

# Verify domain module has no Android dependencies in dependency tree
./gradlew :domain:dependencies --configuration runtime

# Run all tests in domain module
./gradlew :domain:test

# Run code style checks
./gradlew ktlintCheck
./gradlew detekt
```

## Notes

1. **Package Structure**: Follow `com.getaltair.kairos.domain.entity`, `.enums`, `.model`, `.repository` structure consistently.
2. **Temporal Types**: Always use `java.time.*` types (`Instant`, `LocalDate`, `LocalTime`) — do not use `Long` timestamps at domain level.
3. **Immutability**: All entity properties should be `val`. Use `copy()` for updates.
4. **Result Type**: Repository methods should return `Result<T>` wrapper (not implemented yet, but interfaces should reflect this).
5. **JSON Fields**: `blockers` in RecoverySession, `activeDays` in Habit, `subtasks` in Habit, `variantIds` in RoutineHabit — these will be handled by Room type converters in STEP 3.
6. **Default Values**: Entity constructors should have default values where specified (e.g., `estimatedSeconds = 300`, `allowPartialCompletion = true`, `lapseThresholdDays = 3`, `relapseThresholdDays = 7`).
7. **Validation**: Invariants that require external data (like foreign key validation) will be enforced in the data layer, not domain layer.
