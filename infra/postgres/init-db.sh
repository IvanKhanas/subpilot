#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username postgres <<-EOSQL
    SELECT 'CREATE DATABASE subscription' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'subscription')\gexec
    SELECT 'CREATE DATABASE payment' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment')\gexec
    SELECT 'CREATE DATABASE loyalty' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'loyalty')\gexec

    SELECT 'CREATE USER subscription WITH PASSWORD ''${SUBSCRIPTION_DB_PASSWORD}''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'subscription')\gexec
    GRANT ALL PRIVILEGES ON DATABASE subscription TO subscription;

    SELECT 'CREATE USER payment WITH PASSWORD ''${PAYMENT_DB_PASSWORD}''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'payment')\gexec
    GRANT ALL PRIVILEGES ON DATABASE payment TO payment;

    SELECT 'CREATE USER loyalty WITH PASSWORD ''${LOYALTY_DB_PASSWORD}''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'loyalty')\gexec
    GRANT ALL PRIVILEGES ON DATABASE loyalty TO loyalty;
EOSQL

psql -v ON_ERROR_STOP=1 --username postgres --dbname subscription <<-EOSQL
    GRANT ALL ON SCHEMA public TO subscription;
EOSQL

psql -v ON_ERROR_STOP=1 --username postgres --dbname payment <<-EOSQL
    GRANT ALL ON SCHEMA public TO payment;
EOSQL

psql -v ON_ERROR_STOP=1 --username postgres --dbname loyalty <<-EOSQL
    GRANT ALL ON SCHEMA public TO loyalty;
EOSQL
