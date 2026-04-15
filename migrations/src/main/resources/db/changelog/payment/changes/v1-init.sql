--liquibase formatted sql

--changeset xeno:v1-create-payment
CREATE TABLE payment
(
    id                   UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              BIGINT      NOT NULL,
    plan_id              VARCHAR(50) NOT NULL,
    yookassa_payment_id  UUID,
    amount               NUMERIC(10, 2) NOT NULL,
    currency             VARCHAR(10) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

--changeset xeno:v1-create-payment-yookassa-payment-id-index
CREATE INDEX idx_payment_yookassa_payment_id ON payment (yookassa_payment_id);

--changeset xeno:v1-create-outbox-event
CREATE TABLE outbox_payment_event
(
    id           BIGSERIAL    NOT NULL PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP
);

--changeset xeno:v1-create-outbox-event-unpublished-index
CREATE INDEX idx_outbox_event_unpublished ON outbox_payment_event (created_at)
    WHERE published_at IS NULL;
