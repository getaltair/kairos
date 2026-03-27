# Plan: Repository Layer + Core Use Cases (Step 4)

## Task Description

Implement the repository layer and core use cases for the Kairos habit tracking app. This is Step 4 of the implementation plan, building on top of the domain models (Step 2) and Room database/DAOs (Step 3). Repository implementations wrap Room DAOs with domain-layer mapping, while use cases orchestrate business logic and enforce invariants from `docs/06-invariants.md`. At this stage, repositories only talk to Room -- Firestore sync is added in Step 9.

## Objective

When this plan is complete:

- Three repository implementations (`HabitRepositoryImpl`, `CompletionRepositoryImpl`, `PreferencesRepositoryImpl`) connect domain interfaces to Room DAOs
- Six P0 use cases provide validated business operations for the Today screen and habit management
- Validators enforce all habit and completion invariants from the spec
- Composite models (`HabitWithStatus`, `WeeklyStats`) support the Today screen data needs
- All components are wired into Koin DI and accessible from feature modules
- Unit tests cover all validation rules; integration tests cover repository operations

## Problem Statement

Steps 1-3 established the domain models, Room database, and DAOs, but no bridge exists between the domain layer's repository interfaces and the data layer's DAOs. Feature modules (Step 5+) need validated business operations -- not raw database access. Without this layer, the Today screen cannot load habits, completions cannot be recorded with invariant enforcement, and no frequency filtering or stats aggregation exists.

## Solution Approach

1. **Fix KairosDatabase** -- The current `KairosDatabase` class is empty (no entity annotations, no DAO accessors). This must be corrected before repositories can obtain DAOs.
2. **Domain models + Validators** -- Add composite models (`HabitWithStatus`, `WeeklyStats`) to the domain layer and create `HabitValidator`/`CompletionValidator` classes that enforce invariants H-1 through H-6 and C-2 through C-5.
3. **Repository implementations** -- Create `HabitRepositoryImpl`, `CompletionRepositoryImpl`, and `PreferencesRepositoryImpl` in the data module. Each wraps the corresponding DAO, uses existing entity mappers, and returns `Result<T>`.
4. **Use cases** -- Create six P0 use cases in the core module that orchestrate repositories and validators: `CreateHabitUseCase`, `GetTodayHabitsUseCase`, `CompleteHabitUseCase`, `SkipHabitUseCase`, `UndoCompletionUseCase`, `GetWeeklyStatsUseCase`.
5. **DI wiring** -- Expand the Koin `dataModule` to provide DAOs and repository bindings, create a `useCaseModule` in core, and register it in `KairosApp`.
6. **Tests** -- Unit tests for validators and use cases (mocked repos), integration tests for repository implementations (in-memory Room DB).

## Relevant Files

### Existing Files (Read/Modify)

- `data/src/main/kotlin/com/getaltair/kairos/data/database/KairosDatabase.kt` -- Must add entity annotations and abstract DAO accessor methods
- `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt` -- Must expand to provide DAOs and repository bindings via Koin
- `core/build.gradle.kts` -- Must add dependency on `:domain` module and Koin libraries
- `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt` -- Must add `useCaseModule` to Koin startup

### Existing Files (Reference Only)

- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/HabitRepository.kt` -- Interface to implement (9 methods)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/CompletionRepository.kt` -- Interface to implement (8 methods)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/repository/PreferencesRepository.kt` -- Interface to implement (2 methods)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/common/Result.kt` -- Result wrapper used by all repos/use cases
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` -- Domain entity with `isDueToday()` and frequency logic
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Completion.kt` -- Domain entity with init block validations
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/UserPreferences.kt` -- Domain entity (singleton per user)
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/HabitFrequency.kt` -- Sealed class: Daily, Weekdays, Weekends, Custom
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/CompletionType.kt` -- Sealed class: Full, Partial, Skipped, Missed
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt` -- Room DAO with `getTodayHabitsWithCompletions()` Flow, `TodayHabitWithCompletion` wrapper
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/CompletionDao.kt` -- Room DAO with date-string queries, REPLACE conflict strategy
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/UserPreferencesDao.kt` -- Room DAO with singleton pattern
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/HabitEntityMapper.kt` -- Bidirectional `object` mapper using converters
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/CompletionEntityMapper.kt` -- Bidirectional `object` mapper
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/UserPreferencesEntityMapper.kt` -- Bidirectional `object` mapper
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/*.kt` -- All 9 Room entity classes
- `docs/06-invariants.md` -- Full invariant spec (H-1 through H-6, C-1 through C-6)
- `docs/implementation-plan.md` -- Step 4 requirements (lines 363-428)

