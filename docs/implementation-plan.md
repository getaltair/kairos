# Kairos — Implementation Plan

**Purpose:** Step-by-step feature-based build order for spec-driven development with Claude Code. Each step references the relevant docs, lists its dependencies, and defines its "done" criteria.

**Key Constraint:** Solo developer using AI coding agents heavily. Steps are scoped so each is a self-contained prompt-friendly unit with clear inputs, outputs, and validation criteria.

**Backend:** Firebase (Firestore + Auth). Offline-first via Firestore's built-in persistence. No self-hosted infrastructure to maintain.

**ESP32/Presence:** Routes through Home Assistant's built-in MQTT broker. No standalone MQTT service needed.

---

## Dependency Graph (Visual)

```
                        ┌──────────────────────────────────┐
                        │  STEP 1: Project Scaffold         │
                        │  + Multi-Module Setup             │
                        └──────────┬───────────────────────┘
                                   │
                          ┌────────┴────────┐
                          ▼                 ▼
                 ┌──────────────┐   ┌──────────────────┐
                 │ STEP 2:      │   │ STEP 8:          │
                 │ Domain       │   │ Firebase Setup   │
                 │ Models       │   │ (PARALLEL)       │
                 │              │   │                  │
                 └──────┬───────┘   │ 8a. Project +    │
                        │           │     Auth         │
                        ▼           │ 8b. Firestore    │
                 ┌──────────────┐   │     Collections  │
                 │ STEP 3:      │   │ 8c. Security     │
                 │ Room DB      │   │     Rules        │
                 │ + DAOs       │   └────────┬─────────┘
                 └──────┬───────┘            │
                        │                    │
                        ▼                    │
                 ┌──────────────┐            │
                 │ STEP 4:      │            │
                 │ Repositories │            │
                 │ + Use Cases  │            │
                 └──────┬───────┘            │
                        │                    │
               ┌────────┼────────┐           │
               ▼        ▼        ▼           │
        ┌───────────┐ ┌─────────────┐        │
        │ STEP 5:   │ │ STEP 6:     │        │
        │ Today     │ │ Create      │        │
        │ Screen    │ │ Habit Flow  │        │
        └─────┬─────┘ └──────┬──────┘        │
              │               │               │
              └───────┬───────┘               │
                      ▼                       │
              ┌──────────────┐                │
              │ STEP 7:      │                │
              │ Habit Mgmt   │                │
              │ (edit/pause/ │                │
              │  archive)    │                │
              └──────┬───────┘                │
                     │                        │
                     └────────┬───────────────┘
                              ▼
                     ┌──────────────┐
                     │ STEP 9:      │
                     │ Firestore    │
                     │ Sync         │
                     └──────┬───────┘
                            │
               ┌────────────┼───────────────┐
               ▼            ▼               ▼
        ┌───────────┐ ┌───────────┐  ┌──────────────┐
        │ STEP 10:  │ │ STEP 11:  │  │ STEP 14:     │
        │ Pi Kiosk  │ │ Notif.    │  │ Home Widget  │
        │ Dashboard │ │ System    │  └──────────────┘
        │ v1        │ └─────┬─────┘
        └─────┬─────┘       │
              │              ▼
              │        ┌───────────┐
              │        │ STEP 12:  │
              │        │ Recovery  │
              │        │ System    │
              │        └─────┬─────┘
              │              │
              │              ▼
              │        ┌───────────┐
              │        │ STEP 13:  │
              │        │ Routines  │
              │        └─────┬─────┘
              │              │
              │         ┌────┴──────┐
              │         ▼           ▼
              │   ┌───────────┐ ┌───────────┐
              │   │ STEP 15:  │ │ STEP 16:  │
              │   │ WearOS    │ │ Dashboard  │
              │   │ App       │ │ v2 (touch) │
              │   └───────────┘ └───────────┘
              │                       │
              │                       ▼
              │                ┌───────────┐
              │                │ STEP 17:  │
              │                │ ESP32 +   │
              │                │ Presence  │
              └────────────────┤ (via HA)  │
                               └───────────┘
```

---

## Parallel Tracks

| Track A: Android App | Track B: Firebase | Track C: Pi Dashboard |
| -------------------- | ----------------- | --------------------- |
| Steps 1–7, 11–15     | Step 8 (< 1 day)  | Steps 10, 16, 17      |

Firebase setup (Track B) is dramatically simpler than the original self-hosted stack — it's a console-click + config file exercise, not infrastructure engineering. The real work is in Track A (the app) and Track C (the kiosk).

**Critical path to kiosk:** Steps 1 → 2 → 3 → 4 → 5 → 8 → 9 → 10 (~10 days)
**Critical path to core phone app:** Steps 1 → 2 → 3 → 4 → 5 → 6 → 7 → 11 → 12 (~13 days)

---

## Shared Module Strategy

Because the Android app, Pi dashboard, and potentially WearOS all use Kotlin, the project shares domain models across platforms:

| Module      | Platforms              | Contents                                        |
| ----------- | ---------------------- | ----------------------------------------------- |
| `domain`    | Android, Desktop, Wear | Domain models, enums, value objects, interfaces |
| `data`      | Android                | Room entities, DAOs, mappers, repository impls  |
| `core`      | Android                | Use cases, ViewModels                           |
| `app`       | Android                | Phone app (Compose), DI, navigation             |
| `wear`      | WearOS                 | Watch app, tiles, complications                 |
| `dashboard` | Desktop (JVM)          | Pi kiosk app (Compose Desktop)                  |

> **Pragmatic note:** Start with a pure Android project. Extract `domain` to a shared KMP module when you build the dashboard (Step 10). The domain models are plain Kotlin data classes — extraction is mechanical. Don't let module structure block progress on Steps 2–5.

---

## STEP 1: Project Scaffold + Multi-Module Setup

**Dependencies:** None
**Priority:** P0
**Docs:** `07-architecture.md` §Module Structure, §Dependency Injection

### What to build

- Android project with Gradle version catalog
- Module structure (at minimum: `app`, `domain`, `data`, `core`)
- Hilt dependency injection setup
- Build variants (debug, release)
- Timber logging setup
- `.editorconfig`, `detekt` or `ktlint` for code style
- Basic `MainActivity` with empty Compose scaffold
- Navigation component setup (empty shell)

### Module skeleton

```
kairos/
├── app/                          # Android phone app
│   └── src/main/kotlin/.../
│       ├── KairosApp.kt          # Application class + Hilt
│       ├── MainActivity.kt       # Single activity
│       └── navigation/
│           └── KairosNavGraph.kt  # Empty nav shell
├── core/                         # Use cases, ViewModels
├── domain/                       # Pure Kotlin models
├── data/                         # Room, repositories
├── build-logic/                  # Convention plugins
├── gradle/libs.versions.toml     # Version catalog
└── settings.gradle.kts
```

### Key dependency versions to pin

| Library            | Purpose               | Min Version   |
| ------------------ | --------------------- | ------------- |
| Kotlin             | Language              | 2.0+          |
| Compose BOM        | UI toolkit            | Latest stable |
| Hilt               | DI                    | 2.51+         |
| Room               | Database              | 2.6+          |
| WorkManager        | Background jobs       | 2.9+          |
| Navigation Compose | Screen routing        | 2.7+          |
| Firebase BOM       | Firebase dependencies | Latest        |
| Timber             | Logging               | 5.0+          |

