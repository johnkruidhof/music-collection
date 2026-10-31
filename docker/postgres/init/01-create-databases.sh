#!/bin/sh
# Wordt alleen uitgevoerd bij de eerste start (lege data-volume).
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
	CREATE USER music WITH PASSWORD '$APP_DB_PASSWORD';
	CREATE DATABASE musiccollection OWNER music;

	CREATE USER keycloak WITH PASSWORD '$KEYCLOAK_DB_PASSWORD';
	CREATE DATABASE keycloak OWNER keycloak;
SQL