### New Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitWithStatus.kt` -- Composite model for Today screen
- `domain/src/main/kotlin/com/getaltair/kairos/domain/model/WeeklyStats.kt` -- Stats aggregation model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/HabitValidator.kt` -- Enforces H-1, H-2, H-4, H-5, H-6
- `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/CompletionValidator.kt` -- Enforces C-2, C-4, C-5
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt` -- Wraps HabitDao
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/CompletionRepositoryImpl.kt` -- Wraps CompletionDao
- `data/src/main/kotlin/com/getaltair/kairos/data/repository/PreferencesRepositoryImpl.kt` -- Wraps UserPreferencesDao
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/CreateHabitUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/GetTodayHabitsUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/CompleteHabitUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/SkipHabitUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/UndoCompletionUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/usecase/GetWeeklyStatsUseCase.kt`
- `core/src/main/kotlin/com/getaltair/kairos/core/di/UseCaseModule.kt` -- Koin module for use cases
- `domain/src/test/kotlin/com/getaltair/kairos/domain/validator/HabitValidatorTest.kt`
- `domain/src/test/kotlin/com/getaltair/kairos/domain/validator/CompletionValidatorTest.kt`
- `core/src/test/kotlin/com/getaltair/kairos/core/usecase/CreateHabitUseCaseTest.kt`
- `core/src/test/kotlin/com/getaltair/kairos/core/usecase/CompleteHabitUseCaseTest.kt`
- `core/src/test/kotlin/com/getaltair/kairos/core/usecase/GetTodayHabitsUseCaseTest.kt`
- `core/src/test/kotlin/com/getaltair/kairos/core/usecase/UndoCompletionUseCaseTest.kt`

## Implementation Phases

### Phase 1: Foundation

Fix `KairosDatabase` to properly declare entities and DAO accessors. Add the composite domain models (`HabitWithStatus`, `WeeklyStats`) and validators (`HabitValidator`, `CompletionValidator`) to the domain module. Update `core/build.gradle.kts` to depend on `:domain` and Koin.

**Critical gap to fix**: `KairosDatabase` currently has no `@Database(entities = [...])` annotation and no abstract DAO methods. Without this, no DAO can be obtained at runtime. This is a Step 3 gap that must be resolved first.

### Phase 2: Core Implementation

Build repository implementations in the data module. Each wraps a DAO, uses the existing entity mapper objects, and returns `Result<T>`. Build use cases in the core module -- each takes repository interfaces via constructor injection and uses validators for input validation.

Key implementation details:

- **HabitRepositoryImpl**: `getHabitsForDate()` must filter active habits by frequency and day-of-week using `Habit.isDueToday()` logic or DAO-level query
- **CompletionRepositoryImpl**: `insert()` uses REPLACE conflict strategy at the DAO level, but the repository should check for existing completions first and return an appropriate error if the use case didn't already validate
- **PreferencesRepositoryImpl**: Wraps `UserPreferencesDao`. `get()` creates default preferences if none exist.
- **CompleteHabitUseCase**: Validates completion type, partial percent range (C-2), no future dates (C-4), limited backdating (C-5), one-per-day (C-3), then delegates to `CompletionRepository.insert()`
- **UndoCompletionUseCase**: Accepts `completionId` + `completedAt` timestamp, validates within 30-second undo window, then calls `CompletionRepository.delete()`
- **GetTodayHabitsUseCase**: Combines `HabitRepository.getHabitsForDate(today)` with `CompletionRepository.getForDate(today)` and calculates week completion rates to produce `List<HabitWithStatus>`
- **GetWeeklyStatsUseCase**: Queries completions for the past 7 days and aggregates into `WeeklyStats`

