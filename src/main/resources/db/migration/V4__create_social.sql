CREATE TABLE friendship (
    id           UUID        NOT NULL,
    status       VARCHAR(16) NOT NULL,
    user_low_id  UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    user_high_id UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    requested_by UUID        NOT NULL REFERENCES app_user(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_friendship PRIMARY KEY (id),
    CONSTRAINT uq_friendship UNIQUE (user_low_id, user_high_id),
    CONSTRAINT chk_friendship_order CHECK (user_low_id < user_high_id)
);

CREATE TABLE block (
    id         UUID        NOT NULL,
    blocker_id UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    blocked_id UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_block PRIMARY KEY (id),
    CONSTRAINT uq_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_block_not_self CHECK (blocker_id <> blocked_id)
);

CREATE TABLE circle (
    id         UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    owner_id   UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_circle PRIMARY KEY (id),
    CONSTRAINT uq_circle_owner_name UNIQUE (owner_id, name)
);

CREATE TABLE circle_member (
    circle_id   UUID        NOT NULL REFERENCES circle(id) ON DELETE CASCADE,
    app_user_id UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_circle_member PRIMARY KEY (circle_id, app_user_id)
);