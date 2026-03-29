# System Architecture

## Overview

Kairos follows a **Clean Architecture** pattern with clear separation between layers. The system is designed for offline-first operation with Firebase cloud synchronization.

---

## High-Level Architecture

```mermaid
flowchart TB
    subgraph Clients["Client Applications"]
        Android["📱 Android App<br/>Jetpack Compose"]
        WearOS["⌚ WearOS App<br/>Tiles + Compose"]
        Widget["🔲 Home Widget<br/>Glance"]
        Dashboard["🖥️ Pi Kiosk<br/>Compose Desktop"]
    end

    subgraph AppLayers["Application Layers"]
        subgraph Presentation["Presentation Layer"]
            Screens["Screens"]
            ViewModels["ViewModels"]
            Components["UI Components"]
        end

        subgraph Domain["Domain Layer"]
            UseCases["Use Cases"]
            Models["Domain Models"]
            Interfaces["Repository Interfaces"]
        end

        subgraph Data["Data Layer"]
            Repos["Repository Implementations"]
            LocalDS["Local Data Sources"]
            SyncMgr["Sync Manager"]
        end
    end

    subgraph Infrastructure["Infrastructure"]
        Room["Room Database"]
        DataStore["DataStore Preferences"]
        WorkMgr["WorkManager"]
        Notifications["Notification Service"]
    end

    subgraph External["Firebase (Managed)"]
        FireAuth["Firebase Auth"]
        Firestore["Cloud Firestore"]
    end

    subgraph HomeAutomation["Home Automation"]
        HA["Home Assistant"]
        ESP["ESP32 mmWave"]
    end

    Android --> Presentation
    WearOS --> Presentation
    Widget --> ViewModels

    Screens --> ViewModels
    ViewModels --> UseCases
    UseCases --> Models
    UseCases --> Interfaces

    Interfaces --> Repos
    Repos --> LocalDS
    Repos --> SyncMgr

    LocalDS --> Room
    LocalDS --> DataStore
    SyncMgr --> Firestore
    SyncMgr --> FireAuth

    Dashboard -->|"Admin SDK"| Firestore

    WorkMgr --> Repos
    Notifications --> Android
    Notifications --> WearOS

    ESP -->|"MQTT"| HA
    HA -->|"REST"| Dashboard
```

---

## Layer Responsibilities

### Presentation Layer

```mermaid
flowchart LR
    subgraph Presentation["Presentation Layer"]
        subgraph Screens["Screens"]
            Today["TodayScreen"]
            Create["CreateHabitScreen"]
            Detail["HabitDetailScreen"]
            Routine["RoutineRunnerScreen"]
            Recovery["RecoveryScreen"]
            Settings["SettingsScreen"]
        end

        subgraph ViewModels["ViewModels"]
            TodayVM["TodayViewModel"]
            CreateVM["CreateHabitViewModel"]
            RoutineVM["RoutineViewModel"]
            SettingsVM["SettingsViewModel"]
        end

        subgraph UIState["UI State"]
            State["StateFlow<UiState>"]
            Events["Channel<UiEvent>"]
        end
    end

    Screens --> ViewModels
    ViewModels --> UIState
```

| Component  | Responsibility                             |
| ---------- | ------------------------------------------ |
| Screens    | Compose UI, observes ViewModel state       |
| ViewModels | Manages UI state, invokes use cases        |
| UI State   | Immutable state objects, one-way data flow |
| UI Events  | One-time events (navigation, toasts)       |

### Domain Layer

```mermaid
flowchart TB
    subgraph Domain["Domain Layer"]
        subgraph UseCases["Use Cases"]
            CreateHabit["CreateHabitUseCase"]
            CompleteHabit["CompleteHabitUseCase"]
            GetTodayHabits["GetTodayHabitsUseCase"]
            DetectLapses["DetectLapsesUseCase"]
            StartRoutine["StartRoutineUseCase"]
        end

        subgraph Models["Domain Models"]
            Habit["Habit"]
            Completion["Completion"]
            Routine["Routine"]
            HabitWithStatus["HabitWithStatus"]
            WeeklyReport["WeeklyReport"]
        end

        subgraph RepoInterfaces["Repository Interfaces"]
            HabitRepo["HabitRepository"]
            CompletionRepo["CompletionRepository"]
            RoutineRepo["RoutineRepository"]
        end
    end

    UseCases --> Models
    UseCases --> RepoInterfaces
```

