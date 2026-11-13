-- V2: permisos del rol de la aplicación (finanzapp_app). Privilegio mínimo, tabla por tabla.
-- No se usan ALTER DEFAULT PRIVILEGES a propósito: cada tabla nueva debe recibir sus
-- permisos de forma explícita y revisada en su propia migración.

-- Datos maestros: se crean y se editan; no se borran (las categorías se desactivan).
GRANT SELECT, INSERT, UPDATE ON app_user, category, budget TO finanzapp_app;

-- Datos que el usuario sí puede borrar.
GRANT SELECT, INSERT, UPDATE, DELETE ON budget_line, transaction TO finanzapp_app;

-- Auditoría append-only: la app escribe y lee, pero NO puede modificar ni borrar.
-- Así, quien comprometa la app no puede borrar su rastro.
GRANT SELECT, INSERT ON audit_log TO finanzapp_app;
GRANT USAGE ON SEQUENCE audit_log_id_seq TO finanzapp_app;
