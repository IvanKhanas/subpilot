#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username postgres <<-EOSQL
    SELECT 'CREATE DATABASE subscription' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'subscription')\gexec
    SELECT 'CREATE DATABASE payment' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment')\gexec

    SELECT 'CREATE USER subscription WITH PASSWORD ''${SUBSCRIPTION_DB_PASSWORD}''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'subscription')\gexec
    GRANT ALL PRIVILEGES ON DATABASE subscription TO subscription;
EOSQL

psql -v ON_ERROR_STOP=1 --username postgres --dbname subscription <<-EOSQL
    GRANT ALL ON SCHEMA public TO subscription;
EOSQL
