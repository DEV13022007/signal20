# PolarConnect (SIH26060)

PolarConnect is an **offline-first management platform for Antarctic research stations**. It lets a station (e.g. Maitri, Bharati) keep managing inventory, crew, and equipment even when its satellite link to Headquarters is down, and automatically syncs everything — in priority order — the moment the link comes back up.

Built for Smart India Hackathon problem statement **SIH26060**.

---

## Table of contents

- [Why this exists](#why-this-exists)
- [How it works](#how-it-works)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Demo logins](#demo-logins)
- [Backend configuration](#backend-configuration)
- [API overview](#api-overview)
- [Real-time alerts](#real-time-alerts)
- [Offline-first frontend](#offline-first-frontend)
- [PDF reports](#pdf-reports)
- [Roles & access control](#roles--access-control)

---

## Why this exists

Antarctic research stations can lose their satellite uplink for hours or days at a time. Staff still need to log inventory usage, track crew health/rotations, and record equipment status during that window. PolarConnect models this problem directly:

- Every write (inventory, personnel, equipment) is recorded **immediately and locally** at the station, regardless of link status.
- If the station's satellite link is down, the record is queued instead of being pushed to HQ.
- When the link comes back up, queued records are **flushed to HQ in priority order**: `MEDICAL > EQUIPMENT > SUPPLY > ROUTINE`.
- HQ Admins get a live, real-time view (via WebSocket) of alerts across all stations, plus a "Headquarters" view showing both stations side by side, and can download a PDF status report per station.

## How it works

There are two layers of "offline", modeled separately:

1. **Station ↔ HQ sync** (backend, `SyncQueueService` + `SyncRecord`): every inventory/personnel/equipment mutation at a station creates a `SyncRecord`. If `Station.satelliteLinkActive` is true, it's flushed immediately; if false, it sits `PENDING` until the link flips back on, at which point a periodic sweep (`polarconnect.sync.sweep-interval-ms`) drains the queue in priority order.
2. **Browser ↔ backend sync** (frontend, `db/index.js` + `offlineRepo.js`): the browser mirrors stations/inventory/personnel/equipment into IndexedDB (via Dexie) so the UI keeps working if the device itself loses connectivity, and queues any writes made while offline in a local `outbox` for replay later. A "simulate offline" toggle (`lib/simulateOffline.js`) lets you demo this without physically pulling the network.

These two are independent: a station can be fully connected to the internet but still show `PENDING` syncs to HQ because its *satellite* link is modeled as down (and vice versa for a browser that's lost Wi-Fi).

## Tech stack

**Backend** — Java 17, Spring Boot 3.3.5
- Spring Web (REST API), Spring Data JPA, PostgreSQL
- Spring Security + JWT (`jjwt`) for stateless auth
- Spring WebSocket (STOMP) for live alerts
- Apache PDFBox for generated station reports
- springdoc-openapi for Swagger UI

**Frontend** — `polarconnect-ui/`, React 19 + Vite
- `react-router-dom` for routing
- `dexie` (IndexedDB) for offline data mirroring/outbox
- `@stomp/stompjs` for the live alert WebSocket feed
- `recharts` for dashboard charts
- `vite-plugin-pwa` for installable/offline PWA support

## Project structure

```
sih26060/
├── src/main/java/com/example/sih26060/
│   ├── controller/     REST controllers (auth, stations, inventory, personnel, equipment, sync, alerts)
│   ├── service/        Business logic (incl. SyncQueueService, ReportService, AlertService)
│   ├── entity/         JPA entities + enums (Station, InventoryItem, Personnel, Equipment, ...)
│   ├── repository/     Spring Data repositories
│   ├── security/       JWT filter/service, UserPrincipal, station-scoping (AuthorizationSupport)
│   ├── dto/            Request/response payloads
│   ├── config/         WebSocket (STOMP) config, global exception handling
│   └── seed/           DataSeeder — wipes & reseeds demo data (Maitri + Bharati) on every boot
├── src/main/resources/application.properties
├── polarconnect-ui/
│   └── src/
│       ├── api/        HTTP client + offline repo (IndexedDB-backed)
│       ├── components/ StationList, StationDetail, AlertsPanel, CrewPanel, EquipmentPanel, ...
│       ├── pages/      Dashboard, Headquarters, LoginPage
│       ├── hooks/      useAuth, useAlerts, useOnlineStatus, useDashboardData, ...
│       ├── db/         Dexie schema + offline cache helpers
│       └── lib/        authStore, simulateOffline toggle
└── pom.xml
```

## Getting started

### Prerequisites

- Java 17+
- Maven (or use the bundled `./mvnw`)
- Node.js 18+ and npm
- A running PostgreSQL instance

### 1. Database

Create a database matching `application.properties` (or override via env vars — see below):

```sql
CREATE DATABASE polarconnect;
CREATE USER polarconnect WITH PASSWORD 'polarconnect';
GRANT ALL PRIVILEGES ON DATABASE polarconnect TO polarconnect;
```

### 2. Backend

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. On boot, `DataSeeder` wipes and reseeds two demo stations (Maitri, Bharati) with inventory, crew, equipment, and one demo user per role — see [Demo logins](#demo-logins).

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`

### 3. Frontend

```bash
cd polarconnect-ui
npm install
npm run dev
```

The app starts on Vite's dev server (default `http://localhost:5173`) and talks to the backend at `http://localhost:8080`.

Other frontend scripts: `npm run build`, `npm run preview`, `npm run lint` (oxlint).

## Demo logins

Seeded automatically every time the backend starts (`polarconnect.seed.enabled=true`):

| Username         | Password    | Role             | Scope             |
|------------------|-------------|------------------|-------------------|
| `hq.admin`       | `admin123`  | `HQ_ADMIN`       | All stations      |
| `maitri.manager` | `manager123`| `STATION_MANAGER`| Maitri only       |
| `bharati.crew`   | `crew123`   | `CREW`           | Bharati only       |

## Backend configuration

Key properties in `src/main/resources/application.properties`:

| Property | Purpose |
|---|---|
| `spring.datasource.*` | PostgreSQL connection (url/username/password) |
| `polarconnect.sync.sweep-interval-ms` | How often the queue re-checks stations whose link is already up, in case records were queued while it was down (default 30s) |
| `polarconnect.seed.enabled` | Wipes & reseeds Maitri/Bharati on every boot. **Set to `false` before this stops being a demo/prototype.** |
| `polarconnect.jwt.secret` | JWT signing secret. **This is a demo value committed to the repo — replace with an environment-variable-backed secret before any real deployment.** |
| `polarconnect.jwt.expiration-ms` | Token lifetime (default 24h) |

> ⚠️ **Security note:** the JWT secret and DB credentials in `application.properties` are hackathon/demo defaults. Do not reuse them outside local development.

## API overview

All endpoints are under `/api`, and (except `/api/auth/**`) require a `Bearer` JWT.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/login`, `GET /api/auth/me` |
| Stations | `GET /api/stations`, `GET /api/stations/{id}`, `POST /api/stations`, `GET /api/stations/{id}/report` (PDF) |
| Inventory | `GET/POST /api/inventory`, `GET/PUT/DELETE /api/inventory/{id}` |
| Personnel | `GET/POST /api/personnel`, `GET/PUT/DELETE /api/personnel/{id}` |
| Equipment | `GET/POST /api/equipment`, `GET/PUT/DELETE /api/equipment/{id}` |
| Sync | `GET /api/sync/status`, `GET /api/sync/status/{stationId}`, `GET /api/sync/records`, `POST /api/sync/stations/{stationId}/flush` |
| Alerts | `GET /api/alerts` |

Full interactive docs are available via Swagger UI once the backend is running.

## Real-time alerts

The backend exposes a STOMP-over-WebSocket endpoint at `/ws` (no SockJS fallback), broadcasting on `/topic/alerts`. `AlertService` generates alerts (e.g. critical crew health, failed equipment, low stock) and pushes them live; the frontend's `useAlerts` hook subscribes via `@stomp/stompjs` and feeds the status bar / alerts panel.

## Offline-first frontend

- `polarconnect-ui/src/db/index.js` defines the Dexie (IndexedDB) schema mirroring stations, inventory, personnel, equipment, and sync records, plus an `outbox` table for writes made while the browser itself is offline.
- `polarconnect-ui/src/api/offlineRepo.js` and `useOnlineStatus` decide whether to hit the network or fall back to the local cache/outbox.
- `polarconnect-ui/src/lib/simulateOffline.js` provides a manual "pretend the device just lost its link" toggle for demoing the flow without touching the OS network stack.
- The app is also installable as a PWA (`vite-plugin-pwa`).

## PDF reports

`GET /api/stations/{id}/report` (also available as a download button on the Station Detail page) generates a PDF via Apache PDFBox summarizing a station's inventory, crew, and equipment status with a table layout and color-coded status badges (`ReportService`).

## Roles & access control

Defined by `Role` (`STATION_MANAGER`, `CREW`, `HQ_ADMIN`) and enforced by `AuthorizationSupport`:

- `HQ_ADMIN` has no station pinned (`stationId == null`) and can see/query any station, or all of them at once (the Headquarters view).
- `STATION_MANAGER` / `CREW` are pinned to one station. Any request scoped to *their* station (or left unscoped) is served normally; a request for a *different* station is rejected with `403`.

Auth is stateless JWT: `JwtAuthFilter` validates the bearer token on every `/api/**` request before Spring Security's own filters run. `/api/auth/**`, `/ws/**`, and the Swagger/OpenAPI paths are open; everything else requires a valid token.
