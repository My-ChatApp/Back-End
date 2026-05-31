-- Outbox for chat-service async event publish (space-based architecture)

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                   WHERE t.typname = 'outbox_event_status' AND n.nspname = 'app') THEN
        CREATE TYPE app.outbox_event_status AS ENUM ('PENDING', 'PUBLISHED', 'FAILED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS app.outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(128) NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    routing_key     VARCHAR(128) NOT NULL,
    payload         JSONB NOT NULL,
    status          app.outbox_event_status NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_pending_aggregate
    ON app.outbox_events (aggregate_id, event_type)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_outbox_poll
    ON app.outbox_events (status, next_retry_at)
    WHERE status IN ('PENDING', 'FAILED');