| Component             | Responsibility                                       |
| --------------------- | ---------------------------------------------------- |
| Use Cases             | Single business operation, orchestrates repositories |
| Domain Models         | Pure business entities, no framework dependencies    |
| Repository Interfaces | Contracts for data access                            |

### Data Layer

```mermaid
flowchart TB
    subgraph Data["Data Layer"]
        subgraph RepoImpl["Repository Implementations"]
            HabitRepoImpl["HabitRepositoryImpl"]
            CompletionRepoImpl["CompletionRepositoryImpl"]
            RoutineRepoImpl["RoutineRepositoryImpl"]
        end

        subgraph LocalSources["Local Data Sources"]
            HabitDao["HabitDao"]
            CompletionDao["CompletionDao"]
            RoutineDao["RoutineDao"]
            PrefsStore["PreferencesDataStore"]
        end

        subgraph Sync["Sync Layer"]
            SyncManager["SyncManager"]
            FirestoreDS["FirestoreDataSource"]
            AuthManager["AuthManager"]
        end

        subgraph Mappers["Mappers"]
            EntityMapper["Entity ↔ Domain"]
            FirestoreMapper["Domain ↔ Firestore Map"]
        end
    end

    RepoImpl --> LocalSources
    RepoImpl --> Sync
    RepoImpl --> Mappers
```

| Component           | Responsibility                                                                                                                                                                    |
| ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Repositories        | Read from Room, write to Room, trigger sync                                                                                                                                       |
| DAOs                | Room database access objects                                                                                                                                                      |
| SyncManager         | Push local changes to Firestore, apply remote changes to Room                                                                                                                     |
| FirestoreDataSource | Firestore SDK wrapper (snapshot listeners, writes)                                                                                                                                |
| AuthManager         | Firebase Auth state, sign in/out                                                                                                                                                  |
| FirebaseConfigStore | Persists Firebase project credentials in EncryptedSharedPreferences (AES256). Used by self-hosters who provide credentials at runtime instead of bundling `google-services.json`. |
| FirebaseInitializer | Handles manual `FirebaseApp.initializeApp()` when the google-services plugin is not present. Reads stored config from `FirebaseConfigStore`.                                      |
| Mappers             | Transform between Room entities, domain models, and Firestore documents                                                                                                           |

---

## Module Structure

```mermaid
flowchart TB
    subgraph Modules["Project Modules"]
        App["app<br/>Android application"]
        Wear["wear<br/>WearOS application"]
        Dashboard["dashboard<br/>Pi kiosk (Compose Desktop)"]
        Core["core<br/>Shared business logic"]
        Data["data<br/>Data layer implementation"]
        Domain["domain<br/>Domain models + interfaces"]
        UI["ui<br/>Shared UI components"]
    end

    App --> Core
    App --> UI
    Wear --> Core
    Wear --> UI
    Core --> Data
    Core --> Domain
    Data --> Domain
    Dashboard --> Domain
```

### Module Descriptions

| Module      | Contents                                                                                           | Dependencies       |
| ----------- | -------------------------------------------------------------------------------------------------- | ------------------ |
| `app`       | Android app, DI setup (Koin modules including `firebaseModule` and `setupModule`), navigation      | core, ui           |
| `wear`      | WearOS app, tiles, complications                                                                   | core, ui           |
| `dashboard` | Pi kiosk Compose Desktop app, Admin SDK                                                            | domain             |
| `core`      | Use cases, ViewModels                                                                              | data, domain       |
| `data`      | Repositories, DAOs, entities, SyncManager, Firestore, `FirebaseConfigStore`, `FirebaseInitializer` | domain             |
| `domain`    | Domain models, interfaces                                                                          | None (pure Kotlin) |
| `ui`        | Shared Compose components, theme                                                                   | None               |

---

## Data Flow

### Habit Completion Flow

