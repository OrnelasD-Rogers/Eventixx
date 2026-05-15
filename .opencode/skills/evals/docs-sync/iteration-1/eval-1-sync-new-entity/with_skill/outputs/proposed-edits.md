# Proposed Doc Edits — TicketType Entity & Endpoints

## Edit 1: `docs/02-specs/glossary.md` — Add "Ticket Type" entry

**Location:** Append after the existing header (line 3).

**Old content (lines 1-3):**
```markdown
# Glossary

> Domain-specific terminology and ubiquitous language.
```

**New content:**
```markdown
# Glossary

> Domain-specific terminology and ubiquitous language.

## Terms

| Term | Definition |
|------|------------|
| Ticket Type | A category of admission within an event, defined by name (`VARCHAR(100)`), price (`DECIMAL(10,2)`), and available quantity (`INTEGER`). Each ticket type belongs to exactly one event via foreign key (`event_id → events.id`). Examples: "General Admission", "VIP", "Student". |
```

---

## Edit 2: `docs/02-specs/use-cases/UC-001-event-catalog.md` — Optional Execution Log Entry

**Location:** Execution Log table at line 176.

**Old content (line 178):**
```
| T1   | ☑ Completed | 2026-04-30 | Created Maven module, Dockerfile, application.yml. ... |
```

**New content — insert after T4 row (after line 181):**
```
| T2   | ☑ Completed | 2026-05-15 | Added top-level `GET /api/v1/ticket-types/{id}` and `POST /api/v1/ticket-types` endpoints on EventController. New entity `TicketType.java`, request DTO `TicketTypeRequest.java`, and response DTO `TicketTypeResponse.java` follow existing patterns. MapStruct mappers updated. |
```

*Note: Only add if these endpoints represent new work not already tracked in T2's original implementation. If they were part of the original T2 scope, no log entry is needed.*

---

## Edit 3: `docs/03-operations/testing-strategy.md` — Conditional Test File Update

**Condition:** Only apply if new web/integration tests were added for the top-level ticket-type endpoints.

**Location:** §4 Test Package Structure, `web/` subsection (lines 150-154).

**Current:**
```
├── web/                            # Controller tests (@WebMvcTest)
│   ├── EventControllerWebTest.java
│   ├── VenueControllerWebTest.java
│   ├── CategoryControllerWebTest.java
│   └── TicketTypeControllerWebTest.java
```

**If `EventControllerWebTest` was extended with ticket-type test cases (no new file needed):**
— No structural change; test count note in UC-001 Execution Log suffices.

**If a new test file was created (e.g., `TicketTypeControllerWebTest2.java`):**
```
├── web/                            # Controller tests (@WebMvcTest)
│   ├── EventControllerWebTest.java
│   ├── VenueControllerWebTest.java
│   ├── CategoryControllerWebTest.java
│   ├── TicketTypeControllerWebTest.java
│   └── TicketTypeStandaloneControllerWebTest.java
```

*Current assessment: Developer mentioned no test files in the diff. Skip this edit.*

---

## Edit 4: `docs/02-specs/data-model.md` — Verification (No Change)

**Location:** Lines 99-113 (ticket_types table) and lines 18-31 (ER diagram).

**Verification result:** ALREADY COMPLETE. The existing documentation matches the entity:

| Entity field | Table column in data-model.md | Match? |
|--------------|------------------------------|--------|
| `id UUID` | `id UUID PK` | ✅ |
| `name VARCHAR(100)` | `name VARCHAR(100) NOT NULL` | ✅ |
| `price DECIMAL(10,2)` | `price DECIMAL(10,2) NOT NULL, CHECK >= 0` | ✅ |
| `event_id FK → events.id` | `event_id UUID NOT NULL, FK → events(id)` | ✅ |
| `quantityAvailable` | `quantity_available INTEGER NOT NULL, CHECK >= 0` | ✅ (extra — entity has it too) |

**No edit needed.**

---

## Summary Table

| # | Doc | Edit | Surgical Change | Applied |
|---|-----|------|-----------------|---------|
| 1 | `docs/02-specs/glossary.md` | Add "Ticket Type" term + definition | Append table after header | 🔲 |
| 2 | `docs/02-specs/use-cases/UC-001-event-catalog.md` | Optional: add Execution Log entry | Insert row after T4 in table | 🔲 (optional) |
| 3 | `docs/03-operations/testing-strategy.md` | Only if new test files exist | Update §4 file listing | 🔲 (conditional) |
| 4 | `docs/02-specs/data-model.md` | Verify only — no change | None | ✅ |
| 5 | `docs/02-specs/api-contracts/` | No action — OpenAPI annotations suffice | None | ✅ |
