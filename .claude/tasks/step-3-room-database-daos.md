# Plan: Room Database + DAOs

## Task Description

Implement the Room database layer for Kairos, including all entity classes, data access objects (DAOs), type converters, database indices, and the migration framework. Room remains the local source of truth for all data — all reads and writes go through Room first, with Firestore sync added in Step 9.

## Objective

When this plan is complete, the `data` module will have a fully functional Room database with:

- All entity classes mapped from domain models with proper `@Entity` annotations
- Type converters for enums, JSON lists, timestamps, and date/time types
- DAOs with key queries for habits, completions, routines, and recovery sessions
- Proper database indices defined for performance
- Schema export enabled for migrations
- Bidirectional entity ↔ domain mappers
- Unit tests verifying database operations
- Idempotent database initialization

## Problem Statement

The project currently has domain models defined in the `domain` module (STEP 2 complete) but no local persistence layer. The `data` module exists but is empty aside from build configuration. We need to implement Room to provide:

1. Local-first data access for offline functionality
2. Efficient queries for UI screens (Today screen, habit management, etc.)
3. Proper constraint enforcement at the database level (where possible)
4. Foundation for Firestore sync in Step 9

## Solution Approach

The solution follows Clean Architecture principles with the `data` module containing:

1. **Entity classes** - Room entities mapped 1:1 to domain entities with `@Entity` annotations
2. **Type converters** - Handle enum ↔ String, JSON ↔ List, Instant ↔ Long mappings
3. **DAOs** - Data access objects with key queries defined in the ERD
4. **Database class** - RoomDatabase abstraction with proper versioning and schema export
5. **Mappers** - Bidirectional conversion between Room entities and domain models
6. **Koin module** - Dependency injection configuration for database access

Key design decisions:

- Use `TEXT` for all UUID storage (no native UUID support in SQLite)
- Store `Instant` as `Long` (Unix epoch milliseconds) via converter
- Store `LocalDate`/`LocalTime` as `TEXT` with ISO format
- Store enums as `TEXT` via type converters
- Store JSON arrays/lists as `TEXT` with Gson/Moshi serialization
- Implement indices as defined in ERD for query performance
- Start at version 1 with `exportSchema = true` for migration baseline

## Relevant Files

### Existing Files

- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Habit.kt` - Domain habit model to map to entity
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Completion.kt` - Domain completion model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/Routine.kt` - Domain routine model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RoutineHabit.kt` - Domain routine-habit association
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RoutineVariant.kt` - Domain routine variant model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RoutineExecution.kt` - Domain routine execution model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/RecoverySession.kt` - Domain recovery session model
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/UserPreferences.kt` - Domain user preferences model
- All enum files in `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/` - Require type converters
- `data/build.gradle.kts` - Room dependencies already configured
- `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt` - Koin entry point for DI setup
- `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt` - Placeholder for Koin module

### New Files to Create

#### Entity Layer

- `data/src/main/kotlin/com/getaltair/kairos/data/entity/HabitEntity.kt` - Room entity for habits
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/CompletionEntity.kt` - Room entity for completions
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineEntity.kt` - Room entity for routines
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineHabitEntity.kt` - Room entity for routine-habit join
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineVariantEntity.kt` - Room entity for routine variants
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineExecutionEntity.kt` - Room entity for routine executions
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/RecoverySessionEntity.kt` - Room entity for recovery sessions
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/HabitNotificationEntity.kt` - Room entity for notifications
- `data/src/main/kotlin/com/getaltair/kairos/data/entity/UserPreferencesEntity.kt` - Room entity for preferences

#### Type Converters

- `data/src/main/kotlin/com/getaltair/kairos/data/converter/Converters.kt` - Composite type converters
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/EnumConverters.kt` - Enum to String converters
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/DateConverters.kt` - LocalDate/LocalTime converters
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/InstantConverter.kt` - Instant to Long converter
- `data/src/main/kotlin/com/getaltair/kairos/data/converter/JsonConverters.kt` - JSON list/object converters