### Done when

- [x] Project builds and runs on emulator
- [x] Empty Compose screen renders with app bar
- [x] Hilt injection compiles (empty module provided)
- [x] All modules resolve dependencies correctly
- [x] Timber logs appear in Logcat
- [x] Both debug and release variants build

---

## STEP 2: Domain Models + Enumerations

**Dependencies:** Step 1 (module structure exists)
**Priority:** P0
**Docs:** `05-domain-model.md` (full entity definitions), `06-invariants.md` (constraints), `09-state-machines.md` (valid transitions)

### What to build

Pure Kotlin domain models in the `domain` module. Zero framework dependencies — plain data classes and sealed classes/enums.

### Entities to define

| Entity             | Key Fields                                                           | Source Doc                           |
| ------------------ | -------------------------------------------------------------------- | ------------------------------------ |
| `Habit`            | name, anchorBehavior, anchorType, category, frequency, phase, status | 05-domain-model.md §Habit            |
| `Completion`       | habitId, date, type, partialPercent, skipReason                      | 05-domain-model.md §Completion       |
| `Routine`          | name, category, status                                               | 05-domain-model.md §Routine          |
| `RoutineHabit`     | routineId, habitId, orderIndex                                       | 05-domain-model.md §RoutineHabit     |
| `RoutineVariant`   | routineId, name, estimatedMinutes                                    | 05-domain-model.md §RoutineVariant   |
| `RoutineExecution` | routineId, status, currentStepIndex                                  | 05-domain-model.md §RoutineExecution |
| `RecoverySession`  | habitId, type, status, blockers, action                              | 05-domain-model.md §RecoverySession  |
| `UserPreferences`  | notificationEnabled, theme                                           | 05-domain-model.md §UserPreferences  |

### Enumerations to define

| Enum              | Values                                                                                                                                        | Source Doc                         |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| `AnchorType`      | AFTER_BEHAVIOR, BEFORE_BEHAVIOR, AT_LOCATION, AT_TIME                                                                                         | 05-domain-model.md                 |
| `HabitCategory`   | MORNING, AFTERNOON, EVENING, ANYTIME, DEPARTURE                                                                                               | 05-domain-model.md + dashboard req |
| `HabitFrequency`  | DAILY, WEEKDAYS, WEEKENDS, CUSTOM                                                                                                             | 05-domain-model.md                 |
| `HabitPhase`      | ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED                                                                                               | 09-state-machines.md               |
| `HabitStatus`     | ACTIVE, PAUSED, ARCHIVED                                                                                                                      | 09-state-machines.md               |
| `CompletionType`  | FULL, PARTIAL, SKIPPED, MISSED                                                                                                                | 05-domain-model.md                 |
| `SkipReason`      | TOO_TIRED, NO_TIME, NOT_FEELING_WELL, TRAVELING, TOOK_DAY_OFF, OTHER                                                                          | 05-domain-model.md                 |
| `RecoveryType`    | LAPSE, RELAPSE                                                                                                                                | 02-prd-recovery.md                 |
| `SessionStatus`   | PENDING, COMPLETED, ABANDONED                                                                                                                 | 02-prd-recovery.md                 |
| `RecoveryAction`  | RESUME, SIMPLIFY, PAUSE, ARCHIVE, FRESH_START                                                                                                 | 02-prd-recovery.md                 |
| `Blocker`         | TOO_TIRED, FELT_UNWELL, LOW_MOTIVATION, NO_TIME, SCHEDULE_CHANGED, TRAVELING, OVERWHELMED, FORGOT, COULDNT_START, NEEDED_BREAK, DEPRIORITIZED | 02-prd-recovery.md                 |
| `SyncStatus`      | LOCAL_ONLY, SYNCED, PENDING_SYNC, PENDING_DELETE, CONFLICT                                                                                    | 09-state-machines.md               |
| `ExecutionStatus` | NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, ABANDONED                                                                                        | 09-state-machines.md               |
| `RoutineStatus`   | ACTIVE, PAUSED, ARCHIVED                                                                                                                      | 05-domain-model.md                 |
| `Theme`           | SYSTEM, LIGHT, DARK                                                                                                                           | 05-domain-model.md                 |

> **Note:** `DEPARTURE` is added to `HabitCategory` for the Pi dashboard's "Don't Forget" checklist. These are modeled as habits so they get full tracking, sync, and recovery for free.

### Value objects to define

| Value Object | Fields                                      | Purpose                             |
| ------------ | ------------------------------------------- | ----------------------------------- |
| `TimeWindow` | start: LocalTime, end: LocalTime            | Optional time constraints on habits |
| `GeoPoint`   | lat: Double, lon: Double, radiusMeters: Int | Location-based anchors (P2)         |

### State machine validation

Implement `canTransitionTo()` for `HabitPhase` and `HabitStatus` per `09-state-machines.md`:

```kotlin
sealed class HabitPhase {
    fun canTransitionTo(target: HabitPhase): Boolean = when (this) {
        is Onboard -> target is Forming
        is Forming -> target is Maintaining || target is Lapsed
        is Maintaining -> target is Lapsed
        is Lapsed -> target is Forming || target is Relapsed
        is Relapsed -> target is Forming
    }
}
```

### Repository interfaces

Define in `domain` module — implemented in `data` module later:

| Interface               | Key Methods                                                        |
| ----------------------- | ------------------------------------------------------------------ |
| `HabitRepository`       | getById, getActiveHabits, getHabitsForDate, insert, update, delete |
| `CompletionRepository`  | getForHabitOnDate, getForDate, getForDateRange, insert, update     |
| `RoutineRepository`     | getById, getActiveRoutines, getRoutineWithHabits, insert, update   |
| `RecoveryRepository`    | getPendingForHabit, insert, update                                 |
| `PreferencesRepository` | get, update                                                        |

### Done when

- [ ] All entities compile as data classes in `domain` module
- [ ] All enumerations defined with correct values (including DEPARTURE category)
- [ ] `HabitPhase.canTransitionTo()` passes unit tests for all valid/invalid transitions from `09-state-machines.md`
- [ ] `HabitStatus` transitions validated similarly
- [ ] Repository interfaces defined with no implementation
- [ ] `domain` module has zero Android framework dependencies (pure Kotlin)
- [ ] Unit tests cover all invariants from `06-invariants.md` that can be validated at model level (H-1, H-2, H-4, H-5, C-2, C-4, C-5)

---

## STEP 3: Room Database + DAOs

**Dependencies:** Step 2 (domain models define the schema)
**Priority:** P0
**Docs:** `08-erd.md` §Local Database Schema, `06-invariants.md` (constraints to enforce in DB)

### What to build

Room remains the local source of truth — Firestore syncs to/from it, but all reads and writes hit Room first. This preserves offline-first behavior even if Firestore is unavailable.

- Room database class (`KairosDatabase`)
- Room entity classes (mapped from domain models with `@Entity` annotations)
- Type converters (enums, JSON lists, timestamps, LocalDate, LocalTime)
- DAOs for all entities
- Database indices per `08-erd.md` §Index Definitions
- Database migration framework (export schema = true)

### Tables to create

