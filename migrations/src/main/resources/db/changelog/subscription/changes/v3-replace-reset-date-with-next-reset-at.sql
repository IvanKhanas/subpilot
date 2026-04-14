--liquibase formatted sql

--changeset xeno:v3-add-next-reset-at
ALTER TABLE user_free_quota
    ADD COLUMN next_reset_at TIMESTAMP NOT NULL DEFAULT NOW();

--changeset xeno:v3-drop-reset-date
ALTER TABLE user_free_quota
    DROP COLUMN reset_date;
