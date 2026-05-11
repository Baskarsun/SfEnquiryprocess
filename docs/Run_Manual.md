# Run Manual — SF Leasing LOS (lead-service)

> **Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 18 · Redis 8 · Kafka 7.5.3 (Confluent)
> **Last updated:** 2026-05-11 (post Spring Boot migration)

---

## 1. Prerequisites

| Tool | Minimum version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9 | `mvn -version` |
| Docker Desktop **or** Scoop | — | `docker info` **or** `scoop list` |
| Git | any | `git --version` |

> If you use **Docker Compose** you need nothing else for infrastructure.  
> If you use **Scoop (native)** you need `postgresql`, `redis`, and a separately-run Kafka container or skip Kafka in dev.

---

## 2. Clone and Build

```powershell
git clone <repo-url>
cd SFEnquireProcess

# Full build (skips tests)
cd services\lead-service
mvn clean package -DskipTests

# Build + run all tests
mvn clean verify
```

The fat JAR lands at:
```
services\lead-service\target\lead-service-*.jar
```

---

## 3. Start Infrastructure

### Option A — Docker Compose (recommended)

Starts PostgreSQL 18 on **5432**, Redis 8 on **6379**, Zookeeper on **2181**, Kafka on **9092**.

```powershell
cd infrastructure
docker compose up -d

# Verify all containers are healthy
docker compose ps
```

Wait until all four containers show `healthy` before starting the service. Typically takes 30–60 s for Kafka.

Stop everything:
```powershell
docker compose down          # keep volumes (data persists)
docker compose down -v       # destroy volumes (clean slate)
```

### Option B — Scoop (native, no Docker)

Requires `postgresql` and `redis` installed via Scoop:
```powershell
scoop install postgresql redis
```

Run the provided script:
```powershell
.\infrastructure\start-dev.ps1
```

This starts:
- PostgreSQL 18 on **port 5434** — creates `leasing_user` / `leasing_pass` / `leasing_db` if absent
- Redis 8 on **port 6379**

> **Port difference:** The Scoop option uses port **5434** (not 5432). `application.properties` is already set to `127.0.0.1:5434` so no change is needed.

> **Kafka:** The Scoop script does not start Kafka. In dev you can run the service without Kafka — lead events will be silently dropped (try/catch in `LeadEventProducer`). To start Kafka natively, run the Docker Compose Kafka container only:
> ```powershell
> docker compose up -d zookeeper kafka
> ```

> **Note:** `start-dev.ps1` line 59 still prints the old command `mvn quarkus:dev`. Ignore it — the correct command is `mvn spring-boot:run` (see §4).

---

## 4. Start the Service

```powershell
cd services\lead-service

# Dev mode (live reload via spring-boot-devtools, if added)
mvn spring-boot:run

# OR run the pre-built JAR
java -jar target\lead-service-*.jar
```

Startup log ends with:
```
Started LeadServiceApplication in X.XXX seconds
```

**Flyway** runs automatically on startup and applies migrations V1–V6 (`db/migration/`). If the schema already exists and is current, Flyway skips silently.

Override any property at startup:
```powershell
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090 --spring.datasource.url=jdbc:postgresql://myhost:5432/leasing_db"
```

---

## 5. Key URLs

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API explorer |
| `http://localhost:8080/v3/api-docs` | Raw OpenAPI JSON |
| `http://localhost:8080/actuator/health` | Health probe (if actuator on classpath) |
| `http://localhost:8080/api/v1/leads` | Lead CRUD |
| `http://localhost:8080/api/v1/exception-queue` | Exception queue (CPU view) |

---

## 6. Database

### Connection details

| Setting | Docker Compose | Scoop / native |
|---|---|---|
| Host | `127.0.0.1` | `127.0.0.1` |
| Port | `5432` | `5434` |
| Database | `leasing_db` | `leasing_db` |
| User | `leasing_user` | `leasing_user` |
| Password | `leasing_pass` | `leasing_pass` |

### Flyway migrations

| Version | Description |
|---|---|
| V1 | Initial schema — leads, applicants, assignments, dedup results |
| V2 | Reference data — source categories, branch codes |
| V3 | Phase 2 schema — bulk upload, temperature engine tables |
| V4 | Phase 3 schema — prospect validation, KYC |
| V5 | Phase 4 schema — opportunity, quote, application |
| V6 | Phase 5 schema — exception queue, SLA tracking |

Flyway is enabled by default (`spring.flyway.enabled=true`). To run only DB migration without starting the service:
```powershell
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://127.0.0.1:5434/leasing_db -Dflyway.user=leasing_user -Dflyway.password=leasing_pass
```

### Direct DB access
```powershell
# Docker
psql -h 127.0.0.1 -p 5432 -U leasing_user -d leasing_db

# Scoop
psql -h 127.0.0.1 -p 5434 -U leasing_user -d leasing_db
```

---

## 7. Running Tests

```powershell
cd services\lead-service

# All unit tests (pure Mockito, no containers needed)
mvn test

# Single test class
mvn test -Dtest=LeadCreationServiceTest

# Skip tests in package build
mvn package -DskipTests
```

