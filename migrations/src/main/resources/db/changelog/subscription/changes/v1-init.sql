--liquibase formatted sql

--changeset xeno:v1-create-subscription-user
CREATE TABLE subscription_user
(
    user_id       BIGINT      NOT NULL PRIMARY KEY,
    registered_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    blocked       BOOLEAN     NOT NULL DEFAULT FALSE,
    role          VARCHAR(20) NOT NULL DEFAULT 'USER',
    CONSTRAINT chk_subscription_user_role CHECK (role IN ('USER', 'ADMIN'))
);

--changeset xeno:v1-create-user-subscription
CREATE TABLE user_subscription
(
    id              BIGSERIAL   NOT NULL PRIMARY KEY,
    payment_id      UUID        NOT NULL,
    user_id         BIGINT      NOT NULL,
    plan_id         VARCHAR(50) NOT NULL,
    provider        VARCHAR(50) NOT NULL,
    earned_requests INTEGER     NOT NULL,
    activated_at    TIMESTAMP   NOT NULL,
    CONSTRAINT uq_user_subscription_payment_id_provider UNIQUE (payment_id, provider),
    CONSTRAINT fk_user_subscription_subscription_user
        FOREIGN KEY (user_id) REFERENCES subscription_user (user_id),
    CONSTRAINT chk_user_subscription_earned_requests CHECK (earned_requests > 0)
);

--changeset xeno:v1-create-user-subscription-user-id-index
CREATE INDEX idx_user_subscription_user_id ON user_subscription (user_id);

--changeset xeno:v1-create-user-request-balance
CREATE TABLE user_request_balance
(
    user_id            BIGINT      NOT NULL,
    provider           VARCHAR(50) NOT NULL,
    requests_remaining INTEGER     NOT NULL,
    CONSTRAINT user_request_balance_pkey PRIMARY KEY (user_id, provider),
    CONSTRAINT fk_user_request_balance_subscription_user
        FOREIGN KEY (user_id) REFERENCES subscription_user (user_id),
    CONSTRAINT chk_user_request_balance_requests_remaining CHECK (requests_remaining >= 0)
);

--changeset xeno:v1-create-user-model-preference
CREATE TABLE user_model_preference
(
    user_id  BIGINT       NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    CONSTRAINT user_model_preference_pkey PRIMARY KEY (user_id),
    CONSTRAINT fk_user_model_preference_subscription_user
        FOREIGN KEY (user_id) REFERENCES subscription_user (user_id)
);

--changeset xeno:v1-create-subscription-plan
CREATE TABLE subscription_plan
(
    plan_id      VARCHAR(50)    NOT NULL PRIMARY KEY,
    provider     VARCHAR(50)    NOT NULL,
    display_name VARCHAR(200)   NOT NULL,
    price        NUMERIC(10, 2) NOT NULL,
    currency     VARCHAR(10)    NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_subscription_plan_price CHECK (price > 0)
);

--changeset xeno:v1-create-subscription-plan-allocation
CREATE TABLE subscription_plan_allocation
(
    id       BIGSERIAL   NOT NULL PRIMARY KEY,
    plan_id  VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    requests INTEGER     NOT NULL,
    CONSTRAINT fk_subscription_plan_allocation_plan
        FOREIGN KEY (plan_id) REFERENCES subscription_plan (plan_id),
    CONSTRAINT chk_subscription_plan_allocation_requests CHECK (requests > 0)
);

CREATE INDEX idx_subscription_plan_allocation_plan_id ON subscription_plan_allocation (plan_id);

--changeset xeno:v1-seed-subscription-plans
INSERT INTO subscription_plan (plan_id, provider, display_name, price, currency)
VALUES ('openai-basic', 'openai', 'Basic - 100 requests for OpenAI', 199.00, 'RUB'),
       ('openai-pro', 'openai', 'Pro - 300 requests for OpenAI', 499.00, 'RUB');

INSERT INTO subscription_plan_allocation (plan_id, provider, requests)
VALUES ('openai-basic', 'openai', 100),
       ('openai-pro', 'openai', 300);
