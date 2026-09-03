# Waste Collection Routing Platform — Product Spec

Working name: **Maslul** (מסלול)
Market: Israeli municipalities and their waste-hauling contractors
Status: v0.1 draft for build planning

---

## 0. The one decision that shapes everything else

**Do not build a route optimizer in V1.**

Every vendor in this space leads with "we optimize your routes and save you 15%." That is the
hardest part of the system, the slowest to validate, and the least likely to close a first deal.
Municipalities already have routes that work. Their drivers know them. Nobody gets fired for a
suboptimal route.

What they *don't* have — and what the State Comptroller has explicitly flagged as a gap in
Israeli municipalities — is any reliable answer to:

- Did truck 7 actually go out this morning?
- Did we actually service Rehov HaGalil, or is the resident complaining for nothing?
- Is the contractor billing us for pickups that never happened?

**V1 sells proof-of-service and dispatch visibility.** Optimization is V3. The architecture
should make room for the optimizer without waiting on it.

---

## 1. Market context

### Who buys

| Buyer | What they care about | Budget |
|---|---|---|
| **Municipality (רשות מקומית)** | Oversight of the contractor, resident complaints, regulatory reporting | Tender (מכרז), 12–18 month cycle |
| **Hauling contractor (קבלן פינוי)** | Proving service to avoid deductions, fleet utilization, driver onboarding | Faster decision, direct sale |
| **Multi-municipality cluster (איגוד ערים)** | Shared infrastructure, comparison across members | Slow, but larger |

The contractor is the faster first customer. They feel the pain daily and can sign without a
tender. The municipality is the bigger, stickier contract. Design multi-tenant from day one so a
contractor account can later be "shared" with the municipality as a read-only oversight tenant —
that dual view is itself a selling point.

### Existing competition

- **GreenQ** (Jerusalem, founded 2015) — hardware-first: a device on the truck that weighs each
  lifted bin without bin sensors. Has claimed 11+ Israeli cities. Sells via distributors.
- **Sensoneo** (Slovakia) — sensors + WMS + driver app; the page that started this.
- **AMCS** (Ireland), **MOBA** (Germany) — mature, expensive, enterprise, weak Hebrew support.

**The gap:** GreenQ is a telemetry company, not a dispatch company. Sensoneo and AMCS are foreign
products with bolted-on Hebrew. Nobody is shipping a genuinely Hebrew/Arabic-native, RTL-first
dispatch + proof-of-service product tuned for Israeli municipal workflows and Israeli street
geometry. That is the wedge.

**The risk to be honest about:** landfill fees in Israel are low compared to Europe, which
historically has blunted the ROI story for efficiency plays. Another reason to sell *accountability*
rather than *savings*.

---

## 2. Scope

### V1 — "we know what happened" (target: 3–4 months)

- Master data: containers, vehicles, drivers, zones, disposal sites
- Route definition — **imported or drawn manually**, not optimized
- Android driver app: receive route, navigate, mark each stop, work offline
- Live map + shift monitoring for dispatcher
- Plan-vs-actual report, per-address service proof
- Hebrew + Arabic UI, RTL

### V2 — "we can react" (+2 months)

- Mid-shift replanning and stop reassignment between trucks
- Driver ⇄ dispatcher messaging and issue reports with photos
- Resident complaint lookup: address → was it serviced, when, by whom
- Weighbridge tickets (tonnage per trip)

### V3 — "we can improve"

