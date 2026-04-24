--liquibase formatted sql

--changeset ivan:v1-create-audit-log
CREATE TABLE audit_log
(
    id          BIGSERIAL PRIMARY KEY,
    operator    VARCHAR(255) NOT NULL,
    action      VARCHAR(255) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id   VARCHAR(255),
    payload     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);