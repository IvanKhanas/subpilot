--liquibase formatted sql

--changeset xeno:v1-create-user-loyalty-balance
CREATE TABLE user_loyalty_balance
(
    user_id BIGINT NOT NULL PRIMARY KEY,
    points  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_loyalty_balance_points CHECK (points >= 0)
);

--changeset xeno:v1-create-loyalty-transaction
CREATE TABLE loyalty_transaction
(
    id         BIGSERIAL   NOT NULL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    amount     BIGINT      NOT NULL,
    type       VARCHAR(10) NOT NULL,
    payment_id UUID,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT uq_loyalty_transaction_payment_id_type UNIQUE (payment_id, type),
    CONSTRAINT chk_loyalty_transaction_type CHECK (type IN ('EARNED', 'SPENT')),
    CONSTRAINT chk_loyalty_transaction_amount CHECK (amount != 0)
);

--changeset xeno:v1-create-loyalty-transaction-user-id-index
CREATE INDEX idx_loyalty_transaction_user_id ON loyalty_transaction (user_id);