### Phase 3: Integration & Polish

Wire everything into Koin DI. Expand `dataModule` to provide DAO singletons from `KairosDatabase` and bind repository interfaces to implementations. Create `useCaseModule` for use case factories. Register the new module in `KairosApp`. Write unit tests for validators and use cases, and integration tests for repositories.

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
    - Name: foundation-builder
    - Role: Fix KairosDatabase, create domain models (HabitWithStatus, WeeklyStats), create validators (HabitValidator, CompletionValidator), update core/build.gradle.kts
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: repo-builder
    - Role: Implement HabitRepositoryImpl, CompletionRepositoryImpl, PreferencesRepositoryImpl. Expand dataModule to provide DAOs and repository bindings.
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: usecase-builder
    - Role: Implement all 6 use cases (CreateHabit, GetTodayHabits, CompleteHabit, SkipHabit, UndoCompletion, GetWeeklyStats). Create useCaseModule. Update KairosApp.kt.
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: test-builder
    - Role: Write unit tests for validators (HabitValidatorTest, CompletionValidatorTest) and use cases (CreateHabitUseCaseTest, CompleteHabitUseCaseTest, GetTodayHabitsUseCaseTest, UndoCompletionUseCaseTest).
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

### 1. Fix KairosDatabase and Create Domain Foundation