#### DAOs

- `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt` - Habit data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/CompletionDao.kt` - Completion data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineDao.kt` - Routine data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineHabitDao.kt` - Routine-habit association
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineVariantDao.kt` - Routine variant data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineExecutionDao.kt` - Routine execution data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/RecoverySessionDao.kt` - Recovery session data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitNotificationDao.kt` - Notification data access
- `data/src/main/kotlin/com/getaltair/kairos/data/dao/UserPreferencesDao.kt` - Preferences data access

#### Database & DI

- `data/src/main/kotlin/com/getaltair/kairos/data/database/KairosDatabase.kt` - RoomDatabase implementation
- `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt` - Update with Koin providers
- `schemas/1.json` - Exported schema file (auto-generated)

#### Mappers

- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/HabitEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/CompletionEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineHabitEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineVariantEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineExecutionEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RecoverySessionEntityMapper.kt` - Bidirectional mapping
- `data/src/main/kotlin/com/getaltair/kairos/data/mapper/UserPreferencesEntityMapper.kt` - Bidirectional mapping

## Implementation Phases

### Phase 1: Type Converters Foundation

First, implement all type converters needed by entities. This ensures the entities can compile immediately after being created.

- Enum converters for all domain enums
- Date/time converters (LocalDate, LocalTime, Instant)
- JSON converters (using Moshi for JSON serialization)

### Phase 2: Entity Classes

Create all Room entity classes with proper `@Entity` annotations, indices, and foreign key relationships. Entities are mapped 1:1 from domain models.

### Phase 3: DAOs with Key Queries

Implement DAOs with the critical queries defined in the ERD document:

- Today's habits with completion status
- Habits by status/category/phase
- Lapse detection queries
- Routine with ordered habits
- Weekly completion stats

### Phase 4: Database & DI

Create the `KairosDatabase` class and update the `DataModule` with Koin providers for database and DAOs.

### Phase 5: Entity ↔ Domain Mappers

Implement bidirectional mappers for converting between Room entities and domain models. These will be used by repository implementations in Step 4.

### Phase 6: Testing & Validation

Write unit tests using Room's in-memory database to verify:

- CRUD operations
- Query results
- UNIQUE constraint enforcement
- Type converter behavior
- Mapper correctness

## Team Orchestration

- You operate as the team lead and orchestrate the team to execute this plan.
- IMPORTANT: You NEVER operate directly on the codebase. You use `Task` and `Task*` tools to deploy team members to the building, validating, testing, deploying, and other tasks.
- Your job is to act as a high level director of the team, not a builder.
- You'll orchestrate this by using the `Task*` Tools to manage coordination between the team members.
- Communication is paramount. You'll use the `Task*` Tools to communicate with the team members and ensure they're on track to complete the plan.
- Take note of the session id of each team member. This is how you'll reference them.

### Team Members

- Specialist (Data Layer)
    - Name: data-layer-builder
    - Role: Implement Room entities, type converters, DAOs, database, and mappers
    - Agent Type: general-purpose
    - Resume: false

- Quality Engineer (Validator)
    - Name: data-validator
    - Role: Validate database operations, test coverage, and schema correctness
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

- IMPORTANT: Execute every step in order, top to bottom. Each task maps directly to a `TaskCreate` call.
- Before you start, run `TaskCreate` to create the initial task list that all team members can see and execute.

### 1. Setup Type Converters

