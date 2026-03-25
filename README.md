# Kairos

ADHD-optimized habit building and tracking system for Android and WearOS.

> *Kairos* derives from the Greek concept of "the opportune moment"
> It reflects the app's focus on context-based triggers rather than rigid time-based scheduling.

## Philosophy

Traditional habit apps fail ADHD users because they assume neurotypical motivation patterns. Kairos is built on a different foundation:

| Traditional Apps | Kairos |
|----------------|--------|
| Streak-based motivation | Novelty injection for maintenance |
| Binary (done/not done) | Flexible (partial counts) |
| Time-based triggers ("at 7 AM") | Context-based ("after brushing teeth") |
| Achievement/failure framing | Shame-free, neutral language |
| Gamification (points, badges) | None—intrinsic motivation only |

### Core Insight

ADHD brains operate on an **interest-based nervous system**. Habits may never become fully automatic. Kairos serves as **permanent external scaffolding**, not temporary training wheels.

## Features

- **Context-based triggers** — Event-based rather than time-based (e.g., "after brushing teeth" vs "at 7:00 AM")
- **Flexible completion** — Partial credit counts; perfection not required
- **Shame-free recovery** — Built-in recovery system for returning after lapses
- **Routine runner** — Guided sequence of habits for structured moments
- **WearOS integration** — Quick interactions without reaching for phone
- **Cross-device sync** — Works across Android phone, WearOS watch, and future desktop companion

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3.20 |
| Platform | Android (API 31+) |
| UI | Compose Material3 |
| DI | Hilt 2.59.2 |
| Database | Room 2.8.4 |
| Backend | Firebase (Auth, Firestore) |
| Build | Gradle KTS |
| Linting | ktlint, detekt |

## Project Structure

```
kairos/
├── app/              # Android app module
├── ui/               # Shared UI components and theming
├── domain/           # Domain models and interfaces
├── data/             # Data layer (Room, repositories)
├── core/             # Use cases and ViewModels
├── docs/             # Detailed specifications
└── Context/          # Context Engine artifacts
```

See `CLAUDE.md` for detailed development context.

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2024.1.1) or later
- JDK 21
- Android SDK 36

### Build

```bash
# Clone the repository
git clone https://github.com/getaltair/kairos.git
cd kairos

# Build all modules
./gradlew build

# Install debug build on connected device
./gradlew installDebug
```

### Run

```bash
# On connected device/emulator
./gradlew installDebug
adb shell am start -n com.getaltair.kairos/.MainActivity
```

### Linting

```bash
# Check code style
./gradlew ktlintCheck
./gradlew detekt

# Auto-format
./gradlew ktlintFormat
```

### Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Development Status

This is an early-stage project. See `docs/implementation-plan.md` for the complete roadmap.

### Completed

- ✅ Project scaffold with multi-module Gradle setup
- ✅ Compose Material3 theming
- ✅ Navigation component setup
- ✅ Linting configuration (ktlint, detekt)

### In Progress

- 🚧 Domain models and enumerations
- 🚧 Room database schema
- 🚧 Repository layer

### Planned

- Habit tracking UI and logic
- Recovery system
- Routine runner
- Notification system
- WearOS companion
- Firestore sync

## Documentation

Detailed specifications are available in `docs/`:

| Document | Description |
|----------|-------------|
| `00-project-overview.md` | Philosophy and context |
| `implementation-plan.md` | Step-by-step build order |
| `01-prd-core.md` | Core habit tracking requirements |
| `02-prd-recovery.md` | Recovery system requirements |
| `03-prd-routines.md` | Routine runner requirements |
| `04-prd-sync.md` | Cloud synchronization requirements |
| `05-domain-model.md` | Domain entities and relationships |
| `06-invariants.md` | Business rules and constraints |
| `07-architecture.md` | System architecture and components |
| `08-erd.md` | Entity-relationship diagrams |
| `09-state-machines.md` | Lifecycle and state diagrams |
| `10-user-flows.md` | User journey and interaction flows |
| `11-notification-design.md` | Notification system design |
| `12-wearos-design.md` | WearOS integration design |

## Contributing

This project is currently in active early development by the maintainer. Issues and discussions are welcome.

## License

AGPL v3

---

Built with ☕ for ADHD brains, by an ADHD brain.
