# ADR-002: Inter-Service Communication Patterns

- **Status:** Accepted
- **Date:** 2026-04-29
- **Author:** @ornelas
- **Scope:** All microservices interactions

> **Note:** This is a pure decision record. It does NOT contain implementation tasks. Tasks live in the Use Case (`UC-XXX-*.md`) that references this ADR.

## Context and Problem Statement

Microservices in Eventixx need to communicate for two distinct purposes: (1) querying data across contexts, and (2) executing business workflows that span multiple contexts (e.g., reservation + payment). Choosing the wrong pattern leads to tight coupling, cascading failures, or unacceptable latency.

## Decision Drivers

- Reservation flow is long-running and must survive partial failures
- Search requires high-throughput reads with low latency, especially during 10–100× traffic spikes
- User authentication must be synchronous (user is waiting)
- Observability requires tracing across all communication paths
- Docker Compose local environment must remain simple

## Considered Options

### Option 1: Synchronous REST everywhere
- **Pros:** Simple to implement, easy to debug, immediate feedback
- **Cons:** Tight temporal coupling, cascading failures, blocking threads
- **Why not:** Violates Charter principle "Async Over Sync for Cross-Aggregate Operations"

### Option 2: gRPC for internal services
- **Pros:** Low latency, binary protocol, strong typing, HTTP/2 multiplexing
- **Cons:** Requires protobuf definitions; harder to debug locally; adds complexity
- **Why not:** Overkill for a portfolio/Docker Compose project. REST + Kafka is more universally understood by recruiters.

### Option 3: REST for queries + Kafka events for commands/workflows
- **Pros:** Queries are fast and simple; workflows are decoupled and resilient; aligns with 2026 best practices
- **Cons:** Two patterns to maintain; requires event schema governance
- **Why not:** N/A — chosen option

## Decision

Chosen: **Option 3 — REST for synchronous queries, Kafka events for asynchronous commands and workflows**, because it separates read concerns from write/workflow concerns, maximizes resilience, and is the most common pattern in production Java microservices.

## Communication Rules

| Scenario | Pattern | Protocol | Example |
|----------|---------|----------|---------|
| Simple query (sub-100ms, user waiting) | Synchronous | REST HTTP/1.1 | `GET /api/v1/events/{id}` |
| Cross-aggregate business workflow | Asynchronous | Kafka events | `reservation.created` → Saga Orchestrator |
| Domain event publication | Asynchronous | Kafka events | `event.published` → Search Service |
| Service-to-service direct call (query composition) | Synchronous | REST + OpenFeign | `GET /api/v1/users/{id}` from Reservation |

## Kafka Topic Naming Convention

- Domain events: `{domain}.{action}` — e.g., `event.created`, `payment.processed`
- Saga commands: `saga.{saga-name}.command` — e.g., `saga.reservation.execute`
- Saga compensations: `saga.{saga-name}.compensate` — e.g., `saga.reservation.compensate`

## Event Schema Requirements

All events must include:
- `eventId` (UUID)
- `eventType` (string)
- `eventVersion` (semantic versioning, e.g., "1.0")
- `timestamp` (ISO 8601)
- `aggregateId` (UUID of the domain aggregate)
- `correlationId` (UUID for distributed tracing)
- `payload` (domain-specific data)

## Validation

- Integration tests verify event schema compliance
- ArchUnit tests verify no synchronous calls between saga participants
- Trace IDs must be present in 100% of Kafka message headers and HTTP headers

## Related Decisions
- ADR-001: Service Boundaries (defines who talks to whom)
- ADR-004: Saga Pattern (orchestrated via Kafka commands)
- ADR-005: CQRS and Event Sourcing (read model updated via Kafka events)
