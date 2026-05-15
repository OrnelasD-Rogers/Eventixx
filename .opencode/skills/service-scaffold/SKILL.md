---
name: service-scaffold
description: Creates a new Spring Boot microservice from scratch following Eventixx conventions. Use this whenever the user says "create a new service", "criar um novo serviço", or when AGENTS.md lists a service that doesn't exist yet. Also triggers when starting implementation of a new bounded context — don't let the user manually create pom.xml, Dockerfile, application configs, exception classes, Kafka config, or directory structures. Generates everything consistently: PostgreSQL (JPA + Flyway), Elasticsearch, and events-only patterns. Registers in parent pom.xml, AGENTS.md, and docker-compose. Includes self-improvement via lessons learned.
license: MIT
metadata:
  version: "2.0.0"
  domain: infrastructure
  triggers: new service, scaffold, criar servico, create service, new bounded context, new microservice
  role: architect
  related-skills: docs-sync, doc-framework
---

# Service Scaffold

Creates a new microservice following Eventixx conventions. Uses templates in `references/` and scripts in `scripts/` — read those instead of generating inline code.

## Skill Location

This skill's internal files (scripts, references, lessons) are at `<skill-path>`. Reference them with the `<skill-path>/` prefix in all commands and file reads. Project files (services/, AGENTS.md, pom.xml) are relative to the working directory (project root).

## When to Use

- Asked to "create a new service" or "criar um novo serviço"
- Starting implementation of a new bounded context
- AGENTS.md service table lists a service that doesn't exist yet
- You find yourself manually writing pom.xml, Dockerfile, or application configs for a new service

## Core Workflow

### Step 0: Load Lessons Learned

Read `<skill-path>/lessons.md` if it exists. Apply relevant guidance during execution.

### Step 1: Collect Parameters

Ask the user for these. Use the `question` tool.

| Parameter | Required | Default | Example |
|-----------|----------|---------|---------|
| **Service name** (kebab-case) | ✅ | — | `reservation-service` |
| **Bounded context** (PascalCase) | ✅ | — | `Reservation` |
| **Description** | ✅ | — | `Manages ticket reservations` |
| **Database type** | ✅ | — | `postgresql`, `elasticsearch`, or `none` |
| **HTTP port** | ✅ | increment from last | `8083` |
| **DB port** | only if postgresql | increment | `5434` |
| **Kafka topics consumed** | — | — | `payment.confirmed` |
| **Kafka topics produced** | — | — | `reservation.confirmed` |
| **Consumer group** | — | `<service-name>` | `reservation-service` |
| **Has REST controllers?** | — | `yes` | — |
| **Has Eureka client?** | ✅ | `yes` | — |
| **Has Flyway?** | only if postgresql | `yes` | — |

Determine next available ports by reading `AGENTS.md` service table and all `docker-compose*.yml` files. Use `grep` to find used ports.

*Validation checkpoint:* All required parameters collected. No port conflicts with existing services.

### Step 2: Create Directory Structure

```bash
<skill-path>/scripts/create-directories.sh <service-name> <db-type>
```

This creates: `controllers/`, `dto/`, `entities/`, `exceptions/`, `mappers/`, `repositories/`, `services/messaging/`, `config/`, test dirs (`arch/`, `integration/`, `unit/`, `web/`, `repository/` if JPA), `resources/`, and `db/migration/` if postgresql.

### Step 3: Generate Base Files

For each template, read the reference file then adapt:

| File | Template Source | Placeholders |
|------|----------------|--------------|
| `services/<name>/pom.xml` | `<skill-path>/references/pom-template.xml` | SERVICE_NAME, SERVICE_DESCRIPTION; remove `__IF_*__` blocks for unused DB types |
| `services/<name>/Dockerfile` | `<skill-path>/references/dockerfile-template` | HTTP_PORT |
| `src/main/resources/application.yml` | `<skill-path>/references/application-template.yml` | SERVICE_NAME, HTTP_PORT, CONSUMER_GROUP, DB_SERVICE_NAME, DB_PORT; remove unused conditional blocks |
| `src/main/resources/application-dev.yml` | `<skill-path>/references/dev-application-template.yml` | DB_PORT, FLAT_PACKAGE; remove unused conditional blocks |
| `<Package>Application.java` | See pattern below | FLAT_PACKAGE, PASCAL_NAME |
| `ArchitectureTest.java` | `<skill-path>/references/architecture-test-template.java` | FLAT_PACKAGE |
| `PostgresRepositoryTest.java` | `<skill-path>/references/postgres-repository-test-template.java` (only if postgresql) | FLAT_PACKAGE |
| `KafkaConsumerConfig.java` | `<skill-path>/references/kafka-consumer-config-template.java` (only if Kafka) | FLAT_PACKAGE |

**Application.java pattern:**
```java
package com.eventixx.<flat-name>;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class <PascalName>Application {
    public static void main(String[] args) {
        SpringApplication.run(<PascalName>Application.class, args);
    }
}
```

**Name transformations:**
- kebab-case → PascalCase: `reservation-service` → `ReservationService`
- flat package: `reservation-service` → `reservationservice` (remove all hyphens and dots)

