--liquibase formatted sql

--changeset ivan:v2-create-admin-account
CREATE TABLE admin_account
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    scopes        VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_admin_account_username UNIQUE (username)
);
