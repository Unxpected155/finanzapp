#!/bin/sh
# Se ejecuta UNA sola vez, cuando el volumen de datos está vacío (primer arranque).
# Crea los roles con privilegio mínimo y el esquema. Las contraseñas llegan como
# archivos en /run/secrets/ (secretos de Docker), nunca como variables de entorno.
set -eu

OWNER_PW="$(cat /run/secrets/db_owner_password)"
APP_PW="$(cat /run/secrets/db_app_password)"

# Las contraseñas viajan como variables de psql (:'var'), que psql escapa como literales.
psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v owner_pw="$OWNER_PW" -v app_pw="$APP_PW" -v db="$POSTGRES_DB" <<'SQL'
-- Dueño del esquema: lo usa SOLO Flyway. No es superusuario.
CREATE ROLE finanzapp_owner LOGIN PASSWORD :'owner_pw' NOSUPERUSER NOCREATEDB NOCREATEROLE;

-- Rol de la aplicación en runtime: no es dueño de nada, así que no puede hacer DDL
-- y no puede saltarse RLS. Sus permisos se dan tabla por tabla en V2__app_grants.sql.
CREATE ROLE finanzapp_app LOGIN PASSWORD :'app_pw' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;

-- Nadie más puede conectarse ni usar el esquema public.
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO finanzapp_owner, finanzapp_app;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- Esquema propio, del owner. La app solo puede "ver" que existe (USAGE), no crear objetos.
CREATE SCHEMA finanzapp AUTHORIZATION finanzapp_owner;
GRANT USAGE ON SCHEMA finanzapp TO finanzapp_app;

ALTER ROLE finanzapp_owner SET search_path = finanzapp;
ALTER ROLE finanzapp_app SET search_path = finanzapp;
SQL
