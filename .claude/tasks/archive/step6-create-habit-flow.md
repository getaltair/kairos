# Plan: Step 6 - Create Habit Flow

## Task Description

Implement the multi-step habit creation wizard as defined in `docs/implementation-plan.md` Step 6, `docs/01-prd-core.md` UC-1/FR-1, and `docs/10-user-flows.md` Flow 2. This is a four-step wizard (three required, one optional) that lives in the `feature/habit` module and is triggered from the FAB on the Today screen. The wizard collects: habit name, anchor behavior, category, and optional configuration (duration, micro-version, icon, color, frequency). On completion it persists the habit via the existing `CreateHabitUseCase` and navigates back to the Today screen.

## Objective

When this plan is complete, users can create new habits through a polished multi-step wizard accessible from the Today screen FAB. The habit appears on the Today screen immediately after creation (or on the dashboard if DEPARTURE category). All invariants (H-1, H-2, H-4) are enforced, validation errors appear inline, and back navigation within the wizard preserves entered data.

## Problem Statement

The Today screen exists with a FAB that has a `TODO: Step 6` placeholder. The `feature/habit` module is scaffolded (build.gradle.kts exists) but contains no source files. The `CreateHabitUseCase` already exists in the domain layer with `HabitValidator` enforcement, but there is no UI to drive it. Users currently have no way to create habits.

## Solution Approach

Build a Compose-based multi-step wizard inside `feature/habit` using a single `CreateHabitViewModel` that holds all wizard state. Each wizard step is a composable function, with navigation between steps managed by the ViewModel (not the NavHost). The outer NavHost gets a `createHabit` route that hosts the wizard. The Today screen FAB navigates to this route. On successful creation, navigate back to Today screen which auto-reloads.

Key design decisions:

- **Single ViewModel for wizard**: All four steps share one `CreateHabitViewModel` so back navigation preserves state trivially
- **Internal step navigation**: Steps are managed by a `currentStep` index in the ViewModel, not separate nav routes, avoiding complex saved-state-handle wiring
- **Reuse existing CreateHabitUseCase**: The domain layer already handles validation and persistence
- **NavController passed via lambda**: Today screen's FAB calls `onAddHabit()` lambda wired to `navController.navigate("createHabit")` in the NavGraph

## Relevant Files

### Existing Files to Modify

- `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt` - Add `createHabit` route, pass `navController` to TodayScreen
- `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayScreen.kt` - Accept `onAddHabit` lambda, wire FAB
- `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt` - Register `habitModule` in Koin
- `feature/habit/build.gradle.kts` - Already exists with correct dependencies

### New Files

- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt` - Wizard host composable
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt` - Wizard ViewModel with state management
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt` - UI state data classes
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/NameStep.kt` - Step 1: Name input
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/AnchorStep.kt` - Step 2: Anchor selection
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/CategoryStep.kt` - Step 3: Category selection
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/OptionsStep.kt` - Step 4: Optional configuration
- `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/di/HabitModule.kt` - Koin module for ViewModel
- `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModelTest.kt` - ViewModel unit tests

## Implementation Phases

### Phase 1: Foundation

- Wire navigation: Add `createHabit` route to NavGraph, pass `onAddHabit` to TodayScreen, hook FAB
- Create `CreateHabitUiState` with wizard state model
- Create `CreateHabitViewModel` skeleton with step navigation logic
- Set up Koin DI module and register in `KairosApp`

### Phase 2: Core Implementation

- Build all four wizard step composables:
    - **NameStep**: Text input with character count, habit name suggestions, Continue button enabled when name is valid (1-100 chars)
    - **AnchorStep**: Tab-based anchor type selector (After I.../Before I.../When I arrive at.../At a specific time), preset lists with custom input, time picker for AT_TIME
    - **CategoryStep**: Card-based category selector (Morning/Afternoon/Evening/Anytime/Departure) with emoji and description
    - **OptionsStep**: Optional fields (estimated duration slider, micro-version text, icon picker, color picker, frequency selector with custom day-of-week picker)
- Wire `CreateHabitScreen` host composable that renders steps based on `currentStep`
- Implement `createHabit()` in ViewModel calling `CreateHabitUseCase`

### Phase 3: Integration & Polish

- Inline validation errors on each step
- Success animation on creation
- Back navigation preserves data within wizard
- Unit tests for ViewModel
- Verify DEPARTURE category habits do not show on Today screen (already handled by `TodayViewModel.groupByCategory` filter)

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
    - Name: builder-foundation
    - Role: Wire navigation, create UI state model, ViewModel skeleton, DI module, and integrate with KairosApp
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-wizard-ui
    - Role: Build all four wizard step composables and the CreateHabitScreen host, wire ViewModel actions
    - Agent Type: general-purpose
    - Resume: true

- Specialist
    - Name: builder-tests
    - Role: Write ViewModel unit tests covering step navigation, validation, creation flow, and edge cases
    - Agent Type: general-purpose
    - Resume: true

- Quality Engineer (Validator)
    - Name: validator
    - Role: Validate completed work against acceptance criteria (read-only inspection mode)
    - Agent Type: quality-engineer
    - Resume: false

## Step by Step Tasks

### 1. Wire Navigation and Create Foundation

- **Task ID**: foundation
- **Depends On**: none
- **Assigned To**: builder-foundation
- **Agent Type**: general-purpose
- **Parallel**: false (other tasks depend on this)
- Create new branch `feat/step6-create-habit-flow` from `main`
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitUiState.kt`:
    - `enum class WizardStep { NAME, ANCHOR, CATEGORY, OPTIONS }`
    - `data class CreateHabitUiState` with fields: `currentStep: WizardStep`, `name: String`, `nameError: String?`, `anchorType: AnchorType?`, `anchorBehavior: String`, `anchorError: String?`, `anchorTime: String?` (for AT_TIME), `category: HabitCategory?`, `categoryError: String?`, `estimatedSeconds: Int`, `microVersion: String`, `icon: String?`, `color: String?`, `frequency: HabitFrequency`, `activeDays: Set<DayOfWeek>`, `isCreating: Boolean`, `creationError: String?`, `isCreated: Boolean`
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModel.kt`:
    - Constructor injects `CreateHabitUseCase`
    - Exposes `uiState: StateFlow<CreateHabitUiState>`
    - Methods: `onNameChanged(name)`, `onAnchorTypeSelected(type)`, `onAnchorBehaviorChanged(text)`, `onAnchorTimeChanged(time)`, `onCategorySelected(category)`, `onEstimatedSecondsChanged(seconds)`, `onMicroVersionChanged(text)`, `onIconSelected(icon)`, `onColorSelected(color)`, `onFrequencySelected(freq)`, `onActiveDaysChanged(days)`, `goToNextStep()`, `goToPreviousStep()`, `createHabit()`
    - `goToNextStep()` validates current step before advancing (name not blank for NAME, anchorType+behavior for ANCHOR, category for CATEGORY)
    - `createHabit()` builds `Habit` entity from state, calls `CreateHabitUseCase`, sets `isCreated = true` on success
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/di/HabitModule.kt`:
    - `val habitModule = module { viewModelOf(::CreateHabitViewModel) }` (matching today module pattern)
- Modify `app/src/main/kotlin/com/getaltair/kairos/KairosApp.kt`:
    - Import and add `habitModule` to Koin modules list
- Modify `feature/today/src/main/kotlin/com/getaltair/kairos/feature/today/TodayScreen.kt`:
    - Add `onAddHabit: () -> Unit = {}` parameter to `TodayScreen`
    - Wire FAB `onClick` to call `onAddHabit()`
- Modify `app/src/main/kotlin/com/getaltair/kairos/navigation/KairosNavGraph.kt`:
    - Pass `onAddHabit = { navController.navigate("createHabit") }` to `TodayScreen`
    - Add `composable("createHabit")` route that renders `CreateHabitScreen(onBack = { navController.popBackStack() }, onCreated = { navController.popBackStack() })`
- Create empty `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt` placeholder:
    - Accept `onBack` and `onCreated` lambdas
    - Render based on ViewModel `currentStep`, but step content can be placeholder `Text("Step X")` initially

### 2. Build Wizard Step Composables

