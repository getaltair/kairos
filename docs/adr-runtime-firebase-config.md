# ADR: Runtime Firebase Configuration

## Status

Accepted

## Date

2026-03-29

## Context

Kairos originally required a `google-services.json` file at build time, processed by the `com.google.gms.google-services` Gradle plugin. This works well for the developer's friends-and-family group, where CI injects the file via repository secrets during APK builds. However, it creates a barrier for self-hosters who want to run Kairos against their own Firebase project:

- Self-hosters had to fork the repository and add their own `google-services.json` before building from source.
- There was no way to distribute a single APK that works for both the developer's group and independent self-hosters.
- Each self-hoster needed a working Android build environment, which is a significant overhead for non-developers.

The goal was to enable a single APK binary that works in both scenarios without requiring self-hosters to build from source.

## Decision

Firebase configuration was made optional at build time and configurable at runtime via a first-launch setup screen.

### Conditional google-services plugin

In `app/build.gradle.kts`, the `com.google.gms.google-services` plugin is applied only when `google-services.json` exists in the app module directory. When the file is absent, the plugin is skipped entirely and Firebase must be initialized manually at runtime.

### Runtime configuration via setup screen

A setup screen is presented on first launch when no Firebase configuration is detected. Self-hosters create their own Firebase project (with Auth and Firestore enabled), then paste the full contents of their `google-services.json` into a text field. The screen includes instructions, a Configure button, and error/loading states via `FirebaseSetupViewModel`.

### Encrypted credential storage

Credentials are stored in `EncryptedSharedPreferences` via `FirebaseConfigStore`, using AES256_SIV for key encryption and AES256_GCM for value encryption. The encryption key is managed by Android Keystore. This is a one-time setup; stored credentials persist across app restarts.

### Phased Koin module loading

App startup was restructured into two phases:

1. **Phase 1** (always): `setupModule` loads in `KairosApp.onCreate()`, providing `FirebaseConfigStore` and `FirebaseSetupViewModel`.
2. **Phase 2** (after Firebase init): `firebaseModule` (provides `FirebaseAuth` and `FirebaseFirestore` singletons) plus all app modules (`dataModule`, `syncModule`, `authModule`, etc.) are loaded via `loadKoinModules()`.

`KairosApp.firebaseReady` (`StateFlow<Boolean>`) signals to the navigation layer when Phase 2 is complete.

### Refactored Firebase injection

`SyncModule`, `DataModule`, and `AuthModule` were refactored to inject `FirebaseAuth` and `FirebaseFirestore` from the Koin dependency graph rather than obtaining them via static `Firebase.auth` / `Firebase.firestore` calls. This enables the phased loading pattern where these modules are only instantiated after Firebase is initialized.

### Navigation gating

The navigation graph observes `firebaseReady`. When `false`, the start destination is the setup screen. When `true`, the start destination is the today screen (or auth screen if not signed in).

## Alternatives Considered

### 1. Build flavors

Separate "self-hosted" and "managed" product flavors, each with different Firebase configuration strategies. Rejected because it adds build complexity, duplicates APK variants, and self-hosters would still need to modify build files to supply their own configuration.

### 2. Environment variables at build time

Inject Firebase configuration values via environment variables during the Gradle build. Rejected because it still requires building from source with the correct environment set up, which does not solve the single-APK distribution goal.

### 3. Remote config server

Self-hosters would point the app at a configuration endpoint that serves Firebase credentials. Rejected because it adds an infrastructure dependency (the config server), which defeats the simplicity of self-hosting.

### 4. Firebase CLI auto-setup

A script that generates `google-services.json` from Firebase CLI output. Rejected because it requires CLI tooling installed on the user's machine and is not mobile-friendly for users who only have the APK.

## Consequences

### Positive

- A single APK works for both CI-built (pre-configured) and self-hosted (user-configured) scenarios. No fork or custom build required for self-hosters.
- The existing CI pipeline is unchanged. When `google-services.json` is present via secrets injection, the google-services plugin applies normally and the setup screen is never shown.
- Credentials are encrypted at rest via AES256, meeting security expectations for stored authentication material.
- Firebase-dependent features are properly gated. The app cannot reach screens that depend on Firestore or Auth before Firebase is initialized.

### Negative

- App startup logic is more complex due to phased Koin module loading and the two-path initialization (auto-init vs. manual-init).
- Self-hosters must understand how to create a Firebase project and obtain `google-services.json`, which requires navigating the Firebase console.
- The `androidx.security:security-crypto` library (1.0.0) is added as a dependency for `EncryptedSharedPreferences`.
