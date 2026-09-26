# Reservation System

![CI](https://github.com/laertqeska/reservation-system/actions/workflows/ci.yml/badge.svg)

A two-service backend built around one of the classic hard problems in server-side engineering: **never oversell a limited resource under concurrent load**, and coordinate the outcome across service boundaries without losing or duplicating state.

## Current state

- **inventory-service** — implemented and tested. Item and hold management, with the core concurrency-correctness problem proven and fixed.
- **reservation-service** — implemented. Orchestrates the booking lifecycle by calling inventory-service's hold API and persisting the resulting reservation state.

CI runs the full stack on every push via GitHub Actions: `docker compose up` from a clean Ubuntu runner, health-check both services, then a smoke test that creates an item and a reservation end to end.

## Architecture

- **inventory-service** — owns stock (`inventory_item`) and holds (`hold`) in its own PostgreSQL database. Spring Boot 4, Java 21, Flyway, Spring Data JPA.
- **reservation-service** — orchestrates the customer-facing booking lifecycle (`PENDING → HELD | FAILED`), calling inventory-service over HTTP via `RestClient`. Owns its own PostgreSQL database.

Database-per-service, no cross-service foreign keys. `hold.item_id` is a plain column, not a foreign key — this keeps the option open to move hold data to its own service/database later without a schema migration. The tradeoff: referential integrity between the two is enforced in application code, not by the database.

Services communicate over a Docker network by service name (`http://inventory-service:8080`), not `localhost`.

## The core problem: never oversell

A **hold** temporarily reserves stock for a limited window before a booking is confirmed. `available` stock must never be decremented below zero, and the sum of active holds must never exceed the item's `total` — even when hundreds of requests hit the same item simultaneously.

### Note on current scope

`POST /holds` computes and stores `expiresAt`, but nothing currently sweeps or reclaims expired holds automatically. The commit/release/expiry lifecycle is on the roadmap.

### The bug, proven

The first implementation used a straightforward read-check-write pattern: read `available`, check it's `>= qty`, write the decrement. Under a single request this is correct. Under concurrent load it isn't — two transactions can both read the same `available` value before either writes, both pass the check, and both decrement from a value that was already stale.

This was proven with `OversellConcurrencyTest`: 200 threads, released simultaneously via a `CountDownLatch`, all attempting to hold 1 unit each from an item seeded with 50 units, repeated across 10 rounds via `@RepeatedTest`. Against the naive implementation, every round failed — `available` in the database landed at **42–49 instead of 0**, meaning most of the 200 attempts silently lost their decrement to a concurrent write.

Assertions read the database directly via JDBC rather than trusting HTTP response codes, since a service can return the wrong status without the underlying state being wrong (or vice versa).

### The fix

Replaced the read-check-write pattern with a single atomic conditional `UPDATE`:

```sql
UPDATE inventory_item
   SET available = available - :qty
 WHERE id = :id
   AND available >= :qty;
```

The check and the write happen in one statement. PostgreSQL takes a row-level lock for the duration of the `UPDATE`, which serializes concurrent attempts on that specific row — by the time a second concurrent `UPDATE` runs, the first has already committed, so the second re-evaluates `available >= qty` against the real current value instead of a stale in-memory read.

`@Transactional` alone did not fix this: it guarantees atomicity within one transaction, but the race was an isolation problem — under PostgreSQL's default `READ COMMITTED` isolation, two concurrent transactions can each read the same pre-write value.

This approach doesn't require a global or table-wide lock — the row lock only affects the specific item being updated, so holds on different items never block each other.

After the fix, `OversellConcurrencyTest` passes cleanly across all 10 rounds: `available` reaches exactly 0, and exactly 50 of the 200 requests succeed.

## The second problem: coordinating across services

`reservation-service` cannot make the hold call and the reservation insert atomic — they live in different databases, and the HTTP call can fail in ways that leave the two systems disagreeing.

Three failure modes are handled explicitly.

### Idempotency on both sides

`POST /reservations` requires an `Idempotency-Key` header. `reservation-service` dedupes on it via a unique constraint.

The same key is deterministically hashed into a UUID (`UUID.nameUUIDFromBytes`) and sent to inventory as the hold key, so a retry after a crash between the two writes produces the same hold rather than a duplicate.

### Save-before-call

The reservation row is inserted as `PENDING` before the inventory call.

If the call fails with a business error (insufficient stock, item missing), the row is updated to `FAILED` with a `failureReason` — the failed attempt is durable and retrievable, not lost.

### Explicit status mapping

`exchange()` is used instead of the default throwing behavior so each upstream status becomes a typed `HoldOutcome`:

- `Success`
- `InsufficientStock`
- `ItemNotFound`
- `Error`

A `switch` converts each into the corresponding reservation transition.

Upstream errors carry through as `502 Bad Gateway` with the original status; routine business failures are recorded on the reservation.

## API surface

### inventory-service

| Endpoint | Behavior |
|---|---|
| `GET /items/{id}` | `200` with item details, or `404 (NOT_FOUND)` |
| `POST /items` | `201` with created item, `400` on validation failure, `409 (SKU_ALREADY_EXISTS)` on duplicate SKU |
| `POST /holds` | `201` with hold (idempotent — replaying the same `holdKey` returns the original hold), `404 (NOT_FOUND)` for unknown item, `409 (INSUFFICIENT_INVENTORY)` when stock is insufficient |

### reservation-service

| Endpoint | Behavior |
|---|---|
| `POST /reservations` | `201` with reservation. Requires `Idempotency-Key` header. Returns `HELD` on success, `FAILED` with `failureReason` on a business failure. `502` if inventory-service returns an unexpected status. |
| `GET /reservations/{id}` | `200` with reservation, or `404 (NOT_FOUND)` |

Errors use RFC 7807 `ProblemDetail` with an added `code` property for stable, machine-readable error identification alongside the human-readable `detail`.

## Design decisions

### Flyway migrations, `ddl-auto: validate`

Schema changes are explicit, versioned SQL files in source control rather than Hibernate silently altering the schema at startup.

`validate` means the app refuses to start if the entity mappings and the actual schema disagree — a safety net that catches drift in development instead of production.

### Injected `Clock` instead of `Instant.now()`

Every timestamp (`createdAt`, `updatedAt`, `expiresAt`) is derived from a `Clock` bean rather than the system clock directly.

In production it's `Clock.systemUTC()`; in tests it can be substituted with a fixed or controllable clock, making time-dependent behavior testable without `Thread.sleep()`.

### Idempotency via a unique database constraint

Idempotency is guaranteed by a unique database constraint, not just an application-level check.

A `findByHoldKey` check before creating a hold has the same race condition as the stock check itself — two concurrent requests with the same key could both find nothing and both proceed.

The `UNIQUE` constraint on `hold_key` (and on `idempotency_key` on the reservation side) is the actual guarantee; the application check is just the fast, happy-path shortcut.

### No foreign key from `hold` to `inventory_item`

Chosen to avoid coupling the two tables at the schema level, preserving the option to split them across services later without a migration — accepting that referential integrity is enforced in code rather than by the database.

### Sealed `HoldOutcome` + `exchange()` over try/catch + default `retrieve()`

The set of possible upstream outcomes is modeled as a sealed interface, so a `switch` over it is exhaustive — the compiler catches a missing case if a new outcome is added.

The `exchange` handler reads each status without throwing; the switch converts each into a reservation transition or a typed exception.

This keeps "routine business outcome" (insufficient stock) and "genuine error" (inventory service returned 5xx) on separate code paths.

## Cloning and running the project

### Prerequisites

- Docker Desktop (with the Docker Compose v2 plugin — bundled with current Docker Desktop)
- Git

That's it. You do not need a local JDK, Maven, or PostgreSQL — everything runs inside containers.

### 1. Clone the repository

```bash
git clone https://github.com/laertqeska/reservation-system.git
cd reservation-system
```

### 2. Start the full stack

From the repository root:

```bash
docker compose up --build
```

The first run takes a few minutes (Maven dependency resolution + image builds). Subsequent runs are faster because Docker caches the build layers.

Wait until both services log `Started ... Application`.

You can confirm they're up by checking:

```text
http://localhost:8081/actuator/health
http://localhost:8082/actuator/health
```

Expected responses:

```json
{"status":"UP"}
```

- `8081` → inventory-service
- `8082` → reservation-service

### 3. Try it end to end

Create an item in inventory:

```bash
curl -i -X POST http://localhost:8081/items \
  -H 'Content-Type: application/json' \
  -d '{"sku":"WIDGET-1","total":10,"available":10}'
```

Note the returned `id` (usually `1` on a fresh database).

Then create a reservation for it:

```bash
curl -i -X POST http://localhost:8082/reservations \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -d '{"itemId":1,"qty":2}'
```

Expected: `201` with `"status":"HELD"` and a non-null `holdId`.

Re-running the same request with the same `Idempotency-Key` returns the same reservation without creating a second hold.

Fetch it back:

```bash
curl -i http://localhost:8082/reservations/1
```

### 4. Stop

```bash
docker compose down          # keep database volumes
docker compose down -v       # drop database volumes (fresh next start)
```

## Running without Docker

Each service can be run directly with:

```bash
./mvnw spring-boot:run
```

Run it from the relevant module directory.

You'll need:

- a local JDK 21
- a running PostgreSQL instance

The datasource URL, username, and password default to environment-variable-overridable values in `application.yaml`.

## Testing

`OversellConcurrencyTest` runs against a real PostgreSQL instance via Testcontainers (not mocked), using `ExecutorService` + `CountDownLatch` to force maximum simultaneous contention, and `@RepeatedTest(10)` to make a passing result statistically meaningful rather than a lucky single run.

## CI

GitHub Actions runs on every push:

1. `docker compose up` on a clean Ubuntu runner
2. Waits for both services to pass health checks
3. Executes a smoke test that creates an item and a reservation end to end

This proves the project builds and runs from a fresh clone in a fresh environment.

## Roadmap

Sequenced deliberately — correctness of the core resource-contention problem first, orchestration breadth after.

1. **Hold commit/release endpoints and expiry handling**
   - Add commit/release endpoints.
   - Add a scheduled sweeper to reclaim expired holds.
   - `expiresAt` is already computed and stored; nothing currently acts on it.

2. **Network-level failure handling between services**
   - Explicit connect/read timeouts on the `RestClient`
   - Retries with backoff for transient failures
   - Circuit breaking

3. **Reconciliation of stale `PENDING` reservations**
   - Add a scheduled job that finds rows stuck in `PENDING` past a threshold.
   - Resolve them against inventory: was the hold actually created, or did the call time out before reaching inventory?
   - This is the failure mode introduced by save-before-call that is not yet closed.

4. **Load testing and connection-pool tuning**
   - Test under sustained, not just burst, concurrency.
   - Tune database connection pools and related infrastructure under realistic load.
