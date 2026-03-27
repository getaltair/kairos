# Plan: Step 8 - Firebase Project Setup

## Task Description

Configure Firebase project integration for the Kairos habit tracking app. This includes wiring the Google Services Gradle plugin, creating Firestore security rules, defining composite indexes for key queries, setting up Firebase CLI configuration files, and creating a seed/verification utility to validate Firestore collection structure. The Firebase project (`getaltair-kairos`) already exists with `google-services.json` in place, but the Gradle plugin is not applied and no Firebase CLI config files exist.

## Objective

When complete, the Android app will compile and run with Firebase properly configured. Firestore security rules enforce user-scoped data isolation. Composite indexes support the app's primary query patterns. Firebase CLI config files enable one-command deployment of rules and indexes. A seed utility verifies the planned collection structure works end-to-end.

## Problem Statement

The Firebase project exists and dependencies are declared, but the integration is incomplete:

1. The `google-services` Gradle plugin is declared in `libs.versions.toml` but never applied in `app/build.gradle.kts`, meaning Firebase SDK initialization will fail at runtime
2. No Firestore security rules file exists - the database is either locked or open by default
3. No composite indexes are defined for the app's common query patterns (completions by habit+date, habits by status+category)
4. No Firebase CLI configuration exists for deploying rules/indexes
5. No verification that the planned Firestore collection structure (from `docs/08-erd.md`) actually works

## Solution Approach

This is primarily configuration and infrastructure work, not application logic. The approach:

1. Fix the Gradle plugin gap so Firebase SDK initializes correctly
2. Create Firebase CLI project files (`firebase.json`, `.firebaserc`) pointing to `getaltair-kairos`
3. Author `firestore.rules` with user-scoped security from the spec
4. Author `firestore.indexes.json` with composite indexes for key queries
5. Create a lightweight Kotlin test utility in the `sync` module that writes a test document to each planned collection, verifying schema alignment between domain models and Firestore structure
6. Validate the build compiles cleanly with all Firebase configuration in place

## Relevant Files

Existing files to modify:

- `app/build.gradle.kts` - Apply `google-services` plugin (currently missing)
- `gradle/libs.versions.toml` - Already has Firebase deps, verify completeness
- `sync/build.gradle.kts` - Already has Firebase deps, may need test dependencies

Reference files (read-only context):

- `app/google-services.json` - Firebase project config (project: `getaltair-kairos`)
- `docs/08-erd.md` - Firestore collection structure, document schemas, security rules, type mappings
- `docs/implementation-plan.md` - Step 8 requirements and done criteria
- `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/` - Domain models for schema alignment
- `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/` - Enum types used in Firestore documents
- `build-logic/convention/build.gradle.kts` - Convention plugins (for understanding plugin application)
- `settings.gradle.kts` - Module includes (sync module already included)

### New Files

- `firestore.rules` - Firestore security rules (project root, deployed via Firebase CLI)
- `firestore.indexes.json` - Composite index definitions (project root)
- `firebase.json` - Firebase CLI project configuration (project root)
- `.firebaserc` - Firebase project alias mapping (project root)
- `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollections.kt` - Collection path constants
- `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapper.kt` - Domain-to-Firestore document mappers
- `sync/src/test/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollectionsTest.kt` - Unit tests for collection paths
- `sync/src/test/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapperTest.kt` - Unit tests for mappers

## Implementation Phases

### Phase 1: Foundation (Gradle + Firebase CLI Config)

Fix the `google-services` plugin application in `app/build.gradle.kts`. Create Firebase CLI configuration files (`firebase.json`, `.firebaserc`) pointing to the `getaltair-kairos` project. Create `firestore.rules` and `firestore.indexes.json` from the spec.

### Phase 2: Core Implementation (Firestore Integration Code)

Create `FirestoreCollections` object with type-safe collection/document path builders for all planned collections. Create `FirestoreMapper` utilities that convert between domain entities and Firestore document maps, using the type mappings from `08-erd.md`. These live in the `sync` module which already has Firebase and domain dependencies.

### Phase 3: Integration & Polish (Tests + Build Verification)

Write unit tests for collection path builders and mappers. Verify the full project builds with `./gradlew build`. Ensure ktlint/spotless formatting passes.

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
    - Name: builder-firebase-config
    - Role: Create Firebase CLI config files (firebase.json, .firebaserc, firestore.rules, firestore.indexes.json) and fix the google-services plugin application in app/build.gradle.kts
    - Agent Type: backend-engineer
    - Resume: true

- Specialist
    - Name: builder-sync-module
    - Role: Implement FirestoreCollections path constants and FirestoreMapper domain-to-Firestore conversion utilities in the sync module, plus unit tests
    - Agent Type: backend-engineer
    - Resume: true