| Table                 | Entity Class              | Key Constraints                                                      | Source                     |
| --------------------- | ------------------------- | -------------------------------------------------------------------- | -------------------------- |
| `habits`              | `HabitEntity`             | PK: id, Index: (status, category), (phase)                           | 08-erd.md §Core Tables     |
| `completions`         | `CompletionEntity`        | PK: id, FK: habit_id, UNIQUE: (habit_id, date)                       | 08-erd.md §Core Tables     |
| `routines`            | `RoutineEntity`           | PK: id                                                               | 08-erd.md §Routine Tables  |
| `routine_habits`      | `RoutineHabitEntity`      | PK: id, FK: routine_id + habit_id, UNIQUE: (routine_id, order_index) | 08-erd.md §Routine Tables  |
| `routine_variants`    | `RoutineVariantEntity`    | PK: id, FK: routine_id                                               | 08-erd.md §Routine Tables  |
| `routine_executions`  | `RoutineExecutionEntity`  | PK: id, FK: routine_id, Index: (status)                              | 08-erd.md §Routine Tables  |
| `recovery_sessions`   | `RecoverySessionEntity`   | PK: id, FK: habit_id, Index: (habit_id, status)                      | 08-erd.md §Recovery Tables |
| `habit_notifications` | `HabitNotificationEntity` | PK: id, FK: habit_id                                                 | 08-erd.md                  |
| `user_preferences`    | `UserPreferencesEntity`   | PK: id                                                               | 08-erd.md                  |

### Critical indices (from 08-erd.md §Index Definitions)

```sql
CREATE INDEX idx_habits_status_category ON habits(status, category);
CREATE UNIQUE INDEX idx_completions_habit_date ON completions(habit_id, date);
CREATE INDEX idx_completions_date ON completions(date);
CREATE INDEX idx_habits_phase ON habits(phase);
CREATE UNIQUE INDEX idx_routine_habits_order ON routine_habits(routine_id, order_index);
```

### Key DAO queries (from 08-erd.md §Query Examples)

| DAO             | Query                                            | Purpose                              |
| --------------- | ------------------------------------------------ | ------------------------------------ |
| `HabitDao`      | Get today's habits with completion status (JOIN) | Today screen                         |
| `HabitDao`      | Get habits by status and category                | Management screens, departure filter |
| `HabitDao`      | Get lapsed habits (no recent completions)        | Lapse detection worker               |
| `CompletionDao` | Get completions for date range                   | Weekly reports                       |
| `CompletionDao` | Insert with UNIQUE conflict strategy             | One-per-day invariant (C-3)          |
| `RoutineDao`    | Get routine with ordered habits                  | Routine runner                       |

### Entity ↔ Domain mappers

Bidirectional mappers in `data` module:

- `HabitEntity.toDomain() → Habit`
- `Habit.toEntity() → HabitEntity`
- Same pattern for all entities

### Done when

- [ ] Database creates all tables on first launch (verified via DB inspector)
- [ ] Subsequent launches don't error (idempotent)
- [ ] UNIQUE constraint on `(habit_id, date)` in completions prevents duplicates
- [ ] All DAOs compile and basic CRUD operations work
- [ ] Instrumented tests verify: insert, query, update, delete for Habit and Completion
- [ ] Type converters handle all enum types and JSON fields
- [ ] Schema export enabled (`exportSchema = true`)
- [ ] Entity ↔ Domain mappers unit tested

---

## STEP 4: Repository Layer + Core Use Cases

**Dependencies:** Step 3 (DAOs available for repository implementations)
**Priority:** P0
**Docs:** `07-architecture.md` §Domain Layer + §Data Layer, `06-invariants.md` (validation rules), `01-prd-core.md` §Functional Requirements

### What to build

Repository implementations and first use cases. At this stage, repositories only talk to Room — Firestore sync is added in Step 9.

### Repository implementations

| Repository                  | Key Responsibilities                                        |
| --------------------------- | ----------------------------------------------------------- |
| `HabitRepositoryImpl`       | CRUD, query by date/status/category, apply frequency filter |
| `CompletionRepositoryImpl`  | Insert (enforce one-per-day), query by date/range, update   |
| `PreferencesRepositoryImpl` | DataStore-backed preferences access                         |

### Use cases to implement (P0 only)

| Use Case                | Input                                    | Output                      | Invariants Enforced                                                                                 |
| ----------------------- | ---------------------------------------- | --------------------------- | --------------------------------------------------------------------------------------------------- |
| `CreateHabitUseCase`    | Habit fields                             | Result<Habit>               | H-1 (anchor required), H-2 (category required), H-4 (partial always true), H-5 (threshold ordering) |
| `GetTodayHabitsUseCase` | None (uses current date)                 | Flow<List<HabitWithStatus>> | Frequency filtering (daily/weekdays/etc)                                                            |
| `CompleteHabitUseCase`  | habitId, CompletionType, partialPercent? | Result<Completion>          | C-1 (valid type), C-2 (partial range), C-3 (one per day), C-4 (no future)                           |
| `SkipHabitUseCase`      | habitId, SkipReason?                     | Result<Completion>          | Same as above                                                                                       |
| `UndoCompletionUseCase` | completionId                             | Result<Unit>                | Within 30 seconds                                                                                   |
| `GetWeeklyStatsUseCase` | habitId?                                 | Flow<WeeklyStats>           | Read-only aggregation                                                                               |

### Composite model for Today screen

```kotlin
data class HabitWithStatus(
    val habit: Habit,
    val todayCompletion: Completion?,  // null = pending
    val weekCompletionRate: Float      // 0.0 - 1.0
)
```

### Validation layer

`HabitValidator` enforcing invariants from `06-invariants.md`:

- H-1: `anchorBehavior.isNotBlank()`
- H-2: `category` is valid enum
- H-4: `allowPartialCompletion == true` (hardcoded)
- H-5: `relapseThresholdDays > lapseThresholdDays`
- H-6: Timestamp consistency

`CompletionValidator`:

- C-2: If PARTIAL, `partialPercent` in 1..99
- C-4: `date <= today`
- C-5: `date >= today - 7 days`

### Done when

- [ ] `CreateHabitUseCase` creates valid habits and rejects invalid ones
- [ ] `GetTodayHabitsUseCase` returns habits filtered by frequency for current day of week
- [ ] `CompleteHabitUseCase` creates completion, enforces one-per-day, returns error on duplicate
- [ ] `UndoCompletionUseCase` deletes completion within time window, rejects after 30s
- [ ] `GetWeeklyStatsUseCase` calculates correct completion rate
- [ ] All use cases return `Result<T>` (never throw)
- [ ] Unit tests for all validation rules
- [ ] Integration tests (with in-memory Room DB) for repository implementations

---

## STEP 5: Today Screen + Habit Completion UI

**Dependencies:** Step 4 (use cases power the screen)
**Priority:** P0
**Docs:** `01-prd-core.md` §UI Requirements + §UC-2 + §UC-3, `10-user-flows.md` §Flow 3 + §Flow 4, `00-project-overview.md` §Design Principles

### What to build

The primary screen users see every day.

### TodayViewModel

| State Field  | Type                  | Purpose                       |
| ------------ | --------------------- | ----------------------------- |
| `habits`     | List<HabitWithStatus> | Grouped by category           |
| `progress`   | Float                 | Overall completion percentage |
| `date`       | LocalDate             | Today's date                  |
| `isLoading`  | Boolean               | Initial load state            |
| `undoAction` | UndoState?            | 30-second undo window         |