Tests use an **H2 in-memory database** (`MODE=PostgreSQL`) with `spring.flyway.enabled=false` and `spring.jpa.hibernate.ddl-auto=create-drop`. No external services (PostgreSQL, Redis, Kafka) are required.

---

## 8. API Quick-Start

All requests require `X-User-Id` and `X-Device-Id` headers. Replace values as needed.

### Create a lead

```powershell
curl -s -X POST http://localhost:8080/api/v1/leads `
  -H "Content-Type: application/json" `
  -H "X-User-Id: EMP003" `
  -H "X-Device-Id: IMEI-123456" `
  -d '{
    "leadType": "INDIVIDUAL",
    "sourceCategory": "INTERNAL",
    "sourceName": "Walk-In-2026",
    "applicants": [
      {
        "applicantName": "Ravi Kumar",
        "mobile": "9876543210",
        "constitutionType": "INDIVIDUAL"
      }
    ]
  }'
```

Success response (`201 Created`):
```json
{
  "lrn": "LS-202605-000001",
  "tempCustomerNumber": "TMP-2026-000001",
  "warnings": [],
  "routedToExceptionQueue": false
}
```

### Fetch a lead by LRN

```powershell
curl -s http://localhost:8080/api/v1/leads/LS-202605-000001 `
  -H "X-User-Id: EMP003"
```

### Search leads

```powershell
curl -s "http://localhost:8080/api/v1/leads?status=OPEN&page=0&size=20" `
  -H "X-User-Id: EMP003"
```

### Resolve an exception queue entry

```powershell
curl -s -X PATCH http://localhost:8080/api/v1/exception-queue/{id}/resolve `
  -H "Content-Type: application/json" `
  -H "X-User-Id: EMP003" `
  -d '{"resolutionNotes": "Verified via phone call"}'
```

---

## 9. Scheduled Jobs

These run automatically inside the service — no external trigger required.

| Job | Class | Schedule | Purpose |
|---|---|---|---|
| SLA check | `SlaScheduler` | Every 1 hour | Flags leads/prospects approaching SLA breach |
| Exception escalation | `SlaScheduler` | Every 6 hours | Escalates unresolved exception queue entries |
| Temperature degradation | `SlaScheduler` | Daily at 02:00 | Downgrades lead temperature (HOT→WARM→COLD) |

---

## 10. Configuration Reference

Key properties in `src/main/resources/application.properties`:

| Property | Default | Override for |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://127.0.0.1:5434/leasing_db` | Different host/port |
| `spring.data.redis.host` | `localhost` | Remote Redis |
| `spring.data.redis.port` | `6379` | Non-default port |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Cluster / remote Kafka |
| `server.port` | `8080` | Port conflict |
| `adapters.*.stub-mode` | `true` | Enable real external adapters (CIBIL, DMS, PAN, GSTIN) |
| `fraud-service.active` | `true` | Disable fraud screen |
| `env.indicator` | `T` | `B` (beta) or `L` (live/prod) |

All properties can be overridden with environment variables using Spring's relaxed binding convention, e.g.:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://prod-host:5432/leasing_db"
$env:SPRING_DATA_REDIS_HOST = "prod-redis"
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = "prod-kafka:9092"
mvn spring-boot:run
```

---

## 11. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Connection refused` on port 5432 / 5434 | PostgreSQL not running | Run `docker compose up -d postgres` or `.\start-dev.ps1` |
| `Flyway migration failed` on startup | Schema version mismatch | Run `mvn flyway:repair` then restart |
| `KafkaProducerException` in logs | Kafka not running | Start Kafka (`docker compose up -d zookeeper kafka`) or tolerate — lead events are non-blocking |
| `RedisConnectionFailureException` | Redis not running | Run `docker compose up -d redis` or `redis-server --port 6379` |
| Port 8080 already in use | Another process | Add `-Dserver.port=9090` to Maven run command |
| `HikariPool … connection timeout` | Wrong DB port | Check `spring.datasource.url` — Scoop uses 5434, Docker uses 5432 |
| Swagger UI blank / 404 | Service not started | Wait for `Started LeadServiceApplication` in log |

---

## 12. Project Structure (lead-service)

```
services/lead-service/
├── src/main/java/com/sf/leasing/lead/
│   ├── LeadServiceApplication.java         ← Spring Boot entry point
│   ├── api/
│   │   ├── resource/                       ← @RestController endpoints (16 resources)
│   │   ├── dto/request/                    ← Request DTOs
│   │   ├── dto/response/                   ← Response DTOs
│   │   └── exception/GlobalExceptionHandler.java  ← @RestControllerAdvice
│   ├── domain/
│   │   ├── model/                          ← JPA entities (pure, no Panache)
│   │   ├── enums/                          ← Domain enumerations
│   │   └── exception/                      ← BusinessException, ErrorCodes
│   ├── application/                        ← Use-case services (orchestration)
│   ├── service/                            ← Domain services
│   └── infrastructure/
│       ├── persistence/                    ← JpaRepository interfaces (21 repos)
│       ├── messaging/                      ← KafkaTemplate producer
│       └── locking/                        ← Redis sequence generator
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/                       ← Flyway V1–V6
└── src/test/
    ├── java/                               ← Mockito unit tests
    └── resources/application.properties   ← H2 test config
```
