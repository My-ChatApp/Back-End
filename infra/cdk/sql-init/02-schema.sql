-- MyChatApp PostgreSQL schema (local + RDS)
-- Source of truth: designDB.md §2.4

CREATE SCHEMA IF NOT EXISTS app;

CREATE TYPE app.friend_request_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');
CREATE TYPE app.conversation_type AS ENUM ('PRIVATE', 'GROUP');
CREATE TYPE app.member_role AS ENUM ('OWNER', 'MEMBER');
CREATE TYPE app.notification_type AS ENUM (
  'FRIEND_REQUEST',
  'FRIEND_ACCEPTED',
  'MESSAGE',
  'SYSTEM'
);
CREATE TABLE app.users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               CITEXT NOT NULL,
    username            VARCHAR(50) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    display_name        VARCHAR(100) NOT NULL,
    avatar_url          TEXT,
    avatar_public_id    VARCHAR(255),
    bio                 VARCHAR(500),
    phone               VARCHAR(20),
    date_of_birth       DATE,
    gender              VARCHAR(10),
    locale              VARCHAR(10) NOT NULL DEFAULT 'vi',
    online              BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at          TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT chk_users_soft_delete CHECK (deleted_at IS NULL OR is_active = FALSE),
    CONSTRAINT chk_users_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'))
);

CREATE INDEX idx_users_display_name ON app.users (display_name);
CREATE INDEX idx_users_online ON app.users (online) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email_active ON app.users (email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_phone ON app.users (phone)
    WHERE phone IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE app.friend_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id       UUID NOT NULL REFERENCES app.users(id) ON DELETE CASCADE,
    receiver_id     UUID NOT NULL REFERENCES app.users(id) ON DELETE CASCADE,
    status          app.friend_request_status NOT NULL DEFAULT 'PENDING',
    message         TEXT,
    responded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friend_requests_not_self CHECK (sender_id <> receiver_id),
    CONSTRAINT chk_friend_requests_responded CHECK (
        (status = 'PENDING' AND responded_at IS NULL)
        OR (status IN ('ACCEPTED', 'REJECTED') AND responded_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_friend_requests_pending_pair
    ON app.friend_requests (sender_id, receiver_id) WHERE status = 'PENDING';
CREATE UNIQUE INDEX uq_friend_requests_accepted_pair
    ON app.friend_requests (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id))
    WHERE status = 'ACCEPTED';
CREATE INDEX idx_friend_requests_receiver_status ON app.friend_requests (receiver_id, status);
CREATE INDEX idx_friend_requests_sender_status ON app.friend_requests (sender_id, status);

CREATE TABLE app.conversations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type                    app.conversation_type NOT NULL,
    title                   VARCHAR(255),
    description             TEXT,
    avatar_url              TEXT,
    created_by              UUID NOT NULL REFERENCES app.users(id),
    last_message_id         UUID,
    last_message_sender_id  UUID REFERENCES app.users(id),
    last_message_type       VARCHAR(10),
    last_message_at         TIMESTAMPTZ,
    last_message_preview    VARCHAR(500),
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_conversations_group_title CHECK (
        type <> 'GROUP' OR (title IS NOT NULL AND LENGTH(TRIM(title)) > 0)
    ),
    CONSTRAINT chk_conversations_last_message_type CHECK (
        last_message_type IS NULL OR last_message_type IN ('TEXT', 'FILE')
    )
);

CREATE INDEX idx_conversations_last_message_at
    ON app.conversations (last_message_at DESC NULLS LAST) WHERE deleted = FALSE;
CREATE INDEX idx_conversations_created_by ON app.conversations (created_by) WHERE deleted = FALSE;
CREATE INDEX idx_conversations_type ON app.conversations (type) WHERE deleted = FALSE;

CREATE TABLE app.conversation_members (
    conversation_id         UUID NOT NULL REFERENCES app.conversations(id) ON DELETE CASCADE,
    user_id                 UUID NOT NULL REFERENCES app.users(id) ON DELETE CASCADE,
    role                    app.member_role NOT NULL DEFAULT 'MEMBER',
    nickname                VARCHAR(100),
    invited_by              UUID REFERENCES app.users(id),
    last_read_message_id    UUID,
    last_read_at            TIMESTAMPTZ,
    unread_count            INTEGER NOT NULL DEFAULT 0 CHECK (unread_count >= 0),
    muted                   BOOLEAN NOT NULL DEFAULT FALSE,
    pinned                  BOOLEAN NOT NULL DEFAULT FALSE,
    notifications_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    archived                BOOLEAN NOT NULL DEFAULT FALSE,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    left_at                 TIMESTAMPTZ,
    joined_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT chk_conversation_members_left CHECK (
        (deleted = FALSE AND left_at IS NULL) OR (deleted = TRUE AND left_at IS NOT NULL)
    )
);

CREATE INDEX idx_conversation_members_user_id
    ON app.conversation_members (user_id) WHERE deleted = FALSE AND archived = FALSE;
CREATE INDEX idx_conversation_members_user_pinned
    ON app.conversation_members (user_id, pinned DESC, updated_at DESC)
    WHERE deleted = FALSE AND archived = FALSE;
CREATE INDEX idx_conversation_members_conversation
    ON app.conversation_members (conversation_id, user_id);

CREATE TABLE app.notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app.users(id) ON DELETE CASCADE,
    actor_id        UUID REFERENCES app.users(id) ON DELETE SET NULL,
    type            app.notification_type NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    reference_id    UUID,
    data            JSONB NOT NULL DEFAULT '{}',
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expire_at       TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days'),
    CONSTRAINT chk_notifications_read_consistency CHECK (
        (is_read = FALSE AND read_at IS NULL) OR (is_read = TRUE AND read_at IS NOT NULL)
    )
);

CREATE INDEX idx_notifications_user_created
    ON app.notifications (user_id, created_at DESC) WHERE deleted = FALSE;
CREATE INDEX idx_notifications_user_unread
    ON app.notifications (user_id, created_at DESC) WHERE is_read = FALSE AND deleted = FALSE;
CREATE INDEX idx_notifications_reference ON app.notifications (type, reference_id);
CREATE INDEX idx_notifications_data_gin ON app.notifications USING gin (data);

CREATE OR REPLACE FUNCTION app.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON app.users FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER trg_friend_requests_updated_at
    BEFORE UPDATE ON app.friend_requests FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON app.conversations FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER trg_conversation_members_updated_at
    BEFORE UPDATE ON app.conversation_members FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
