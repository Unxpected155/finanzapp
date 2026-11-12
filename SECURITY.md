# Política de seguridad

## Reportar una vulnerabilidad

**No abras un issue público.** Un issue es visible para todo el mundo antes de que exista una corrección.

Usa el reporte privado de GitHub: pestaña **Security → Report a vulnerability**
(<https://github.com/Unxpected155/finanzapp/security/advisories/new>).

Incluye, si puedes:
- Qué componente está afectado (backend, frontend, base de datos, pipeline, imagen Docker).
- Pasos para reproducirlo o una prueba de concepto.
- El impacto que crees que tiene.

## Qué esperar

Es un proyecto personal mantenido por una sola persona:
- Confirmación de recibido: en un plazo de **7 días**.
- Evaluación inicial y plan: en un plazo de **30 días**.
- Te aviso cuando esté corregido y, si quieres, te doy crédito en el advisory.

## Alcance

- ✅ El código de este repositorio y su pipeline de CI/CD.
- ✅ Las imágenes de contenedor que se publiquen desde este repositorio.
- ❌ La instancia en ejecución: no está expuesta a internet (solo accesible por una red privada), así que no hay nada que escanear. Por favor no lo intentes.
- ❌ Vulnerabilidades en dependencias de terceros sin un impacto demostrable en este proyecto: repórtalas al proyecto original.

## Versiones soportadas

Solo la última versión de la rama `main`.
