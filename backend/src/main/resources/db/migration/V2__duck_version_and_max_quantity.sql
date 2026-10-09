-- Optimistic locking: every change to a duck bumps its version, so stale edits and deletes are rejected.
ALTER TABLE duck
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    -- Must match Duck.MAX_QUANTITY. Also stops merged quantities from ever overflowing the INT column.
    ADD CONSTRAINT ck_duck_max_quantity CHECK (quantity <= 1000000);