- **Task ID**: fix-database-and-domain-models
- **Depends On**: none
- **Assigned To**: foundation-builder
- **Agent Type**: general-purpose
- **Parallel**: false (must complete before tasks 2 and 3)
- Fix `data/src/main/kotlin/com/getaltair/kairos/data/database/KairosDatabase.kt`:
    - Add `@Database(entities = [HabitEntity::class, CompletionEntity::class, RoutineEntity::class, RoutineHabitEntity::class, RoutineVariantEntity::class, RoutineExecutionEntity::class, RecoverySessionEntity::class, HabitNotificationEntity::class, UserPreferencesEntity::class], version = 1, exportSchema = true)` annotation
    - Add `@TypeConverters(RoomTypeConverters::class)` annotation
    - Add abstract DAO accessor methods: `abstract fun habitDao(): HabitDao`, `abstract fun completionDao(): CompletionDao`, `abstract fun userPreferencesDao(): UserPreferencesDao`, `abstract fun routineDao(): RoutineDao`, `abstract fun routineHabitDao(): RoutineHabitDao`, `abstract fun routineVariantDao(): RoutineVariantDao`, `abstract fun routineExecutionDao(): RoutineExecutionDao`, `abstract fun recoverySessionDao(): RecoverySessionDao`, `abstract fun habitNotificationDao(): HabitNotificationDao`
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/model/HabitWithStatus.kt`:
    - `data class HabitWithStatus(val habit: Habit, val todayCompletion: Completion?, val weekCompletionRate: Float)`
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/model/WeeklyStats.kt`:
    - `data class WeeklyStats(val habitId: UUID?, val totalDays: Int, val completedCount: Int, val partialCount: Int, val skippedCount: Int, val missedCount: Int, val completionRate: Float)`
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/HabitValidator.kt`:
    - `object HabitValidator` with `fun validate(habit: Habit): Result<Unit>` enforcing: H-1 (anchorBehavior not blank), H-2 (valid category enum -- always true since it's a sealed class, but check it's not null context), H-4 (allowPartialCompletion == true), H-5 (relapseThresholdDays > lapseThresholdDays), H-6 (timestamp ordering: createdAt <= updatedAt, pausedAt/archivedAt >= createdAt if set)
    - Return `Result.Error(message)` for first failing rule, `Result.Success(Unit)` if all pass
- Create `domain/src/main/kotlin/com/getaltair/kairos/domain/validator/CompletionValidator.kt`:
    - `object CompletionValidator` with `fun validate(completion: Completion, today: LocalDate = LocalDate.now()): Result<Unit>` enforcing: C-2 (if PARTIAL, partialPercent in 1..99; if not PARTIAL, partialPercent == null), C-4 (date <= today), C-5 (date >= today - 7 days)
    - The Completion entity already has some init-block checks -- the validator adds date-based rules the entity can't enforce alone
- Update `core/build.gradle.kts`:
    - Add `implementation(project(":domain"))` to dependencies
    - Add Koin dependencies: `implementation(platform(libs.koin.bom))`, `implementation(libs.koin.core)`

### 2. Implement Repository Layer

- **Task ID**: implement-repositories
- **Depends On**: fix-database-and-domain-models
- **Assigned To**: repo-builder
- **Agent Type**: general-purpose
- **Parallel**: true (can run alongside task 3)
- Create `data/src/main/kotlin/com/getaltair/kairos/data/repository/HabitRepositoryImpl.kt`:
    - Implements `HabitRepository` interface
    - Constructor takes `HabitDao`
    - Uses `HabitEntityMapper` object for all conversions
    - `getById()`: calls `habitDao.getById(id)`, maps to domain, wraps in Result
    - `getActiveHabits()`: calls `habitDao.getActiveHabits()`, maps list
    - `getHabitsForDate(date)`: gets active habits, then filters by frequency using domain logic (Daily = always, Weekdays = Mon-Fri check, Weekends = Sat-Sun check, Custom = check activeDays contains day-of-week)
    - `getByStatus()`: uses existing DAO methods
    - `getByCategory()`: calls `habitDao.getByCategory()`
    - `getLapsedHabits()`: calls `habitDao.getLapsedHabits()` with a default threshold
    - `insert()`: maps to entity, calls `habitDao.insert()`, returns domain object
    - `update()`: maps to entity fields, calls `habitDao.update()` with all individual parameters (matching the DAO's parameter-based update signature), returns updated domain object
    - `delete()`: calls `habitDao.delete(id)`
    - Wrap all operations in try-catch returning `Result.Error` on exception
- Create `data/src/main/kotlin/com/getaltair/kairos/data/repository/CompletionRepositoryImpl.kt`:
    - Implements `CompletionRepository` interface
    - Constructor takes `CompletionDao`
    - Uses `CompletionEntityMapper` object
    - All date parameters must be converted to ISO string format (`date.toString()`) for DAO queries
    - `getForHabitOnDate()`: calls `completionDao.getForHabitOnDate(habitId, date.toString())`
    - `getForDate()`: calls `completionDao.getForDate(date.toString())`
    - `getForDateRange()`: calls `completionDao.getForDateRange(start.toString(), end.toString())`
    - `getForHabitInDateRange()`: calls `completionDao.getForHabitInRange()`
    - `insert()`: maps to entity, calls `completionDao.insert()` (REPLACE strategy handles one-per-day at DB level)
    - `update()`: calls `completionDao.update()` with individual parameters
    - `delete()`: calls `completionDao.delete(id)`
    - `getLatestForHabit()`: calls `completionDao.getForHabit(habitId)` and returns first element
- Create `data/src/main/kotlin/com/getaltair/kairos/data/repository/PreferencesRepositoryImpl.kt`:
    - Implements `PreferencesRepository` interface
    - Constructor takes `UserPreferencesDao`
    - Uses `UserPreferencesEntityMapper` object
    - `get()`: calls `userPreferencesDao.get()`. If null, creates default `UserPreferences()`, inserts it, and returns it.
    - `update()`: maps to entity, calls individual-parameter update on DAO
- Update `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt`:
    - Add DAO singletons: `single { get<KairosDatabase>().habitDao() }`, etc. for all 9 DAOs
    - Add repository bindings: `single<HabitRepository> { HabitRepositoryImpl(get()) }`, `single<CompletionRepository> { CompletionRepositoryImpl(get()) }`, `single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }`

### 3. Implement Use Cases

- **Task ID**: implement-use-cases
- **Depends On**: fix-database-and-domain-models
- **Assigned To**: usecase-builder
- **Agent Type**: general-purpose
- **Parallel**: true (can run alongside task 2)
- Create `core/src/main/kotlin/com/getaltair/kairos/core/usecase/CreateHabitUseCase.kt`:
    - Constructor injection: `HabitRepository`
    - `suspend operator fun invoke(habit: Habit): Result<Habit>`
    - Validates with `HabitValidator.validate(habit)` -- return error if invalid
    - Calls `habitRepository.insert(habit)`
- Create `core/src/main/kotlin/com/getaltair/kairos/core/usecase/GetTodayHabitsUseCase.kt`:
    - Constructor injection: `HabitRepository`, `CompletionRepository`
    - `suspend operator fun invoke(): Result<List<HabitWithStatus>>`
    - Gets habits for today via `habitRepository.getHabitsForDate(LocalDate.now())`
    - Gets completions for today via `completionRepository.getForDate(LocalDate.now())`
    - Gets completions for past 7 days via `completionRepository.getForDateRange()` for week rate calculation
    - Maps each habit to `HabitWithStatus(habit, todayCompletion, weekCompletionRate)`
    - Week completion rate = (completions for this habit in last 7 days) / (days this habit was due in last 7 days)
- Create `core/src/main/kotlin/com/getaltair/kairos/core/usecase/CompleteHabitUseCase.kt`:
    - Constructor injection: `HabitRepository`, `CompletionRepository`
    - `suspend operator fun invoke(habitId: UUID, type: CompletionType, partialPercent: Int? = null): Result<Completion>`
    - Validate habit exists: `habitRepository.getById(habitId)` -- error if not found
    - Reject `CompletionType.Missed` (C-1: only system can create MISSED)
    - Create `Completion(habitId = habitId, date = LocalDate.now(), type = type, partialPercent = partialPercent)`
    - Validate with `CompletionValidator.validate(completion)`
    - Check one-per-day: `completionRepository.getForHabitOnDate(habitId, LocalDate.now())` -- error if exists (C-3)
    - Insert via `completionRepository.insert(completion)`
- Create `core/src/main/kotlin/com/getaltair/kairos/core/usecase/SkipHabitUseCase.kt`:
    - Constructor injection: `HabitRepository`, `CompletionRepository`
    - `suspend operator fun invoke(habitId: UUID, skipReason: SkipReason? = null): Result<Completion>`
    - Validate habit exists
    - Create `Completion(habitId, date = LocalDate.now(), type = CompletionType.Skipped, skipReason = skipReason)`
    - Check one-per-day, validate, insert
- Create `core/src/main/kotlin/com/getaltair/kairos/core/usecase/UndoCompletionUseCase.kt`:
    - Constructor injection: `CompletionRepository`
    - `suspend operator fun invoke(completionId: UUID, completedAt: java.time.Instant = java.time.Instant.now()): Result<Unit>`
    - The `completedAt` parameter represents when the original completion was made
    - Check 30-second undo window: `Duration.between(completedAt, Instant.now()).seconds <= 30` -- error if expired
    - Delete via `completionRepository.delete(completionId)`
- Create `core/src/main/kotlin/com/getaltair/kairos/core/usecase/GetWeeklyStatsUseCase.kt`:
    - Constructor injection: `HabitRepository`, `CompletionRepository`
    - `suspend operator fun invoke(habitId: UUID? = null): Result<WeeklyStats>`
    - Calculate date range: `today - 6 days` to `today` (7-day window)
    - If habitId provided: get completions for that habit in range, count by type
    - If null: get all completions in range, aggregate across habits
    - Return `WeeklyStats` with counts and `completionRate = (completed + partial) / totalDays`
- Create `core/src/main/kotlin/com/getaltair/kairos/core/di/UseCaseModule.kt`:
    - Koin module providing all 6 use cases: `factory { CreateHabitUseCase(get()) }`, etc.
- Update `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt`:
    - Add import for `useCaseModule`
    - Add `useCaseModule` to the `modules()` list

### 4. Write Tests

- **Task ID**: write-tests
- **Depends On**: implement-repositories, implement-use-cases
- **Assigned To**: test-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `domain/src/test/kotlin/com/getaltair/kairos/domain/validator/HabitValidatorTest.kt`:
    - Test H-1: blank anchorBehavior returns error
    - Test H-1: valid anchorBehavior passes
    - Test H-4: allowPartialCompletion = false returns error
    - Test H-5: relapseThresholdDays <= lapseThresholdDays returns error
    - Test H-5: relapseThresholdDays > lapseThresholdDays passes
    - Test H-6: updatedAt before createdAt returns error
    - Test H-6: valid timestamp ordering passes
    - Test: fully valid habit returns Success
- Create `domain/src/test/kotlin/com/getaltair/kairos/domain/validator/CompletionValidatorTest.kt`:
    - Test C-2: PARTIAL with partialPercent 0 returns error
    - Test C-2: PARTIAL with partialPercent 100 returns error
    - Test C-2: PARTIAL with partialPercent 50 passes
    - Test C-2: FULL with non-null partialPercent returns error
    - Test C-4: future date returns error
    - Test C-5: date older than 7 days returns error
    - Test C-5: date exactly 7 days ago passes
    - Test: valid completion returns Success
- Create `core/src/test/kotlin/com/getaltair/kairos/core/usecase/CreateHabitUseCaseTest.kt`:
    - Test: valid habit is created successfully
    - Test: invalid habit (blank anchor) returns validation error
    - Test: repository error is propagated
- Create `core/src/test/kotlin/com/getaltair/kairos/core/usecase/CompleteHabitUseCaseTest.kt`:
    - Test: full completion creates successfully
    - Test: partial completion with valid percent creates successfully
    - Test: duplicate completion for same day returns error (C-3)
    - Test: MISSED type rejected (C-1)
    - Test: non-existent habit returns error
- Create `core/src/test/kotlin/com/getaltair/kairos/core/usecase/GetTodayHabitsUseCaseTest.kt`:
    - Test: returns habits with completion status
    - Test: calculates week completion rate correctly
    - Test: empty habit list returns empty result
- Create `core/src/test/kotlin/com/getaltair/kairos/core/usecase/UndoCompletionUseCaseTest.kt`:
    - Test: undo within 30 seconds succeeds
    - Test: undo after 30 seconds returns error
- Add test dependencies to `core/build.gradle.kts` if not present:
    - `testImplementation(libs.junit)`
    - `testImplementation(libs.kotlinx.coroutines.test)`
    - `testImplementation("io.mockk:mockk:1.13.13")` or equivalent mock library available in catalog
    - Check `gradle/libs.versions.toml` for existing mock library before adding

### 5. Validate All Work

- **Task ID**: validate-all
- **Depends On**: write-tests
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Verify all acceptance criteria are met (see below)
- Verify code compiles: `./gradlew build` (or at minimum `./gradlew :domain:compileKotlin :data:compileKotlin :core:compileKotlin`)
- Verify tests pass: `./gradlew :domain:test :core:test`
- Verify Koin module definitions are consistent (all use case constructors match Koin factory params)
- Verify no circular dependencies between modules
- Check that all repository methods handle exceptions and return `Result.Error` (never throw)
- Verify invariant enforcement matches `docs/06-invariants.md` exactly
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

- [ ] `KairosDatabase` properly declares all 9 entities and provides abstract DAO accessors
- [ ] `HabitRepositoryImpl` implements all 9 methods of `HabitRepository` interface
- [ ] `CompletionRepositoryImpl` implements all 8 methods of `CompletionRepository` interface
- [ ] `PreferencesRepositoryImpl` implements both methods, creates defaults on first `get()`
- [ ] `CreateHabitUseCase` creates valid habits and rejects invalid ones (H-1, H-2, H-4, H-5)
- [ ] `GetTodayHabitsUseCase` returns habits filtered by frequency for current day of week
- [ ] `CompleteHabitUseCase` creates completion, enforces one-per-day (C-3), rejects MISSED type (C-1), validates partial percent (C-2)
- [ ] `SkipHabitUseCase` creates SKIPPED completion with optional reason
- [ ] `UndoCompletionUseCase` deletes completion within 30s window, rejects after 30s
- [ ] `GetWeeklyStatsUseCase` calculates correct completion rate for 7-day window
- [ ] All use cases return `Result<T>` (never throw)
- [ ] All repositories return `Result<T>` (never throw)
- [ ] `HabitValidator` unit tests cover all rules (H-1, H-4, H-5, H-6)
- [ ] `CompletionValidator` unit tests cover all rules (C-2, C-4, C-5)
- [ ] Use case unit tests cover happy path and error cases
- [ ] Koin modules provide all DAOs, repositories, and use cases
- [ ] `KairosApp.kt` loads the new `useCaseModule`
- [ ] `core/build.gradle.kts` depends on `:domain` and Koin
- [ ] Project compiles without errors

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew :domain:compileKotlin` -- Verify domain module compiles (validators, models)
- `./gradlew :data:compileKotlin` -- Verify data module compiles (repositories, updated DI)
- `./gradlew :core:compileKotlin` -- Verify core module compiles (use cases, DI)
- `./gradlew :app:compileKotlin` -- Verify app module compiles (updated KairosApp)
- `./gradlew :domain:test` -- Run validator unit tests
- `./gradlew :core:test` -- Run use case unit tests
- `./gradlew build` -- Full project build to check for any cross-module issues

