# ADR-008: Framework Choice — Quarkus vs Spring Boot for Microservices

| Field | Value |
|---|---|
| Status | **ACCEPTED — Option B executed** — migration to Spring Boot 3.3 complete (2026-05-11) |
| Date | 2026-05-11 |
| Deciders | Enterprise Architecture Guild, Engineering Lead, Platform SRE |
| Supersedes | EA §Technology Stack (Spring Boot mandate) |

---

## Context

The Enterprise Architecture document (EA §Technology Stack) mandates **Spring Boot** as the application framework for all microservices, enforcing **Clean Architecture (Ports & Adapters)** as the internal pattern.

The first microservice delivered — `lead-service` — was implemented using **Quarkus** (with Hibernate ORM Panache, RESTEasy Reactive, SmallRye Messaging, and Quarkus Scheduler). The service correctly applies Clean Architecture layering (domain / application / infrastructure) and honours the API-first mandate (OpenAPI via SmallRye OpenAPI). It is already in active development with domain models, business rules, and Flyway migrations in place.

This ADR documents the conflict, evaluates both paths forward, and proposes a decision for the Architecture Guild to ratify.

---

## Decision Drivers

- **Delivery velocity** — avoid re-doing working code
- **EA compliance** — framework alignment must be deliberate and documented
- **K8s operational fit** — startup time and memory footprint matter for on-prem cluster capacity
- **Developer experience** — Dapr sidecar integration, testing ergonomics
- **Clean Architecture fidelity** — the EA's real intent is enforcing the pattern, not the brand
- **Long-term maintainability** — consistent choice across all services reduces cognitive load

---

## Options Considered

### Option A — Approve Quarkus as EA-Compliant Alternative (Recommended)

Formally amend the EA §Technology Stack table to accept Quarkus alongside Spring Boot, with the following conditions:

1. All services must implement **Clean Architecture** (domain / application / infrastructure layers) regardless of framework.
2. All services must expose **OpenAPI v3** specs (Quarkus: SmallRye OpenAPI; Spring Boot: SpringDoc).
3. All services must integrate with **Dapr** sidecars for east-west communication and pub/sub.
4. **No mixed-mode** per service: a service picks one framework and stays with it.
5. New services default to **Quarkus** on K8s targets; Spring Boot remains acceptable where the team has stronger expertise.

**Rationale:**

| Criterion | Quarkus | Spring Boot |
|---|---|---|
| Clean Architecture support | Full — no framework coupling in domain/application layers | Full |
| Jakarta EE standards | Yes — Jakarta REST, CDI, JPA, Validation, Transactions | Yes — via Spring's Jakarta wrappers |
| K8s startup time | ~50–200ms (JVM); ~15ms (native) | ~2–5s (JVM); ~30ms (native) |
| Memory footprint (JVM) | ~100–150MB per pod | ~200–350MB per pod |
| Dapr integration | Via HTTP API (Dapr SDK or plain HTTP client) — identical | Identical |
| Kafka integration | SmallRye Reactive Messaging — annotation-driven | Spring Kafka — annotation-driven |
| Redis integration | Quarkus Redis client | Spring Data Redis |
| Scheduled jobs | `@Scheduled` (Quarkus) | `@Scheduled` (Spring) — same API name |
| OpenAPI | SmallRye OpenAPI — auto-generated | SpringDoc — auto-generated |
| Test tooling | Quarkus Test + REST Assured + Mockito | Spring Boot Test + MockMvc + Mockito |
| Native compilation | GraalVM native image — first-class | Spring Native — improving but less mature |
| On-prem K8s fit | Excellent (lower resource per replica → more density) | Good |
| Ecosystem maturity | Red Hat enterprise support available | VMware/Broadcom enterprise support |

On-prem Kubernetes clusters have a fixed node capacity. Quarkus's lower JVM memory footprint (~100–150 MB vs ~200–350 MB per pod) directly increases the number of service replicas possible per node without additional hardware, which is significant given the EA's 99.99% uptime requirement and HPA-based scaling.

**Migration cost of staying on Quarkus:** Zero. The `lead-service` code is complete and correct.

**Risk:** Framework divergence between services increases cognitive overhead when engineers context-switch. Mitigated by the Clean Architecture template (the layer structure is identical regardless of framework).

---

### Option B — Migrate lead-service to Spring Boot

Rewrite the `lead-service` from Quarkus to Spring Boot, aligning it with the EA mandate without amending the document.

**Scope of migration:**

