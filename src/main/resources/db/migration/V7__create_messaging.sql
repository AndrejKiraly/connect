CREATE TABLE conversation
(
    id           UUID        NOT NULL,
    type         VARCHAR(16) NOT NULL,
    user_low_id  UUID        REFERENCES app_user (id),
    user_high_id UUID        REFERENCES app_user (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_conversation PRIMARY KEY (id),
    CONSTRAINT chk_conversation_type CHECK (type IN ('DIRECT', 'GROUP')),
    CONSTRAINT chk_conversation_direct_pair CHECK (
        type <> 'DIRECT'
            OR (user_low_id IS NOT NULL AND user_high_id IS NOT NULL AND user_low_id < user_high_id)
        ),
    CONSTRAINT chk_conversation_pair_direct_only CHECK (
        type = 'DIRECT'
            OR (user_low_id IS NULL AND user_high_id IS NULL)
        )
);

CREATE UNIQUE INDEX uq_conversation_direct
    ON conversation (user_low_id, user_high_id)
    WHERE type = 'DIRECT';

CREATE TABLE message
(
    id              UUID        NOT NULL,
    conversation_id UUID        NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    sender_id       UUID        NOT NULL REFERENCES app_user (id),
    body            TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_message PRIMARY KEY (id),
    CONSTRAINT chk_message_body CHECK (char_length(body) <= 4000 AND btrim(body) <> '')
);

CREATE INDEX idx_message_conversation ON message (conversation_id, id);

CREATE TABLE conversation_member
(
    conversation_id      UUID        NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    app_user_id          UUID        NOT NULL REFERENCES app_user (id),
    last_read_message_id UUID,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_conversation_member PRIMARY KEY (conversation_id, app_user_id)
);

CREATE INDEX idx_conversation_member_user ON conversation_member (app_user_id);