#### Config & Exception Classes

Read reference files from `event-catalog-service`, then create analogous versions with adjusted package name:

```bash
# Read these before writing analogous versions:
services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/config/OpenApiConfig.java
services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/config/SecurityConfig.java
services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/exceptions/*.java
services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/services/messaging/DomainEventPublisher.java
services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/services/messaging/KafkaDomainEventPublisher.java
```

**MapStructConfig.java** (always, no reference needed):
```java
@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING,
              unmappedTargetPolicy = ReportingPolicy.IGNORE)
public class MapStructConfig {}
```

#### Kafka Domain Events (if producing)

Create a record for each produced topic in `services/messaging/` following the pattern from `event-catalog-service`. Create a `KafkaPublisher` class analogous to `KafkaDomainEventPublisher`.

*Validation checkpoint:* All template placeholders replaced. Package names consistent. Conditional blocks removed for unused features.

### Step 4: Register in Parent POM & AGENTS.md

1. **Parent pom.xml** — Read project root `pom.xml`, add `<module>services/<name></module>` in alphabetical order within `<modules>`.
2. **AGENTS.md** — Add a new row to the service table (alphabetical): `| <Name> | <Bounded Context> | \`<db-name>\` | <port> |`
3. **docker-compose.<service>.yml** — If the service needs infrastructure (DB), create a Docker Compose file following the pattern in `docker-compose.event-catalog.yml`.

*Validation checkpoint:* Parent pom.xml has the new module. AGENTS.md table is updated. Docker compose exists if needed.

### Step 5: Validate Build

```bash
./mvnw clean install -pl services/<service-name> -DskipTests
```

If compilation fails, read errors, fix (likely package name mismatches), re-run. For test files, skip tests during validation — just ensure main source compiles.

*Validation checkpoint:* Zero compilation errors.

### Step 6: Save Lessons Learned

Append to `<skill-path>/lessons.md`:

```markdown
## YYYY-MM-DD — <service-name>

- [What was learned during this scaffold]
- [Edge cases or deviations from standard pattern]
- [Ports assigned, any conflicts found]
```

## Reference Guide

### Service Name Transformations

| kebab-case | PascalCase | flat package | DB service |
|------------|-----------|--------------|------------|
| `reservation-service` | `ReservationService` | `reservationservice` | `reservation-db` |
| `saga-orchestrator` | `SagaOrchestrator` | `sagaorchestrator` | `saga-db` |
| `payment-service` | `PaymentService` | `paymentservice` | `payment-db` |
| `notification-service` | `NotificationService` | `notificationservice` | `notification-db` |
| `check-in-service` | `CheckInService` | `checkinservice` | `checkin-db` |

### Port Allocation Reference

| Service | HTTP Port | DB Port |
|---------|-----------|---------|
| event-catalog-service | 8080 | 5433 |
| search-service | 8082 | 9200 (ES) |
| reservation-service | 8083 | 5434 |
| payment-service | 8084 | 5435 |
| saga-orchestrator | 8085 | 5436 |
| notification-service | 8086 | 5437 |
| check-in-service | 8087 | 5438 |

### Docker Compose Template

For services needing a database, create `docker-compose.<service>.yml`:

```yaml
services:
  <service-db>:
    image: postgres:16-alpine
    ports:
      - "<db-port>:5432"
    environment:
      POSTGRES_DB: eventixx
      POSTGRES_USER: eventixx
      POSTGRES_PASSWORD: eventixx
    volumes:
      - <service-db>-data:/var/lib/postgresql/data
    networks:
      - <service>-network

volumes:
  <service-db>-data:

networks:
  <service>-network:
    driver: bridge
```

## Constraints

### MUST DO
- Collect all parameters before creating any files
- Read reference files from existing services before creating analogous ones
- Validate compilation after scaffold completes
- Update parent pom.xml, AGENTS.md, and docker-compose
- Save lessons learned
- Check ALL docker-compose files for port conflicts, not just AGENTS.md

### MUST NOT DO
- Create files without parameters first
- Guess package names or ports
- Copy files verbatim without adjusting package names
- Skip build validation
- Create services conflicting with existing ports
- Overwrite existing service directories

## Output

After execution, provide a summary:

```markdown
## Scaffold Summary

| Item | Status |
|------|--------|
| `services/<name>/` created | ✅ |
| `pom.xml` with dependencies | ✅ |
| Base classes (exceptions, config) | ✅ |
| Application configs (yml) | ✅ |
| Kafka config (if applicable) | ✅ |
| Dockerfile | ✅ |
| Test base files | ✅ |
| Registered in parent `pom.xml` | ✅ |
| Registered in `AGENTS.md` | ✅ |
| `docker-compose.<service>.yml` | ✅ |
| Build validation (`mvn clean install`) | ✅ |

**Next steps:**
1. Implement domain entities and repositories
2. Create REST controllers and DTOs
3. Implement service layer with business logic
4. Write tests (unit → web → integration)
5. Update documentation (data-model, api-contracts, use case)
```
