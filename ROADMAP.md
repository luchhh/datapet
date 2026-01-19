# DataPet Roadmap

Log Aggregation & Alerting System - Implementation Phases

## Architecture Overview

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Service A     │  │   Service B     │  │   Service C     │
│  (logs → Kafka) │  │  (logs → Kafka) │  │  (logs → Kafka) │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌──────────────────────────────────────────────────────────────┐
│                         KAFKA                                 │
│  topics: logs.raw, logs.parsed, alerts.triggered             │
└──────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Log Parser     │  │  Aggregator     │  │  Alert Router   │
│  Consumer       │  │  (windowed      │  │  (Slack, email) │
│                 │  │   stats)        │  │                 │
└────────┬────────┘  └────────┬────────┘  └─────────────────┘
         │                    │
         ▼                    ▼
┌─────────────────┐  ┌─────────────────┐
│  Storage        │  │  Query API      │
│  (Postgres)     │◄─┤  (HTTP + search)│
└─────────────────┘  └─────────────────┘
```

---

## Phase 1: Foundation [COMPLETED]

**Goal:** Docker infra, project skeleton, basic log flow

- [x] Update docker-compose.yml (add Postgres, Kafka UI)
- [x] Create Postgres schema (logs table)
- [x] Update deps.edn with new dependencies
- [x] Create config.clj (load from env/edn)
- [x] Create db.clj (connection pool with HikariCP)
- [x] Create basic producer.clj (emit JSON logs to Kafka)
- [x] Create parser consumer (read raw → parse → store in Postgres)
- [x] Create simulator (fake services emitting logs)
- [x] Wire up core.clj entry point

**Verification:**
```bash
docker-compose up -d
clj -M:run
# Check Postgres:
docker exec postgres psql -U datapet -d datapet -c "SELECT count(*) FROM logs"
```

---

## Phase 2: Query API

**Goal:** HTTP API to search logs

- [ ] Create api/server.clj (HTTP server lifecycle)
- [ ] Create api/routes.clj with endpoints:
  - `GET /api/logs` - list/search logs
  - `GET /api/logs/:id` - get single log
  - `GET /api/stats` - basic counts
- [ ] Add query params: service, level, time range, limit, search

**Verification:**
```bash
curl http://localhost:3000/api/logs
curl "http://localhost:3000/api/logs?service=auth-api&level=error"
curl "http://localhost:3000/api/logs?search=failed&limit=10"
curl http://localhost:3000/api/stats
```

---

## Phase 3: Aggregator

**Goal:** Windowed statistics (errors/min, log volume by service)

- [ ] Design aggregator state (atom with time windows)
- [ ] Consumer that updates rolling stats
- [ ] Persist aggregations periodically
- [ ] API endpoint: `GET /api/metrics`

**Verification:**
```bash
curl http://localhost:3000/api/metrics
# Should show live-updating stats like:
# {"errors_per_minute": 5, "by_service": {"auth-api": 120, ...}}
```

---

## Phase 4: Alerting

**Goal:** Rule-based alerting system

- [ ] Design rule DSL (data-driven rules)
- [ ] Rule evaluation engine
- [ ] Alert storage (when triggered)
- [ ] Router stubs (console output)
- [ ] API endpoints:
  - `GET /api/alerts/rules` - list rules
  - `POST /api/alerts/rules` - create rule
  - `GET /api/alerts` - triggered alerts history

**Example rule:**
```clojure
{:name "high-error-rate"
 :condition [:> [:rate :level "error" :window "5m"] 10]
 :action [:console]}
```

**Verification:**
```bash
curl http://localhost:3000/api/alerts/rules
curl http://localhost:3000/api/alerts
```

---

## Phase 5: Polish

**Goal:** Production-readiness

- [ ] Structured logging for DataPet itself
- [ ] Health check endpoints (`GET /health`)
- [ ] Graceful shutdown
- [ ] Basic dashboard data endpoints
- [ ] Docker build for the Clojure app (Dockerfile)

---

## Handoff Strategy

| Phase | Who Writes | Learning Focus |
|-------|-----------|----------------|
| 1 | Claude | Watch patterns: namespaces, component lifecycle, Kafka consumers |
| 2 | Claude | Watch patterns: Ring handlers, HoneySQL queries, API design |
| 3 | You + guidance | Practice: atoms, state management, consumer loops |
| 4 | You + guidance | Practice: DSL design, data-driven rules |
| 5 | You solo | Independence: apply everything learned |

---

## Running the Project

```bash
# Start infrastructure
docker-compose up -d

# Run DataPet
clj -M:run

# Services available:
# - Kafka UI: http://localhost:8080
# - DataPet API: http://localhost:3000 (after Phase 2)
# - Postgres: localhost:5433
```

---

## Project Structure

```
pipelines/
├── docker-compose.yml
├── docker/
│   └── init.sql              # Postgres schema
├── deps.edn
├── resources/
│   └── logback.xml           # Logging config
├── src/datapet/
│   ├── core.clj              # Entry point, component lifecycle
│   ├── config.clj            # Configuration loading
│   ├── kafka/
│   │   ├── producer.clj      # Log producer SDK
│   │   └── consumer.clj      # Base consumer utilities
│   ├── parser/
│   │   └── core.clj          # Log parsing & enrichment
│   ├── storage/
│   │   ├── db.clj            # Connection pool
│   │   └── logs.clj          # Log persistence & queries
│   ├── aggregator/
│   │   └── core.clj          # Windowed stats (Phase 3)
│   ├── alerting/
│   │   ├── rules.clj         # Rule DSL & evaluation (Phase 4)
│   │   └── router.clj        # Alert dispatch (Phase 4)
│   ├── api/
│   │   ├── server.clj        # Ring/Reitit setup (Phase 2)
│   │   └── routes.clj        # Endpoints (Phase 2)
│   └── simulator/
│       └── core.clj          # Fake log generator
└── test/datapet/
    └── ...
```
