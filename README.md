# FinanzApp

App web personal (PWA) para manejar presupuesto, gastos, metas y deudas, construida para **aprender seguridad de aplicaciones** haciendo: threat modeling, seguridad de base de datos, supply chain, GitHub Advanced Security y Docker.

> 🚧 En construcción — Fase 0: repositorio y baseline de seguridad.

Stack previsto: Java + Spring Boot, PostgreSQL, React + TypeScript (PWA), Docker.

## Desarrollo local

Requisitos: Java 25, Docker Desktop.

```powershell
./scripts/New-DevSecrets.ps1     # 1. genera ./secrets/ (una sola vez)
docker compose up -d --wait      # 2. Postgres en 127.0.0.1:5432
./backend/mvnw -f backend/pom.xml spring-boot:run   # 3. la app en http://localhost:8080
```

Tests (levantan su propio Postgres con Testcontainers):

```powershell
./backend/mvnw -f backend/pom.xml verify
```

| Rol de Postgres | Para qué | Puede |
|---|---|---|
| `postgres` | Administración manual | Todo |
| `finanzapp_owner` | Flyway (migraciones) | DDL en el esquema `finanzapp` |
| `finanzapp_app` | La app en runtime | Solo DML; `audit_log` solo `INSERT`/`SELECT` |

## Seguridad

¿Encontraste una vulnerabilidad? No abras un issue: sigue [SECURITY.md](SECURITY.md).

## Licencia

[MIT](LICENSE)
