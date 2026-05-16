-- Add version (optimistic locking) and audit fields to events
ALTER TABLE events
    ADD COLUMN version      BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_by   VARCHAR(255),
    ADD COLUMN deleted_reason VARCHAR(500);

-- Add version and audit fields to venues
ALTER TABLE venues
    ADD COLUMN version      BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_by   VARCHAR(255),
    ADD COLUMN deleted_reason VARCHAR(500);

-- Add version and audit fields to ticket_types
ALTER TABLE ticket_types
    ADD COLUMN version      BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_by   VARCHAR(255),
    ADD COLUMN deleted_reason VARCHAR(500);
