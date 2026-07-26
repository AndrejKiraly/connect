CREATE TABLE app_user_settings
(
    app_user_id           UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    default_post_lifespan VARCHAR(16) NOT NULL DEFAULT 'FOREVER',
    language              VARCHAR(10) NOT NULL DEFAULT 'EN',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_user_settings PRIMARY KEY (app_user_id),
    CONSTRAINT chk_app_user_settings_default_post_lifespan
        CHECK (default_post_lifespan IN ('DAY', 'WEEK', 'MONTH', 'FOREVER')),
    CONSTRAINT chk_app_user_settings_language
        CHECK (language IN ('SK', 'EN', 'DE'))
);