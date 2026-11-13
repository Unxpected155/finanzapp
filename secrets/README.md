# secrets/

Secretos de **desarrollo local**. Todo lo que hay en esta carpeta está en `.gitignore`, salvo este README.

Genéralos con:

```powershell
./scripts/New-DevSecrets.ps1
```

| Archivo | Lo usa |
|---|---|
| `db_superuser_password` | El superusuario `postgres` (solo administración manual) |
| `db_owner_password` | `finanzapp_owner`: Flyway (migraciones) |
| `db_app_password` | `finanzapp_app`: la aplicación en runtime |

- Un archivo por secreto, sin salto de línea al final.
- Docker Compose los monta en `/run/secrets/`; Spring Boot los lee con `configtree`.
- Si los cambias después del primer arranque, Postgres **no** actualiza los roles (los scripts de `initdb` corren una sola vez). Borra el volumen para recrearlos: `docker compose down -v`. Esto borra los datos locales.
