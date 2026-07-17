ALTER TABLE app_user
    ADD CONSTRAINT app_user_username_lowercase CHECK (username = lower(username)),
    ADD CONSTRAINT app_user_email_lowercase CHECK (email = lower(email));