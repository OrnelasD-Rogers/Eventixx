# ADR-005: CQRS and Event Sourcing

- **Status:** Accepted
- **Date:** 2026-04-29
- **Author:** @ornelas
- **Scope:** Search Service, Reservation Service, Payment Service

> **Note:** This is a pure decision record. It does NOT contain implementation tasks. Tasks live in the Use Case (`UC-XXX-*.md`) that references this ADR.

## Context and Problem Statement

Eventixx has divergent read and write patterns:
- **Writes** are transactional, consistency-sensitive, and low-frequency (creating events, processing reservations).
- **Reads** are high-frequency, latency-sensitive, and require complex filtering (searching events by date, location, category, price).

Applying the same model for both leads to either poor query performance or overly complex write logic. CQRS separates these concerns. Event Sourcing provides an audit trail and enables temporal queries, which are valuable for reservation and payment domains.

## Decision Drivers

- Search requires full-text search, facets, and fast aggregations (Elasticsearch) to survive 10–100× read spikes without impacting write paths
- Reservation/Payment workflows benefit from event replay and audit trails
- Must demonstrate advanced architectural patterns for senior roles
- Must not over-complicate the entire system (scope control)

## Considered Options

### Option 1: CQRS everywhere
- **Pros:** Consistent pattern across all services; maximum separation of concerns
- **Cons:** Massive overhead; every service needs dual models; event sourcing all state is complex
- **Why not:** Over-engineering for a portfolio project. Adds months of work without proportional learning value.

### Option 2: No CQRS, traditional CRUD + database joins
- **Pros:** Simple; familiar
- **Cons:** Cannot demonstrate CQRS/event sourcing knowledge; search will be slow in PostgreSQL
- **Why not:** Fails the primary goal of showcasing senior-level patterns.

### Option 3: Selective CQRS + Partial Event Sourcing
- **Pros:** Focuses complexity where it matters; Search gets dedicated read model; Reservation/Payment get audit trails
- **Cons:** Two patterns coexist; requires clear documentation
- **Why not:** N/A — chosen option

## Decision

Chosen: **Option 3 — Selective CQRS + Partial Event Sourcing**

### CQRS Applied To:
- **Search Service:** Write model = consumes Kafka events from Event Catalog. Read model = Elasticsearch index optimized for queries.
- **Reservation Service:** Commands (create/cancel) go to PostgreSQL. Read queries for "my reservations" can use a simple read model projection in PostgreSQL.

### Event Sourcing Applied To:
- **Reservation Service:** Reservation aggregate state is reconstructible from `reservation.created`, `reservation.confirmed`, `reservation.cancelled` events stored in Kafka (log compaction enabled).
- **Payment Service:** Payment events (`payment.processed`, `payment.failed`) are the source of truth for audit and reconciliation.

### NOT Applied To:
- **User Service:** Traditional state storage. No need for event sourcing user profiles.
- **Notification Service:** Simple listener; no event sourcing needed.
- **Check-in Service:** Simple validation; no event sourcing needed.

## Architecture

```
┌─────────────────┐           ┌─────────────┐           ┌──────────────────┐
│  Event Catalog  │──event──▶│    Kafka    │──event──▶│ Search Service   │
│  Service        │ created   │             │ created   │ (Elasticsearch)  │
└─────────────────┘           └─────────────┘           └──────────────────┘

┌─────────────────┐           ┌─────────────┐          ┌──────────────────┐
│  Reservation    │──event──▶│    Kafka    │◀─replay─│ Reservation      │
│  Service        │ created   │  (compacted)│          │ Aggregate        │
└─────────────────┘           └─────────────┘          └──────────────────┘
```

## Kafka Log Compaction

For event-sourced topics (`reservation.events`, `payment.events`), enable log compaction to retain the latest state per aggregate:

```properties
cleanup.policy=compact
min.compaction.lag.ms=86400000
retention.ms=-1
```

## Validation

- Search Service integration test: write an event, assert it appears in Elasticsearch within 5 seconds
- Reservation Service: replay events from Kafka and verify aggregate state matches PostgreSQL snapshot
- Event schema evolution: add a new field to an event and verify backward compatibility

## Related Decisions
- ADR-002: Communication Patterns (Kafka as event backbone)
- ADR-003: Data Strategy (Elasticsearch for read models)
- ADR-004: Saga Pattern (saga events are part of the event stream)
