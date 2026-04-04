--liquibase formatted sql

--changeset xeno:v1-create-chat-model-preference
CREATE TABLE IF NOT EXISTS chat_model_preference
(
    chat_id BIGINT       NOT NULL PRIMARY KEY,
    model   VARCHAR(100) NOT NULL
);
