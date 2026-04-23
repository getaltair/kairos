# Python / FastAPI: Testing

Test framework, structure, and conventions.

## Framework

`pytest` is the default. Use `pytest-asyncio` for async code and
`httpx.AsyncClient` + `ASGITransport` for FastAPI endpoint tests.

## Structure

- Tests live in a top-level `tests/` directory that mirrors the source tree.
- One test module per source module: `src/pkg/foo.py` → `tests/test_foo.py`.
- Integration tests (touch DB, external services) go in `tests/integration/`
  and are marked with `@pytest.mark.integration` so they can be run or skipped
  as a group.

## Naming

- `test_<what>_<condition>_<expected>` — e.g., `test_create_user_duplicate_raises_409`.
- The middle clause is optional for trivially simple tests but required when
  multiple conditions are tested against the same function.

## Fixtures

- Shared fixtures go in `conftest.py` at the closest reasonable scope. Don't
  hoist a fixture to the project root `conftest.py` unless two or more
  directories need it.
- Database fixtures: one per scope that makes sense (`session`, `module`,
  `function`). Default to `function` scope and only broaden when measured to
  be slow — a per-function transaction rollback is cheap and keeps isolation.
- Never mutate shared module-scoped fixtures inside a test.

## FastAPI test client

Use the async client for async handlers:

```python
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.fixture
async def client():
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as c:
        yield c

async def test_health(client):
    response = await client.get("/health")
    assert response.status_code == 200
```

`TestClient` (sync) from `fastapi.testclient` is acceptable for non-async
handlers but prefer `AsyncClient` uniformly — mixing the two leads to subtle
event-loop issues.

## Assertions

- Prefer plain `assert` over `self.assert*` — pytest rewrites them with
  rich introspection.
- For dict/list equality, let pytest show the diff; don't break expected values
  across multiple asserts.
- Use `pytest.raises(SpecificError, match="...")` for exception tests — match
  on a regex against the exception message so tests fail loudly when error
  messages change.

## Mocking

- Use `unittest.mock.patch` for third-party code.
- **Don't** mock your own code. If a function needs mocking to be testable,
  that's a design signal — consider dependency injection via FastAPI's
  `Depends()` and override in tests with `app.dependency_overrides`.
- `MagicMock(spec=SomeClass)` is preferable to bare `MagicMock()` — it
  catches attribute typos.

## Coverage

Aim for coverage on behavior, not line counts. A handler with three branches
should have at least three tests; a trivial getter might not need one at all.
Target 80% line coverage as a floor but don't game it — a 100%-covered
codebase full of `assert result is not None` tests is worse than 75% coverage
with meaningful assertions.

## Environment

Tests must not hit external services by default. Mock the network at the
httpx/requests level, or use `pytest-httpx` / `respx`. Tests that require
real network access go behind a marker: `@pytest.mark.network`.