## Notes

- **KairosDatabase gap**: The current `KairosDatabase` class is empty -- no entities listed, no DAO accessors. This is a Step 3 gap that must be fixed in Task 1 before any repository can work. The builder should also verify the Room KSP annotation processor is properly configured in `data/build.gradle.kts` (look for `ksp(libs.androidx.room.compiler)`).
- **KSP processor**: The `data/build.gradle.kts` does NOT have the KSP plugin or room compiler dependency. The `core/data/build.gradle.kts` does. The foundation-builder must add `alias(libs.plugins.ksp)` to data's plugins block and `ksp(libs.androidx.room.compiler)` to dependencies. Check `gradle/libs.versions.toml` for the exact alias.
- **DAO date format**: `CompletionDao` uses `String` for date parameters (ISO format "YYYY-MM-DD"). Repository implementations must use `date.toString()` (which produces ISO format from `LocalDate`).
- **HabitDao update signature**: The `update()` method takes individual parameters (not an entity object), so `HabitRepositoryImpl.update()` must extract all fields from the domain `Habit` and pass them individually, using converters for enum-to-string conversions.
- **Sealed class enums**: Enums like `HabitFrequency`, `CompletionType`, `HabitStatus` are sealed classes, not Kotlin enums. Pattern matching uses `is` checks (e.g., `is HabitFrequency.Daily`).
- **Existing Completion init block**: The `Completion` data class already has an `init` block that validates some type/partialPercent/skipReason consistency. The `CompletionValidator` adds date-based rules that the entity can't enforce.
- **Mock library**: Check `gradle/libs.versions.toml` for an existing mock library (MockK, Mockito-Kotlin) before adding a new dependency. If none exists, add MockK to the version catalog first.
- **DI framework is Koin**, not Hilt. The CLAUDE.md rules file mentions Hilt but the project actually uses Koin 4.2.0 (see memory: `kairos/di-framework.md`).
