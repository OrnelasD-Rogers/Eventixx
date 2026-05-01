CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- Categories
-- ============================================================
CREATE TABLE categories
(
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100)  NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_categories_name ON categories (name) WHERE deleted_at IS NULL;

-- ============================================================
-- Venues
-- ============================================================
CREATE TABLE venues
(
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(200) NOT NULL,
    address    VARCHAR(500) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    country    VARCHAR(100) NOT NULL,
    capacity   INTEGER      NOT NULL CHECK (capacity > 0),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_venues_city_country ON venues (city, country) WHERE deleted_at IS NULL;
CREATE INDEX idx_venues_name ON venues (name) WHERE deleted_at IS NULL;

-- ============================================================
-- Events
-- ============================================================
CREATE TABLE events
(
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title        VARCHAR(300) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'ENDED'))
        DEFAULT 'DRAFT',
    start_time   TIMESTAMP    NOT NULL,
    end_time     TIMESTAMP    NOT NULL,
    venue_id     UUID         NOT NULL,
    category_id  UUID         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    deleted_at   TIMESTAMP,

    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id) REFERENCES venues (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_events_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_events_end_after_start
        CHECK (end_time > start_time)
);

CREATE INDEX idx_events_status_start_time ON events (status, start_time) WHERE deleted_at IS NULL;
CREATE INDEX idx_events_category_id ON events (category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_events_venue_id ON events (venue_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_events_published_at ON events (published_at) WHERE deleted_at IS NULL AND status = 'PUBLISHED';

-- ============================================================
-- Ticket Types
-- ============================================================
CREATE TABLE ticket_types
(
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id           UUID          NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    price              DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    quantity_available INTEGER       NOT NULL CHECK (quantity_available >= 0),
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP,

    CONSTRAINT fk_ticket_types_event
        FOREIGN KEY (event_id) REFERENCES events (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_ticket_types_event_id ON ticket_types (event_id) WHERE deleted_at IS NULL;

-- ============================================================
-- Trigger function to auto-update updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_venues_updated_at
    BEFORE UPDATE ON venues
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_ticket_types_updated_at
    BEFORE UPDATE ON ticket_types
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
