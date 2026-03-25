# Kairos - Claude Code Context

## Project Overview

Kairos is an ADHD-optimized habit building and tracking system for Android and WearOS. The name derives from the Greek concept of "the opportune moment"—reflecting the app's focus on context-based triggers rather than rigid time-based scheduling.

### Core Philosophy

ADHD brains operate on an **interest-based nervous system** rather than importance-based motivation. Habits may never become fully automatic for ADHD users. Kairos serves as **permanent external scaffolding**, not temporary training wheels.

### Design Principles

| Principle | Description |
|----------|-------------|
| Executive Function Externalization | System does cognitive work—reminders, decisions live in the app |
| Sustainable Imperfection | Design for cycling engagement, not streaks |
| Immediate Dopamine | Every interaction provides instant positive feedback |
| Context Over Time | Event-based triggers ("after brushing teeth") vs time-based |
| Flexible Structure | Structured options with escape hatches |
| Shame-Free Recovery | App never judges—missed days are data |
| Built-in Novelty | Combat interest-based boredom with variation |

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language** | Kotlin 2.3.20 | Primary language |
| **Platform** | Android (API 31+) | Target platform (minSdk 31, compileSdk 36) |
| **UI** | Compose Material3 | Declarative UI |
| **DI** | Hilt 2.59.2 | Dependency injection (configured, not yet wired) |
| **Database** | Room 2.8.4 | Local persistence (not yet implemented) |
| **Navigation** | Navigation Compose 2.9.7 | Screen navigation |
| **Backend** | Firebase (Auth, Firestore) | Cloud sync (not yet configured) |
| **Logging** | Timber 5.0.1 | Logging (configured, not yet used) |
| **Build** | Gradle KTS | Build system with version catalog |
| **Linting** | ktlint 14.0.1, detekt 1.23.8 | Code quality |

## Project Structure

```
kairos/
├── app/              # Android app module (presentation)
│   └── src/main/
│       ├── kotlin/com/getaltair/kairos/
│       │   ├── MainActivity.kt          # Single activity
│       │   ├── KairosApp.kt            # Application class
│       │   └── navigation/
│       │       └── KairosNavGraph.kt   # Navigation shell
│       └── res/                        # Android resources
├── ui/               # Shared UI components and theming
│   └── src/main/kotlin/com/getaltair/kairos/ui/theme/
│       ├── Theme.kt                 # Material3 theme
│       ├── Color.kt                 # Color palette
│       └── Type.kt                  # Typography
├── domain/           # Domain layer (entities, interfaces) — empty, needs Step 2
├── data/             # Data layer (Room, repositories) — empty, needs Step 3
├── core/             # Core utilities, ViewModels — empty, needs Step 4
├── docs/             # Project documentation
└── Context/          # Context Engine artifacts
    ├── Features/     # Feature specifications
    ├── Decisions/    # Architecture Decision Records
    └── Backlog/      # Ideas and Bugs
```

## Implementation Status

### Completed (Step 1: Project Scaffold)
- ✅ Multi-module Gradle setup with version catalog
- ✅ Android app module with Compose Material3
- ✅ Shared UI module with theme
- ✅ Navigation component with empty HomeScreen
- ✅ ktlint and detekt linting configured
- ✅ Hilt and Timber dependencies added
- ✅ Backup and data extraction rules configured

### Next Steps (Per Implementation Plan)
- **Step 2**: Domain models and enumerations (entities: Habit, Completion, Routine, etc.)
- **Step 3**: Room database + DAOs
- **Step 4**: Repositories + Use Cases
- **Step 5**: Today screen
- **Step 6**: Create habit flow
- **Step 7**: Habit management (edit/pause/archive)

See `docs/implementation-plan.md` for complete step-by-step breakdown.

## Development Workflow

### Context Engine

This project uses the Context Engine workflow for systematic feature development:

1. **Plan** — Create feature specs in `Context/Features/`
2. **Research** — Document architecture decisions in `Context/Decisions/`
3. **Implement** — Build following Clean Architecture layers
4. **Review** — Capture findings and improvements

### Build Commands

```bash
# Build all modules
./gradlew build

# Run on connected device/emulator
./gradlew installDebug

# Run linting
./gradlew ktlintCheck
./gradlew detekt

# Auto-format code
./gradlew ktlintFormat

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Module Strategy

The project follows Clean Architecture with these module responsibilities:

| Module | Dependency Direction | Responsibility |
|--------|---------------------|---------------|
| `domain` | None (pure Kotlin) | Entities, value objects, repository interfaces |
| `data` | `domain` | Room entities, DAOs, repository implementations |
| `core` | `domain` | Use cases, ViewModels |
| `ui` | None | Shared Compose components, theming |
| `app` | `domain`, `core`, `ui` | Android app, DI wiring, navigation |

## Important Notes

- **Never shame the user**: Design for sustainable imperfection
- **Streaks are harmful**: Track engagement, not streaks
- **Context > Time**: Event-based triggers work better for ADHD
- **Return is a skill**: Coming back after a break demonstrates resilience

## Documentation

See `docs/` for detailed specifications:
- `00-project-overview.md` — Philosophy and context
- `implementation-plan.md` — Step-by-step build order
- `01-prd-core.md` — Core habit tracking requirements
- `02-prd-recovery.md` — Recovery system requirements
- `03-prd-routines.md` — Routine runner requirements
- `04-prd-sync.md` — Cloud synchronization requirements
- `05-domain-model.md` — Domain entities and relationships
- `06-invariants.md` — Business rules and constraints
- `07-architecture.md` — System architecture and components
- `08-erd.md` — Entity-relationship diagrams
- `09-state-machines.md` — Lifecycle and state diagrams
- `10-user-flows.md` — User journey and interaction flows
- `11-notification-design.md` — Notification system design
- `12-wearos-design.md` — WearOS integration design