| Quarkus Component | Spring Boot Equivalent | Migration Effort |
|---|---|---|
| `@Path`, `@GET/@POST` (JAX-RS) | `@RestController`, `@GetMapping/@PostMapping` | Medium — mechanical rewrite |
| `PanacheEntityBase` / Panache finders | `JpaRepository<Lead, UUID>` + JPQL | Medium — finder methods change |
| `@Inject` (CDI) | `@Autowired` / constructor injection | Low — find/replace |
| `@ApplicationScoped` | `@Service`, `@Component` | Low |
| `@Transactional` (Jakarta) | `@Transactional` (Spring) — same annotation name | Low |
| `@Scheduled` (Quarkus) | `@Scheduled` (Spring) — same name, different config | Low |
| SmallRye Reactive Messaging | Spring Kafka `@KafkaListener` + `KafkaTemplate` | Medium |
| Quarkus Redis client | Spring Data Redis `RedisTemplate` | Medium |
| Quarkus Flyway | Spring Boot Flyway auto-configuration | Low — config change only |
| SmallRye OpenAPI | SpringDoc OpenAPI | Low |
| `quarkus-junit5`, REST Assured | `spring-boot-test`, MockMvc | Medium — test rewrites |
| `application.properties` keys | Spring Boot property key conventions | Low |

**Estimated effort:** 3–4 developer-weeks for `lead-service` alone. Each subsequent service adds similar cost if they were to diverge.

**Business value delivered:** Zero. Functionality does not change; no user-facing improvement.

**Risk:** Introduces a regression window during migration. All existing tests must be rewritten and re-validated. Delays Phase 2 features by 3–4 weeks.

---

### Option C — Hybrid (not recommended)

Keep Quarkus for `lead-service`; mandate Spring Boot for all future services. This maximises inconsistency without delivering either the efficiency of a single-framework policy or the correctness of a clean decision.

---

## Decision

**Recommended: Option A** — Approve Quarkus as an EA-compliant alternative.

The EA document should be amended as follows in §Technology Stack:

> **Application Framework:** Spring Boot **or Quarkus**. Both are approved for microservice implementation, subject to the Clean Architecture mandate. Services must not mix frameworks internally. New services should default to Quarkus for on-prem Kubernetes deployments where startup time and memory density are optimisation goals. Spring Boot remains approved where team expertise warrants it.

The Architecture Guild should ratify this ADR and update the EA document before the next service is scaffolded, to prevent future undocumented divergence.

---

## Consequences

### If Option A is ratified:

- `lead-service` requires no changes.
- The EA document is amended. No existing decision is violated retroactively — it is formally corrected.
- A **Golden Path template repository** must provide two flavours: `service-template-quarkus` and `service-template-springboot`, both enforcing identical Clean Architecture package structures.
- The `docs/Implementation_Plan.md` reference to "Spring Boot" should be updated to "Spring Boot / Quarkus".

### If Option B is chosen:

- A 3–4 week migration sprint is required before Phase 2 work begins.
- The migration should be done in a feature branch with a full regression test run before merge.
- All Panache finder calls must be replaced with Spring Data JPA repository methods.
- The `pom.xml` parent BOM changes from `quarkus-bom` to `spring-boot-starter-parent`.

---

## Implementation Steps for Option A (post-ratification)

1. Update `Enterprise Architecture.md` §Technology Stack table — add Quarkus alongside Spring Boot.
2. Create `services/service-template-quarkus/` — Golden Path for Quarkus services (same package structure as `lead-service`).
3. Create `services/service-template-springboot/` — Golden Path for Spring Boot services (matching layer structure).
4. Update `docs/Implementation_Plan.md` to reflect dual-framework policy.
5. Document the choice rationale in the Architecture Guild meeting notes.

---

## Implementation Steps for Option B (if migration chosen)

Below is the complete Spring Boot migration plan for `lead-service`.

### Step 1 — Replace `pom.xml`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.5.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 2 — Domain Layer (no changes required)

The domain layer (`domain/model/`, `domain/enums/`, `domain/exception/`) contains only JPA entities and plain Java enums. JPA annotations (`@Entity`, `@Column`, `@Enumerated`, etc.) are Jakarta Persistence API — identical in both frameworks. **Zero changes needed here.**

### Step 3 — Replace Lead Entity: Remove Panache, Add Spring Data Repository

**Before (Quarkus Panache):**
```java
public class Lead extends PanacheEntityBase {
    public static Lead findByLrn(String lrn) {
        return find("lrn", lrn).firstResult();
    }
}
```

