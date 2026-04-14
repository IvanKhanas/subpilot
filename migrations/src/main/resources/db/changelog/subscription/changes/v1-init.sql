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
    user_id         BIGINT      NOT NULL,
    plan_id         VARCHAR(50) NOT NULL,
    provider        VARCHAR(50) NOT NULL,
    earned_requests INTEGER     NOT NULL,
    activated_at    TIMESTAMP   NOT NULL,
    CONSTRAINT fk_user_subscription_subscription_user
        FOREIGN KEY (user_id) REFERENCES subscription_user (user_id),
    CONSTRAINT chk_user_subscription_earned_requests CHECK (earned_requests > 0)
);

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
