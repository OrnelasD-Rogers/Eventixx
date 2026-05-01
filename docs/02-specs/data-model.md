# Data Model

> Entity-relationship diagrams and schema definitions per service. Updated as migrations are created.

---

## Event Catalog Service (`event-catalog-db`)

### Overview

The Event Catalog Service owns four tables: `categories`, `venues`, `events`, and `ticket_types`. All tables implement **soft delete** via a `deleted_at` timestamp to preserve referential integrity and audit history.

### ER Diagram

```
+-------------+       +-------------+       +----------------+       +---------------+
| categories  |       |   venues    |       |     events     |       | ticket_types  |
+-------------+       +-------------+       +----------------+       +---------------+
| id (PK)     |       | id (PK)     |       | id (PK)        |       | id (PK)       |
| name (UQ)   |       | name        |       | title          |       | event_id (FK) |
| description |       | address     |       | description    |       | name          |
| created_at  |       | city        |       | status         |       | price         |
| updated_at  |       | country     |       | start_time     |       | qty_available |
| deleted_at  |       | capacity    |       | end_time       |       | created_at    |
+-------------+       | created_at  |       | venue_id (FK)  |       | updated_at    |
                      | updated_at  |       | category_id(FK)|       | deleted_at    |
                      | deleted_at  |       | created_at     |       +---------------+
                      +-------------+       | updated_at     |
                                            | published_at   |
                                            | deleted_at     |
                                            +----------------+
```

### Tables

#### `categories`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | PK, `uuid_generate_v4()` | |
| `name` | `VARCHAR(100)` | NOT NULL, UNIQUE | e.g. "Music", "Sports", "Theater" |
| `description` | `VARCHAR(500)` | | Optional metadata |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | Auto-updated by trigger |
| `deleted_at` | `TIMESTAMP` | | Soft delete marker |

**Indexes:** `idx_categories_name` (partial, `deleted_at IS NULL`)

---

#### `venues`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | PK, `uuid_generate_v4()` | |
| `name` | `VARCHAR(200)` | NOT NULL | Venue display name |
| `address` | `VARCHAR(500)` | NOT NULL | Street address |
| `city` | `VARCHAR(100)` | NOT NULL | Search/filter dimension |
| `country` | `VARCHAR(100)` | NOT NULL | Search/filter dimension |
| `capacity` | `INTEGER` | NOT NULL, CHECK > 0 | Max attendees |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | Auto-updated by trigger |
| `deleted_at` | `TIMESTAMP` | | Soft delete marker |

**Indexes:**
- `idx_venues_city_country` (partial, `deleted_at IS NULL`)
- `idx_venues_name` (partial, `deleted_at IS NULL`)

---

#### `events`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | PK, `uuid_generate_v4()` | |
| `title` | `VARCHAR(300)` | NOT NULL | Event display name |
| `description` | `TEXT` | | Long-form description |
| `status` | `VARCHAR(20)` | NOT NULL, CHECK IN ('DRAFT','PUBLISHED','CANCELLED','ENDED'), DEFAULT 'DRAFT' | Lifecycle state |
| `start_time` | `TIMESTAMP` | NOT NULL | Event start |
| `end_time` | `TIMESTAMP` | NOT NULL | Event end |
| `venue_id` | `UUID` | NOT NULL, FK → venues(id), ON DELETE RESTRICT | |
| `category_id` | `UUID` | NOT NULL, FK → categories(id), ON DELETE RESTRICT | |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | Auto-updated by trigger |
| `published_at` | `TIMESTAMP` | | Set when status transitions to PUBLISHED |
| `deleted_at` | `TIMESTAMP` | | Soft delete marker |

**Constraints:**
- `chk_events_end_after_start`: `end_time > start_time`

**Indexes:**
- `idx_events_status_start_time` (partial, `deleted_at IS NULL`)
- `idx_events_category_id` (partial, `deleted_at IS NULL`)
- `idx_events_venue_id` (partial, `deleted_at IS NULL`)
- `idx_events_published_at` (partial, `deleted_at IS NULL AND status = 'PUBLISHED'`)

---

#### `ticket_types`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | PK, `uuid_generate_v4()` | |
| `event_id` | `UUID` | NOT NULL, FK → events(id), ON DELETE RESTRICT | |
| `name` | `VARCHAR(100)` | NOT NULL | e.g. "General Admission", "VIP" |
| `price` | `DECIMAL(10,2)` | NOT NULL, CHECK >= 0 | |
| `quantity_available` | `INTEGER` | NOT NULL, CHECK >= 0 | Inventory for this type |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() | Auto-updated by trigger |
| `deleted_at` | `TIMESTAMP` | | Soft delete marker |

**Indexes:** `idx_ticket_types_event_id` (partial, `deleted_at IS NULL`)

---

### Shared Patterns

#### Soft Delete
All four tables include `deleted_at TIMESTAMP`. Queries must filter `WHERE deleted_at IS NULL` to exclude logically deleted rows. This preserves referential integrity and enables audit/replay scenarios.

> **Decision:** Soft delete chosen over hard delete to support:
> - Referential integrity (events referencing venues cannot have venue hard-deleted)
> - Audit trails for compliance/analytics
> - Accidental deletion recovery

#### Auto-Update `updated_at`
A PostgreSQL trigger function `update_updated_at_column()` is attached to all four tables, ensuring `updated_at` is refreshed on every `UPDATE`.

---

## Future Services

| Service | Database | Notes |
|---------|----------|-------|
| Search Service | Elasticsearch | Read model populated via Kafka events |
| User Service | PostgreSQL (isolated) | Own instance, not shared |
| Reservation Service | PostgreSQL (isolated) | Own instance |
| Notification Service | — | Stateless consumer |