- **Task ID**: setup-type-converters
- **Depends On**: none
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/converter/Converters.kt` with composite type converter class
- Implement enum type converters for `AnchorType`, `HabitCategory`, `HabitFrequency`, `HabitPhase`, `HabitStatus`, `CompletionType`, `SkipReason`, `RoutineStatus`, `ExecutionStatus`, `RecoveryType`, `SessionStatus`, `RecoveryAction`, `Blocker`, `Theme`
- Create `InstantConverter` to convert between `Instant` and `Long` (Unix epoch milliseconds)
- Create `LocalDateConverter` to convert between `LocalDate` and `String` (ISO format YYYY-MM-DD)
- Create `LocalTimeConverter` to convert between `LocalTime` and `String` (ISO format HH:mm)
- Create `JsonListConverter` to handle `List<T>` ↔ `String` serialization using Moshi
- Create `JsonMapConverter` to handle `Map<String, Any>` ↔ `String` serialization
- Create `DayOfWeekListConverter` for `Set<DayOfWeek>` ↔ `String` (comma-separated values)
- Annotate converter classes with `@ProvidedTypeConverter`

### 2. Create HabitEntity

- **Task ID**: create-habit-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/HabitEntity.kt`
- Annotate with `@Entity(tableName = "habits")`
- Map all fields from domain `Habit` entity
- Use proper column names (snake_case) and data types
- Apply type converters for enums, dates, JSON fields
- Add indices:
    - Primary key on `id`
    - `Index(value = ["status", "category"])` for Today screen queries
    - `Index(value = ["phase"])` for lapse detection
    - `Index(value = ["userId"])` for sync queries (nullable)
- Add foreign key relationship to completions if applicable (handled via DAO queries)
- Add `userId` field (nullable TEXT for local-only, populated by sync in Step 9)

### 3. Create CompletionEntity

- **Task ID**: create-completion-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/CompletionEntity.kt`
- Annotate with `@Entity(tableName = "completions")`
- Map all fields from domain `Completion` entity
- Add UNIQUE constraint: `Unique(entity = ["habitId", "date"])` to enforce one-per-day
- Add indices:
    - Primary key on `id`
    - `Index(value = ["habitId", "date"])` for habit completion queries
    - `Index(value = ["date"])` for date-range queries

### 4. Create RoutineEntity

- **Task ID**: create-routine-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineEntity.kt`
- Annotate with `@Entity(tableName = "routines")`
- Map all fields from domain `Routine` entity
- Add primary key on `id`
- Add `userId` field (nullable for sync)

### 5. Create RoutineHabitEntity

- **Task ID**: create-routine-habit-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineHabitEntity.kt`
- Annotate with `@Entity(tableName = "routine_habits")`
- Map all fields from domain `RoutineHabit` entity
- Add indices:
    - Primary key on `id`
    - `Index(value = ["routineId"])`
    - `Unique(entity = ["routineId", "orderIndex"])` to enforce unique order
- Add foreign key indices for `habitId` and `routineId`

### 6. Create RoutineVariantEntity

- **Task ID**: create-routine-variant-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineVariantEntity.kt`
- Annotate with `@Entity(tableName = "routine_variants")`
- Map all fields from domain `RoutineVariant` entity
- Add primary key on `id`
- Add foreign key index on `routineId`

### 7. Create RoutineExecutionEntity

- **Task ID**: create-routine-execution-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/RoutineExecutionEntity.kt`
- Annotate with `@Entity(tableName = "routine_executions")`
- Map all fields from domain `RoutineExecution` entity
- Add indices:
    - Primary key on `id`
    - `Index(value = ["status"])` for active execution queries
    - Foreign key index on `routineId`
- Add foreign key index on `variantId` (nullable)

### 8. Create RecoverySessionEntity

- **Task ID**: create-recovery-session-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/RecoverySessionEntity.kt`
- Annotate with `@Entity(tableName = "recovery_sessions")`
- Map all fields from domain `RecoverySession` entity
- Add indices:
    - Primary key on `id`
    - `Index(value = ["habitId", "status"])` for pending session queries
- Add foreign key index on `habitId`

### 9. Create HabitNotificationEntity