- **Task ID**: wizard-ui
- **Depends On**: foundation
- **Assigned To**: builder-wizard-ui
- **Agent Type**: general-purpose
- **Parallel**: false (depends on foundation)
- Read the existing codebase patterns (TodayScreen, HabitCard, CompletionBottomSheet) to match Material3 styling
- Read all domain enums (AnchorType, HabitCategory, HabitFrequency) and the Habit entity to understand the data model
- Read `docs/10-user-flows.md` Flow 2 and `docs/implementation-plan.md` Step 6 for detailed UI requirements
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/NameStep.kt`:
    - Auto-focused `OutlinedTextField` for habit name
    - Character count indicator (x/100)
    - Suggestion chips for common habits: "Take medication", "Exercise", "Read", "Meditate", "Journal", "Drink water"
    - Tapping suggestion fills the text field
    - `Continue` button enabled when name is 1-100 chars
    - Show `nameError` inline below text field if present
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/AnchorStep.kt`:
    - Header: "When will you do this?"
    - `ScrollableTabRow` or `SingleChoiceSegmentedButtonRow` for anchor type tabs: "After I...", "Before I...", "When I arrive at...", "At a specific time"
    - For AFTER_BEHAVIOR: preset chips (Wake up, Brush teeth, Have breakfast, Get to work) + custom text input
    - For BEFORE_BEHAVIOR: preset chips (Go to bed, Leave for work, Eat dinner) + custom text input
    - For AT_LOCATION: text input for location name (location picker deferred to P2)
    - For AT_TIME: `TimePickerDialog` or inline time picker
    - Show `anchorError` inline if present
    - `Continue` button enabled when anchor type selected and behavior text filled (or time selected for AT_TIME)
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/CategoryStep.kt`:
    - Grid of selectable category cards (2 columns)
    - Each card: emoji + display name + description
    - Morning ("Before noon"), Afternoon ("12 PM - 6 PM"), Evening ("After 6 PM"), Anytime ("No specific time"), Departure ("Don't Forget - shown on doorway dashboard")
    - Selected state with Material3 `OutlinedCard` with border highlight
    - Show `categoryError` inline if present
    - `Continue` button enabled when category selected
- Create `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/steps/OptionsStep.kt`:
    - All fields optional with clear defaults shown
    - Duration: `Slider` from 1-60 minutes (default 5 min / 300 seconds), with label showing current value
    - Micro-version: `OutlinedTextField` with helper text "Smallest possible version of this habit"
    - Frequency: `SingleChoiceSegmentedButtonRow` (Daily/Weekdays/Weekends/Custom)
    - When Custom frequency selected: `FilterChip` row for each day of week (Mon-Sun)
    - Icon: grid of predefined icons (Material icons relevant to habits)
    - Color: row of predefined color circles (Material3 palette)
    - `Create Habit` button (primary, always enabled since all fields have defaults)
    - `Skip` link text to create with defaults
- Update `feature/habit/src/main/kotlin/com/getaltair/kairos/feature/habit/CreateHabitScreen.kt`:
    - Replace placeholder content with actual step composables
    - `Scaffold` with `TopAppBar` showing step indicator (e.g., "Step 1 of 4") and back arrow
    - Back arrow calls `viewModel.goToPreviousStep()` (or `onBack()` if on step 1)
    - `AnimatedContent` or crossfade transition between steps
    - `LaunchedEffect` on `uiState.isCreated` to call `onCreated()`
    - Show creation error as `Snackbar` if `creationError` is set

### 3. Write ViewModel Unit Tests

- **Task ID**: tests
- **Depends On**: wizard-ui
- **Assigned To**: builder-tests
- **Agent Type**: general-purpose
- **Parallel**: false (depends on wizard-ui completing the ViewModel logic)
- Read existing test patterns from `feature/today/src/test/kotlin/com/getaltair/kairos/feature/today/TodayViewModelTest.kt` and `domain/src/test/kotlin/com/getaltair/kairos/domain/usecase/CreateHabitUseCaseTest.kt`
- Create `feature/habit/src/test/kotlin/com/getaltair/kairos/feature/habit/CreateHabitViewModelTest.kt`:
    - Test initial state defaults (step = NAME, name = "", etc.)
    - Test `onNameChanged` updates state
    - Test `goToNextStep` from NAME with blank name sets `nameError`
    - Test `goToNextStep` from NAME with valid name advances to ANCHOR
    - Test `goToNextStep` from ANCHOR without anchor type sets `anchorError`
    - Test `goToNextStep` from ANCHOR with valid anchor advances to CATEGORY
    - Test `goToNextStep` from CATEGORY without category sets `categoryError`
    - Test `goToNextStep` from CATEGORY with valid category advances to OPTIONS
    - Test `goToPreviousStep` from ANCHOR goes back to NAME (preserving name)
    - Test `goToPreviousStep` from NAME does nothing (stays on NAME)
    - Test `createHabit` success sets `isCreated = true`
    - Test `createHabit` failure sets `creationError`
    - Test `createHabit` builds Habit with correct defaults (ONBOARD phase, ACTIVE status, 300s estimated, DAILY frequency, allowPartialCompletion = true)
    - Test custom frequency with activeDays flows through to Habit entity
    - Mock `CreateHabitUseCase` with MockK

### 4. Final Validation

- **Task ID**: validate-all
- **Depends On**: foundation, wizard-ui, tests
- **Assigned To**: validator
- **Agent Type**: quality-engineer
- **Parallel**: false
- Verify all new files follow existing code patterns (Koin DI, ViewModel pattern, Compose conventions)
- Verify `CreateHabitUiState` captures all fields from the Habit entity needed for creation
- Verify all invariants are enforced:
    - H-1: Anchor required (anchorBehavior not blank, anchorType set)
    - H-2: Category required
    - H-4: allowPartialCompletion hardcoded to true
- Verify DEPARTURE category is available in CategoryStep
- Verify navigation: FAB -> wizard -> back to Today on create
- Verify back navigation within wizard preserves data
- Verify inline validation errors (not just toast)
- Run all validation commands
- Operate in validation mode: inspect and report only, do not modify files

## Acceptance Criteria

- [ ] FAB on Today screen navigates to Create Habit wizard
- [ ] User can create habit in 5 taps: Name -> Anchor -> Category -> (skip options) -> Create
- [ ] Anchor is required -- cannot proceed without it (H-1)
- [ ] Category is required -- cannot proceed without it (H-2)
- [ ] DEPARTURE category available and selectable
- [ ] Habit appears on Today screen immediately after creation (non-DEPARTURE habits)
- [ ] Optional fields (icon, color, duration, micro-version, frequency) work when provided
- [ ] Custom frequency with day-of-week picker works
- [ ] Back navigation within wizard preserves entered data
- [ ] Validation errors shown inline (not just toast)
- [ ] Habit created with correct defaults: ONBOARD phase, ACTIVE status, 300s estimated, DAILY frequency, allowPartialCompletion = true
- [ ] ViewModel unit tests pass
- [ ] Project builds without errors (`./gradlew build`)

## Validation Commands

Execute these commands to validate the task is complete:

- `cd /home/rghamilton3/workspace/getaltair/kairos && ./gradlew :feature:habit:build` - Verify the habit feature module builds
- `cd /home/rghamilton3/workspace/getaltair/kairos && ./gradlew :feature:habit:test` - Run habit feature unit tests
- `cd /home/rghamilton3/workspace/getaltair/kairos && ./gradlew :app:build` - Verify the full app builds with new navigation wiring
- `cd /home/rghamilton3/workspace/getaltair/kairos && ./gradlew ktlintCheck` - Verify code style compliance

## Notes

- The `CreateHabitUseCase` already exists in `domain/src/main/kotlin/com/getaltair/kairos/domain/usecase/CreateHabitUseCase.kt` and is already wired in `core/src/main/kotlin/com/getaltair/kairos/core/di/UseCaseModule.kt`. No new use case is needed.
- The `HabitValidator` already enforces H-1 (anchor not blank), H-4 (partial completion true), H-5 (threshold ordering), and H-6 (timestamp consistency). The ViewModel should enforce H-2 (category required) at the UI level since it's a required wizard step.
- Success animation is listed in the implementation plan's "done when" criteria. A simple confetti or checkmark animation on the Today screen after returning from creation is sufficient. Can use `AnimatedVisibility` with a scale/fade effect.
- Location picker for AT_LOCATION anchor type is deferred to P2 per the spec. Use a simple text input for now.
- The existing `TodayViewModel.groupByCategory` already filters out DEPARTURE habits, so DEPARTURE habits will not appear on the Today screen without any additional work.
- The project uses Koin (not Hilt) for DI. Follow the `todayModule` pattern: `viewModelOf(::CreateHabitViewModel)`.
