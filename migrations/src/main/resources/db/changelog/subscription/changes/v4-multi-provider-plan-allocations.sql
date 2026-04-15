--liquibase formatted sql

--changeset xeno:v4-drop-user-subscription-payment-id-unique
ALTER TABLE user_subscription DROP CONSTRAINT uq_user_subscription_payment_id;

--changeset xeno:v4-add-user-subscription-payment-id-provider-unique
ALTER TABLE user_subscription ADD CONSTRAINT uq_user_subscription_payment_id_provider
    UNIQUE (payment_id, provider);