- **Task ID**: create-habit-notification-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/HabitNotificationEntity.kt`
- Annotate with `@Entity(tableName = "habit_notifications")`
- Add primary key on `id`
- Add foreign key index on `habitId`

### 10. Create UserPreferencesEntity

- **Task ID**: create-user-preferences-entity
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: true
- Create `data/src/main/kotlin/com/getaltair/kairos/data/entity/UserPreferencesEntity.kt`
- Annotate with `@Entity(tableName = "user_preferences")`
- Map all fields from domain `UserPreferences` entity
- Add primary key on `id`
- Add index on `userId` (nullable)

### 11. Create HabitDao

- **Task ID**: create-habit-dao
- **Depends On**: create-habit-entity, create-completion-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitDao.kt`
- Annotate with `@Dao`
- Implement CRUD operations: `getAll`, `getById`, `insert`, `update`, `delete`, `deleteAll`
- Implement `getActiveHabits()`: Query WHERE status = 'ACTIVE'
- Implement `getHabitsByStatusAndCategory(status, category)`: Management screens, departure filter
- Implement `getLapsedHabits(thresholdDays)`: Lapse detection query from ERD
- Implement `getHabitsByPhase(phase)`: Phase-based queries
- Implement `getTodayHabitsWithCompletions(date)`: JOIN query for Today screen (per ERD example)
- Implement `getByCategory(category)`: Category-based filtering

### 12. Create CompletionDao

- **Task ID**: create-completion-dao
- **Depends On**: create-completion-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/CompletionDao.kt`
- Annotate with `@Dao`
- Implement CRUD operations: `getAll`, `getById`, `insert`, `update`, `delete`, `deleteAll`
- Implement `getForHabitOnDate(habitId, date)`: Get completion for specific habit/date
- Implement `getForDate(date)`: Get all completions for a day
- Implement `getForDateRange(startDate, endDate)`: Weekly reports query
- Implement `getForHabitInRange(habitId, startDate, endDate)`: Habit completion history
- Implement `insertWithConflictStrategy(completion)`: Use OnConflictStrategy.REPLACE for updates
- Use `@Insert(onConflict = OnConflictStrategy.REPLACE)` annotation for insert method

### 13. Create RoutineDao and Related DAOs

- **Task ID**: create-routine-daos
- **Depends On**: create-routine-entity, create-routine-habit-entity, create-routine-variant-entity, create-routine-execution-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineDao.kt` with CRUD operations
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineHabitDao.kt` with:
    - `getByRoutineId(routineId)`: Get all habits for a routine
    - `insert`, `update`, `delete` operations
    - Query to get ordered habits by `orderIndex`
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineVariantDao.kt` with CRUD operations
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/RoutineExecutionDao.kt` with:
    - `getByRoutineId(routineId)`: Get all executions for a routine
    - `getActiveExecution()`: Query WHERE status = 'IN_PROGRESS'
    - `insert`, `update`, `delete` operations

### 14. Create RecoverySessionDao

- **Task ID**: create-recovery-session-dao
- **Depends On**: create-recovery-session-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/RecoverySessionDao.kt`
- Annotate with `@Dao`
- Implement CRUD operations
- Implement `getPendingForHabit(habitId)`: Query WHERE habitId AND status = 'PENDING'
- Implement `getByHabit(habitId)`: Get all recovery sessions for a habit
- Implement `getPendingSessions()`: Get all pending recovery sessions

### 15. Create HabitNotificationDao and UserPreferencesDao

- **Task ID**: create-notification-preferences-dao
- **Depends On**: create-habit-notification-entity, create-user-preferences-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/HabitNotificationDao.kt` with CRUD operations
- Create `data/src/main/kotlin/com/getaltair/kairos/data/dao/UserPreferencesDao.kt` with:
    - `get()`: Get single preferences record
    - `insert`, `update` operations

### 16. Create KairosDatabase

- **Task ID**: create-kairos-database
- **Depends On**: create-habit-dao, create-completion-dao, create-routine-daos, create-recovery-session-dao, create-notification-preferences-dao
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/database/KairosDatabase.kt`
- Annotate abstract class with `@Database`:
    - Set `entities = [all entity classes]`
    - Set `version = 1`
    - Set `exportSchema = true`
