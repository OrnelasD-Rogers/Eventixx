# ADR-003: Data Strategy — Database Per Service

- **Status:** Accepted
- **Date:** 2026-04-29
- **Author:** @ornelas
- **Scope:** All microservices with persistent state

> **Note:** This is a pure decision record. It does NOT contain implementation tasks. Tasks live in the Use Case (`UC-XXX-*.md`) that references this ADR.

## Context and Problem Statement

In a microservices architecture, data ownership is the primary boundary. If services share a database, they become tightly coupled at the persistence layer, undermining independent deployability and scalability. Eventixx needs a clear data strategy that respects bounded contexts while remaining operable in a local Docker Compose environment.

## Decision Drivers

- Charter principle: "Database Per Service" is non-negotiable
- Local development must be simple (single Docker Compose file)
- Production-readiness must be demonstrated (separate instances preferred)
- CQRS requires separate read models
- Polyglot persistence is impressive but adds operational overhead

## Considered Options

### Option 1: Single PostgreSQL instance, separate schemas per service
- **Pros:** Lower infrastructure cost; simpler Docker Compose; easy backups
- **Cons:** Shared resource contention; not true isolation; scaling limitations; risk of accidental cross-schema queries
- **Why not:** Violates the spirit of "Database Per Service".

### Option 2: Separate PostgreSQL instances per service (polyglot-lite)
- **Pros:** True isolation; independent scaling; production-realistic; enforces service boundaries at the infrastructure level
- **Cons:** Higher memory footprint locally; more containers to manage; slower startup
- **Why not:** N/A — chosen option.

### Option 3: Full polyglot persistence (PostgreSQL + MongoDB + Elasticsearch + Redis + ...)
- **Pros:** "Right tool for the job" narrative; impressive for recruiters
- **Cons:** Operational nightmare for a solo portfolio project; distracts from core learning goals
- **Why not:** Over-engineering. We use PostgreSQL as the primary transactional store, Elasticsearch for search, and Redis for cache/CQRS read models.

## Decision

Chosen: **Option 2 — Separate PostgreSQL instances per service for local development. Elasticsearch for search index. Redis for cache and CQRS read models.**

This demonstrates true database-per-service isolation, a key senior-level microservices competency, while remaining feasible locally thanks to available resources (20GB RAM).

## Data Ownership Matrix

| Service | Primary Store | Host (Docker) | Port | Read Model (if CQRS) | Technology |
|---------|---------------|---------------|------|---------------------|------------|
| User Service | PostgreSQL | `user-db` | 5432 | — | PostgreSQL |
| Event Catalog Service | PostgreSQL | `event-catalog-db` | 5433 | — | PostgreSQL |
| Search Service | — | — | — | Search index | Elasticsearch |
| Reservation Service | PostgreSQL | `reservation-db` | 5434 | — | PostgreSQL |
| Payment Service | PostgreSQL | `payment-db` | 5435 | — | PostgreSQL |
| Saga Orchestrator Service | PostgreSQL | `saga-db` | 5436 | — | PostgreSQL |
| Notification Service | PostgreSQL | `notification-db` | 5437 | — | PostgreSQL |
| Check-in Service | PostgreSQL | `checkin-db` | 5438 | — | PostgreSQL |
| Shared Cache | — | — | — | Cache / Session store | Redis |

## Cross-Service Data Access Rules

1. **NO direct database access** from one service to another's schema.
2. **API Composition** for real-time queries requiring data from multiple services.
3. **Event-Driven Replication** for denormalized read models (Search Service consumes `event.*` topics).
4. **CQRS** explicitly separates write models (PostgreSQL) from read models (Elasticsearch / Redis).

## PostgreSQL Connection Convention

Each service connects to its own PostgreSQL instance. No schema prefix is needed.

```yaml
# Example: event-catalog-service application.yml
spring:
  datasource:
    url: jdbc:postgresql://event-catalog-db:5433/eventixx
    username: eventixx
    password: eventixx
```

Flyway migrations are standard: `db/migration/V1__init.sql`

## Validation

- Integration tests use Testcontainers with one PostgreSQL container per service under test
- Docker Compose defines one PostgreSQL container per service (see `deployment.md`)
- ArchUnit tests verify no hardcoded JDBC URLs pointing to another service's database
- Health checks verify each service can connect only to its own database instance

## Related Decisions
- ADR-001: Service Boundaries (defines which data belongs to which context)
- ADR-005: CQRS and Event Sourcing (read models in Elasticsearch/Redis)
