-- V1: esquema inicial. Lo ejecuta Flyway como finanzapp_owner.
-- La BD se defiende sola: cada regla de negocio importante es también un CHECK, UNIQUE o FK,
-- para que un bug en la app no pueda guardar datos inválidos.

CREATE TABLE app_user (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name  text        NOT NULL CHECK (length(display_name) BETWEEN 1 AND 100),
    created_at    timestamptz NOT NULL DEFAULT now(),
    last_login_at timestamptz
);

CREATE TABLE category (
    id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid        NOT NULL REFERENCES app_user (id),
    name           text        NOT NULL CHECK (length(name) BETWEEN 1 AND 50),
    category_group text        NOT NULL CHECK (category_group IN ('INGRESO', 'NECESIDAD', 'DESEO', 'AHORRO')),
    active         boolean     NOT NULL DEFAULT true,
    created_at     timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, name),
    -- Permite FKs compuestas (id, user_id): ver transaction y budget_line.
    UNIQUE (id, user_id)
);

CREATE TABLE budget (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES app_user (id),
    year       smallint    NOT NULL CHECK (year BETWEEN 2000 AND 2100),
    month      smallint    NOT NULL CHECK (month BETWEEN 1 AND 12),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, year, month),
    UNIQUE (id, user_id)
);

CREATE TABLE budget_line (
    id             uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid          NOT NULL,
    budget_id      uuid          NOT NULL,
    category_id    uuid          NOT NULL,
    planned_amount numeric(14,2) NOT NULL CHECK (planned_amount >= 0),
    UNIQUE (budget_id, category_id),
    -- FKs compuestas: la línea, su presupuesto y su categoría son del MISMO usuario.
    -- La BD rechaza mezclar datos de dos usuarios aunque la app se equivoque (threat model 5.1-T).
    FOREIGN KEY (budget_id, user_id)   REFERENCES budget (id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id, user_id) REFERENCES category (id, user_id)
);

CREATE TABLE transaction (
    id             uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid          NOT NULL,
    category_id    uuid          NOT NULL,
    tx_date        date          NOT NULL CHECK (tx_date >= DATE '2000-01-01'),
    description    text          NOT NULL CHECK (length(description) BETWEEN 1 AND 200),
    -- Siempre positivo: el signo lo da el grupo de la categoría (ingreso vs gasto).
    amount         numeric(14,2) NOT NULL CHECK (amount >= 0),
    currency       char(3)       NOT NULL DEFAULT 'CRC' CHECK (currency IN ('CRC', 'USD')),
    payment_method text          CHECK (length(payment_method) <= 30),
    source         text          NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'CSV', 'EMAIL')),
    dedup_hash     text          CHECK (length(dedup_hash) <= 128),
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    UNIQUE (user_id, dedup_hash),
    -- ON DELETE RESTRICT (por defecto): no se borra historial al borrar una categoría.
    FOREIGN KEY (category_id, user_id) REFERENCES category (id, user_id)
);

-- La vista del mes filtra por usuario y rango de fechas.
CREATE INDEX transaction_user_date_idx ON transaction (user_id, tx_date);

-- Registro de eventos de seguridad. Append-only para la app (ver V2).
CREATE TABLE audit_log (
    id          bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- NULL permitido: un login fallido puede no tener usuario.
    user_id     uuid        REFERENCES app_user (id),
    event_type  text        NOT NULL CHECK (length(event_type) BETWEEN 1 AND 50),
    details     jsonb       NOT NULL DEFAULT '{}'::jsonb,
    ip          inet,
    occurred_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_user_time_idx ON audit_log (user_id, occurred_at);