- Quality Engineer (Validator)
    - Name: validator
    - Role: Validate completed work against acceptance criteria (read-only inspection mode). Verify build compiles, rules match spec, indexes cover required queries, and mappers handle all domain types.
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

- IMPORTANT: Execute every step in order, top to bottom. Each task maps directly to a `TaskCreate` call.
- Before you start, run `TaskCreate` to create the initial task list that all team members can see and execute.

### 1. Fix Google Services Plugin and Verify Build

- **Task ID**: fix-google-services-plugin
- **Depends On**: none
- **Assigned To**: builder-firebase-config
- **Agent Type**: backend-engineer
- **Parallel**: true (can run alongside task 2)
- Apply `alias(libs.plugins.google.services)` in the `plugins {}` block of `app/build.gradle.kts`
- Read `app/build.gradle.kts` and `gradle/libs.versions.toml` first to understand existing structure
- The plugin alias is `google-services` in libs.versions.toml (id: `com.google.gms.google-services`, version: `4.5.0`)
- Verify `google-services.json` exists at `app/google-services.json`
- Run `./gradlew :app:assembleDebug` to verify the build compiles with the plugin applied
- If the build fails, diagnose and fix (the plugin must be applied AFTER `com.android.application`)

### 2. Create Firebase CLI Configuration Files