- Route sequencing optimizer (OR-Tools CVRPTW)
- Fill-level sensor ingestion → dynamic stop lists
- Regulatory exports (תמיר / אל"ה, Ministry of Environmental Protection)
- RFID bin identification

### Explicitly not building

- **The navigation engine.** Truck-profile routing, map matching, turn-by-turn voice. Licensed
  or self-hosted OSS. Building this is a multi-year project.
- **Bin fill-level sensor hardware.** Integrate with existing vendors if a customer has them.
- **Payroll, invoicing, ERP.** Export CSV and let their systems handle it.
- **iOS app.** Fleet devices are Android. Revisit only if a customer demands it.

---

## 3. Domain model

Skeleton first — these are the entities and their relationships, no behavior yet.

```
Tenant
 ├── Zone            (geofenced collection area, e.g. "Nazareth – Old City")
 ├── Site            (depot | transfer station | landfill | recycling facility)
 ├── Vehicle         (plate, type, capacity_m3, capacity_kg, height_cm, width_cm, body_type)
 ├── Driver          (user, license class, assigned default vehicle)
 ├── ServicePoint    ← the core spatial entity
 │    └── Container  (0..n per point)
 ├── RouteTemplate   (reusable ordered list of ServicePoints + schedule rule)
 └── Shift
      ├── RouteInstance   (today's materialization of a template, versioned)
      │    └── Stop       (ordered; planned + actual)
      ├── TripLeg         (depot → stops → disposal site → back; one per truck load)
      └── Event[]         (append-only log from the device)
```

### ServicePoint vs Container — get this right early

Israeli collection has two distinct modes and the model must handle both:

- **Point collection** — a 1100L communal bin, an underground bin (פח טמון קרקע), a compactor.
  One `ServicePoint` = one physical location with 1..n `Container`s. Precise geo-coordinate.
  Driver stops, services, moves on.
- **Segment collection** — residential 240L bins put out per building along a street. There is no
  meaningful per-bin stop. The unit of work is *a street segment, direction of travel, side of
  street*.

Modeling the second as a list of point stops produces 300-stop routes that are useless to navigate
and impossible to optimize. Model it as `ServicePoint { type: SEGMENT, geometry: LineString,
side: LEFT|RIGHT|BOTH }` and let the driver app treat it as "drive this line, then confirm."

This distinction is the single most common modeling mistake in this domain. European vendors get
it wrong in Israeli old-city neighborhoods constantly.

### Container

```
Container
  id
  service_point_id
  external_ref        (municipality's own asset number)
  rfid_tag            (nullable — V3)
  waste_stream        GENERAL | PACKAGING_ORANGE | PAPER_BLUE | GLASS_PURPLE | CARTON | ORGANIC_BROWN | TEXTILE | E_WASTE
  volume_liters       120 | 240 | 360 | 660 | 1100 | 5000 (underground) | ...
  container_type      WHEELIE | COMMUNAL | UNDERGROUND | COMPACTOR | ROLLOFF
  required_body_type  REAR_LOADER | SIDE_LOADER | CRANE | HOOK_LIFT
  status              ACTIVE | DAMAGED | REMOVED
  access_notes        free text — "gate code 4471", "reverse in from north only"
```

`required_body_type` is not optional. An underground bin needs a crane truck. Sending a rear
loader is a wasted trip, and a system that allows it will lose credibility on week one.

### Stop — planned and actual side by side

```
Stop
  route_instance_id
  sequence            (planned order)
  service_point_id
  planned_arrival     (nullable)
  planned_duration_s

  actual_arrival      (nullable)
  actual_departure    (nullable)
  outcome             PENDING | COLLECTED | BLOCKED | NOT_PRESENTED | INACCESSIBLE | SKIPPED | PARTIAL
  outcome_reason      (enum, required when outcome != COLLECTED)
  recorded_by         AUTO_GEOFENCE | DRIVER | DISPATCHER
  media_ids[]
```

The `planned` / `actual` split on one row is deliberate. Every valuable report in this product is
a comparison of those two column groups. Splitting them across tables makes the core query a join
you'll write forty times.

### Outcome reasons (the enum that decides whether people trust the product)

```
BLOCKED_BY_VEHICLE      car parked blocking the bin
BLOCKED_BY_CONSTRUCTION
BIN_NOT_PRESENTED       resident didn't put it out
BIN_MISSING             bin should be here and isn't
BIN_DAMAGED
ACCESS_ROAD_CLOSED
OVERFLOW_LEFT_BEHIND    couldn't take it all
TRUCK_FULL              deferred to next trip
SAFETY_HAZARD
```

Every non-collection needs a reason and, for the contested ones (BLOCKED, NOT_PRESENTED,
BIN_MISSING), a photo. That photo is what ends the argument between the municipality and the
contractor. It is the product's core value artifact — treat it as a first-class object, not an
attachment.

---

## 4. Architecture

### The shape

```
                        ┌─────────────────────────┐
   Android              │      Ops Console        │
   Driver App           │   React SPA (RTL)       │
   (Kotlin/Compose)     └───────────┬─────────────┘
        │                           │
        │  REST + batch sync        │  REST + SSE (live positions)
        │                           │
        └───────────┬───────────────┘
                    │
        ┌───────────▼────────────────────────────────┐
        │        maslul-api  (Spring Boot)           │
        │        modular monolith                    │
        │  ┌──────────┬──────────┬────────────────┐  │
        │  │ assets   │ dispatch │ telemetry      │  │
        │  ├──────────┼──────────┼────────────────┤  │
        │  │ sync     │ reports  │ identity       │  │
        │  └──────────┴──────────┴────────────────┘  │
        └───┬─────────────┬──────────────┬───────────┘
            │             │              │
   ┌────────▼───┐  ┌──────▼─────┐  ┌─────▼──────┐
   │ PostgreSQL │  │   Redis    │  │    S3      │
   │ + PostGIS  │  │ live state │  │  photos    │
   └────────────┘  └────────────┘  └────────────┘
            │
   ┌────────▼──────────────┐
   │  routing engine       │   GraphHopper (self-hosted, truck profile)
   │  (separate process)   │   or HERE Fleet Telematics (managed)
   └───────────────────────┘
```

That's it. One deployable service, one database, one cache, one object store, one routing engine.

### Why a modular monolith and not microservices

Run the numbers before splitting anything:

- **Target scale:** 200 vehicles, 50,000 service points, 20 tenants.
- **Telemetry:** 200 trucks × 1 ping / 5s × 10h/day = **1.4M points/day**. At ~60 bytes that's
  90 MB/day raw, ~33 GB/year. PostGIS handles this without breaking a sweat, especially with
  monthly partitioning on the breadcrumb table.
- **Peak write rate:** 40 writes/sec. A single Postgres instance does this on a laptop.
- **Peak concurrent users:** ~250 drivers + ~30 dispatchers.

There is no volume argument for Kafka, for a separate telemetry service, or for anything
event-sourced. Enforce module boundaries in the codebase (separate Gradle modules, no cross-module
entity imports, communicate via interfaces) so that *if* a module ever needs to be extracted,
it can be. Don't pay the distributed-systems tax before there's a distributed-systems problem.

Revisit when: a single tenant crosses ~500 vehicles, or a customer demands data residency
separation, or the reporting workload starts interfering with the write path (fix that with a
read replica first, not a service split).

### Module boundaries

| Module | Owns | Key tables |
|---|---|---|
| `identity` | tenants, users, devices, auth | `tenant`, `user`, `device`, `refresh_token` |
| `assets` | service points, containers, vehicles, sites, zones | `service_point`, `container`, `vehicle`, `site` |
| `dispatch` | templates, shifts, route instances, stops, replanning | `route_template`, `shift`, `route_instance`, `stop` |
| `sync` | device event ingestion, idempotency, conflict resolution | `device_event`, `sync_cursor` |
| `telemetry` | GPS breadcrumbs, live positions, map matching | `breadcrumb` (partitioned), Redis live state |
| `reports` | plan-vs-actual, service proof, tonnage, exports | read-only views + materialized views |

### Storage choices

**PostgreSQL + PostGIS** — one database, three workloads:
- Transactional (routes, stops, assets) — normal tables.
- Spatial queries (which service points are in this zone, nearest point to this GPS fix) —
  PostGIS with GiST indexes.
- Time-series (breadcrumbs) — a partitioned table, monthly partitions, drop partitions older
  than the retention policy. **Not TimescaleDB in V1** — plain declarative partitioning is
  enough at 1.4M rows/day and one less thing to operate.

**Redis** — live truck positions (last known fix per vehicle, TTL 5 min), active shift state
for the dispatcher map, and device sync locks. Nothing durable. If Redis is lost, the live map
goes blank for 30 seconds and refills. Nothing else breaks.

**S3** — photos, only. Uploaded directly from the device via pre-signed PUT so photo bytes never
touch the API. Lifecycle rule: transition to infrequent access at 90 days.

### Routing engine — buy vs self-host

| | GraphHopper self-hosted | HERE Fleet Telematics | Google Routes API |
|---|---|---|---|
| Truck profiles (weight/height/width) | Yes, configurable | Yes, best-in-class | Limited |
| Israeli road data | OSM — good in cities, patchy elsewhere | HERE proprietary — good | Good |
| Offline navigation on device | Needs custom work | SDK supports it | No |
| Hebrew/Arabic turn instructions | Needs work | Yes | Yes |
| Cost | Server cost only | Per-transaction, expensive at fleet scale | Per-transaction |
| Custom truck restrictions (your own "don't send a truck here" edits) | Full control | Limited | None |

**Recommendation: GraphHopper self-hosted, with a curated OSM extract of Israel.**

The deciding factor is the last row. You *will* discover streets that OSM says are passable and a
26-ton rear loader cannot use. You need to be able to add those restrictions yourself, from driver
feedback, without waiting on a vendor. That capability compounds into a real moat — after two years
of operation you own the best truck-passability map of Israeli residential streets in existence,
and no foreign competitor can replicate it.

Trade-off accepted: you own map data quality, including the gaps in OSM coverage in Arab towns and
in unrecognized villages. Budget for a data-quality workflow (see §9).

---

## 5. The hard problem: offline-first sync

This is where products in this category actually fail. A garbage truck spends its day in
underground parking garages, stairwell-shadowed alleys, and industrial dead zones. If the app
needs connectivity to record a stop, drivers stop using it within a week and the whole value
proposition collapses.

### Principle

**The device is the source of truth for what happened. The server is the source of truth for what
should happen.**

That single sentence resolves almost every conflict case. Route assignments flow server → device
and the server wins. Stop outcomes flow device → server and the device wins.

### Device-side design

```
Room DB on device:
  route_cache      current RouteInstance + all Stops + cached map tiles
  event_queue      append-only, never mutated, never deleted until ACKed
  breadcrumb_queue ring buffer, lossy-tolerable
```

Every event is generated on-device with:

```
Event
  event_id        UUID v4, generated on device      ← idempotency key
  device_seq      monotonic counter per device      ← ordering
  device_time     device wall clock (untrusted)
  monotonic_ms    SystemClock.elapsedRealtime()     ← trusted for durations
  type            SHIFT_START | ARRIVED | STOP_OUTCOME | ISSUE | TIP_ARRIVED | SHIFT_END
  payload         JSON
```

`device_time` is untrusted — drivers change phone clocks, timezones shift, devices reboot mid-shift.
Record it, but compute all SLA-relevant timing from `monotonic_ms` deltas anchored to the last
server-confirmed timestamp. The server stamps `server_received_at` on ingestion and that is what
reports use.

### Server-side ingestion

```
POST /v1/sync/events        body: { device_id, events: [...] }
```

- Upsert by `event_id` — replaying the same batch is a no-op. Devices will replay batches,
  constantly, because they lose connectivity mid-POST and cannot tell whether the write landed.
- Process in `device_seq` order within a batch; reject and quarantine gaps rather than applying
  out of order.
- Respond with the highest `device_seq` durably persisted. The device deletes everything up to
  that watermark and nothing more.
- Cap batch size at ~200 events. A device offline for a full shift will have ~500; let it drain
  in three batches rather than one 8 MB request over a marginal connection.

### Breadcrumbs are a separate, lossier channel

```
POST /v1/sync/breadcrumbs   body: { device_id, points: [{lat,lng,t,speed,heading,accuracy}] }
```

Batched every 30s when online, buffered in a bounded ring buffer when not. **If the buffer
overflows, drop the oldest points.** A gap in the GPS trace is a cosmetic problem. A dropped stop
outcome is a billing dispute. Never let these two share a queue or a retry budget — that mistake
turns a nice-to-have into a data-loss bug.

Delta-encode and gzip the batch. Trucks run on cellular data plans the customer pays for; a driver
app that burns 500 MB/month becomes a line item someone wants removed.

### Replanning conflicts

The dispatcher edits a route while the truck is mid-shift. Handle it with route versioning:

1. Server increments `route_instance.version`, pushes via FCM (with polling fallback — FCM on
   cheap Android fleet devices is unreliable).
2. Device fetches the new version but **stages it**. It does not swap the active route while the
   driver is navigating to a stop.
3. Swap happens at the next natural boundary: after a stop outcome is recorded, or on explicit
   driver acknowledgment. Show a non-blocking banner: "מסלול עודכן — 3 נקודות נוספו."
4. If the driver already serviced a stop that the new version removed, keep the outcome as an
   **off-plan event**. Never discard recorded work. Off-plan collections are surfaced in the
   dispatcher's exception queue — they usually indicate a real-world thing worth knowing about.

### Auto-advance: the feature that makes it usable

Under Israeli traffic law a driver may not operate a handheld device while driving; a mounted
device with minimal interaction is what's permitted. And in a rear-loader crew the driver often
isn't the one who could tap anyway — the loader (a worker on foot) is.

So: **geofence-based auto-arrival.** When the vehicle enters a 25 m radius of the next service
point below 5 km/h, auto-record `ARRIVED`. When it leaves the radius above 10 km/h, auto-record
`COLLECTED` and advance to the next stop. The driver only touches the screen for exceptions.

This inverts the interaction model: the default path is zero taps, and the UI exists for the
5–10% of stops that go wrong. Design the whole app around that.

Tuning notes: 25 m is a starting point, tune per zone — dense underground-bin areas may need 15 m,
rural routes 40 m. Log every auto-decision with the GPS fix and accuracy that produced it, so
disputed stops can be audited.

---

## 6. API skeleton

Versioned, REST, JSON. No GraphQL — the client set is small and known.

```
# Device / driver app
POST   /v1/auth/device/register        device_code → device credentials
POST   /v1/auth/device/token           refresh
GET    /v1/shifts/current              today's shift for this device's driver
POST   /v1/shifts/{id}/start           vehicle, odometer, crew, pre-trip checklist
GET    /v1/routes/{id}                 full route incl. stops; ETag + version
GET    /v1/routes/{id}/delta?from=v3   incremental update
POST   /v1/sync/events                 batched, idempotent
POST   /v1/sync/breadcrumbs            batched, lossy
POST   /v1/media/presign               → S3 pre-signed PUT
POST   /v1/shifts/{id}/end             odometer, summary

# Ops console
GET    /v1/fleet/live                  SSE stream of position updates
GET    /v1/shifts?date=&zone=&status=
PATCH  /v1/routes/{id}                 reorder / add / remove stops → new version
POST   /v1/routes/{id}/reassign        move stops to another vehicle
GET    /v1/exceptions?date=&resolved=  the dispatcher's work queue
GET    /v1/service-history?address=    resident complaint lookup
GET    /v1/reports/plan-vs-actual
POST   /v1/exports/regulatory          async job → S3 link

# Admin
CRUD   /v1/service-points, /v1/containers, /v1/vehicles, /v1/drivers, /v1/zones, /v1/sites
POST   /v1/service-points/import       CSV/GeoJSON bulk load
```

Auth: OIDC for console users, long-lived device credentials + short-lived JWT for the app. Tenant
scoping enforced in a single place — a `TenantContext` filter plus Postgres row-level security as
a second net. Multi-tenant data leakage is the one bug that ends a municipal contract instantly;
belt and braces is warranted here even though it's redundant.

---

## 7. Driver app — UI design

### Physical context (this drives every decision)

- Device is **mounted**, roughly 60–80 cm from the driver's eyes, at an angle.
- Ambient light is direct Israeli sun from 07:00. Screen glare is constant.
- The person interacting may be wearing dirty work gloves.
- The vehicle vibrates. Precision tapping is not available.
- The primary operator may be the **loader on foot**, not the driver.
- Shift starts around 05:00–06:00. Nobody is patient at 05:00.

Design consequences: minimum touch target **56 dp** (not the 48 dp Material default), maximum
contrast, no thin fonts, no swipe gestures for anything important, no long-press, no
bottom-sheet drag. Buttons and voice, nothing else.

### Screens

**S1 — Shift start**
Vehicle picker (default: last used, one tap to confirm). Odometer entry with a big numeric pad.
Crew present (checkboxes). Pre-trip checklist if the customer requires one. One large green
"התחל משמרת" button.
*Target: 15 seconds from app open to driving.*

**S2 — Route overview**
Map with the full route drawn and a progress header: `24 / 118 נקודות · 3.2 שעות משוערות`.
Collapsible list below. Read-only — this is orientation, not the working screen.

**S3 — Active navigation** *(the screen that's up 95% of the time)*
- Full-bleed map, north-up locked, truck icon centered low.
- Top bar: next turn arrow + street name, very large.
- Bottom card: next service point name, distance, container count and stream (colored chips —
  green/orange/blue/purple map to the Israeli bin color scheme, which every driver already knows).
- Persistent progress bar.
- Voice guidance in the driver's chosen language.
- **Zero required interaction.** Auto-arrival handles the normal path.

**S4 — Stop actions** *(appears on arrival, dismissible)*
One big primary button: **בוצע** (Collected). Below it, four exception buttons in a 2×2 grid, each
with icon + label: חסום (Blocked), לא הוצא (Not presented), חסר (Missing), גדוש (Overflow).
Tapping an exception opens the camera immediately — photo is mandatory for contested outcomes,
and asking for it later means it never happens.
*Target: 1 tap for the normal case, 3 for an exception including the photo.*

**S5 — Issue report**
Free-standing report not tied to a stop: illegal dumping, damaged container, blocked street,
vehicle fault. Camera-first, then category, then optional voice note (voice, not typing — see the
gloves). Auto-attaches GPS and timestamp.

**S6 — Disposal trip**
Triggered by the driver ("truck full") or automatically when estimated load exceeds capacity.
Navigates to the assigned transfer station. On arrival: photograph the weighbridge ticket. OCR it
server-side and let the dispatcher correct — don't make the driver type numbers.

**S7 — Shift end**
Summary: stops completed, exceptions, distance, trips. Final odometer. Sync status indicator with
an explicit "all data uploaded" confirmation — drivers need to see that their work landed before
they hand back the device.

**S8 — Messages**
Dispatcher ⇄ driver. Text with TTS readout. Quick-reply buttons only, no keyboard while moving.

### Persistent sync indicator

A small always-visible chip: green (synced) / amber (N events queued) / grey (offline, N queued).
Never an error dialog, never a blocking modal. Offline is the normal state, not an error, and the
app must never communicate otherwise.

### Language

Hebrew and Arabic, RTL layout, switchable per driver in-app (not per device). Voice guidance in
the same language. Street names present a real problem in mixed cities: signage may be Hebrew but
the driver's UI Arabic. **Show the street name in the map's native script and speak it in the UI
language** — this needs field validation with actual drivers in weeks 1–2, not assumptions.

Android TTS quality: Hebrew is adequate, Arabic is more variable across devices. Validate on the
exact device model the fleet will use before committing. Consider bundling a specific TTS engine
rather than relying on whatever ships on a ₪500 tablet.

### Android technical stack

Kotlin, Jetpack Compose, Room, WorkManager for sync, foreground service for location (with a
persistent notification — Android will kill background location otherwise). Target the cheapest
viable ruggedized Android tablet; test on 4 GB RAM and Android 11, not on a Pixel.

Battery: continuous GPS + screen-on for 10 hours means the device must be wired to vehicle power.
Specify that in the deployment requirements or you'll spend month one debugging "the app stops
working after lunch."

---

## 8. Ops console — UI design

React, RTL-first (not RTL-retrofitted — pick a component library with genuine RTL support and
verify it before committing; retrofitting RTL is a multi-week tax you don't need to pay).

**C1 — Live map (default landing screen)**
All active vehicles, colored by status (on route / at disposal / idle >10 min / offline). Click a
truck → side panel with driver, progress, current stop, recent events. Idle and offline are the
alerts that matter — those are the ones that make a customer say "I need this."

**C2 — Daily dispatch board**
Grid: vehicles × shifts. Assign driver + template to each. Show conflicts (vehicle double-booked,
driver unavailable, route needs a body type this vehicle doesn't have). One button to publish
the day.

**C3 — Route editor**
Map + ordered stop list. Drag to reorder, drag between vehicles to reassign. Live recalculation
of distance and duration. Publish → increments version → pushes to devices.

**C4 — Exception queue** *(the daily work surface)*
Every non-collection outcome, chronological, with photo, location, driver note. Each gets an
action: schedule a return visit, notify the resident, dismiss, escalate to enforcement. This
screen is what the municipality's waste manager will live in.

**C5 — Service history lookup**
Search by address or map click. Returns: last serviced, by whom, outcome, photo if any. Answers a
resident complaint in ten seconds. Trivially simple screen, disproportionately valuable in demos —
put it in the sales deck.

**C6 — Reports**
Plan vs actual per route/zone/date. Completion rate. Tonnage by stream. Vehicle utilization.
Export to Excel — municipal staff will re-cut everything in Excel regardless, so make export
excellent rather than fighting it with dashboards.

**C7 — Asset admin**
Service points, containers, vehicles, drivers, zones, sites. Map-based service point editor with
bulk import and a duplicate detector (municipal asset registries are duplicate-ridden).

---

## 9. Israel-specific requirements

### Language and localization

- Hebrew and Arabic UI, RTL throughout, per-user preference.
- Hebrew and Arabic TTS for navigation, validated on target hardware.
- Address formats: Israeli addresses are `רחוב <name> <number>, <city>` but municipal asset
  registries are wildly inconsistent. Build a normalizer and expect to hand-correct the first
  import for every customer. Budget a week per onboarding for this — it is not a one-off cost.
- Numbers, dates, and exports stay LTR inside RTL layouts. This breaks in subtle ways; test it.

### Map data

OSM coverage in Israel is good in Jewish-majority cities, meaningfully thinner in Arab towns and
in unrecognized Bedouin villages in the Negev. Since these are exactly the places with narrow
streets and difficult access, this is a product risk, not just a data risk.

Mitigation — build a **map correction loop** in V1, not V3:
1. Driver reports "this street is impassable for this truck" from S5, one tap.
2. Correction lands in the ops exception queue for confirmation.
3. Confirmed corrections write to a `road_restriction` table layered over OSM at routing time.
4. Optionally contribute non-proprietary fixes back to OSM.

After two years this restriction layer is the most defensible asset in the company. Treat it as
such from day one — version it, back it up, never lose it.

Base data sources worth evaluating: OSM Israel extract, GovMap / data.gov.il layers, and the
municipality's own GIS (most have one; quality varies enormously).

### Regulatory and reporting

- **Ministry of Environmental Protection** — municipalities report waste quantities and recycling
  rates. Your tonnage data feeds this.
- **תמיר (TMIR)** — packaging waste corporation. Collection data for the orange bin stream drives
  their payments to municipalities. Municipalities care about this because it's revenue.
- **אל"ה (ELA)** — beverage container corporation.
- **Weighbridge tickets** — the legal basis for tonnage. Photo + OCR + dispatcher confirmation is
  the pragmatic path; direct integration with transfer station systems is a V3+ conversation.

Do not build these exports until you have a customer telling you the exact format. Every
municipality's finance department wants it slightly differently. Build a flexible export layer;
build specific reports on demand.

### Privacy and labor law — treat as a blocking requirement

Continuous GPS tracking of employees is legally sensitive in Israel and the compliance work is not
optional. Get an Israeli lawyer on this before the first deployment; the outline:

- **Protection of Privacy Law, 5741-1981** and the **Data Security Regulations (2017)** — a
  database holding employee location data will likely fall under at least "medium" security level,
  with corresponding obligations (access control, logging, incident procedures, periodic audit).
- **Employee monitoring** — Israeli labour court jurisprudence (the Isakov line of cases on
  employer monitoring) sets a high bar: monitoring requires a written, communicated policy;
  genuine and informed consent; proportionality; and a legitimate purpose.
- Practical design requirements that follow:
  - **Track only during an active shift.** Location collection starts on `SHIFT_START` and stops
    on `SHIFT_END`, hard-enforced in the app. No exceptions, no "just in case" background tracking.
  - Visible in-app indicator whenever location is being recorded.
  - Defined retention: breadcrumbs 12 months, then aggregate and drop the raw trace.
  - Role-based access — the dispatcher sees live positions; nobody gets bulk historical trace
    export without an audited reason.
- **Union exposure.** Municipal waste crews are often unionized or work through contractors with
  organized workforces. A tracking product introduced without worker buy-in can be blocked
  outright. The counter-narrative is real and worth leading with: the Netanya deployment reportedly
  found drivers welcomed it, because it ended disputes over whether a bin was serviced and
  eliminated wasted trips to already-empty bins. **Sell it to drivers as protection from false
  accusations, and mean it.**

*(This section is an engineering brief, not legal advice — you need a lawyer to sign off on the
actual implementation.)*

### Operational quirks

- **Shabbat and holidays.** Collection schedules shift around Shabbat, Jewish holidays, and in
  Arab municipalities around Muslim and Christian holidays. The scheduling model needs a
  per-tenant, per-zone calendar with multiple holiday sets — not a hardcoded Mon–Fri assumption.
  Erev-chag half-days are a real case. Get this into the data model early; retrofitting a calendar
  engine is painful.
- **Security incidents.** Routes get closed at short notice. Mid-shift replanning isn't a
  nice-to-have here.
- **Underground bins (פחים טמוני קרקע)** are widespread in Israeli city centers and need crane
  trucks — hence `required_body_type` being mandatory, not optional.
- **Old city cores** — Nazareth, Akko, Jerusalem, Jaffa, Safed. Streets where OSM geometry is
  wrong, turn restrictions are undocumented, and the real answer is often a small-vehicle sub-fleet
  with its own routing profile. Support multiple vehicle profiles from V1.

---

## 10. Non-functional requirements

| Requirement | Target | Notes |
|---|---|---|
| Driver app offline endurance | Full 10h shift, no connectivity | Hard requirement, not aspirational |
| Event durability | Zero loss of stop outcomes | Breadcrumb loss acceptable |
| Live position freshness | < 30s when online | Dispatcher-facing |
| API p99 latency | < 300ms | Excluding report generation |
| Sync batch acceptance | < 2s for 200 events | On 3G |
| Availability | 99.5% business hours (05:00–18:00 IST) | Driver app degrades gracefully; console does not need 24/7 |
| Data retention | Breadcrumbs 12mo, events 7 years, photos 3 years | 7 years driven by municipal contract audit periods — confirm per contract |
| RPO / RTO | 1h / 4h | Nightly + WAL archiving is sufficient |

Deployment: AWS eu-central or an Israeli region if a customer demands data residency (some
municipal tenders do — check before signing). Single-region, multi-AZ. Kubernetes only if you're
already comfortable operating it; ECS or plain EC2 with a load balancer is defensible at this
scale and less to run.

---

## 11. Build order

**M0 — Skeleton (weeks 1–3)**
Domain model, migrations, tenant scoping, auth, empty module structure. Asset CRUD + CSV import.
No UI beyond a scaffold. Goal: the shape is right and everything after this is filling in.

**M1 — Driver app happy path (weeks 4–9)**
Manually defined route (or CSV import) → Android app displays ordered stops → navigation via
GraphHopper → geofence auto-arrival → outcomes recorded → offline queue → sync. No optimizer,
no replanning, minimal console.
**This is the demo that sells the first deal.**

**M2 — Ops console (weeks 10–14)**
Live map, dispatch board, exception queue, service history lookup, plan-vs-actual report.

**M3 — Pilot (weeks 15–20)**
One contractor, 3–5 trucks, one zone. Expect to spend most of this on address normalization,
map corrections, and geofence tuning. Do not add features during the pilot.

**M4 — Replanning + media (weeks 21–26)**
Mid-shift edits, route versioning, messaging, weighbridge tickets.

**M5+ — Optimization and sensors**
OR-Tools sequencing, fill-level integration, regulatory exports. Driven by what the pilot
customer actually asks for, not by this document.

---

## 12. Open decisions

These need answers before or during M0:

1. **First customer: contractor or municipality?** Changes the console's emphasis (utilization vs
   oversight) and the sales cycle length. Decide before M2.
2. **Hardware:** BYOD phones or supplied ruggedized tablets? Affects device management, the
   support burden, and whether you can guarantee TTS quality. Recommend supplied tablets —
   controlling the hardware removes an entire class of support tickets.
3. **GraphHopper vs HERE.** The recommendation above assumes you want the restriction layer as a
   moat. If time-to-first-demo dominates, HERE is faster to stand up and you can migrate later —
   but keep routing behind an interface so that migration stays possible.
4. **Multi-tenancy model:** shared schema with RLS (simpler, recommended) vs schema-per-tenant
   (easier data residency story for a paranoid municipal tender). Hard to change later.
5. **Segment collection modeling.** Confirm with a real hauler how much of their work is segment
   vs point before finalizing. If it's overwhelmingly point-based in your target city, the
   LineString support can be deferred — but confirm, don't assume.
6. **Legal review of the tracking model** — start this early, it's a long-lead item.

---

## 13. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Driver / union resistance to tracking | **High** | Lead with driver benefit; involve crews before deployment; strict shift-bounded tracking |
| Municipal tender cycles (12–18mo) | **High** | Sell to contractors first for revenue and reference customers |
| OSM data gaps in Arab towns and periphery | Medium | Correction loop from day one; treat as an asset being built |
| GreenQ incumbency in 11+ cities | Medium | Different wedge — dispatch and proof-of-service, not truck telemetry hardware |
| Low Israeli landfill fees weaken the savings story | Medium | Sell accountability and complaint resolution, not efficiency ROI |
| Hebrew/Arabic TTS quality on cheap hardware | Medium | Validate in week 1; bundle a TTS engine if needed |
| Address data quality on onboarding | Medium | Budget it explicitly per customer; build good import tooling |
| Scope creep into optimization too early | Medium | This document; revisit §0 whenever it's tempting |