```mermaid
sequenceDiagram
    participant UI as TodayScreen
    participant VM as TodayViewModel
    participant UC as CompleteHabitUseCase
    participant Repo as HabitRepository
    participant DAO as CompletionDao
    participant Sync as SyncManager
    participant FS as Firestore

    UI->>VM: onCompleteHabit(habitId, type)
    VM->>UC: invoke(habitId, type)

    UC->>UC: Validate completion
    UC->>Repo: createCompletion(completion)

    Repo->>DAO: insert(completionEntity)
    DAO-->>Repo: Success

    Repo->>Sync: onCompletionCreated(completion)
    Repo-->>UC: Completion created
    UC-->>VM: Result.Success

    VM->>VM: Update UI state
    VM-->>UI: New state emitted

    Note over Sync,FS: Async — does not block UI
    Sync->>FS: document.set(completion.toMap())
    FS-->>Sync: Ack (or queued if offline)
```

### Sync Data Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant SM as SyncManager
    participant FS as Firestore
    participant Room as Room DB

    Note over App,Room: Push Flow (Local → Firestore)

    App->>Room: Write entity
    Room-->>SM: Notify change
    SM->>FS: document.set(entity.toMap())
    Note over FS: Queued locally if offline
    Note over FS: Pushed automatically on reconnect

    Note over App,Room: Pull Flow (Firestore → Local)

    FS-->>SM: Snapshot listener fires (remote change)
    SM->>SM: Map Firestore doc to entity
    SM->>Room: upsert(entity)
    Room-->>App: Flow emits updated data
    App->>App: UI updates automatically
```

### Routine Execution Flow

```mermaid
sequenceDiagram
    participant UI as RoutineRunner
    participant VM as RoutineViewModel
    participant UC as RunRoutineUseCase
    participant Timer as TimerService
    participant Repo as RoutineRepository

    UI->>VM: startRoutine(routineId)
    VM->>UC: start(routineId)
    UC->>Repo: createExecution(routineId)
    Repo-->>UC: Execution created
    UC->>Timer: startTimer(firstHabit.duration)
    UC-->>VM: Execution started

    loop Each Habit
        Timer-->>VM: onTick(remaining)
        VM-->>UI: Update timer display

        alt Timer expires
            Timer-->>VM: onComplete()
            VM->>UI: Show "Done?" prompt
        else User taps Done
            UI->>VM: onHabitDone()
        end

        VM->>UC: completeStep(stepIndex)
        UC->>Repo: recordStepCompletion()
        UC->>Timer: startTimer(nextHabit.duration)
    end

    UC->>Repo: completeExecution()
    UC->>Repo: createHabitCompletions()
    UC-->>VM: Routine complete
    VM-->>UI: Show summary
```

---

## Background Processing

```mermaid
flowchart TB
    subgraph Workers["WorkManager Workers"]
        Lapse["LapseDetectionWorker<br/>Daily at midnight"]
        Fresh["FreshStartWorker<br/>Monday + 1st of month"]
        Missed["MissedCompletionWorker<br/>Daily at midnight"]
        Reminder["ReminderWorker<br/>Per habit schedule"]
    end

    subgraph Triggers["Triggers"]
        Time["Time-based"]
    end

    subgraph Actions["Actions"]
        DetectLapse["Detect lapsed habits"]
        CreateRecovery["Create recovery sessions"]
        CreateMissed["Create MISSED completions"]
        Notify["Send notifications"]
    end

    Time --> Lapse
    Time --> Fresh
    Time --> Missed
    Time --> Reminder

    Missed --> CreateMissed
    Lapse --> DetectLapse --> CreateRecovery --> Notify
    Fresh --> Notify
```

### Worker Schedule

| Worker                 | Schedule                         | Constraints     |
| ---------------------- | -------------------------------- | --------------- |
| MissedCompletionWorker | Daily, midnight                  | None            |
| LapseDetectionWorker   | Daily, 00:00-06:00               | Battery not low |
| FreshStartWorker       | Monday 06:00, 1st of month 06:00 | None            |
| ReminderWorker         | Per-habit schedule               | None            |

> **Note:** There is no SyncWorker. Firestore SDK handles sync automatically via snapshot listeners and its built-in offline queue. No periodic push/pull needed.

---

## Notification Architecture

```mermaid
flowchart TB
    subgraph Sources["Notification Sources"]
        Reminder["Habit Reminders"]
        Recovery["Recovery Prompts"]
        Fresh["Fresh Start"]
        Routine["Routine Timer"]
        System["Sync Status"]
    end

    subgraph Channels["Notification Channels"]
        ReminderCh["habit_reminders<br/>Normal priority"]
        RecoveryCh["recovery<br/>Low priority"]
        RoutineCh["routine_timer<br/>High priority"]
        SystemCh["system<br/>Default priority"]
    end

    subgraph Manager["NotificationManager"]
        Builder["NotificationBuilder"]
        Scheduler["AlarmManager"]
        Handler["Action Handler"]
    end

    Reminder --> ReminderCh
    Recovery --> RecoveryCh
    Fresh --> RecoveryCh
    Routine --> RoutineCh
    System --> SystemCh

    ReminderCh --> Manager
    RecoveryCh --> Manager
    RoutineCh --> Manager
    SystemCh --> Manager
