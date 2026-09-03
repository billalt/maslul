# Maslul — waste collection routing platform

Read `docs/spec.md` before any non-trivial task. It is the source of truth for scope,
domain model, and architecture. If a task conflicts with the spec, stop and say so —
do not silently resolve the conflict.

## Non-negotiable working rules

1. **Skeleton before logic.** For any new feature, first produce the types, interfaces,
   empty method bodies, and migration. Stop. Wait for my review. Then implement.
2. **No speculative features.** Build exactly what the task asks. No "while I was here
   I also added…". No configuration options nobody requested. No abstraction with one
   implementation. If you think something extra is needed, say so and wait.
3. **Plain loops, not Streams.** In Kotlin/Java, use `for` loops. No `.stream()` chains.
   Readability at 06:00 during a production incident beats elegance.
4. **Answer directly.** If I ask why something is broken, tell me. Do not ask me leading
   questions to help me discover it.
5. **Small changes.** One task = one vertical slice = one reviewable PR. If a task looks
   like it needs more than ~400 changed lines, split it and tell me how.

## Architecture guardrails

- **Modular monolith. Do not split into microservices.** See spec §4 for the volume
  numbers that justify this. Do not introduce Kafka, event sourcing, CQRS, or a message
  broker. If you believe the workload has outgrown this, show me the numbers first.
- Module boundaries are enforced: `identity`, `assets`, `dispatch`, `sync`, `telemetry`,
  `reports`. **No cross-module entity imports.** Modules talk through interfaces defined
  in the calling module.
- One Postgres. PostGIS for spatial, declarative partitioning for breadcrumbs.
  No TimescaleDB, no separate time-series store.
- Redis holds nothing durable. If Redis is wiped, only the live map degrades.
- Routing engine sits behind an interface (`RoutingProvider`) so GraphHopper vs HERE
  stays swappable. That is the only abstraction-for-future-flexibility I want.

## Multi-tenancy — treat as a security boundary

Every query is tenant-scoped. Enforcement is in two places and both are mandatory:
`TenantContext` filter on the request path, and Postgres row-level security on the table.
A test that proves cross-tenant isolation ships with every new table. Do not skip this
because it feels redundant — it is deliberately redundant.

## Domain vocabulary (use these exact terms)

- **ServicePoint** — a location to be serviced. Type `POINT` or `SEGMENT`. Never "bin location".
- **Container** — a physical bin at a ServicePoint. Never "bin" in code.
- **Stop** — a ServicePoint within a RouteInstance, with planned and actual columns on one row.
- **RouteTemplate** vs **RouteInstance** — template is reusable, instance is today's, versioned.
- **Event** — device-originated, append-only, idempotent by `event_id`.
- **Breadcrumb** — GPS point. Lossy. Never in the same queue as Events.

## Things that will get a PR rejected

- Discarding a recorded stop outcome for any reason (see spec §5, off-plan events)
- Events and breadcrumbs sharing a retry budget or queue
- Location tracking outside an active shift — this is a legal requirement, not a preference
- Any `TODO: handle offline` — offline is the normal state, handle it now
- Hardcoded Hebrew or Arabic strings outside resource files
- LTR-assuming layout in console or driver app
- A `required_body_type` check being skipped when assigning stops to vehicles

## Localization

Hebrew and Arabic, RTL. All user-facing strings in resource files from the first commit —
retrofitting i18n is a week of work I am not spending. Numbers, dates, plate numbers, and
IDs stay LTR inside RTL text.

## Testing

- Unit tests for domain logic. Testcontainers (Postgres+PostGIS) for repository tests.
- Every sync endpoint gets an idempotency test: send the same batch three times, assert
  identical state.
- Every new endpoint gets a cross-tenant isolation test.
- No mocking the database. No mocking what you don't own.

## Commands

(fill in once the build is scaffolded)
- Build: `./gradlew build`
- Test: `./gradlew test`
- Migrate: `./gradlew flywayMigrate`
- Run locally: `docker compose up -d && ./gradlew bootRun`
