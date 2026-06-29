CREATE TABLE app_user_settings (
    app_user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    default_post_lifespan VARCHAR(16) NOT NULL DEFAULT 'FOREVER',
    language VARCHAR(10) NOT NULL DEFAULT 'EN',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_default_post_lifespan CHECK ( default_post_lifespan IN ('DAY', 'WEEK', 'MONTH', 'FOREVER')),
    CONSTRAINT chk_language CHECK ( language IN ('SK', 'EN', 'DE') )
)