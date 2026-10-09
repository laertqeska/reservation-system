# Reservation System

![CI](https://github.com/laertqeska/reservation-system/actions/workflows/ci.yml/badge.svg)

Two Spring Boot services (inventory and reservation) that I built to learn how to handle concurrency and consistency between services. Java 21, Postgres, Flyway, Docker Compose.

- **inventory-service** keeps stock and holds. Port 8081.
- **reservation-service** takes a booking request, asks inventory to hold the stock, and saves the result. Port 8082.

Each service has its own database. There's no foreign key from `hold` to `inventory_item`, so I could split them up later without a migration. The cost is that I have to keep them consistent in code.

## Running it

```bash
git clone https://github.com/laertqeska/reservation-system.git
cd reservation-system
docker compose up --build
```

`docker compose down -v` wipes the databases.

## The overselling bug

My first version of `POST /holds` read `available`, checked it was big enough, and wrote the new value. That works for one request. I wrote a test that fires 200 threads at an item with 50 units, and `available` ended up at 42 to 49 instead of 0. Most of the decrements were getting lost.

`@Transactional` didn't help, because the problem was two transactions reading the same value before either one wrote. What fixed it was one atomic update:

```sql
UPDATE inventory_item
   SET available = available - :qty
 WHERE id = :id AND available >= :qty;
```

Now the check and the write happen together, and Postgres locks just that row. After the fix, exactly 50 of the 200 requests succeed and `available` ends at 0, across 10 repeated runs.

## Tests and CI

`OversellConcurrencyTest` runs against real Postgres with Testcontainers. CI runs `docker compose up` on every push, waits for health checks, then creates an item and a reservation as a smoke test.
