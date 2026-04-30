# ADR-001: Service Boundaries and Bounded Contexts

- **Status:** Accepted
- **Date:** 2026-04-29
- **Author:** @ornelas
- **Scope:** Entire system architecture

> **Note:** This is a pure decision record. It does NOT contain implementation tasks. Tasks live in the Use Case (`UC-XXX-*.md`) that references this ADR.

## Context and Problem Statement

Eventixx is a ticketing platform that touches multiple business capabilities: event management, search, user identity, reservations, payments, notifications, and event entry validation. In a microservices architecture, defining clear service boundaries is critical to avoid creating a distributed monolith or anemic entity services.

## Decision Drivers

- Each service must own its data exclusively (Database Per Service principle)
- Services must be independently deployable
- Boundaries must align with DDD bounded contexts and business capabilities
- Must avoid chatty interfaces and synchronous coupling

## Considered Options

### Option 1: Entity-Based Decomposition (CRUD services)
- **Pros:** Simple to map (UserService, EventService, OrderService)
- **Cons:** Anemic domain models, no business logic ownership, high coupling
- **Why not:** Violates DDD principles; creates distributed monolith

### Option 2: Business Capability Decomposition (DDD bounded contexts)
- **Pros:** Natural language boundaries, autonomous teams, clear data ownership
- **Cons:** Requires deeper domain analysis upfront
- **Why not:** N/A — chosen option

## Decision

Chosen: **Option 2 — Business Capability Decomposition**, because it aligns with DDD, ensures each service has a rich domain model, and prevents anemic services.

## Consequences

### Positive
- Clear data ownership per service
- Independent scalability (Search can scale separately from Catalog)
- Natural alignment with event-driven architecture (each context publishes its own events)
- Easier to explain in technical interviews (bounded contexts are a senior-level concept)

### Negative / Risks
- Cross-context queries require API composition or CQRS read models
- Eventual consistency must be embraced and documented
- Initial setup is more complex than a monolith

## Service Boundaries

| Service | Bounded Context | Data Ownership | Key Events Published |
|---------|----------------|----------------|---------------------|
| **API Gateway** | Infrastructure | N/A | N/A |
| **Discovery Service** | Infrastructure | N/A | N/A |
| **User Service** | Identity & Access | users, credentials | `user.registered`, `user.authenticated` |
| **Event Catalog Service** | Event Management | events, venues, categories, ticket_types | `event.created`, `event.updated`, `event.published`, `event.cancelled` |
| **Search Service** | Search & Discovery | search index (Elasticsearch) | N/A (read-only consumer) |
| **Reservation Service** | Reservation | reservations, ticket_allocations | `reservation.created`, `reservation.confirmed`, `reservation.cancelled` |
| **Payment Service** | Payment (Mock) | payments, transactions | `payment.processed`, `payment.failed` |
| **Saga Orchestrator Service** | Process Coordination | saga_states | saga command events |
| **Notification Service** | Notifications | notification_logs | N/A (listener only) |
| **Check-in Service** | Event Entry | check_in_tokens, check_in_records | `checkin.completed` |

## Validation

- Verify no shared database tables between services
- Verify each service has a clear public API contract (OpenAPI)
- Verify services can be started independently (ArchUnit or integration tests)

## Related Decisions
- ADR-003: Data Strategy (Database Per Service enforcement)
- ADR-002: Communication Patterns (how services interact across boundaries)
