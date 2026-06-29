CREATE TABLE app_user
(
    id            UUID         NOT NULL,
    username      VARCHAR(40)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(150) NOT NULL,
    birth_date    date         NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_appuser PRIMARY KEY (id)
);