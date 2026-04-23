# Kairos: Project Overview

## Executive Summary

Kairos is an ADHD-optimized habit building and tracking system for Android and WearOS. The name derives from the Greek concept of "the opportune moment"—reflecting the app's focus on context-based triggers rather than rigid time-based scheduling.

### Core Insight

> ADHD brains operate on an **interest-based nervous system** rather than the importance-based motivation neurotypical habit advice assumes.

This means habits may never become fully automatic for ADHD users. Kairos serves as **permanent external scaffolding**, not temporary training wheels.

---

## Philosophy

### The Problem with Existing Solutions

```mermaid
mindmap
  root((Traditional Habit Apps))
    Streak Focus
      Broken streaks trigger shame
      All-or-nothing mentality
      Punishment for missed days
    Gamification
      Points create external dependency
      Badges lose meaning quickly
      Leaderboards cause comparison anxiety
    Time-Based Triggers
      Assumes consistent schedules
      Ignores time blindness
      Fails when routine varies
    Binary Completion
      Done or not done
      No partial credit
      Perfectionism paralysis
```

### Kairos Differentiators

| Differentiator | Traditional Apps            | Kairos Approach                        |
| -------------- | --------------------------- | -------------------------------------- |
| Recovery       | Afterthought or absent      | First-class feature                    |
| Completion     | Binary (done/not done)      | Flexible (partial counts)              |
| Engagement     | Streak-based motivation     | Novelty injection for maintenance      |
| Messaging      | Achievement/failure framing | Shame-free, neutral language           |
| Triggers       | Time-based ("at 7 AM")      | Context-based ("after brushing teeth") |
| Gamification   | Points, badges, streaks     | None—intrinsic motivation only         |

---

## Design Principles

```mermaid
flowchart TB
    subgraph Principles["Core Design Principles"]
        P1["Executive Function<br/>Externalization"]
        P2["Sustainable<br/>Imperfection"]
        P3["Immediate<br/>Dopamine"]
        P4["Context Over<br/>Time"]
        P5["Flexible<br/>Structure"]
        P6["Shame-Free<br/>Recovery"]
        P7["Built-in<br/>Novelty"]
    end

    P1 --> E1["System does cognitive work<br/>Reminders, decisions, structure<br/>live in the app"]

    P2 --> E2["Expect cycling between<br/>engagement and disengagement<br/>Design for the return"]

    P3 --> E3["Every interaction provides<br/>instant positive feedback<br/>No delayed gratification"]

    P4 --> E4["Event-based triggers work better<br/>than time-based due to<br/>time blindness"]

    P5 --> E5["Rigid routines suffocate<br/>No structure causes chaos<br/>Sweet spot: structured options"]

    P6 --> E6["App never judges<br/>Missed days are data<br/>Coming back IS the skill"]

    P7 --> E7["Combat interest-based<br/>nervous system boredom<br/>with variation and refresh"]
```

### Principle Details

#### 1. Executive Function Externalization

The system does cognitive work so the user doesn't have to. Reminders, decisions, and structure live in the app, not the user's head.

#### 2. Sustainable Imperfection

Expect cycling between engagement and disengagement. Design for the return, not the streak.

#### 3. Immediate Dopamine

Every interaction provides instant positive feedback. No delayed gratification requirements.

#### 4. Context Over Time

Event-based triggers ("after brushing teeth") work better than time-based ("at 7:00 AM") due to time blindness.

#### 5. Flexible Structure

Rigid routines suffocate; no structure causes chaos. The sweet spot: structured options with escape hatches.

#### 6. Shame-Free Recovery

The app never judges. Missed days are data, not failures. Coming back IS the skill being developed.

#### 7. Built-in Novelty

Combat the interest-based nervous system's boredom with variation, rotation, and refresh mechanisms.

---

## Target Users

```mermaid
mindmap
  root((Kairos Users))
    Primary
      Adults with ADHD
      Self-diagnosed or formal
      Struggled with habit apps
      Want structure without rigidity
    Secondary
      ADHD-adjacent traits
      Executive function challenges
      Autism spectrum
      Depression/anxiety
    Excluded
      Neurotypical optimization seekers
      Gamification enthusiasts
      Streak chasers
```

### User Characteristics

| Characteristic            | Implication for Design                |
| ------------------------- | ------------------------------------- |
| Time blindness            | Context triggers over time triggers   |
| Working memory deficits   | Externalize all reminders             |
| Rejection sensitivity     | Zero shame, zero judgment             |
| Interest-based motivation | Novelty injection, immediate feedback |
| Perfectionism tendency    | Partial completion always valid       |
| Inconsistent energy       | Energy tracking, flexible scheduling  |

