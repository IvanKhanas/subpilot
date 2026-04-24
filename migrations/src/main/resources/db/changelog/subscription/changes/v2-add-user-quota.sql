--liquibase formatted sql

--changeset xeno:v2-create-user-free-quota
CREATE TABLE user_free_quota
(
    user_id            BIGINT      NOT NULL,
    provider           VARCHAR(50) NOT NULL,
    requests_remaining INTEGER     NOT NULL,
    next_reset_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT user_free_quota_pkey PRIMARY KEY (user_id, provider),
    CONSTRAINT fk_user_free_quota_subscription_user
        FOREIGN KEY (user_id) REFERENCES subscription_user (user_id),
    CONSTRAINT chk_user_free_quota_requests_remaining CHECK (requests_remaining >= 0)
);

--changeset xeno:v2-populate-user-free-quota
INSERT INTO user_free_quota (user_id, provider, requests_remaining, next_reset_at)
SELECT user_id, provider, 10, NOW()
FROM user_request_balance;

--changeset xeno:v2-clear-user-request-balance
UPDATE user_request_balance
SET requests_remaining = 0;