```

### Notification Actions

| Notification Type | Actions                |
| ----------------- | ---------------------- |
| Habit Reminder    | Complete, Snooze, Skip |
| Recovery Prompt   | Open Recovery, Dismiss |
| Fresh Start       | View Habits, Dismiss   |
| Routine Timer     | Done, Skip, Pause      |

---

## WearOS Architecture

```mermaid
flowchart TB
    subgraph Phone["Phone App"]
        PhoneDB["Room Database"]
        PhoneSync["SyncManager"]
    end

    subgraph Watch["WearOS App"]
        subgraph WearUI["UI"]
            TileService["HabitTileService"]
            Complication["HabitComplication"]
            WearScreens["Wear Compose Screens"]
        end

        subgraph WearData["Data"]
            WearCache["Local Cache"]
            DataLayer["Data Layer API"]
        end
    end

    subgraph Cloud["Firebase"]
        Firestore["Cloud Firestore"]
    end

    PhoneDB <--> DataLayer
    DataLayer <--> WearCache

    PhoneSync <--> Firestore

    WearCache --> TileService
    WearCache --> Complication
    WearCache --> WearScreens

    Note over DataLayer: Wear Data Layer API<br/>syncs subset of data<br/>between phone and watch
```

### Phone-Watch Sync Strategy

| Data Type           | Sync Strategy               |
| ------------------- | --------------------------- |
| Today's Habits      | Full sync via Data Layer    |
| Completions (today) | Full sync via Data Layer    |
| Historical Data     | Not synced to watch         |
| Routines            | Active routine only         |
| Settings            | Subset (notification prefs) |

---

## Pi Kiosk Dashboard Architecture

```mermaid
flowchart TB
    subgraph Dashboard["Pi Kiosk (Compose Desktop)"]
        subgraph DashUI["UI"]
            DeparturePanel["Departure Checklist"]
            HabitPanel["Today's Habits"]
            UpNextPanel["Coming Up"]
        end

        subgraph DashData["Data"]
            AdminSDK["Firebase Admin SDK"]
            StateHolder["Dashboard State"]
        end

        subgraph DashControl["Control"]
            HTTPServer["HTTP Server<br/>Mode switching"]
        end
    end

    subgraph External["External"]
        Firestore["Cloud Firestore"]
        HA["Home Assistant"]
    end

    AdminSDK -->|"Snapshot listeners"| Firestore
    AdminSDK --> StateHolder
    StateHolder --> DashUI
    HA -->|"REST: /mode"| HTTPServer
    HTTPServer --> StateHolder
```

The dashboard uses the Firebase Admin SDK (JVM) for privileged read access to Firestore. It does not use Firebase Auth — the service account key provides server-level access appropriate for a trusted device on the home network.

---

## Dependency Injection

```mermaid
flowchart TB
    subgraph Modules["Koin Modules"]
        SetupModule["SetupModule<br/>Always loaded"]
        FirebaseModule["FirebaseModule<br/>Loaded after Firebase init"]
        DatabaseModule["DatabaseModule<br/>Loaded after Firebase init"]
        AppModule["AppModule<br/>Loaded after Firebase init"]
        WorkerModule["WorkerModule<br/>Loaded after Firebase init"]
    end

    subgraph Provides["Provided Dependencies"]
        ConfigStore["FirebaseConfigStore"]
        SetupVM["FirebaseSetupViewModel"]
        FireAuth["FirebaseAuth"]
        FireFS["FirebaseFirestore"]
        DB["KairosDatabase"]
        DAOs["All DAOs"]
        Repos["All Repositories"]
        UseCases["All Use Cases"]
        SyncMgr["SyncManager"]
        WorkMgr["WorkManager"]
    end

    SetupModule --> ConfigStore
    SetupModule --> SetupVM
    FirebaseModule --> FireAuth
    FirebaseModule --> FireFS
    FirebaseModule --> SyncMgr
    DatabaseModule --> DB --> DAOs
    AppModule --> Repos
    AppModule --> UseCases
    WorkerModule --> WorkMgr
