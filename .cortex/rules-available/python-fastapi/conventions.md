# Python / FastAPI: Conventions

File naming, module layout, import organization.

## File and module naming

- `snake_case.py` for all modules.
- Test files mirror the module they test: `foo.py` → `tests/test_foo.py`.
- Package directories contain `__init__.py` even in src-layout — the file may
  be empty, but it must exist.

## Layout

Prefer the src-layout for any package that will be installed:

```
project/
  src/
    mypackage/
      __init__.py
      api/
      models/
      services/
  tests/
  pyproject.toml
```

For FastAPI apps that are not installed as packages, a flat layout is fine:

```
app/
  main.py
  routers/
  models/
  services/
  tests/
```

## Import organization

Three groups, separated by a blank line, in this order:

1. Standard library
2. Third-party
3. Local / first-party

Within each group, sort alphabetically by module name. `ruff` enforces this
via the `I` (isort) rule group — keep that enabled.

Absolute imports for first-party code. Relative imports (`from .foo import X`)
are acceptable only within a package, never across package boundaries.

## Type annotations

Public function signatures — including every FastAPI route handler — must
have type annotations on parameters and return type. Private helpers
(`_leading_underscore`) may omit them when the body makes the type obvious.

Prefer modern syntax: `list[int]` over `List[int]`, `X | None` over
`Optional[X]`, `dict[str, Any]` over `Dict[str, Any]`. Requires `from
__future__ import annotations` on Python < 3.10 or `python_requires >= 3.10`.

## FastAPI-specific

- Every route handler declares `response_model=` — don't rely on return type
  inference alone for the OpenAPI schema.
- Dependencies come through `Depends()`, not module-level globals. That
  includes DB sessions, settings, and auth context.
- Use `async def` for handlers that await anything (DB, HTTP, sleep). Use
  plain `def` for handlers that don't — FastAPI will run those in a threadpool
  automatically. Mixing `async def` with blocking calls is worse than plain
  `def`.
- Pydantic v2: use `model_config = ConfigDict(...)`, not the `class Config:`
  nested class. The old syntax still works but is deprecated.
- Prefer `APIRouter` in feature modules; assemble them in `main.py`. Don't
  decorate routes directly on the top-level `FastAPI()` instance except for
  tiny apps.
