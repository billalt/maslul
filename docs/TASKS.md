# Claude Code task breakdown — M0 and M1

Sizing rule: each task is one vertical slice, compiles, has tests, reviewable in one PR.
If a task balloons past ~400 changed lines, it was scoped wrong — split it.

Workflow per task:
1. `claude` from the right subdirectory
2. Paste the task prompt
3. **Review the skeleton it produces. Do not skip this.** Correct the shape now, not after
   the logic is written.
4. Say "implement" once the skeleton is right
5. Review, run tests, commit

---

## M0 — Skeleton (target: 3 weeks)

### M0.1 — Project scaffold
> Read docs/spec.md §4. Scaffold a Spring Boot 3 / Kotlin project under `api/` as a
> multi-module Gradle build with modules: identity, assets, dispatch, sync, telemetry,
> reports, plus a `common` module and an `app` module that wires them. Add Flyway,
> PostGIS via Testcontainers, and a docker-compose with Postgres+PostGIS and Redis.
> No domain code yet — just the structure, a health endpoint, and a passing test.
> Include a ArchUnit test that fails if one feature module imports another's entities.

### M0.2 — Tenant and identity
> Implement the `identity` module: Tenant, User, Device entities and migrations.
> TenantContext filter that resolves tenant from the JWT and makes it available to
> repositories. Enable Postgres RLS on tenant-scoped tables. Skeleton first.

### M0.3 — Assets: ServicePoint and Container
> Read spec §3 carefully, especially ServicePoint POINT vs SEGMENT. Implement the
> `assets` module: ServicePoint (with PostGIS geometry column supporting both Point and
> LineString), Container, Vehicle, Site, Zone. Migrations, repositories, CRUD endpoints.
> Skeleton first — I want to review the geometry modeling before you write logic.

### M0.4 — Bulk import
> CSV and GeoJSON import for ServicePoints and Containers. Must handle: duplicate
> detection by proximity + external_ref, Hebrew and Arabic text, malformed addresses
> (report them, don't fail the whole import). Return a per-row result report.

### M0.5 — Dispatch skeleton
> Implement `dispatch`: RouteTemplate, Shift, RouteInstance (versioned), Stop with the
> planned/actual column split from spec §3. Materialize a RouteInstance from a template
> for a given date. No optimization — order comes from the template.

---

## M1 — Driver app happy path (target: 6 weeks)

### M1.1 — Sync ingestion  ← the highest-risk task, do it early
> Read spec §5 in full. Implement the `sync` module: POST /v1/sync/events with
> idempotency by event_id, device_seq ordering, gap quarantine, and a watermark response.
> Separately POST /v1/sync/breadcrumbs, lossy, gzip. These must not share code paths.
> Skeleton first. Then implement. Then write the test that sends the same batch 3x and
> asserts identical state, plus a test with an out-of-order device_seq.

### M1.2 — Routing provider
> Define a `RoutingProvider` interface (route between stops, turn-by-turn instructions,
> matrix for N points). Implement a GraphHopper-backed adapter with a truck profile.
> Add a `road_restriction` table and layer it over GraphHopper's results at query time.
> Include a stub implementation for tests.

### M1.3 — Android scaffold
> (run from `driver/`) Scaffold a Kotlin/Jetpack Compose Android app targeting API 30+.
> Room for local storage, WorkManager for sync, a foreground location service.
> RTL layout, Hebrew and Arabic resource files, no hardcoded strings.
> Screens as empty composables per spec §7: S1–S8. Navigation wired. No logic.

### M1.4 — Device sync client
> Implement the device side of spec §5: append-only event queue in Room, monotonic
> sequence, batching, watermark handling, bounded breadcrumb ring buffer that drops
> oldest on overflow. Must survive process death and airplane mode. Write instrumented
> tests for both.

### M1.5 — Navigation + geofence auto-advance
> Implement S3 and the geofence logic from spec §5: auto-ARRIVED at 25m below 5km/h,
> auto-COLLECTED on exit above 10km/h. Radius configurable per zone. Log every
> auto-decision with the GPS fix and accuracy that produced it.

### M1.6 — Stop actions
> Implement S4 per spec §7. One primary "בוצע" button, 2x2 exception grid, mandatory
> camera for contested outcomes, direct-to-S3 upload via presigned URL. 56dp minimum
> touch targets. Test with the screen at arm's length in sunlight before you call it done.

### M1.7 — Shift lifecycle
> S1 and S7. Location collection starts on SHIFT_START and stops on SHIFT_END, enforced
> in the service — this is a legal requirement per spec §9, not a UX detail.
> Explicit "all data uploaded" confirmation on shift end.

---

## What NOT to hand to Claude Code

- Geofence radius tuning — needs real trucks on real streets
- Hebrew/Arabic TTS quality validation — needs the actual device and a native speaker
- Map data corrections — needs drivers telling you what's wrong
- The privacy/labour law implementation review — needs a lawyer
- Whether the exception queue is actually usable — needs a dispatcher using it
- Deciding contractor-first vs municipality-first — that's your call