```

### Phased Koin Initialization

Koin modules load in two phases to support both standard builds (with `google-services.json` baked in) and self-hosted builds (where the user provides Firebase credentials at runtime).

**Phase 1 -- Always loaded on startup:**

`KairosApp.onCreate()` calls `startKoin` with only `setupModule`. This module provides `FirebaseConfigStore` (encrypted credential storage) and `FirebaseSetupViewModel` (setup screen logic). These are available immediately regardless of Firebase state.

**Phase 2 -- Conditional, after Firebase is available:**

Once Firebase is initialized, `KairosApp` calls `loadKoinModules()` with `firebaseModule` and all 11 remaining app modules (`dataModule`, `syncModule`, `authModule`, `domainModule`, etc.). The `firebaseModule` must load first because `dataModule`, `syncModule`, and `authModule` inject `FirebaseAuth` and `FirebaseFirestore` from the Koin graph.

**Three startup paths determine when Phase 2 runs:**

| Path          | Condition                                    | Behavior                                                                                                                                                                                                    |
| ------------- | -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Auto-init     | `google-services.json` present at build time | Firebase auto-initializes via the google-services plugin. Phase 2 runs immediately in `onCreate()`.                                                                                                         |
| Stored config | Returning self-hoster with saved credentials | `FirebaseInitializer` reads credentials from `FirebaseConfigStore` and calls `FirebaseApp.initializeApp()`. Phase 2 runs immediately in `onCreate()`.                                                       |
| Fresh setup   | New self-hoster, no config yet               | Phase 2 is deferred. The app shows the Firebase Setup Screen. After the user pastes valid `google-services.json` content, the setup flow initializes Firebase and triggers Phase 2 via `loadKoinModules()`. |

`KairosApp` exposes a `firebaseReady: StateFlow<Boolean>` that the navigation layer observes. When `false`, the nav graph routes to the setup screen. When `true`, it routes to the today screen.

---

## Firebase Configuration & Self-Hosting

Kairos supports two paths for Firebase project configuration: build-time (standard) and runtime (self-hosted).

### Build-Time Path

When `app/google-services.json` exists, the `com.google.gms.google-services` Gradle plugin applies automatically and Firebase initializes via the default `FirebaseApp` mechanism on app startup. This is the path for CI builds and development environments that bundle the project's own Firebase credentials.

### Runtime Path (Self-Hosting)

For self-hosted builds distributed without `google-services.json` (e.g., GitHub Releases APKs), Firebase credentials are provided by the user at first launch:

1. The google-services Gradle plugin is conditionally applied in `app/build.gradle.kts` -- it is only added when `google-services.json` exists in the app module directory.
2. On first launch without bundled credentials, the app shows the **Firebase Setup Screen** before any other content.
3. The user pastes the contents of a `google-services.json` file obtained from their own Firebase project.
4. `FirebaseSetupViewModel` parses the JSON, extracting `project_id`, `mobilesdk_app_id`, `current_key` (API key), `storage_bucket`, and `project_number` (GCM sender ID).
5. Validated credentials are saved to `FirebaseConfigStore` (EncryptedSharedPreferences with AES256 encryption).
6. `FirebaseInitializer` constructs `FirebaseOptions` from the stored values and calls `FirebaseApp.initializeApp()`.
7. Phase 2 Koin modules load, and the app navigates to the today screen.

### Conditional google-services Plugin

```kotlin
// app/build.gradle.kts
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
```

When the plugin is absent, no `google-services.json` processing occurs at build time. The app compiles and runs normally but defers Firebase initialization to runtime.

### Navigation Gating

`KairosNavGraph` uses `KairosApp.firebaseReady` to choose the start destination:

- `firebaseReady = false` -- start destination is `"firebase_setup"`
- `firebaseReady = true` -- start destination is `"today"`

After successful setup, the navigation clears the back stack so the user cannot navigate back to the setup screen.

---

## Error Handling Strategy

```mermaid
flowchart TB
    subgraph Errors["Error Types"]
        Validation["ValidationException<br/>Invalid input"]
        NotFound["NotFoundException<br/>Entity missing"]
        Network["NetworkException<br/>Connectivity"]
        Auth["AuthException<br/>Authentication"]
    end

    subgraph Handling["Handling Strategy"]
        UseCaseH["Use Case Layer<br/>Catch, wrap in Result"]
        ViewModelH["ViewModel Layer<br/>Map to UI state"]
        UIH["UI Layer<br/>Display message"]
    end

    subgraph Recovery["Recovery Actions"]
        Retry["Retry operation"]
        Redirect["Navigate to auth"]
        Show["Show error message"]
        Log["Log for debugging"]
    end

    Errors --> UseCaseH --> ViewModelH --> UIH

    Validation --> Show
    NotFound --> Show
    Network --> Retry
    Auth --> Redirect
