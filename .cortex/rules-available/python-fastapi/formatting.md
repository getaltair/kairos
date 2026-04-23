# Python / FastAPI: Formatting and tooling

Toolchain, configuration, and non-negotiables.

## Required tools

| Tool | Purpose | Config location |
|---|---|---|
| `ruff` | Lint + format | `pyproject.toml` `[tool.ruff]` |
| `mypy` | Type check | `pyproject.toml` `[tool.mypy]` |
| `pytest` | Test runner | `pyproject.toml` `[tool.pytest.ini_options]` |
| `bandit` | Security scan | `pyproject.toml` `[tool.bandit]` |

Commit `pyproject.toml` with these configured. `ruff` has replaced
`black` + `isort` + `flake8` — don't introduce those as separate tools.

## Ruff configuration

Minimum viable config:

```toml
[tool.ruff]
line-length = 100
target-version = "py311"  # or whatever the project's minimum Python is

[tool.ruff.lint]
select = [
  "E", "F", "W",  # pycodestyle + pyflakes
  "I",            # isort
  "UP",           # pyupgrade
  "B",            # bugbear
  "SIM",          # simplify
  "RUF",          # ruff-specific
]
ignore = []

[tool.ruff.lint.per-file-ignores]
"tests/**/*.py" = ["S101"]  # asserts are fine in tests
```

Line length: 100 is the project default. Don't drop to 79 (too short for
modern screens) or raise past 120 (hurts diffs in split-pane views).

## Mypy configuration

Use strict mode unless the codebase has significant legacy surface:

```toml
[tool.mypy]
python_version = "3.11"
strict = true
warn_unused_ignores = true
warn_redundant_casts = true
```

`strict = true` enables roughly: `disallow_untyped_defs`,
`disallow_incomplete_defs`, `check_untyped_defs`, `no_implicit_optional`,
`warn_return_any`, and more. Turning it on from day one is vastly cheaper than
bolting it on later.

## Non-negotiables

- No `# noqa` without a specific code (`# noqa: E501`) and a reason.
- No `# type: ignore` without the specific error code (`# type: ignore[arg-type]`)
  and a reason on the same or preceding line.
- Never commit with `ruff check` failing. `ruff format --check` must also
  pass.

## Pre-commit

`pre-commit` is recommended but optional. If used, it runs `ruff check
--fix`, `ruff format`, `mypy` on changed files, and `bandit` on the src tree.

## Dependency management

Use `uv` or `poetry` — don't mix. `uv` is faster and increasingly the
default for new projects. Avoid `pip install` directly into the environment
except inside Dockerfiles; the lockfile (`uv.lock` or `poetry.lock`) is the
source of truth for what's installed.

## Python version

Projects should declare a minimum Python version in `pyproject.toml`:

```toml
[project]
requires-python = ">=3.11"
```

3.11 is a reasonable floor as of 2025 — it has the exception groups, task
groups, and speed improvements that most modern async code relies on. Drop
to 3.10 only if a target environment demands it; don't support 3.9 in new
code.