- Declare all DAOs as abstract functions
- Follow package name: `com.getaltair.kairos.data`
- Verify `schemaDirectory` is configured in `data/build.gradle.kts`

### 17. Update DataModule with Koin Providers

- **Task ID**: update-data-module
- **Depends On**: create-kairos-database
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Update `data/src/main/kotlin/com/getaltair/kairos/data/di/DataModule.kt`
- Add `single { provideDatabase(androidContext()) }` provider
- Add DAO providers for all DAOs: `single { get<KairosDatabase>().habitDao() }`, etc.
- Update `KairosApp.kt` to include `dataModule` in Koin modules (if not already)
- Verify module resolves correctly

### 18. Create HabitEntityMapper

- **Task ID**: create-habit-mapper
- **Depends On**: create-habit-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/HabitEntityMapper.kt`
- Implement `toDomain(entity: HabitEntity): Habit` conversion
- Implement `toEntity(domain: Habit): HabitEntity` conversion
- Handle null values appropriately
- Convert date/time strings to `LocalDate`/`LocalTime`
- Convert enum strings to enum values

### 19. Create CompletionEntityMapper

- **Task ID**: create-completion-mapper
- **Depends On**: create-completion-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/CompletionEntityMapper.kt`
- Implement bidirectional `toDomain` and `toEntity` methods
- Convert timestamp longs to `Instant`
- Convert date strings to `LocalDate`

### 20. Create RoutineEntityMapper

- **Task ID**: create-routine-mapper
- **Depends On**: create-routine-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineEntityMapper.kt`
- Implement bidirectional mapping methods

### 21. Create RoutineHabitEntityMapper

- **Task ID**: create-routine-habit-mapper
- **Depends On**: create-routine-habit-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineHabitEntityMapper.kt`
- Implement bidirectional mapping methods
- Convert variantIds JSON string to `List<UUID>`

### 22. Create RoutineVariantEntityMapper

- **Task ID**: create-routine-variant-mapper
- **Depends On**: create-routine-variant-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineVariantEntityMapper.kt`
- Implement bidirectional mapping methods

### 23. Create RoutineExecutionEntityMapper

- **Task ID**: create-routine-execution-mapper
- **Depends On**: create-routine-execution-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RoutineExecutionEntityMapper.kt`
- Implement bidirectional mapping methods
- Convert stepResults JSON string if present

### 24. Create RecoverySessionEntityMapper

