#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE chat' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'chat')\gexec
    SELECT 'CREATE DATABASE subscription' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'subscription')\gexec
    SELECT 'CREATE DATABASE payment' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment')\gexec
EOSQL
