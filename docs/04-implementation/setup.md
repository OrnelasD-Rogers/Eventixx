# Setup

> How to run this project in 5 minutes.

---

## Prerequisites

- **Java 21** (Amazon Corretto recommended via SDKMAN)
- **Docker** + **Docker Compose**
- **Maven Wrapper** (`./mvnw`) — bundled, no local Maven install required

Verify Java version:
```bash
java -version  # Expected: openjdk 21
```

---

## Quick Start

### 1. Build the Project

```bash
./mvnw clean install
```

This compiles all service modules and runs unit tests.

### 2. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- 7× PostgreSQL instances (one per service, ports 5432–5438)
- Kafka (KRaft mode, port 9092)
- Elasticsearch (port 9200)
- Redis (port 6379)
- Eureka Server (port 8761)

Wait for healthchecks to pass (~30 seconds):
```bash
docker-compose ps
```

### 3. Verify Database Migrations

Check that Flyway migrations ran for `event-catalog-service`:
```bash
docker logs event-catalog-db | grep "Flyway"
```

Or connect to the database:
```bash
docker exec -it event-catalog-db psql -U eventixx -d eventixx -c "\dt"
```

Expected tables: `categories`, `venues`, `events`, `ticket_types`, `flyway_schema_history`.

### 4. Run a Service

```bash
./mvnw spring-boot:run -pl services/event-catalog-service
```

The service will:
- Register with Eureka at `http://localhost:8761`
- Connect to its dedicated PostgreSQL instance
- Be ready on `http://localhost:8080`

### 5. API Documentation

With the service running, OpenAPI docs are available at:
```
http://localhost:8080/swagger-ui.html
```

---

## Development Notes

### MapStruct + Lombok
MapStruct code generation is automatic during compilation. No manual step required. The `maven-compiler-plugin` in the root POM configures annotation processors in the correct order (Lombok first, then MapStruct).

### Adding a New Service
1. Create `services/<service-name>/pom.xml` with parent `eventixx-parent`
2. Add module to root `pom.xml`
3. Add database container to `docker-compose.yml` (next available port)
4. Create Flyway migration directory: `src/main/resources/db/migration/`

### Quality Checks

All quality tools run automatically during `./mvnw verify`:

```bash
# Run all quality checks (tests + SpotBugs + PMD + Checkstyle)
./mvnw verify

# Run only static analysis (skip tests)
./mvnw verify -DskipTests

# Run only ArchUnit architecture tests
./mvnw test -pl services/event-catalog-service -Dtest=ArchitectureTest

# Run checks for a specific service
./mvnw verify -pl services/event-catalog-service
```

Tools configured in the parent POM:
- **SpotBugs + FindSecBugs**: Bytecode-level bug and security vulnerability detection
- **PMD + CPD**: Code smells, copy-paste detection, and best practice enforcement
- **Checkstyle**: Google Java Style formatting with 120-character line length
- **ArchUnit**: Architecture rules (package cycles, layer independence, naming conventions, Spring proxy rules)

### Stopping Everything
```bash
docker-compose down -v  # -v removes named volumes
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `port is already allocated` | Kill existing containers: `docker-compose down` |
| Flyway migration fails | Check `docker logs event-catalog-db` for SQL errors |
| MapStruct mapper not found | Ensure `mvnw clean compile` ran after adding `@Mapper` |
| Eureka shows no services | Verify `eureka.client.service-url.defaultZone` in `application.yml` |

