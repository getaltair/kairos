# Kotlin / Android: Testing

Test types, frameworks, and conventions.

## Three layers

| Layer | Location | Runtime | What belongs here |
|---|---|---|---|
| Unit | `src/test/` | JVM (no Android framework) | Pure logic, ViewModels, use cases, repository unit tests with fakes |
| Instrumented | `src/androidTest/` | Device or emulator | DB (Room), integration, UI end-to-end, anything needing a real Context |
| Screenshot/Compose UI | `src/androidTest/` or Paparazzi in `src/test/` | Either | Composable layout verification |

Default to unit tests on the JVM — they're 10-100x faster and run in CI
without an emulator. Only promote to instrumented when the code genuinely
depends on Android framework classes.

## Frameworks

- JUnit 4 is still the Android default (tooling assumes it). JUnit 5 is
  fine for pure JVM modules but not for `androidTest`.
- `kotlinx-coroutines-test` for coroutines: `runTest { }` with a
  `TestDispatcher` injected via constructor.
- `Turbine` for testing `Flow` emissions — don't hand-roll
  `.toList()` collection with timeouts.
- `MockK` for mocking Kotlin (handles `final` classes, coroutines,
  extension functions). Avoid `mockito-kotlin` in new code.
- `Robolectric` only when the class under test is tightly coupled to
  Android framework and refactoring is out of scope. Prefer extracting
  logic first.

## Naming

Backtick names for readability:

```kotlin
@Test
fun `login emits InvalidCredentials when password is empty`() { ... }
```

Pattern: `<what> <condition> <expected>`. One assertion focus per test.
When multiple assertions describe one behavior (state + side effect),
that's fine — splitting creates redundant setup.

## Structure

- `given / when / then` comments are optional but helpful for dense tests.
- Fixture builders over shared setup for complex objects: a `fun
  aUser(name: String = "a", …) = User(…)` helper beats a mutable
  `@Before` field.
- Prefer composition of small fixtures over a monolithic test base class.

## ViewModel tests

Inject dispatchers via a `DispatcherProvider` or pass the dispatcher into
the ViewModel constructor — never hardcode `Dispatchers.Main`.

```kotlin
class LoginViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit with empty password emits error`() = runTest {
        val vm = LoginViewModel(fakeAuth, StandardTestDispatcher(testScheduler))
        vm.state.test {
            assertEquals(Idle, awaitItem())
            vm.submit("user", "")
            assertEquals(Error("password required"), awaitItem())
        }
    }
}
```

`MainDispatcherRule` swaps `Dispatchers.Main` for `StandardTestDispatcher`
in the `@Before` / `@After` — copy the standard implementation from the
Android Architecture samples.

## Compose tests

Use `createComposeRule()` for isolated Composables, `createAndroidComposeRule<Activity>()`
when you need a real Activity. Assert against semantic properties, not
pixels:

```kotlin
composeTestRule.setContent { Greeting("World") }
composeTestRule.onNodeWithText("Hello World").assertExists()
```

Add `testTag` sparingly — prefer querying by text, content description,
or role. `testTag` couples tests to implementation.

## Fakes over mocks

Write a `FakeUserRepository` that implements the same interface as the
real one. Fakes survive refactors better than chains of `every { } returns`
mock setup. Reach for `MockK` when a fake is disproportionate to the test.

Never mock the System Under Test. Never mock data classes. Never mock an
interface you control when a fake would do.

## Flaky tests

Flaky tests are bugs. Quarantine immediately (`@Ignore("FLAKY: #123")`),
file the issue, and treat as a P1 in CI. Retry loops or `Thread.sleep`
are not fixes — they hide race conditions.

`IdlingResource` is the right tool for instrumented tests awaiting async
work. Don't poll.

## Coverage

Aim for behavior coverage, not line counts. A ViewModel with four state
transitions needs four tests. A DAO that just delegates to Room needs no
test — test the repository that uses it with an in-memory Room DB.

Jacoco reports are useful as a trend, not a gate. Don't fail CI on a 2%
coverage drop; do fail it on an untested public API.
