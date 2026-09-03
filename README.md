# Reservation System — Inventory Service

A backend project built around one of the classic hard problems in server-side engineering: **never oversell a limited resource, even under heavy concurrent load.**

Current state: `inventory-service` is implemented and tested — item and hold management, with the core concurrency-correctness problem proven and fixed. `reservation-service` (the booking orchestration layer) is designed but not yet implemented — see Roadmap.

## Architecture

- **inventory-service** — owns stock (`inventory_item`) and holds (`hold`) in its own PostgreSQL database. Spring Boot 4.1, Java 21, Flyway migrations, Spring Data JPA.
- **reservation-service** — planned orchestration layer for the customer-facing booking lifecycle (PENDING → HELD → CONFIRMED), calling inventory-service over HTTP. Not yet implemented.
- **Database-per-service, no cross-service foreign keys.** `hold.item_id` is a plain column, not a foreign key — this keeps the option open to move hold data to its own service/database later without a schema migration. The tradeoff: referential integrity between the two is enforced in application code, not by the database.

## The core problem: never oversell

A **hold** temporarily reserves stock for a limited window (10 minutes) before a booking is confirmed. `available` stock must never be decremented below zero, and the sum of active holds must never exceed the item's total — even when hundreds of requests hit the same item simultaneously.

**Note on current scope:** `POST /holds` computes and stores `expiresAt`, but nothing currently sweeps or reclaims expired holds automatically — a hold created today stays in `HELD` status. The commit/release/expiry lifecycle is not yet implemented (see Roadmap).

### The bug, proven

The first implementation used a straightforward read-check-write pattern: read `available`, check it's `>= qty`, write the decrement. Under a single request this is correct. Under concurrent load it isn't — two transactions can both read the same `available` value before either writes, both pass the check, and both decrement from a value that was already stale.

This was proven with `OversellConcurrencyTest`: 200 threads, released simultaneously via a `CountDownLatch`, all attempting to hold 1 unit each from an item seeded with 50 units, repeated across 10 rounds via `@RepeatedTest`. Against the naive implementation, every round failed — `available` in the database landed at **42–49 instead of 0**, meaning most of the 200 attempts silently lost their decrement to a concurrent write. Assertions read the database directly via JDBC rather than trusting HTTP response codes, since a service can return the wrong status without the underlying state being wrong (or vice versa).

### The fix

Replaced the read-check-write pattern with a single atomic conditional `UPDATE`:

```sql
UPDATE inventory_item
   SET available = available - :qty
 WHERE id = :id
   AND available >= :qty
```

The check and the write happen in one statement. PostgreSQL takes a row-level lock for the duration of the UPDATE, which serializes concurrent attempts on *that specific row* — by the time a second concurrent UPDATE runs, the first has already committed, so the second re-evaluates `available >= qty` against the real current value instead of a stale in-memory read. `@Transactional` alone did not fix this: it guarantees atomicity within one transaction, but the race was an isolation problem — under PostgreSQL's default READ COMMITTED isolation, two concurrent transactions can each read the same pre-write value.

This approach doesn't require a global or table-wide lock — the row lock only affects the specific item being updated, so holds on different items never block each other.

After the fix, `OversellConcurrencyTest` passes cleanly across all 10 rounds: `available` reaches exactly 0, and exactly 50 of the 200 requests succeed.

## API surface

| Endpoint | Behavior |
|---|---|
| `GET /items/{id}` | 200 with item details, or 404 (`NOT_FOUND`) |
| `POST /items` | 201 with created item, 400 on validation failure, 409 (`SKU_ALREADY_EXISTS`) on duplicate SKU |
| `POST /holds` | 201 with hold (idempotent — replaying the same `holdKey` returns the original hold), 404 (`NOT_FOUND`) for unknown item, 409 (`INSUFFICIENT_INVENTORY`) when stock is insufficient |

Errors use RFC 7807 `ProblemDetail` with an added `code` property for stable, machine-readable error identification alongside the human-readable `detail`.

## Design decisions

**Flyway migrations, `ddl-auto: validate`.** Schema changes are explicit, versioned SQL files in source control rather than Hibernate silently altering the schema at startup. `validate` means the app refuses to start if the entity mappings and the actual schema disagree — a safety net that catches drift in development instead of production.

**Injected `Clock` instead of `Instant.now()`.** Every timestamp (`createdAt`, `updatedAt`, `expiresAt`) is derived from a `Clock` bean rather than the system clock directly. In production it's `Clock.systemUTC()`; in tests it can be substituted with a fixed or controllable clock, making time-dependent behavior testable without `Thread.sleep()`.

**Idempotency via a unique database constraint on `hold_key`, not just an application-level check.** A `findByHoldKey` check before creating a hold has the same race condition as the stock check itself — two concurrent requests with the same key could both find nothing and both proceed. The `UNIQUE` constraint on `hold_key` is the actual guarantee; the application check is just the fast, happy-path shortcut.

**No foreign key from `hold` to `inventory_item`.** Chosen to avoid coupling the two tables at the schema level, preserving the option to split them across services later without a migration — accepting that referential integrity is enforced in code rather than by the database.

## Running locally

```bash
docker compose up inventory-db -d
docker compose up --build          # full containerized run
```

`inventory-service` listens on `8081` (host-mapped) when run via Docker Compose, or `8080` when run directly.

## Testing

`OversellConcurrencyTest` runs against a real PostgreSQL instance via Testcontainers (not mocked), using `ExecutorService` + `CountDownLatch` to force maximum simultaneous contention, and `@RepeatedTest(10)` to make a passing result statistically meaningful rather than a lucky single run.

## Roadmap

Sequenced deliberately — correctness of the core resource-contention problem first, orchestration breadth after:

- `reservation-service`: booking lifecycle orchestration calling inventory-service's hold API
- Hold `commit`/`release` endpoints and a scheduled sweeper to reclaim expired holds (`expiresAt` is already computed and stored; nothing acts on it yet)
- Failure handling between services: timeouts, retries, circuit breaking, compensating release on partial failure
- Load testing and connection-pool tuning under sustained (not just burst) concurrency
