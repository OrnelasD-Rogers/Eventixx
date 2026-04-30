# ADR-004: Saga Pattern for Distributed Transactions

- **Status:** Accepted
- **Date:** 2026-04-29
- **Author:** @ornelas
- **Scope:** Reservation and Payment workflows

> **Note:** This is a pure decision record. It does NOT contain implementation tasks. Tasks live in the Use Case (`UC-XXX-*.md`) that references this ADR.

## Context and Problem Statement

The ticket reservation flow spans multiple bounded contexts: Reservation (holds inventory), Payment (processes mock payment), and Notification (confirms the booking). Traditional ACID transactions cannot span multiple microservices with independent databases. The Saga pattern is the standard solution for managing distributed transactions in this scenario.

## Decision Drivers

- Must guarantee inventory is released if payment fails
- Must be observable and debuggable (portfolio project needs clarity)
- Must be resilient to partial failures

## Considered Options

### Option 1: Two-Phase Commit (2PC)
- **Pros:** Strong consistency
- **Cons:** Blocking protocol; reduced availability; poor performance; single point of failure
- **Why not:** Deprecated for microservices. Violates availability goals.

### Option 2: Saga Choreography (event-driven, no central coordinator)
- **Pros:** Highly decoupled; no single point of failure; services are independent
- **Cons:** Distributed business logic; hard to trace workflow; difficult to debug
- **Why not:** Debugging saga failures is a nightmare without a central state.

### Option 3: Saga Orchestration (central coordinator)
- **Pros:** Centralized visibility; clear workflow definition; easier debugging and monitoring; explicit compensation logic
- **Cons:** Orchestrator is a central dependency; requires state persistence
- **Why not:** N/A — chosen option

## Decision

Chosen: **Option 3 — Saga Orchestration with a dedicated Saga Orchestrator Service**, because operational visibility and debuggability are critical for a portfolio project. The orchestrator maintains saga state persistently and sends commands to participant services via Kafka.

## Saga: Ticket Reservation Flow

```
Step 1: Create Reservation
  → Reservation Service reserves tickets
  ← On success: proceed
  ← On failure: saga fails immediately

Step 2: Process Payment (Mock)
  → Payment Service processes mock payment
  ← On success: proceed
  ← On failure: COMPENSATE Step 1 (release reservation)

Step 3: Confirm Reservation
  → Reservation Service marks reservation as CONFIRMED
  ← On success: saga completed
  ← On failure: COMPENSATE Step 2 (refund mock) + Step 1 (release reservation)

Step 4: Send Confirmation (async, fire-and-forget)
  → Notification Service listens to reservation.confirmed event
```

## Compensation Rules

| Step | Compensation Action | Idempotency Key |
|------|--------------------|-----------------|
| 1 (Reserve) | Release held tickets | `sagaId` + `release-reservation` |
| 2 (Payment) | Reverse mock transaction | `sagaId` + `refund-payment` |

## Saga State Persistence

The Saga Orchestrator Service persists state in PostgreSQL:

```sql
CREATE TABLE saga_state (
    saga_id UUID PRIMARY KEY,
    saga_type VARCHAR(50) NOT NULL,
    current_step INTEGER NOT NULL DEFAULT 0,
    max_steps INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL, -- IN_PROGRESS, COMPLETED, FAILED, COMPENSATING
    payload JSONB NOT NULL,
    steps_completed JSONB DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

## Idempotency Requirement

Every saga participant (Reservation Service, Payment Service) must implement idempotent operations. Each command includes a `sagaId` that acts as an idempotency key. Services must check: "Have I already processed this sagaId?"

## Validation

- Integration test: happy path (reserve → pay → confirm)
- Integration test: payment failure triggers compensation
- Integration test: idempotency (same sagaId executed twice has no side effects)
- ArchUnit: verifies Saga Orchestrator does not contain business logic (only coordination)

## Related Decisions
- ADR-002: Communication Patterns (Kafka commands for saga steps)
- ADR-003: Data Strategy (saga state table in PostgreSQL)