### Screen layout (from 01-prd-core.md §Today Screen)

```
┌─────────────────────────────────┐
│  Header: Date + Progress Ring   │
├─────────────────────────────────┤
│  🌅 Morning                    │
│  ┌───────────────────────────┐  │
│  │ ○ Take medication         │  │
│  │   After brushing teeth    │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ ✓ Drink water (done)     │  │
│  └───────────────────────────┘  │
│                                 │
│  ☀️ Afternoon                  │
│  ┌───────────────────────────┐  │
│  │ ○ Review calendar         │  │
│  └───────────────────────────┘  │
│                                 │
│              [+ FAB]            │
└─────────────────────────────────┘
```

### Habit card states (from 01-prd-core.md §Habit Card States)

| State               | Visual                        | Interaction            |
| ------------------- | ----------------------------- | ---------------------- |
| Pending             | Unfilled circle, full opacity | Tap → completion sheet |
| Completed (FULL)    | Checkmark, muted colors       | Tap → undo option      |
| Completed (PARTIAL) | Half-filled indicator         | Tap → detail           |
| Skipped             | Skip icon, different bg       | Tap → detail           |

### Completion bottom sheet

- **✓ Done** — full completion, immediate animation + haptics
- **◐ Partial** — percentage slider (1-99), then confirms
- **⊘ Skip Today** — optional skip reason picker, then confirms

### Non-functional targets (from 01-prd-core.md)

| Metric                            | Target  |
| --------------------------------- | ------- |
| Today screen load                 | < 500ms |
| Completion tap to visual feedback | < 100ms |
| Touch targets                     | ≥ 48dp  |
| Color contrast                    | ≥ 4.5:1 |

### Done when