```

### Result Type Pattern

```mermaid
classDiagram
    class Result~T~ {
        <<sealed>>
    }

    class Success~T~ {
        +data: T
    }

    class Error {
        +exception: Exception
        +message: String
    }

    class Loading {
    }

    Result <|-- Success
    Result <|-- Error
    Result <|-- Loading
```

---

## Security Architecture

```mermaid
flowchart TB
    subgraph Client["Client Security"]
        Proguard["ProGuard/R8<br/>Code obfuscation"]
        LocalDB["Room DB<br/>On-device only"]
    end

    subgraph Transport["Transport Security"]
        TLS["TLS<br/>All Firebase connections"]
    end

    subgraph Backend["Firebase Security"]
        Auth["Firebase Auth<br/>Identity verification"]
        Rules["Firestore Rules<br/>Per-user access control"]
        Isolation["User data isolation<br/>auth.uid == userId"]
    end

    subgraph Dashboard["Dashboard Security"]
        ServiceKey["Service Account Key<br/>Local file on Pi"]
        Network["Home network only"]
    end

    Client --> Transport --> Backend
    Dashboard --> Transport --> Backend
```

### Security Rules

| Rule             | Implementation                                   |
| ---------------- | ------------------------------------------------ |
| Token management | Firebase SDK (automatic refresh, secure storage) |
| Network          | TLS enforced by Firebase                         |
| Data isolation   | Firestore rules: `request.auth.uid == userId`    |
| PII in logs      | Prohibited, enforced by lint                     |
| Dashboard auth   | Service account key on trusted device            |

---

## Performance Considerations

### Database Optimization

| Optimization             | Purpose                      |
| ------------------------ | ---------------------------- |
| Indices on query columns | Fast habit/completion lookup |
| Paging for history       | Memory efficiency            |
| Precomputed views        | Today screen performance     |

### UI Optimization

| Optimization            | Purpose                     |
| ----------------------- | --------------------------- |
| Lazy composition        | Only render visible items   |
| Stable keys             | Minimize recomposition      |
| Async image loading     | Smooth scrolling            |
| Remember/derivedStateOf | Avoid redundant computation |

### Sync Optimization

| Optimization                     | Purpose                            |
| -------------------------------- | ---------------------------------- |
| Snapshot listeners (not polling) | Real-time with minimal reads       |
| Scoped listeners                 | Only listen to active collections  |
| Firestore offline cache          | Reduces network reads on app start |
| Batch writes                     | Group related Firestore updates    |

---

## Deployment Architecture

```mermaid
flowchart LR
    subgraph Development["Development"]
        Local["Local builds"]
        Debug["Debug variants"]
        Emulator["Firebase Emulator Suite"]
    end

    subgraph CI["CI/CD"]
        GitHub["GitHub Actions"]
        Build["Build + Test"]
        Sign["Sign APK/AAB"]
    end

    subgraph Distribution["Distribution"]
        PlayStore["Google Play Store"]
        Internal["Internal testing"]
        GitHub2["GitHub Releases"]
    end

    Development --> CI --> Distribution
```

### Build Variants

| Variant | Firebase                | Logging    | Debuggable |
| ------- | ----------------------- | ---------- | ---------- |
| debug   | Emulator or dev project | Verbose    | Yes        |
| release | Production project      | Error only | No         |

### Firebase Emulator Suite

For local development, use the Firebase Emulator Suite to avoid hitting production:

- Auth emulator (no real email sending)
- Firestore emulator (local data, no charges)
- Configured via `firebase.json` and detected automatically by Android SDK