**After (Spring Data JPA):**
```java
// domain/model/Lead.java — remove PanacheEntityBase; keep all JPA annotations
@Entity
@Table(name = "leads")
public class Lead { /* fields unchanged */ }

// infrastructure/persistence/LeadRepository.java
@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findByLrn(String lrn);
    Optional<Lead> findByTempCustomerNumber(String tempCustomerNumber);

    @Query("SELECT l FROM Lead l WHERE " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:assignedTo IS NULL OR l.assignedUserId = :assignedTo) AND " +
           "(:branchCode IS NULL OR l.assignedBranchCode = :branchCode) AND " +
           "(:temperature IS NULL OR l.temperature = :temperature)")
    Page<Lead> search(
        @Param("status")      LeadStatus status,
        @Param("assignedTo")  String assignedTo,
        @Param("branchCode")  String branchCode,
        @Param("temperature") LeadTemperature temperature,
        Pageable pageable
    );
}
```

### Step 4 — Replace Resource with RestController

**Before (Quarkus JAX-RS):**
```java
@Path("/api/v1/leads")
@Produces(MediaType.APPLICATION_JSON)
public class LeadResource {
    @POST public Response createLead(...) {}
    @GET  public Response searchLeads(...) {}
}
```

**After (Spring MVC):**
```java
@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Lead Management")
public class LeadController {

    private final LeadCreationService leadCreationService;
    private final LeadRepository leadRepository;

    public LeadController(LeadCreationService leadCreationService,
                          LeadRepository leadRepository) {
        this.leadCreationService = leadCreationService;
        this.leadRepository = leadRepository;
    }

    @PostMapping
    public ResponseEntity<CreateLeadResponse> createLead(
            @Valid @RequestBody CreateLeadRequest req,
            @RequestHeader("X-User-Id")       String userId,
            @RequestHeader("X-Device-Id")     String primaryDeviceId,
            @RequestHeader(value = "X-Device-Id-Alt", required = false) String secondaryDeviceId,
            @RequestHeader(value = "X-Channel", defaultValue = "DESKTOP") String channelHeader,
            @RequestHeader(value = "X-GPS-Lat", required = false) Double latitude,
            @RequestHeader(value = "X-GPS-Lon", required = false) Double longitude) {

        leadCreationService.validateUserAndDevice(userId, primaryDeviceId, secondaryDeviceId);
        Channel channel = parseChannel(channelHeader);
        CreateLeadResponse result = leadCreationService.createLead(req, userId, channel, latitude, longitude);

        return result.routedToExceptionQueue
            ? ResponseEntity.accepted().body(result)
            : ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{lrn}")
    public ResponseEntity<Lead> getLeadByLrn(
            @PathVariable String lrn,
            @RequestHeader("X-User-Id") String userId) {
        return leadRepository.findByLrn(lrn)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Lead>> searchLeads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String temperature,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userId) {

        LeadStatus parsedStatus = parseEnum(LeadStatus.class, status, "status");
        LeadTemperature parsedTemp = parseEnum(LeadTemperature.class, temperature, "temperature");
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(
            leadRepository.search(parsedStatus, assignedTo, branchCode, parsedTemp, pageable));
    }
}
```

### Step 5 — Service Layer

Replace `@ApplicationScoped` (CDI) with `@Service`. Replace `@Inject` with constructor injection. All business logic is unchanged.

### Step 6 — application.properties keys

| Quarkus key | Spring Boot key |
|---|---|
| `quarkus.datasource.db-kind=postgresql` | `spring.datasource.driver-class-name=org.postgresql.Driver` |
| `quarkus.datasource.jdbc.url=` | `spring.datasource.url=` |
| `quarkus.datasource.username=` | `spring.datasource.username=` |
| `quarkus.flyway.migrate-at-start=true` | `spring.flyway.enabled=true` |
| `quarkus.kafka.bootstrap-servers=` | `spring.kafka.bootstrap-servers=` |
| `quarkus.redis.hosts=` | `spring.data.redis.host=`, `spring.data.redis.port=` |
| `quarkus.http.port=8080` | `server.port=8080` |

### Step 7 — Tests

Replace `@QuarkusTest` + REST Assured with `@SpringBootTest` + MockMvc or `@WebMvcTest`. The test logic (given/when/then scenarios) is identical; only the test harness annotations change.

---

## References

- Enterprise Architecture.md §Technology Stack
- [Quarkus vs Spring Boot performance benchmarks](https://quarkus.io/blog/quarkus-vs-spring-performance/)
- ADR-007: Clean Architecture as Mandated Internal Pattern
- `services/lead-service/` — current Quarkus implementation