- [ ] Today screen loads and displays habits grouped by category (MORNING → AFTERNOON → EVENING → ANYTIME)
- [ ] DEPARTURE category habits excluded from Today screen (they're for the Pi dashboard)
- [ ] Progress ring shows accurate completion percentage
- [ ] Tapping habit opens completion bottom sheet with Done/Partial/Skip
- [ ] "Done" creates FULL completion with checkmark animation + haptics
- [ ] "Partial" shows slider, creates PARTIAL completion with percentage
- [ ] "Skip" optionally captures reason, creates SKIPPED completion
- [ ] Completed habits remain visible with visual distinction (not hidden)
- [ ] Undo snackbar appears for 30 seconds after completion
- [ ] Empty state shows illustration + "Add your first habit" when no habits exist
- [ ] "All done" state shows subtle celebration when all habits completed
- [ ] Screen survives rotation and process death (SavedStateHandle)

---

## STEP 6: Create Habit Flow

**Dependencies:** Step 5 (Today screen to navigate back to, FAB triggers creation)
**Priority:** P0
**Docs:** `01-prd-core.md` §UC-1 + §FR-1, `10-user-flows.md` §Flow 2

### What to build

Multi-step habit creation wizard. Three required steps, one optional step.

### Flow steps (from 10-user-flows.md §Flow 2)

| Step        | Required | Screen                                          | Input                      |
| ----------- | -------- | ----------------------------------------------- | -------------------------- |
| 1. Name     | Yes      | Text input with suggestions                     | 1-100 characters           |
| 2. Anchor   | Yes      | Type selector + preset list + custom            | AnchorType + behavior text |
| 3. Category | Yes      | Selector (5 options including DEPARTURE)        | Category enum              |
| 4. Options  | No       | Duration, micro-version, icon, color, frequency | All optional with defaults |

### Anchor selection (from 10-user-flows.md §Anchor Selection)

| Tab                 | Presets                                           | Custom               |
| ------------------- | ------------------------------------------------- | -------------------- |
| After I...          | Wake up, Brush teeth, Have breakfast, Get to work | Free text            |
| Before I...         | Go to bed, Leave for work, Eat dinner             | Free text            |
| When I arrive at... | Home, Work, Gym                                   | Location picker (P2) |
| At a specific time  | —                                                 | Time picker          |

### Category selection (includes DEPARTURE for dashboard items)

| Category     | Label        | Description                |
| ------------ | ------------ | -------------------------- |
| 🌅 Morning   | Morning      | Before noon                |
| ☀️ Afternoon | Afternoon    | 12 PM – 6 PM               |
| 🌙 Evening   | Evening      | After 6 PM                 |
| ⏰ Anytime   | Anytime      | No specific time           |
| 🚪 Departure | Don't Forget | Shown on doorway dashboard |

### Defaults for optional fields

| Field                    | Default          | Source                |
| ------------------------ | ---------------- | --------------------- |
| `estimatedSeconds`       | 300 (5 min)      | 05-domain-model.md    |
| `frequency`              | DAILY            | 05-domain-model.md    |
| `allowPartialCompletion` | true (hardcoded) | 06-invariants.md H-4  |
| `phase`                  | ONBOARD          | 01-prd-core.md FR-1.6 |
| `status`                 | ACTIVE           | Implicit              |
| `lapseThresholdDays`     | 3                | 08-erd.md             |
| `relapseThresholdDays`   | 7                | 08-erd.md             |

### Done when

- [ ] User can create habit in 5 taps: Name → Anchor → Category → (skip options) → Create
- [ ] Anchor is required — cannot proceed without it (H-1)
- [ ] Category is required — cannot proceed without it (H-2)
- [ ] DEPARTURE category available and creates habit visible on dashboard (not Today screen)
- [ ] Habit appears on Today screen immediately after creation (non-DEPARTURE habits)
- [ ] Success animation plays on creation
- [ ] Optional fields (icon, color, duration, micro-version, frequency) work when provided
- [ ] Custom frequency with day-of-week picker works
- [ ] Back navigation preserves entered data within the wizard
- [ ] Validation errors shown inline (not just toast)

---

## STEP 7: Habit Management

**Dependencies:** Step 6 (habits exist to manage)
**Priority:** P0
**Docs:** `01-prd-core.md` §UC-4 + §FR-4, `09-state-machines.md` §Habit Status State Machine, `06-invariants.md` H-6

### What to build

Edit, pause, resume, archive, restore, delete habits. Backdate completions. Habit detail view.

### Habit detail screen

- Habit name, anchor, category, duration
- Calendar view of recent completions (last 30 days)
- Weekly completion rate
- Edit / Pause / Archive actions

### Status transitions (from 09-state-machines.md §Habit Status)

| Action  | From             | To        | Side Effects                             |
| ------- | ---------------- | --------- | ---------------------------------------- |
| Pause   | ACTIVE           | PAUSED    | Set pausedAt, cancel notifications       |
| Resume  | PAUSED           | ACTIVE    | Clear pausedAt, restore notifications    |
| Archive | ACTIVE or PAUSED | ARCHIVED  | Set archivedAt, cancel notifications     |
| Restore | ARCHIVED         | ACTIVE    | Clear archivedAt                         |
| Delete  | ARCHIVED         | (removed) | Confirmation dialog, cascade completions |

### Use cases to implement

| Use Case                    | Input                   | Output             |
| --------------------------- | ----------------------- | ------------------ |
| `EditHabitUseCase`          | habitId, updated fields | Result<Habit>      |
| `PauseHabitUseCase`         | habitId                 | Result<Habit>      |
| `ResumeHabitUseCase`        | habitId                 | Result<Habit>      |
| `ArchiveHabitUseCase`       | habitId                 | Result<Habit>      |
| `RestoreHabitUseCase`       | habitId                 | Result<Habit>      |
| `DeleteHabitUseCase`        | habitId                 | Result<Unit>       |
| `BackdateCompletionUseCase` | habitId, date, type     | Result<Completion> |
| `GetHabitDetailUseCase`     | habitId                 | Flow<HabitDetail>  |

### Backdate rules (from 06-invariants.md C-4, C-5)

- Cannot backdate to future
- Cannot backdate more than 7 days
- Cannot backdate to a date that already has a completion

### Done when

- [ ] Habit detail screen shows habit info + completion calendar + weekly rate
- [ ] Edit form pre-fills all current values, saves correctly
- [ ] Pause/Resume/Archive/Restore transitions work and are validated
- [ ] Delete requires confirmation, removes habit and completions
- [ ] Backdate completion works for past 7 days, rejects outside window
- [ ] Archived habits viewable in a separate list (Settings → Archived Habits)

---

## STEP 8: Firebase Project Setup

**Dependencies:** Step 2 (domain models for schema alignment). Can be done in parallel with Steps 3–7.
**Priority:** P0
**Docs:** `08-erd.md` §Remote Schema (Firestore) — collection structure, document schemas, and security rules already defined

### What to build

Firebase project configuration. Primarily console/config work, not application code.

### 8a. Firebase project + Auth

- Create Firebase project in Firebase console
- Enable Authentication with Email/Password provider
- Optional: enable Google OAuth provider
- Download `google-services.json` into `app/` module
- Add Firebase BOM to Gradle version catalog

### 8b. Firestore collection structure

Firestore is schemaless — collections are created on first write. Verify planned structure from `08-erd.md` §Firestore Collection Structure:

```
users/{userId}
users/{userId}/habits/{habitId}
users/{userId}/completions/{completionId}
users/{userId}/routines/{routineId}
users/{userId}/routines/{routineId}/habits/{id}
users/{userId}/routines/{routineId}/variants/{id}
users/{userId}/routine_executions/{id}
users/{userId}/recovery_sessions/{id}
users/{userId}/preferences/{id}
users/{userId}/deletions/{id}
```

Create a seed script or manual test to verify one document writes to each collection.

### 8c. Firestore security rules

Deploy security rules from `08-erd.md` §Firestore Security Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

Deploy via Firebase CLI (`firebase deploy --only firestore:rules`).

### 8d. Firestore indexes

Create composite indexes for common queries:

| Collection    | Index Fields             | Purpose                        |
| ------------- | ------------------------ | ------------------------------ |
| `completions` | habitId ASC, date DESC   | Recent completions for a habit |
| `habits`      | status ASC, category ASC | Today screen query             |
| `habits`      | status ASC, phase ASC    | Lapse detection                |

Deploy via `firestore.indexes.json`.

### 8e. Firebase Admin SDK setup (for Pi dashboard)

- Generate service account key (JSON) from Firebase console
- Store securely — used by Pi dashboard in Step 10
- Verify Admin SDK can read Firestore collections from a simple Kotlin JVM test

### 8f. Runtime Firebase configuration (self-hosting support)

Enable a single APK to work for both CI-built (pre-configured) and self-hosted (user-configured) scenarios:

- **Conditional google-services plugin**: In `app/build.gradle.kts`, apply the `com.google.gms.google-services` plugin only when `google-services.json` exists in the app module. When absent, the plugin is skipped and Firebase must be initialized manually at runtime.
- **FirebaseConfigStore**: An `EncryptedSharedPreferences`-backed store (AES256_SIV key encryption, AES256_GCM value encryption) that persists the user-provided `google-services.json` content. Located in the setup feature module.
- **FirebaseInitializer**: Handles two initialization paths:
    1. **Auto-init**: When `google-services.json` is present at build time, the google-services plugin handles Firebase configuration automatically.
    2. **Manual-init**: When no build-time config exists, reads stored credentials from `FirebaseConfigStore`, parses them into `FirebaseOptions`, and calls `FirebaseApp.initializeApp()`.
- **Setup screen + ViewModel**: A first-launch screen with instructions, a text field for pasting JSON content, a Configure button, and error/loading states. `FirebaseSetupViewModel` validates input, stores credentials, and triggers initialization.
- **Phased Koin module loading in KairosApp**:
    1. `setupModule` loads immediately in `onCreate()` (provides `FirebaseConfigStore` + `FirebaseSetupViewModel`)
    2. After Firebase initializes successfully, `firebaseModule` + all app modules (`dataModule`, `syncModule`, `authModule`, etc.) are loaded via `loadKoinModules()`
    3. `KairosApp.firebaseReady` (StateFlow<Boolean>) signals navigation to proceed
- **Navigation gating**: Start destination is the setup screen when `firebaseReady = false`, today screen when `firebaseReady = true`
- **SetupModule and FirebaseModule**: Two new Koin modules. `SetupModule` provides the config store and setup ViewModel (always loaded). `FirebaseModule` provides `FirebaseAuth` and `FirebaseFirestore` singletons (loaded dynamically after init).
- **SyncModule/DataModule/AuthModule refactoring**: These modules were refactored to inject `FirebaseAuth` and `FirebaseFirestore` from the Koin graph rather than obtaining them via static `Firebase.auth` / `Firebase.firestore` calls, enabling the phased loading pattern.

### Done when

- [x] Firebase project exists with Auth enabled (email/password)
- [x] `google-services.json` in Android project, app compiles with Firebase BOM
- [x] Security rules deployed — authenticated user can only access own data
- [x] Unauthenticated requests rejected
- [x] Service account key generated for Pi dashboard
- [x] Admin SDK reads work from a simple Kotlin JVM test program
- [x] Composite indexes created for key queries
- [x] google-services plugin conditional (applied only when file exists)
- [x] FirebaseConfigStore encrypts and persists credentials
- [x] FirebaseInitializer supports both auto-init and manual-init paths
- [x] Setup screen renders with instructions, JSON input, Configure button, and error states
- [x] Phased Koin loading: setupModule first, remaining modules after Firebase init
- [x] Navigation gates Firebase-dependent screens behind setup screen
- [x] SyncModule/DataModule/AuthModule inject Firebase instances from Koin graph
- [x] Self-hosted APK (no google-services.json) works end-to-end after setup
- [x] CI-built APK (with google-services.json) skips setup screen entirely

---

## STEP 9: Firestore Sync Integration

**Dependencies:** Step 7 (local app functional), Step 8 (Firebase project configured)
**Priority:** P0
**Docs:** `08-erd.md` §Remote Schema, `06-invariants.md` S-1 (local source of truth)

### What to build

Bidirectional sync between Room (local) and Firestore (cloud). Architecture:

- **All reads come from Room** — UI never queries Firestore directly
- **All writes go to Room first** — then sync to Firestore asynchronously
- **Firestore snapshot listener pushes remote changes to Room**

This preserves offline-first (invariant S-1) while leveraging Firestore's built-in offline cache as a bonus.

### 9a. Firebase Authentication UI

- Login screen: email/password
- Sign-up screen: create account
- Forgot password flow
- Auth state observer → enable/disable sync
- Sign out flow (clear Firebase auth, optionally keep local data)

### 9b. Sync manager

```kotlin
// Conceptual architecture — not literal code
class SyncManager(
    private val firestore: FirebaseFirestore,
    private val habitDao: HabitDao,
    private val auth: FirebaseAuth
) {
    // Push: Room change → Firestore
    fun onLocalHabitChanged(habit: Habit) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users/$userId/habits")
            .document(habit.id)
            .set(habit.toFirestoreMap())
    }

    // Pull: Firestore change → Room
    fun startListening() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users/$userId/habits")
            .addSnapshotListener { snapshot, error ->
                snapshot?.documentChanges?.forEach { change ->
                    val habit = change.document.toHabit()
                    when (change.type) {
                        ADDED, MODIFIED -> habitDao.upsert(habit.toEntity())
                        REMOVED -> habitDao.delete(habit.id)
                    }
                }
            }
    }
}
```

### 9c. Conflict resolution

Last-write-wins based on `updatedAt` timestamp. Single-user, sequential interactions — real conflicts are extremely rare.

### 9d. Sync status UI

| State         | Icon | Text         |
| ------------- | ---- | ------------ |
| Synced        | 🟢   | "Synced"     |
| Syncing       | 🔄   | "Syncing..." |
| Offline       | 🟡   | "Offline"    |
| Error         | 🔴   | "Sync error" |
| Not signed in | ⚫   | "Not synced" |

### 9e. Settings screen: Sync & Account

- Status, last sync time, account email
- Sign Out button
- Delete Account option

### 9f. Initial sync / migration

When user signs in for the first time with existing local data:

1. Check if Firestore has data for this user
2. If empty: push all local data to Firestore
3. If both have data: merge (last-write-wins by updatedAt)
4. Start real-time listener

### Why Room stays as source of truth (not replaced by Firestore cache)

- Room supports complex queries (JOINs for today's habits + completions)
- Room is accessible to WorkManager workers (lapse detection)
- Room data survives Firebase SDK updates/cache clears
- Firestore cache is opaque — can't query it with SQL

### Done when

- [ ] User can sign up, sign in, sign out via Firebase Auth
- [ ] Habits created locally appear in Firestore within 5 seconds (when online)
- [ ] Habits modified on another device appear locally via snapshot listener
- [ ] App works 100% offline — no errors, no degraded features
- [ ] Going offline → changes → back online → changes sync correctly
- [ ] Sync status indicator reflects actual state
- [ ] First-time sign-in with existing local data pushes to Firestore
- [ ] Deletions propagate bidirectionally
- [ ] Completions, routines, recovery sessions all sync (not just habits)

---

## STEP 10: Pi Kiosk Dashboard v1 (Read-Only)

**Dependencies:** Step 9 (data flowing to Firestore so dashboard can read it)
**Priority:** P1
**Docs:** `12-wearos-design.md` (glanceable design principles apply)

> This is a new component not fully spec'd in the existing docs. Requirements derived from user needs: "don't forget to take with you" checklist + today's habits + reminders, displayed on a 15.6" touchscreen on a Pi 5.

### What to build

A **Kotlin Desktop application** using Compose Multiplatform, reading from Firestore via the Firebase Admin SDK.

### 10a. Project setup

- New `dashboard` module using Compose Desktop
- Share `domain` module with Android app (extract to KMP at this point, or copy data classes)
- Firebase Admin SDK dependency (JVM library)
- Load service account key from local config file
- Auto-start on Pi boot (systemd service)
- Fullscreen/kiosk mode (no window decorations, hide cursor)

### 10b. Data access via Firebase Admin SDK

Admin SDK runs as a privileged server client — no user auth needed, reads bypass security rules. For a personal device on your own network this is appropriate.

```kotlin
// Pseudocode
val firestore = FirestoreClient.getFirestore()
val userId = "your-firebase-uid"  // from config file

// Real-time listener — updates within seconds of phone changes
firestore.collection("users/$userId/habits")
    .whereEqualTo("status", "ACTIVE")
    .addSnapshotListener { snapshots, error ->
        val habits = snapshots?.documents?.map { it.toHabit() }
        updateDashboardState(habits)
    }
```

### 10c. Dashboard layout (15.6" landscape)

```
┌─────────────────────────────────────────────────────────┐
│  KAIROS                              Thu, Mar 26 · 7:41 AM │
├──────────────────────┬──────────────────────────────────┤
│  🚪 DON'T FORGET     │  TODAY'S HABITS                  │
│                      │                                  │
│  □ Keys              │  🌅 Morning                      │
│  □ Wallet            │  ○ Take medication                │
│  □ Lunch bag         │  ○ Drink water                    │
│  □ Badge             │  ✓ Stretch (done)                │
│                      │                                  │
│                      │  ☀️ Afternoon                    │
│                      │  ○ Review calendar                │
│                      │                                  │
│                      │  ── Completed: 1/4 (25%) ──      │
├──────────────────────┴──────────────────────────────────┤
│  COMING UP: Take medication (after brushing teeth)      │
│             Review calendar (after lunch)                │
└─────────────────────────────────────────────────────────┘
```

**Left panel:** DEPARTURE category habits — the "Don't Forget" checklist.
**Right panel:** Today's non-departure habits with completion status, grouped by category.
**Bottom bar:** Next 2–3 pending habits with their anchor context.

### 10d. Display considerations

| Concern                   | Solution                                                        |
| ------------------------- | --------------------------------------------------------------- |
| Readability at 3-4 feet   | Minimum 24pt body text, 36pt+ headers                           |
| Nighttime glare           | Dark theme, auto-dim after quiet hours                          |
| Screen burn-in            | Subtle position shift every 10 min, or clock during quiet hours |
| Stale data                | "Last updated: X seconds ago" — alert if > 2 min stale          |
| Firestore connection lost | Show cached last-known state + "⚠ Offline" indicator            |

### 10e. Pi deployment

- Compose Desktop fat JAR or native distribution
- Systemd service for auto-start:

    ```ini
    [Unit]
    Description=Kairos Dashboard
    After=network-online.target

    [Service]
    ExecStart=/usr/bin/java -jar /opt/kairos/dashboard.jar
    Restart=always
    Environment=DISPLAY=:0

    [Install]
    WantedBy=graphical.target
    ```

- Disable screen blanking (`xset s off`, `xset -dpms`)
- Service account key at `/opt/kairos/service-account.json`

### Done when

- [ ] Dashboard launches fullscreen on Pi 5 at boot
- [ ] "Don't Forget" checklist shows DEPARTURE habits from Firestore
- [ ] Today's non-departure habits displayed with status, grouped by category
- [ ] "Coming Up" section shows next 2-3 pending habits with anchors
- [ ] Firestore real-time listener updates within seconds of phone changes
- [ ] Dark theme, readable at 3-4 feet
- [ ] Recovers gracefully if Firestore connection drops (shows cached state)
- [ ] Survives Pi reboot (systemd auto-restart)

---

## STEP 11: Notification System

**Dependencies:** Step 7 (habits exist to notify about)
**Priority:** P0
**Docs:** `11-notification-design.md` (complete spec), `06-invariants.md` D-2 (no punitive messaging)

### What to build

Notification channels, habit reminders, quiet hours, notification action handling.

### 11a. Notification channels (from 11-notification-design.md)

| Channel         | ID                | Importance |
| --------------- | ----------------- | ---------- |
| Habit Reminders | `habit_reminders` | Default    |
| Recovery        | `recovery`        | Low        |
| Routine Timer   | `routine_timer`   | High       |
| System          | `system`          | Default    |

### 11b. Habit reminder notifications

- Schedule via AlarmManager for exact timing
- Content: habit name (title) + anchor behavior (body)
- Actions: Done, Snooze (10 min), Skip
- Action handling via BroadcastReceiver → Use Case → Room (→ Firestore sync)

### 11c. Persistent reminders (optional per habit)

- Follow-up at 15 min, 30 min, 60 min if not acknowledged
- Max 3 follow-ups, cancelled when completed or dismissed

### 11d. Quiet hours

- Default: 10 PM – 7 AM (user configurable)
- Exception: routine timer (user-initiated)

### 11e. Notification settings UI

- Global toggle, per-channel toggles
- Quiet hours pickers
- Default reminder times by category

### Messaging compliance (from 06-invariants.md D-2)

- ❌ "Don't forget!", "You missed X days", "Your streak is at risk"
- ✅ "Time for:", "Ready when you are", "After [anchor]: [habit]"

### Done when

- [ ] Four notification channels created on app startup
- [ ] Reminder fires at configured time with correct content
- [ ] "Done" action creates FULL completion without opening app
- [ ] "Snooze" reschedules by 10 minutes
- [ ] "Skip" creates SKIPPED completion without opening app
- [ ] Quiet hours prevent notifications between configured times
- [ ] Persistent reminders re-notify up to 3 times
- [ ] All notification text passes shame-free review

---

## STEP 12: Recovery System

**Dependencies:** Step 11 (notifications for recovery prompts)
**Priority:** P0
**Docs:** `02-prd-recovery.md` (complete spec), `09-state-machines.md` §Recovery Session, `06-invariants.md` REC-1 through REC-4

### What to build

Background lapse detection, recovery session UI, fresh start flow.

### 12a. Missed completion worker

- WorkManager job at end of day (midnight)
- Active habits due today with no completion → Completion(MISSED)

### 12b. Lapse detection worker

- WorkManager daily between midnight and 6 AM
- At `lapseThresholdDays` (3): phase → LAPSED, create RecoverySession, notify
- At `relapseThresholdDays` (7): LAPSED → RELAPSED, update session type

### 12c. Recovery session UI (from 10-user-flows.md §Flow 6)

Welcome → Blocker Selection (optional) → Action Selection → Confirmation

### 12d. Recovery actions

| Action      | Effect                                  |
| ----------- | --------------------------------------- |
| RESUME      | Phase → FORMING                         |
| SIMPLIFY    | Activate micro-version, phase → FORMING |
| PAUSE       | Status → PAUSED                         |
| ARCHIVE     | Status → ARCHIVED                       |
| FRESH_START | Phase → FORMING, reset progress         |

### 12e. Fresh start prompts

- Monday mornings + 1st of month for PAUSED/LAPSED habits
- Batched notification: "New week. Any habits ready for a comeback?"

### Done when

- [ ] Missed completions created at end of day
- [ ] Lapse detection correctly identifies lapsed habits at threshold
- [ ] Recovery notification with shame-free messaging
- [ ] Recovery session UI flows correctly through all screens
- [ ] All five recovery actions work and update phase/status
- [ ] Phase transitions validated (LAPSE → RELAPSE one-way)
- [ ] One pending session per habit enforced
- [ ] Fresh start prompt fires Monday mornings
- [ ] Recovery sessions sync to Firestore

---

## STEP 13: Routine System

**Dependencies:** Step 12 (technically only needs Step 7, but 12 completes the core app)
**Priority:** P1
**Docs:** `03-prd-routines.md` (complete spec), `09-state-machines.md` §Routine Execution, `06-invariants.md` R-1 through R-5 + E-1 through E-3

### What to build

Routine creation, timer-led runner, completion tracking.

### 13a. Routine creation

- Builder screen: name, category, habit picker, drag-and-drop reorder
- Validation: min 2 habits (R-1), sequential order indices (R-2)

### 13b. Routine runner

- Step indicator, habit name, countdown timer, Done/Skip/Pause controls
- "Up next" preview

### 13c. Timer service

- Foreground service with ongoing notification
- Survives backgrounding and app termination
- Haptic at timer expiry

### 13d. Completion tracking (invariant E-3)

Atomic Room transaction creates:

- `RoutineExecution` with status COMPLETED
- Individual `Completion` per habit (FULL or SKIPPED)

### Key invariants

- R-1: ≥ 2 habits
- E-1: Only one IN_PROGRESS execution at a time
- E-3: Completions created atomically

### Done when

- [ ] Create routine with 2+ habits
- [ ] Runner shows countdown, advances on Done/Skip
- [ ] Timer survives backgrounding (foreground service)
- [ ] Pause/resume preserves timer state
- [ ] Completion creates individual habit completions atomically
- [ ] Only one routine runs at a time
- [ ] Summary screen after completion
- [ ] Routines sync to Firestore

---

## STEP 14: Home Screen Widget

**Dependencies:** Step 5 (Today screen use cases)
**Priority:** P1
**Docs:** `07-architecture.md` §High-Level Architecture

### What to build

Glance widget showing today's progress and pending habits.

```
┌─────────────────────────────┐
│  Kairos • 3/5 done          │
│  ━━━━━━━━━━━━━━━░░░░░  60%  │
│  ○ Take medication          │
│  ○ Drink water              │
│  ✓ Stretch                  │
└─────────────────────────────┘
```

### Done when

- [ ] Widget in picker, shows progress + pending habits (max 5)
- [ ] Tap opens app, updates on data change
- [ ] Handles empty and all-done states

---

## STEP 15: WearOS App

**Dependencies:** Step 13 (routines), Step 11 (notifications)
**Priority:** P1
**Docs:** `12-wearos-design.md` (complete spec)

### 15a. Data Layer sync (Wear Data Layer API, phone ↔ watch)

### 15b. Habit Tile (today's habits, tap to complete)

### 15c. Complication (SHORT_TEXT: "3 left", RANGED_VALUE: progress ring)

### 15d. Watch app screens (habit list, detail, routine runner)

### 15e. Offline behavior (cache + queue on watch)

### Done when

- [ ] Tile shows habits, completion syncs phone → Room → Firestore
- [ ] Complication accurate
- [ ] Routine runner on watch with Done/Skip
- [ ] Works when phone disconnected (queues actions)
- [ ] Phone notifications bridge to watch with actions

---

## STEP 16: Dashboard v2 (Interactive + Schedule)

**Dependencies:** Step 10 (v1 running), Step 9 (sync for write-back)
**Priority:** P2

### 16a. Touch completion

- Tap items → write completion to Firestore via Admin SDK
- Syncs to phone within seconds
- Visual check-off animation

### 16b. Schedule display

- Upcoming habits with estimated times
- Current time prominent (helps time-blind ADHD users)

### 16c. Standby / active mode

- **Active:** full Kairos dashboard
- **Standby:** large clock, date, or blank/dim
- Switchable via local HTTP endpoint: `POST /mode {"mode": "active|standby"}`
- Prepares for ESP32 presence triggering in Step 17

### Done when

- [ ] Touch completion writes to Firestore, appears on phone in < 5s
- [ ] Schedule shows upcoming habits
- [ ] Active/standby mode switchable via HTTP API

---

## STEP 17: ESP32 + Presence Detection (via Home Assistant)

**Dependencies:** Step 16 (dashboard has standby/active mode)
**Priority:** P2

### Architecture

```
ESP32 (mmWave) → HA MQTT Broker → HA Automation → Dashboard HTTP API
                                                 → (optional) Phone notification
```

### 17a. ESP32 firmware

Use **ESPHome** — native HA integration, mmWave component support, OTA updates, no custom C++:

```yaml
# esphome config example
binary_sensor:
    - platform: ld2410
      has_target:
          name: Doorway Presence
```

### 17b. Home Assistant automation

```yaml
automation:
    - alias: "Kairos - Presence Detected"
      trigger:
          - platform: state
            entity_id: binary_sensor.doorway_presence
            to: "on"
      action:
          - service: rest_command.kairos_dashboard_active

    - alias: "Kairos - Presence Cleared"
      trigger:
          - platform: state
            entity_id: binary_sensor.doorway_presence
            to: "off"
            for: { seconds: 30 }
      action:
          - service: rest_command.kairos_dashboard_standby

rest_command:
    kairos_dashboard_active:
        url: "http://pi5.local:8888/mode"
        method: POST
        payload: '{"mode": "active"}'
        content_type: "application/json"
    kairos_dashboard_standby:
        url: "http://pi5.local:8888/mode"
        method: POST
        payload: '{"mode": "standby"}'
        content_type: "application/json"
```

### Done when

- [ ] ESPHome device detects presence, publishes to HA
- [ ] Dashboard switches to active within 2 seconds of detection
- [ ] Returns to standby 30 seconds after presence clears
- [ ] Reliable for 24+ hours without intervention

---

## Integration Testing Milestones

### Milestone 1: First Sync Round-Trip (after Steps 5 + 8 + 9)

- Create habit on phone → appears in Firebase console within 5s
- **This validates the entire pipeline. Do it ASAP.**

### Milestone 2: Dashboard Shows Live Data (after Milestone 1 + Step 10)

- Complete habits on phone → Pi dashboard updates via Firestore listener
- DEPARTURE items visible in "Don't Forget" panel

### Milestone 3: Notification → Completion Loop (after Steps 11 + 9)

- Reminder fires → user taps "Done" → completion syncs → dashboard updates

### Milestone 4: Full Daily Workflow (after Steps 12 + 13)

- Morning routine runner → notifications → lapse detection → recovery

### Milestone 5: Presence-Triggered Dashboard (after Step 17)

- Approach door → mmWave → HA → dashboard activates → walk away → standby

---

## Timeline Mapping

| Step                          | Priority | Est. Effort  | Can Parallel With  |
| ----------------------------- | -------- | ------------ | ------------------ |
| 1. Scaffold + Modules         | P0       | 0.5 day      | —                  |
| 2. Domain Models              | P0       | 1 day        | 8 (Firebase setup) |
| 3. Room DB + DAOs             | P0       | 1.5 days     | 8 (any remaining)  |
| 4. Repositories + Use Cases   | P0       | 1.5 days     | —                  |
| 5. Today Screen + Completion  | P0       | 2 days       | —                  |
| 6. Create Habit Flow          | P0       | 1.5 days     | —                  |
| 7. Habit Management           | P0       | 1.5 days     | —                  |
| 8. Firebase Setup             | P0       | 0.5 day      | Steps 2–5          |
| 9. Firestore Sync             | P0       | 1.5 days     | —                  |
| 10. Pi Dashboard v1           | P1       | 2 days       | Step 11            |
| 11. Notification System       | P0       | 2 days       | Step 10            |
| 12. Recovery System           | P0       | 2 days       | —                  |
| 13. Routine System            | P1       | 3 days       | —                  |
| 14. Home Widget               | P1       | 1 day        | Steps 12–13        |
| 15. WearOS App                | P1       | 3 days       | —                  |
| 16. Dashboard v2 (touch)      | P2       | 1.5 days     | Step 15            |
| 17. ESP32 + Presence (via HA) | P2       | 1.5 days     | Step 16            |
| **Total**                     |          | **~26 days** |                    |

> **5 days saved vs. self-hosted plan** — from eliminating Docker infrastructure (Step 8: 2 days → 0.5 day) and simplifying sync (no custom queue/conflict system).

> **Critical path to working kiosk:** Steps 1 → 2 → 3 → 4 → 5 → 8 → 9 → 10 = ~10 days.

> **Critical path to core phone app:** Steps 1 → 2 → 3 → 4 → 5 → 6 → 7 → 11 → 12 = ~13 days.

---

## Firebase-Specific Considerations

### Cost

At personal/household scale, Kairos stays well within Firebase's free tier (Spark plan):

| Resource          | Free Tier       | Kairos Est. Usage |
| ----------------- | --------------- | ----------------- |
| Firestore reads   | 50K/day         | ~500/day          |
| Firestore writes  | 20K/day         | ~50/day           |
| Firestore storage | 1 GB            | < 10 MB           |
| Auth              | 10K users/month | 1-2 users         |

You'll likely never pay anything.

### Firestore Query Limitations

No JOINs. "Get today's habits with completion status" requires two queries:

1. `users/{uid}/habits` WHERE status == ACTIVE
2. `users/{uid}/completions` WHERE date == today

Joined client-side in the repository layer. Fine — dataset is small (< 20 habits, < 20 completions/day). Room handles the JOIN locally.

### Data Model: Relational vs. Document

| Relational (Room)               | Firestore                                  | Reason                              |
| ------------------------------- | ------------------------------------------ | ----------------------------------- |
| `routine_habits` join table     | Subcollection: `routines/{id}/habits/{id}` | Natural nesting                     |
| Foreign key constraints         | Application-level validation               | Firestore has no FK enforcement     |
| Unique index `(habit_id, date)` | Application-level check                    | Firestore has no unique constraints |

Room keeps relational structure with proper constraints. Firestore uses denormalized documents. Sync layer maps between them.

---

## Design Decisions Summary

| #   | Decision                                              | Rationale                                                              |
| --- | ----------------------------------------------------- | ---------------------------------------------------------------------- |
| 1   | "Don't Forget" items = habits with DEPARTURE category | Gets tracking, sync, recovery for free. No new entity.                 |
| 2   | Room stays as local source of truth                   | JOINs, WorkManager access, survives Firebase SDK changes.              |
| 3   | Firebase Admin SDK for Pi dashboard                   | Pi is trusted device. Admin SDK simpler than second auth flow.         |
| 4   | ESPHome for ESP32 firmware                            | Native HA integration, OTA updates, no custom C++.                     |
| 5   | HA automation triggers dashboard mode                 | Dashboard stays simple. HA is the automation hub.                      |
| 6   | Last-write-wins for sync conflicts                    | Single user, sequential interactions. Real conflicts vanishingly rare. |
| 7   | KMP extraction deferred to Step 10                    | Start pure Android. Extract domain when dashboard needs it.            |
| 8   | Completed habits stay in place (not moved to bottom)  | Stable spatial layout helps ADHD — muscle memory for positions.        |
