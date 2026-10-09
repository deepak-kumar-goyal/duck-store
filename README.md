# Duck Store

A small warehouse application (React UI + REST API) and an order-pricing endpoint for a rubber duck shop.

- **Backend**: Java 21, Spring Boot, Spring Data JPA, Flyway, Maven (`backend/`)
- **Frontend**: React + Vite (`frontend/`)
- **Database**: MySQL 8 in Docker (`docker-compose.yml`)

## Prerequisites

- Docker (with Compose)
- JDK 21
- Node.js 20+ and npm
- Free local ports: `3306` (MySQL), `8080` (backend) and `5173` (frontend). If one is taken, see
  [Using a different port or your own MySQL](#using-a-different-port-or-your-own-mysql).

## Run it

From the repository root:

```bash
# 1. Database (MySQL 8, database "duckstore", user "duckuser" / "duckpassword", port 3306)
docker compose up -d

# 2. Backend on http://localhost:8080 (Flyway creates the schema on first start)
cd backend
./mvnw spring-boot:run

# 3. Frontend on http://localhost:5173 (proxies /api to the backend)
cd frontend
npm install
npm run dev
```

The backend reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from the environment; the defaults match
`docker-compose.yml`, so no setup is needed locally. The pricing numbers (rates, thresholds, fees) are in
`backend/src/main/resources/application.properties` under `pricing.*`. The frontend dev server proxies `/api` to
`http://localhost:8080`; set `VITE_API_URL` (see `frontend/.env.example`) to point it elsewhere.

### Using a different port or your own MySQL

The database must be **MySQL 8.0.19 or newer**. MariaDB and older MySQL versions are not supported: adding a duck
uses a single atomic `INSERT ... ON DUPLICATE KEY UPDATE` with a row alias (`AS incoming`), which they do not
accept. That statement is what keeps the merge rule correct under concurrent requests.

To use your own MySQL instead of Docker, create an empty database and a user that can create tables in it, then
point the backend at it (Flyway creates the schema on start):

```sql
CREATE DATABASE duckstore;
CREATE USER 'duckuser'@'%' IDENTIFIED BY 'duckpassword';
GRANT ALL ON duckstore.* TO 'duckuser'@'%';
```

```bash
export DB_URL=jdbc:mysql://localhost:3306/duckstore   # change host, port or database name here
export DB_USER=duckuser
export DB_PASSWORD=duckpassword
cd backend && ./mvnw spring-boot:run
```

If port `3306` is taken, change the left side of `"3306:3306"` in `docker-compose.yml` (for example `"3307:3306"`)
and set `DB_URL=jdbc:mysql://localhost:3307/duckstore`. If port `8080` is taken, start the backend with
`SERVER_PORT=8081 ./mvnw spring-boot:run` and change the proxy target in `frontend/vite.config.js` to match.

## Run the tests

```bash
cd backend
./mvnw test
```

Frontend tests (Vitest + React Testing Library, no backend needed; `fetch` is mocked):

```bash
cd frontend
npm test
```

Docker must be running for the backend tests: integration tests start a throwaway MySQL 8 with Testcontainers and apply the real Flyway
migration. Pricing and packaging rules are plain unit tests (`domain` package, no Spring or database).

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/ducks` | Add a duck, or merge into the existing one (same color, size, price) |
| `GET` | `/api/ducks` | List non-deleted ducks sorted by quantity |
| `PUT` | `/api/ducks/{id}` | Edit price and quantity only |
| `DELETE` | `/api/ducks/{id}` | Logical delete |
| `POST` | `/api/orders/quote` | Package and price an order (stock is not changed) |
| `POST` | `/api/orders` | Same pricing, and atomically takes the ordered units from stock |

Colors: `Red, Green, Yellow, Black`. Sizes: `XLarge, Large, Medium, Small, XSmall`. Shipping modes:
`Land, Air, Sea`. Values are case-sensitive.

```bash
curl -X POST localhost:8080/api/ducks -H 'Content-Type: application/json' \
  -d '{"color":"Red","size":"XLarge","price":200.00,"quantity":10}'

curl -X POST localhost:8080/api/orders/quote -H 'Content-Type: application/json' \
  -d '{"color":"Red","size":"XLarge","quantity":10,"country":"India","shippingMode":"Land"}'
```

Errors always have the shape `{ "message": "...", "fieldErrors": { "field": "reason" } }` with status 400
(validation), 404 (unknown duck / no matching duck for an order), or 409 (conflict, stale edit, insufficient stock).

## Design

Layers are controller -> service -> repository. Packaging and pricing live in the `domain` package, which has no
Spring or JPA imports. Request/response DTOs are records, separate from the JPA entity.

The warehouse is split by layer: `warehouse/controller`, `warehouse/service`, `warehouse/repository`,
`warehouse/model` (the JPA entity) and `warehouse/dto` (request/response records). The store follows the same
layout without a repository or entity of its own: `store/controller`, `store/service`, `store/dto`.

The pure domain is split by concern: `domain/model` (shared enums), `domain/packaging` and `domain/pricing`.

- **Packaging** (`PackagingSelector`): Strategy pattern in two stages. A `PackageStrategy` per package
  (`WoodPackage`, `CardboardPackage`, `PlasticPackage`) picks the package from the size, then a
  `ProtectionStrategy` per shipping mode (`LandProtection`, `AirProtection`, `SeaProtection`) picks the
  protection from the mode and the package. The first matching strategy wins, and a missing one fails loudly.
- **Pricing** (`PricingCalculator`): Strategy pattern. Each rule (`VolumeDiscountRule`, `PackageRule`,
  `DestinationRule`, `ShippingRule`) is a small `PricingRule` that returns itemized `Adjustment`s. Adding a rule
  means adding a class and listing it in `PricingCalculator.of(...)`. The rules contain no numbers: every rate,
  threshold and fee is read from the `pricing.*` keys of `application.properties` (bound and validated by
  `config/PricingProperties`, wired to the domain in `config/PricingConfig`), so the `domain` package stays free of
  Spring. A missing or negative value stops the application at startup.
- **API paths** are constants: `web/ApiPaths` in the backend and `API_ENDPOINTS` in `frontend/src/constants.js`.

## Decisions on ambiguous points

**Warehouse**

- **Merge invariant under concurrency.** The database has a unique key on `(color, size, price)` and the add is a
  single `INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + new` statement, so two simultaneous adds can
  neither create duplicates nor lose quantity (covered by a 20-thread test).
- **Ids can have gaps.** Ids are stable and unique, but not guaranteed to be consecutive. MySQL reserves the next
  auto-increment id before it knows the insert will turn into a merge, so every merging add skips one id (for
  example 1, 3, 4 after one merge). This is accepted on purpose: avoiding it would mean a separate "look up, then
  insert" step, which re-opens the race (and, under InnoDB gap locks, deadlocks) that the single atomic statement
  closes. The merge invariant matters more than gap-free ids.
- **Price is money, not a double.** Stored as `DECIMAL(10,2)`, handled as `BigDecimal`. Prices with more than two
  decimals or that are not positive are rejected (400). Quantity on add must be at least 1; editing may set it to 0.
- **Re-adding a deleted duck.** Deleted ducks keep their row (and their unique key). Adding a duck with the same
  color, size and price revives that row with exactly the new quantity (the old stock is not resurrected).
- **Status of `POST /api/ducks`.** 201 when a new record was inserted, 200 when the quantity was merged into an
  existing record (including a revived deleted one). The decision uses MySQL's affected-rows result of the same
  atomic statement, so it is correct under concurrency too.
- **Editing into an existing duck.** Changing a price so that it equals another duck's price (same color and size,
  including a logically deleted one) is rejected with 409 and a specific message rather than silently merging.
- **Concurrent edits.** Ducks have a version (optimistic locking). A merging add, an edit and a delete all bump it,
  so an edit or delete based on stale data fails with 409 ("changed by someone else") instead of overwriting.
- **Maximum stock.** At most 1,000,000 units per duck record: enforced by validation (400) on each request and by a
  database CHECK constraint, so a merge that would exceed it returns 409 and never overflows.
- **List order.** Ascending by quantity (lowest stock first), ties by id.
- **Delete.** Logical (`deleted = true`), confirmed in the UI. Deleting or editing an already deleted duck is a 404.

**Store**

- **Which price is used.** The order carries no price. The API uses the cheapest non-deleted duck of the requested
  color and size that has enough stock for the whole order. No such color/size at all: 404. Exists but not enough
  stock: 409.
- **Quote vs order.** `POST /api/orders/quote` only prices and never changes stock, so it can be called repeatedly.
  `POST /api/orders` returns the same body (status 201) and takes the units with a single conditional
  `UPDATE ... WHERE quantity >= n`, so concurrent orders can never take the same units. If the stock changed
  between pricing and taking it, the loser gets 409. Orders are not stored: only the stock is reduced.
- **Order of calculation.**
  1. Base cost = quantity x price.
  2. More than 100 units: -20% of the base cost.
  3. Package (+5% wood, +10% plastic, -1% cardboard) and destination (+18% USA, +13% Bolivia, +19% India, +15%
     otherwise) percentages are each taken from the same subtotal (base cost after the volume discount). They do
     not compound on each other.
  4. Shipping is added last: Sea +400, Land +10 per unit, Air +30 per unit (reduced by 15% above 1000 units,
     shown as a separate negative line).
- **Rounding.** Every line is rounded to 2 decimals `HALF_UP` and the total is the exact sum of the lines, so the
  breakdown always adds up to the cent.
- **Boundaries.** "More than 100" and "exceeds 1000" are strict: exactly 100 or 1000 units get no discount.
- **Country matching.** Case-insensitive and trimmed: `USA`, `Bolivia`, `India`. Anything else is "other" (+15%).
  Variants such as "United States" are not treated as the USA.
- **Response.** `packageType`, `protections`, `unitPrice`, `baseCost`, `breakdown` (each adjustment; negative =
  discount) and `total`.
