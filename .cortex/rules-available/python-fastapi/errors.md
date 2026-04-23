# Python / FastAPI: Error handling

How to raise, catch, propagate, and log errors.

## Exception propagation

- Let exceptions propagate by default. Don't catch them unless you have a
  specific action to take (retry, transform, log + re-raise).
- **Never** use bare `except:` or plain `except Exception:` without either
  re-raising or logging the full traceback with `logger.exception(...)`.
- Catch the narrowest exception type that covers the case. `except OSError`
  is better than `except Exception` when you're handling file I/O.

## Never swallow, always transform or log

Two acceptable patterns:

```python
# Transform: convert low-level error to domain error
try:
    row = db.get(key)
except sqlite3.IntegrityError as e:
    raise UserAlreadyExists(key) from e

# Log and re-raise: when caller doesn't need the detail but ops does
try:
    payment_gateway.charge(amount)
except PaymentGatewayError:
    logger.exception("payment failed", extra={"user_id": user.id})
    raise
```

The `from e` chain is mandatory when you wrap. Don't drop the traceback.

## FastAPI: HTTPException

- Always include a status code: `raise HTTPException(status_code=404,
  detail="...")`. Never `raise HTTPException()` with no args.
- `detail` is user-facing — don't leak internals (stack traces, SQL
  fragments, internal IDs the user shouldn't see).
- For validation errors, let Pydantic raise — don't re-implement field
  validation inside the handler.

## Custom exceptions

Define domain exceptions in a single module per package (`errors.py` or
`exceptions.py`). Inherit from `Exception`, not `BaseException`.

```python
class DomainError(Exception):
    """Base for all application errors."""

class UserAlreadyExists(DomainError):
    def __init__(self, username: str):
        super().__init__(f"user {username!r} already exists")
        self.username = username
```

Register a FastAPI exception handler that maps `DomainError` subclasses to
appropriate HTTP responses, so route handlers don't need to re-raise as
`HTTPException`:

```python
@app.exception_handler(UserAlreadyExists)
async def _user_exists_handler(request, exc):
    return JSONResponse(status_code=409, content={"detail": str(exc)})
```

## Async-specific

- Never call `time.sleep()` inside an `async def` — use `await asyncio.sleep(...)`.
- Wrap blocking I/O in `await asyncio.to_thread(...)` rather than calling it
  directly in an async handler.
- `except asyncio.CancelledError` must re-raise after cleanup. Swallowing
  it breaks task cancellation and is almost always a bug.

## Logging

- Use `logging.getLogger(__name__)` — never the root logger directly.
- `logger.exception(...)` inside an `except` block; `logger.error(...)` is
  for error messages without traceback context.
- Include structured context via `extra={...}` rather than f-string
  interpolation into the message. This makes logs machine-parseable.
