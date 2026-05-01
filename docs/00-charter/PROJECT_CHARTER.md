# Eventixx Charter — Project Constitution

> **Status:** Accepted  
> **Last updated:** 2026-04-29  
> **Owner:** @ornelas  
> **Audience:** Developers, code reviewers, recruiters, and technical stakeholders.  
>
> *This document is the single source of truth for the scope, purpose, and architectural principles of this project. Every Architecture Decision Record (ADR) must be compatible with the rules established here.*

---

## 1. Project Purpose

**Eventixx** is an event-ticketing backend designed to handle the operational realities of high-demand sales: extreme traffic spikes (10–100× baseline during popular event releases), concurrent inventory contention, and distributed transactions spanning reservation, payment, and fulfillment. It is inspired by platforms like Ticketmaster, where seconds of downtime or double-booking directly translate to revenue loss and user churn.

This is a study and portfolio project built to demonstrate how senior Java/Spring Boot engineers architect systems under realistic load constraints. It targets remote mid-level/senior developer positions in the US and EU.

The architecture addresses specific failure modes inherent to this domain:

| Operational Challenge | Architectural Response |
|---|---|
| **Traffic spikes overwhelm monolithic read/write paths** | CQRS with dedicated read models (Elasticsearch/Redis) |
| **Concurrent seat reservations race against limited inventory** | DDD bounded contexts with isolated databases; optimistic locking at the aggregate level |
| **A reservation spans inventory, payment, and notification — any step can fail** | Saga pattern (orchestrated via Kafka) for distributed transaction consistency |
| **Services become temporarily unreachable under load** | Circuit breakers, retries, and timeouts (Resilience4j) |
| **Debugging failures across 5+ services is impossible without context** | Distributed tracing, structured logging, and metrics (OpenTelemetry) |
| **Services need to evolve independently without breaking consumers** | API-first design with versioned contracts and async event-driven integration (Kafka) |

---

## 2. Defined Scope (In-Scope)

### 2.1 Product Domain

For this version, the system supports:

1. **Event Catalog Management** — CRUD operations for events, venues, categories, and ticket types.
2. **Event Search & Discovery** — Full-text search with filters (date, location, category, price) and facet navigation.
3. **Ticket Reservation & Purchase** — End-to-end flow: search → select tickets → reserve → pay (mock) → confirm. Managed via Saga pattern.
4. **User Authentication** — JWT-based registration and login.
5. **Notifications** — Async confirmation and failure notifications via events.
6. **Check-in** — Token-based (UUID) ticket validation at event entry.

### 2.2 Tech Stack

- **Language / Framework:** Java 21, Spring Boot 4.0.6
- **API Gateway:** Spring Cloud Gateway
- **Service Discovery:** Netflix Eureka
- **Messaging / Event Streaming:** Apache Kafka (KRaft mode, no Zookeeper)
- **Databases:**
  - PostgreSQL ( transactional data per service)
  - Elasticsearch (search index / CQRS read model)
  - Redis (cache / CQRS read model)
- **Observability:** OpenTelemetry, Micrometer, Prometheus, Grafana, structured JSON logging
- **Resilience:** Resilience4j (circuit breaker, retry, timeout, rate limiter)
- **Security:** Spring Security, JWT (io.jsonwebtoken)
- **Containerization:** Docker, Docker Compose
- **Database Migrations:** Flyway
- **Testing:** JUnit 5, Mockito, Testcontainers

---

## 3. Anti-Scope (Explicitly Out of Scope)

> *Items listed here are deliberately excluded from this version of the design to maintain focus and viability. If an ADR proposes something on this list, it must be rejected or require a formal Charter revision.*

- **Real Payment Gateway Integration (Stripe, PayPal, etc.):** Payment processing is mocked to focus on Saga pattern and distributed transaction logic, not PCI compliance.
- **Frontend / Mobile Applications:** Eventixx is a pure backend portfolio project. API documentation (OpenAPI) serves as the interface contract.
- **Multi-Region / Geo-Distribution:** All infrastructure runs locally via Docker Compose. Cloud deployment patterns may be documented but not implemented.
- **Kubernetes Orchestration:** Docker Compose is sufficient for local development and demonstration. K8s manifests are out of scope for the MVP.
- **Spring Cloud Config Server:** Each service manages its own `application.yml`. Centralized configuration adds operational overhead without educational value for this stage.
- **Advanced Machine Learning / Recommendations:** No recommendation engine or ML-based pricing.
- **QR Code Image Generation:** Ticket tokens are UUID strings. QR image generation is deferred.

---

## 4. Architectural Principles

> *These principles are inviolable. Any ADR that contradicts them must justify a formal amendment to this Charter.*

### 4.1 Database Per Service
Every microservice owns its data exclusively. No shared database schemas, no direct cross-service queries. Data synchronization happens via events or API composition.

### 4.2 Async Over Sync for Cross-Aggregate Operations
Operations that span multiple bounded contexts (e.g., reservation → payment → confirmation) must use asynchronous event-driven communication. Synchronous calls are reserved for simple queries with sub-100ms SLA and max 2-3 service hops.

### 4.3 Event-Driven Integration
Services communicate state changes through domain events published to Kafka. This enables loose coupling, independent scalability, and temporal decoupling.

### 4.4 Design for Failure and Observability
Every external call must have explicit timeouts, retries, and circuit breakers. Every request must carry a correlation ID. All services expose health/readiness probes and emit structured logs, metrics, and traces.

### 4.5 API-First Design
All service APIs are defined with OpenAPI specifications before implementation. Contracts are versioned (URL versioning: `/api/v1/...`).

### 4.6 One File Per Use Case
Each use case is a single self-contained specification file that includes requirements, flow, architecture decisions, implementation plan, and acceptance criteria.

---

## 5. Architectural Decisions Already Made

> *Summary of decisions already consolidated. They have associated ADRs or serve as starting points for future ADRs.*

| ID | Decision | Context / Justification |
|---|---|---|
| **D1** | **Java 21 + Spring Boot 4.0.6** | Aligned with 2026 market demands (Java 21 preferred in US/EU senior roles). Spring Boot 4.x is mandatory for modern microservices. |
| **D2** | **Apache Kafka (KRaft)** | Required for event sourcing, CQRS, saga, and high-throughput event streaming. KRaft simplifies operations (no Zookeeper). |
| **D3** | **PostgreSQL as default transactional DB** | Most demanded relational DB in Java roles. Database-per-service pattern enforced. |
| **D4** | **Resilience4j over Netflix Hystrix** | Hystrix is in maintenance mode. Resilience4j is the modern standard for Spring Boot 3.x. |
| **D5** | **OpenTelemetry + Micrometer Tracing** | Industry standard replacing Zipkin/Jaeger directly. Demonstrates modern observability practices. |
| **D6** | **Spring Cloud Gateway + Eureka** | Standard Spring stack for API Gateway and service discovery in Docker Compose environments. |

---

## 6. Standard ADR Format

Every Architecture Decision Record (ADR) must follow the template in `references/adr-template.md`.

---

## 7. References

- [`use-cases/`](../02-specs/use-cases/) — Detailed use case specifications.
- [`01-decisions/`](../01-decisions/) — Architecture Decision Records.

---

> *"This document is alive. If the context changes, this Charter can be updated via a specific ADR that justifies the change in principles or scope."*