- **Task ID**: create-firebase-cli-config
- **Depends On**: none
- **Assigned To**: builder-firebase-config
- **Agent Type**: backend-engineer
- **Parallel**: true (can run alongside task 1 since these are independent files)
- Create `firebase.json` in project root with Firestore rules and indexes file references
- Create `.firebaserc` in project root with project alias `default` pointing to `getaltair-kairos`
- Create `firestore.rules` with security rules from `docs/08-erd.md` section "Firestore Security Rules":
    ```
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
- Create `firestore.indexes.json` with composite indexes from the implementation plan:
    - `completions`: habitId ASC + date DESC (recent completions for a habit)
    - `habits`: status ASC + category ASC (Today screen query)
    - `habits`: status ASC + phase ASC (lapse detection)
- These are subcollection indexes under `users/{userId}/`
- Ensure JSON is valid and follows Firebase index file format

### 3. Create FirestoreCollections Path Constants

- **Task ID**: create-firestore-collections
- **Depends On**: none
- **Assigned To**: builder-sync-module
- **Agent Type**: backend-engineer
- **Parallel**: true (can run alongside tasks 1 and 2)
- Create `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollections.kt`
- Define an object with constants/functions for all Firestore collection paths from `docs/08-erd.md`:
    - `users/{userId}` (root user document)
    - `users/{userId}/habits/{habitId}`
    - `users/{userId}/completions/{completionId}`
    - `users/{userId}/routines/{routineId}`
    - `users/{userId}/routines/{routineId}/habits/{id}` (routine habits subcollection)
    - `users/{userId}/routines/{routineId}/variants/{id}` (routine variants subcollection)
    - `users/{userId}/routine_executions/{id}`
    - `users/{userId}/recovery_sessions/{id}`
    - `users/{userId}/preferences/{id}`
    - `users/{userId}/deletions/{id}`
- Use type-safe path builder functions (e.g., `fun habits(userId: String): String`)
- Keep it simple: pure path string builders, no Firestore SDK dependency needed in these constants
- Read the domain entities to ensure alignment between local models and collection structure

### 4. Create FirestoreMapper Utilities

- **Task ID**: create-firestore-mappers
- **Depends On**: create-firestore-collections
- **Assigned To**: builder-sync-module
- **Agent Type**: backend-engineer
- **Parallel**: false (depends on task 3 for collection path awareness)
- Create `sync/src/main/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapper.kt`
- Implement `toFirestoreMap()` and `fromFirestoreMap()` conversion functions for each domain entity:
    - `Habit` <-> Firestore `HabitDocument` (map)
    - `Completion` <-> Firestore `CompletionDocument` (map)
    - `Routine`, `RoutineHabit`, `RoutineVariant`, `RoutineExecution` (map each)
    - `RecoverySession` <-> Firestore document (map)
    - `UserPreferences` <-> Firestore document (map)
- Follow the type mapping table from `docs/08-erd.md` section "Local to Remote Type Mapping":
    - Unix timestamp (Long) -> Firestore Timestamp
    - Date string -> string (keep YYYY-MM-DD)
    - Time string -> string (keep HH:mm)
    - Enums -> uppercase string name
    - JSON arrays -> Firestore arrays
    - JSON objects -> Firestore maps
    - Boolean Int (0/1) -> Boolean
- Read all domain entity files in `domain/src/main/kotlin/com/getaltair/kairos/domain/entity/` first
- Read all enum files in `domain/src/main/kotlin/com/getaltair/kairos/domain/enums/` first
- Include a `version: Long` field in each document map for future sync conflict resolution

### 5. Write Unit Tests

- **Task ID**: write-unit-tests
- **Depends On**: create-firestore-mappers
- **Assigned To**: builder-sync-module
- **Agent Type**: backend-engineer
- **Parallel**: false (depends on implementation being complete)
- Create `sync/src/test/kotlin/com/getaltair/kairos/sync/firestore/FirestoreCollectionsTest.kt`
    - Test all path builder functions produce correct Firestore paths
    - Test with realistic UUIDs
    - Verify subcollection nesting is correct
- Create `sync/src/test/kotlin/com/getaltair/kairos/sync/firestore/FirestoreMapperTest.kt`
    - Test round-trip conversion: domain entity -> map -> domain entity
    - Test all entity types
    - Test enum serialization/deserialization
    - Test nullable field handling
    - Test timestamp conversion
- Ensure `sync/build.gradle.kts` has the necessary test dependencies (junit, mockk if needed)
- Run `./gradlew :sync:test` to verify all tests pass

### 6. Full Build Verification and Code Quality

- **Task ID**: verify-build-and-quality
- **Depends On**: fix-google-services-plugin, create-firebase-cli-config, write-unit-tests
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false (must run after all implementation tasks complete)
- Run `./gradlew build` to verify the entire project builds without errors
- Run `./gradlew :sync:test` to verify all unit tests pass
- Run `./gradlew spotlessCheck` to verify code formatting
- Verify `firestore.rules` matches the spec in `docs/08-erd.md` exactly
- Verify `firestore.indexes.json` covers all three composite indexes from the implementation plan
- Verify `firebase.json` references the correct rules and indexes files
- Verify `.firebaserc` points to project `getaltair-kairos`
- Verify `FirestoreCollections` covers all 10 collection paths from the ERD
- Verify `FirestoreMapper` handles all domain entity types with correct type mappings
- Verify the `google-services` plugin is applied correctly (after `com.android.application`)
- Check that no secrets or credentials are committed (google-services.json should be in .gitignore for production, but is needed for dev)
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

- [ ] `google-services` Gradle plugin applied in `app/build.gradle.kts` - Firebase SDK initializes at runtime
- [ ] `google-services.json` present in `app/` module, app compiles with Firebase BOM
- [ ] `firestore.rules` deployed-ready with user-scoped security (authenticated user can only access own data under `users/{userId}`)
- [ ] `firestore.indexes.json` contains composite indexes for: completions (habitId+date), habits (status+category), habits (status+phase)
- [ ] `firebase.json` and `.firebaserc` enable `firebase deploy --only firestore` from project root
- [ ] `FirestoreCollections` provides type-safe path builders for all 10 collection paths
- [ ] `FirestoreMapper` converts all domain entities to/from Firestore document maps with correct type mappings
- [ ] Unit tests pass for collection paths and mapper round-trips
- [ ] `./gradlew build` succeeds with zero errors
- [ ] `./gradlew spotlessCheck` passes (code formatting clean)
- [ ] No credentials or secrets inadvertently committed

## Validation Commands

Execute these commands to validate the task is complete:

- `./gradlew :app:assembleDebug` - Verify the app module builds with google-services plugin
- `./gradlew :sync:test` - Run sync module unit tests
- `./gradlew build` - Full project build verification
- `./gradlew spotlessCheck` - Code formatting check
- `cat firestore.rules` - Inspect security rules content
- `cat firestore.indexes.json` - Inspect index definitions
- `cat firebase.json` - Inspect Firebase CLI config
- `cat .firebaserc` - Inspect project alias

## Notes

- **Manual steps required by user**: Firebase console actions (enabling Auth providers, generating service account key for Pi dashboard) cannot be automated by agents. The Firebase project already exists and google-services.json is in place, so 8a is mostly complete. Service account key generation (8e) is deferred to Step 10 when the Pi dashboard is built.
- **Google Services plugin ordering**: The `com.google.gms.google-services` plugin MUST be applied after `com.android.application` in the plugins block. Applying it before will cause a build failure.
- **Firestore indexes are subcollection indexes**: Since habits and completions live under `users/{userId}/`, the index definitions must specify the correct `collectionGroup` or be scoped to the subcollection path.
- **The `sync` module already has Firebase dependencies**: `sync/build.gradle.kts` already declares `firebase-bom`, `firebase-auth`, `firebase-firestore`, `koin`, and `coroutines`. No dependency changes needed there.
- **Domain models use `java.time` types**: The mappers need to convert `Instant` to Firestore `Timestamp` and `LocalDate`/`LocalTime` to strings. Read the domain entities carefully before implementing.
- **DI framework is Koin, not Hilt**: The CLAUDE.md rules file incorrectly references Hilt. This project uses Koin 4.2.0 for dependency injection.