- **Task ID**: create-recovery-session-mapper
- **Depends On**: create-recovery-session-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/RecoverySessionEntityMapper.kt`
- Implement bidirectional mapping methods
- Convert blockers JSON string to `List<Blocker>`

### 25. Create UserPreferencesEntityMapper

- **Task ID**: create-user-preferences-mapper
- **Depends On**: create-user-preferences-entity
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create `data/src/main/kotlin/com/getaltair/kairos/data/mapper/UserPreferencesEntityMapper.kt`
- Implement bidirectional mapping methods
- Convert notificationChannels JSON map to/from

### 26. Write Unit Tests for Database Operations

- **Task ID**: write-database-tests
- **Depends On**: update-data-module, create-habit-mapper, create-completion-mapper
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create test directory structure: `data/src/test/kotlin/com/getaltair/kairos/data/`
- Create `HabitDaoTest.kt` with:
    - Test insert, query, update, delete operations
    - Test getTodayHabitsWithCompletions query
    - Test frequency filtering logic in DAO
- Create `CompletionDaoTest.kt` with:
    - Test UNIQUE constraint enforcement (duplicate insert should fail)
    - Test date range queries
- Use `@RunWith(AndroidJUnit4::class)` and in-memory database
- Verify all tests pass

### 27. Write Unit Tests for Type Converters

- **Task ID**: write-converter-tests
- **Depends On**: setup-type-converters
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Create converter test file: `data/src/test/kotlin/com/getaltair/kairos/data/converter/ConvertersTest.kt`
- Test all enum converters round-trip
- Test date/time converters
- Test JSON converters
- Verify all converter tests pass

### 28. Verify Idempotent Database Initialization

- **Task ID**: verify-database-initialization
- **Depends On**: write-database-tests, write-converter-tests
- **Assigned To**: data-layer-builder
- **Agent Type**: general-purpose
- **Parallel**: false
- Write integration test to verify:
    - Database creates on first launch
    - Subsequent launches don't error
    - Schema file is exported to `schemas/1.json`
- Run test and verify success

### 29. Validate All Tests Pass

- **Task ID**: validate-all
- **Depends On**: write-database-tests, write-converter-tests, verify-database-initialization
- **Assigned To**: data-validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Run all unit tests in `data` module: `./gradlew :data:test`
- Verify all tests pass
- Check test coverage is adequate
- Verify no lint warnings or errors
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

1. Database creates all tables on first launch (verified via test)
2. Subsequent launches don't error (idempotent)
3. UNIQUE constraint on `(habit_id, date)` in completions prevents duplicates
4. All DAOs compile and basic CRUD operations work
5. Instrumented tests verify: insert, query, update, delete for Habit and Completion
6. Type converters handle all enum types and JSON fields correctly
7. Schema export enabled (`exportSchema = true`) with `schemas/1.json` generated
8. Entity ↔ Domain mappers correctly convert bidirectionally
9. All unit tests pass
10. No compilation errors or warnings in `data` module
11. Koin module successfully provides database and DAOs

## Validation Commands

Execute these commands to validate the task is complete:

```bash
# Build the data module
./gradlew :data:build

# Run all unit tests
./gradlew :data:test

# Run specific test class (for debugging)
./gradlew :data:test --tests HabitDaoTest
./gradlew :data:test --tests CompletionDaoTest
./gradlew :data:test --tests ConvertersTest

# Verify schema export
ls -la data/schemas/

# Clean and rebuild to verify idempotency
./gradlew :data:clean :data:build

# Run tests again after clean
./gradlew :data:test
```

## Notes

1. **Room Version**: Project uses Room 2.8.4 as defined in `gradle/libs.versions.toml`
2. **Koin Integration**: The project uses Koin (not Hilt) for dependency injection. DI module is at `com.getaltair.kairos.data.di` (note: reference in `KairosApp.kt` says `core.data.di` but actual file is at `com.getaltair.kairos.data.di`)
3. **Namespace**: The `data` module namespace is `com.getaltair.kairos.data`
4. **Schema Directory**: Configured as `$projectDir/schemas` in `data/build.gradle.kts`
5. **Timestamp Strategy**: Store `Instant` as `Long` (Unix epoch ms) via converter, more efficient than String storage
6. **UUID Storage**: Use `TEXT` for UUIDs - SQLite doesn't have native UUID type
7. **JSON Serialization**: Use Moshi (lighter than Gson) for JSON list/map serialization
8. **Moshi Dependency**: Need to add Moshi to `data/build.gradle.kts` for JSON converters:
    ```kotlin
    implementation("com.squareup.moshi:moshi:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    ```
9. **Migration Strategy**: Start at version 1, schema export creates `schemas/1.json` baseline. Future migrations will increment version and add `Migration` objects.
10. **Index Names**: Use descriptive index names per ERD examples (e.g., `idx_habits_status_category`)
11. **Foreign Keys**: Room doesn't enforce FK constraints at SQL level, but `@ForeignKey` annotations document relationships
12. **Nullable userId**: `userId` fields are nullable for local-first functionality - populated by sync layer in Step 9
13. **Test Database**: Use `androidx.room:room-testing` for in-memory database in tests
14. **Coroutine Support**: Use `@Transaction` for multi-step operations in repositories (Step 4)
15. **One-Per-Day Invariant**: The UNIQUE constraint on `(habit_id, date)` enforces invariant C-3 at DB level