---

## Platform Strategy

```mermaid
flowchart LR
    subgraph Devices["Supported Platforms"]
        Phone["📱 Android Phone<br/>Primary device"]
        Watch["⌚ WearOS<br/>Quick interactions"]
        Widget["🔲 Home Widget<br/>At-a-glance status"]
        Kiosk["🖥️ Pi Kiosk<br/>Doorway dashboard"]
    end

    subgraph Integration["Integrations"]
        HA["🏠 Home Assistant<br/>Presence detection"]
        ESP["🔘 ESP32 mmWave<br/>Presence sensor"]
    end

    subgraph Future["Future Platforms"]
        Desktop["💻 Desktop<br/>Work habits"]
    end

    Phone <--> Watch
    Phone <--> Widget
    Phone <--> Kiosk
    ESP --> HA
    HA --> Kiosk
    Phone -.-> Desktop
```

### Platform Rationale

**Android + WearOS First**

- Personal devices, always available
- WearOS reduces phone dependency (phone often causes distraction)
- Widget provides passive awareness

**Pi Kiosk Dashboard**

- 15.6" touchscreen at doorway powered by Pi 5
- "Don't Forget" departure checklist (keys, wallet, lunch, etc.)
- Today's habits at a glance — no phone required
- Compose Desktop app sharing domain models with Android
- Reads from Firestore via Admin SDK (real-time updates)
- Presence-triggered via ESP32 mmWave sensor → Home Assistant

**Home Assistant + ESP32**

- mmWave presence sensor detects approach/departure at doorway
- HA automation switches dashboard between standby and active mode
- Uses HA's built-in MQTT broker — no standalone MQTT infrastructure

**Future Expansion**

- Desktop companion for work-related habits

**Self-Hosting**

Kairos supports self-hosting via runtime Firebase configuration. Self-hosters create their own Firebase project and paste the `google-services.json` credentials into the app at first launch. This is a one-time setup step; credentials are encrypted and persist across app restarts. CI-built APKs distributed to the developer's friends-and-family group come pre-configured with Firebase credentials injected via secrets, so no setup screen is shown.

---

## Success Metrics

### What We Track

| Metric                          | Purpose              | Display to User? |
| ------------------------------- | -------------------- | ---------------- |
| Completion rate                 | Overall engagement   | Yes (weekly %)   |
| Habits maintained 30+ days      | Long-term success    | Yes (count)      |
| Recovery success rate           | Return effectiveness | Yes (%)          |
| Time to complete after reminder | Notification tuning  | No               |
| Blocker distribution            | Pattern analysis     | No               |
| Feature usage                   | Product decisions    | No               |

### What We Explicitly Don't Track

```mermaid
flowchart TB
    subgraph Forbidden["Never Track or Display"]
        S["Longest Streak"]
        M["Missed Day Counts"]
        C["User Comparisons"]
        L["Leaderboards"]
        R["Rankings"]
    end

    S --> Reason1["Triggers shame when broken"]
    M --> Reason2["Emphasizes failure over progress"]
    C --> Reason3["Not helpful for ADHD users"]
    L --> Reason4["Creates unhealthy competition"]
    R --> Reason5["External validation dependency"]
```

---

## Document Index

This project documentation consists of the following documents:

| Document                               | Description                                |
| -------------------------------------- | ------------------------------------------ |
| `00-project-overview.md`               | This document—philosophy and context       |
| `01-PRD-001-core.md`                   | Core habit tracking requirements           |
| `01-PRD-002-recovery.md`               | Recovery system requirements               |
| `01-PRD-003-routines.md`               | Routine runner requirements                |
| `01-PRD-004-sync.md`                   | Cloud synchronization requirements         |
| `02-domain-model.md`                   | Domain entities and relationships          |
| `03-invariants.md`                     | Business rules and constraints             |
| `04-architecture.md`                   | System architecture and components         |
| `05-erd.md`                            | Entity-relationship diagrams               |
| `06-state-machines.md`                 | Lifecycle and state diagrams               |
| `07-user-flows.md`                     | User journey and interaction flows         |
| `08-SUB-001-notifications.md`          | Notification system design                 |
| `09-PLAT-001-wearos.md`                | WearOS integration design                  |
| `10-PLAN-001-kairos.md`                | Implementation plan (feature build order)  |
| `../adr-runtime-firebase-config.md`    | ADR: Runtime Firebase configuration        |
