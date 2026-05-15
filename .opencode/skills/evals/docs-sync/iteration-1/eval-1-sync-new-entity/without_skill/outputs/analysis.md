# Docs Impact Analysis — New TicketType Entity & Endpoints

**Scenario:** Developer added `TicketType` entity (id UUID, name VARCHAR(100), price DECIMAL(10,2), event_id FK) + `GET /api/v1/ticket-types/{id}` and `POST /api/v1/ticket-types` in `EventController`.

## Changed Files & Categorization

| Changed File | Type | Layer |
|---|---|---|
| `entities/TicketType.java` | Entity (JPA) | Data Model |
| `controllers/EventController.java` | Controller (modified) | API Contract |
| `dto/TicketTypeRequest.java` | DTO (new) | API Contract |
| `dto/TicketTypeResponse.java` | DTO (new) | API Contract |

---

## Affected Documentation

### 1. `docs/02-specs/data-model.md` — UPDATE needed

**Why:** This file defines all database schemas. A new entity `ticket_types` must be documented.

**Specific changes:**
- Add a `ticket_types` table section under "Event Catalog Service" with columns:

  | Column | Type | Constraints | Notes |
  |---|---|---|---|
  | `id` | `UUID` | PK, `uuid_generate_v4()` | |
  | `event_id` | `UUID` | NOT NULL, FK → events(id), ON DELETE RESTRICT | |
  | `name` | `VARCHAR(100)` | NOT NULL | e.g. "General Admission", "VIP" |
  | `price` | `DECIMAL(10,2)` | NOT NULL, CHECK >= 0 | |
  | `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | |
  | `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | Auto-updated by trigger |
  | `deleted_at` | `TIMESTAMP` | | Soft delete marker |

- Add index: `idx_ticket_types_event_id` (partial, `deleted_at IS NULL`)
- Add FK arrow in ER diagram: `ticket_types.event_id` → `events.id`
- Update "Shared Patterns" section (soft delete / auto-update) to mention 4 → 5 tables

**Note:** The scenario entity omits `quantity_available`, `created_at`, `updated_at`, `deleted_at`. The data-model should reflect the *actual* entity state. If these fields are intentionally absent, the data-model matches the code; if they were forgotten, this is a code gap, not a docs gap.

### 2. `docs/02-specs/api-contracts/` — CREATE needed

**Why:** Charter principle §4.5 ("API-First Design") requires OpenAPI specs. The `api-contracts/` directory exists but is empty.

**Specific changes:**
- Create `docs/02-specs/api-contracts/event-catalog-service.yaml` (or extend if one exists)
- Add paths:
  - `GET /api/v1/ticket-types/{id}` — returns `TicketTypeResponse`
  - `POST /api/v1/ticket-types` — accepts `TicketTypeRequest`, returns `TicketTypeResponse`
- Add schemas:
  - `TicketTypeRequest` — properties: `name` (string, max 100), `price` (number, >= 0), `eventId` (UUID)
  - `TicketTypeResponse` — properties: `id` (UUID), `name`, `price`, `eventId`
- Add `404` response to GET (ticket type not found)
- Add `400` response to POST (validation error)
- Tag as "Ticket Types"

### 3. `docs/02-specs/use-cases/UC-001-event-catalog.md` — UPDATE needed

**Why:** This use case describes the event catalog flow. It already mentions "Organizer defines ticket types" in step 3 and lists `TicketTypeControllerWebTest` in Task 4. But the new endpoints are inside `EventController`, not a separate controller, which may affect naming/coverage.

**Specific changes:**
- Verify step 3 still reflects the API shape (was `/api/v1/events/{eventId}/ticket-types`, now `/api/v1/ticket-types`). If the path changed, update step 3.
- In Task 2 artifacts list: note that ticket type endpoints were added to `EventController` rather than a standalone `TicketTypeController` (if the scenario departs from original plan).
- In Task 4 test list: update to reflect which controller class tests ticket type endpoints. If `TicketTypeControllerWebTest.java` no longer exists, update the test count and references.
- If the entity has no `quantity_available`, update the validation exception table (remove "Ticket quantity exceeds venue capacity" row).

### 4. `docs/02-specs/glossary.md` — UPDATE needed

**Why:** A new domain concept was introduced.

**Specific changes:**
- Add entry: **Ticket Type** — A category of admission for an event (e.g., "General Admission", "VIP"). Defines price and name.

### 5. `docs/03-operations/testing-strategy.md` — UPDATE needed

**Why:** This file lists every test class per entity. Adding TicketType requires new tests.

**Specific changes:**
- Add `unit/TicketTypeServiceTest.java` under unit tests section
- Add `web/TicketTypeControllerWebTest.java` (or note these are covered by `EventControllerWebTest` if endpoints live in EventController)
- Add `repository/TicketTypeRepositorySoftDeleteTest.java` under repository tests
- Add `integration/TicketTypeIntegrationTest.java` under integration tests
- Update target counts: +1 unit, +1 web, +1 repository, +1 integration
- Update test package structure diagram

### 6. `docs/01-decisions/ADR-001-service-boundaries.md` — REVIEW (likely no change)

**Why:** ADR-001 already lists `ticket_types` as part of Event Catalog Service's data ownership (line 57). No change needed unless the table name differs.

### 7. `docs/04-implementation/setup.md` — UPDATE needed (minor)

**Why:** The "Expected tables" list in the database verification step (line 60) should be checked.

**Specific changes:**
- If `ticket_types` was not previously listed, add it. But looking at the current file, it's already there. If the entity is truly new, ensure this list includes `ticket_types` with a note about when it appears.

### 8. `docs/03-operations/security-guide.md` — REVIEW (likely no change)

**Why:** The new endpoints follow the same auth pattern (`@RequestHeader("X-User-Id")` for writes, public reads). No new auth rules needed.

### 9. `docs/02-specs/sequence-diagrams.md` — UPDATE needed (optional)

**Why:** If ticket type creation is part of a multi-step flow involving other services, a sequence diagram may help.

**Specific changes:**
- Add sequence diagram for "Organizer creates ticket types for event" showing: Client → EventController → TicketTypeService → EventRepository (verify event exists) → TicketTypeRepository (save) → response.

### 10. `docs/00-charter/PROJECT_CHARTER.md` — No change needed

**Why:** The charter talks about "Event Catalog Management — CRUD for events, venues, categories, and ticket types" at a high level. Adding a TicketType entity is within scope. No charter change needed.

---

## Summary

| Doc | Action | Priority |
|---|---|---|
| `data-model.md` | Add `ticket_types` table section | **High** |
| `api-contracts/` | Create OpenAPI spec for new endpoints | **High** (mandatory per §4.5) |
| `UC-001-event-catalog.md` | Update API path references, test artifact lists | **Medium** |
| `glossary.md` | Add "Ticket Type" definition | **Medium** |
| `testing-strategy.md` | Add test class entries & update counts | **Medium** |
| `setup.md` | Verify expected tables list | **Low** |
| `sequence-diagrams.md` | Add ticket type creation diagram (optional) | **Low** |
| `ADR-001-service-boundaries.md` | No change | — |
| `PROJECT_CHARTER.md` | No change | — |
| `security-guide.md` | No change | — |
| `deployment.md` | No change | — |
