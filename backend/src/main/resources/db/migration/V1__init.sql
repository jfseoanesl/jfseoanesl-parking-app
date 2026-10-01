-- Esquema inicial (07-data-model.md). Sintaxis portable entre SQLite (desarrollo/test) y
-- MySQL (produccion futura, decisions/ADR-002-persistencia-sqlite.md): sin AUTOINCREMENT,
-- sin ROWID ni ENUM nativos; enums como VARCHAR + CHECK; IDs como CHAR(36) (UUID).

CREATE TABLE role (
    id   CHAR(36)    NOT NULL PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE app_user (
    id            CHAR(36)     NOT NULL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role_id       CHAR(36)     NOT NULL REFERENCES role (id),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);

CREATE TABLE vehicle_type (
    id     CHAR(36)     NOT NULL PRIMARY KEY,
    code   VARCHAR(30)  NOT NULL,
    name   VARCHAR(100) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_vehicle_type_code UNIQUE (code)
);

CREATE TABLE tariff (
    id                CHAR(36)      NOT NULL PRIMARY KEY,
    name              VARCHAR(150)  NOT NULL,
    description       VARCHAR(255),
    vehicle_type_id   CHAR(36)      NOT NULL REFERENCES vehicle_type (id),
    modality          VARCHAR(20)   NOT NULL CHECK (modality IN ('FULL_AM', 'FULL_PM', 'FULL_DAY', 'BY_TIME')),
    value             DECIMAL(12,2) NOT NULL,
    start_time        TIME,
    end_time          TIME,
    tolerance_minutes INT,
    min_time_minutes  INT,
    time_unit         VARCHAR(10) CHECK (time_unit IN ('MINUTES', 'HOURS')),
    fraction_minutes  INT,
    rounding          VARCHAR(10)   NOT NULL DEFAULT 'UP' CHECK (rounding IN ('UP', 'DOWN', 'NEAREST')),
    priority          INT           NOT NULL,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    valid_from        DATE          NOT NULL,
    valid_to          DATE,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    created_by        CHAR(36) REFERENCES app_user (id),
    updated_by        CHAR(36) REFERENCES app_user (id)
);

CREATE INDEX ix_tariff_lookup ON tariff (vehicle_type_id, active, modality);

CREATE TABLE cash_session (
    id                   CHAR(36)      NOT NULL PRIMARY KEY,
    employee_id          CHAR(36)      NOT NULL REFERENCES app_user (id),
    status               VARCHAR(10)   NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_at            TIMESTAMP     NOT NULL,
    initial_balance      DECIMAL(12,2) NOT NULL,
    closed_at            TIMESTAMP,
    cash_collected       DECIMAL(12,2),
    electronic_collected DECIMAL(12,2),
    other_collected      DECIMAL(12,2),
    total_collected      DECIMAL(12,2),
    vehicles_served      INT,
    expected_cash        DECIMAL(12,2),
    reported_cash        DECIMAL(12,2),
    difference           DECIMAL(12,2),
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL,
    -- BR-022: unica caja abierta por empleado. Columna generada + indice unico portable
    -- (SQLite y MySQL 8 soportan ambos "GENERATED ALWAYS AS (...) VIRTUAL").
    employee_open        CHAR(36) GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN employee_id ELSE NULL END) VIRTUAL
);

CREATE UNIQUE INDEX ux_cash_session_employee_open ON cash_session (employee_open);

CREATE TABLE payment_method (
    id     CHAR(36)     NOT NULL PRIMARY KEY,
    code   VARCHAR(30)  NOT NULL,
    name   VARCHAR(100) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_payment_method_code UNIQUE (code)
);

CREATE TABLE parking_session (
    id                 CHAR(36)      NOT NULL PRIMARY KEY,
    plate              VARCHAR(20)   NOT NULL,
    vehicle_type_id    CHAR(36)      NOT NULL REFERENCES vehicle_type (id),
    entry_datetime     TIMESTAMP     NOT NULL,
    exit_datetime      TIMESTAMP,
    status             VARCHAR(20)   NOT NULL CHECK (status IN ('EN_PARQUEADERO', 'FINALIZADO', 'ANULADO')),
    entry_employee_id  CHAR(36)      NOT NULL REFERENCES app_user (id),
    exit_employee_id   CHAR(36) REFERENCES app_user (id),
    applied_tariff_id  CHAR(36) REFERENCES tariff (id),
    calculated_amount  DECIMAL(12,2),
    notes              VARCHAR(255),
    cancel_reason      VARCHAR(255),
    cancelled_by       CHAR(36) REFERENCES app_user (id),
    cancelled_at       TIMESTAMP,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    -- BR-001: unica sesion activa por placa.
    plate_active       VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status = 'EN_PARQUEADERO' THEN plate ELSE NULL END) VIRTUAL
);

CREATE UNIQUE INDEX ux_parking_session_plate_active ON parking_session (plate_active);
CREATE INDEX ix_parking_session_plate ON parking_session (plate);

CREATE TABLE payment (
    id                 CHAR(36)      NOT NULL PRIMARY KEY,
    parking_session_id CHAR(36)      NOT NULL REFERENCES parking_session (id),
    cash_session_id    CHAR(36)      NOT NULL REFERENCES cash_session (id),
    calculated_amount  DECIMAL(12,2) NOT NULL,
    charged_amount     DECIMAL(12,2) NOT NULL,
    payment_method_id  CHAR(36)      NOT NULL REFERENCES payment_method (id),
    operation_number   VARCHAR(30)   NOT NULL,
    user_id            CHAR(36)      NOT NULL REFERENCES app_user (id),
    paid_at            TIMESTAMP     NOT NULL,
    created_at         TIMESTAMP     NOT NULL,
    -- BR-021: idempotencia de cobro (a lo sumo un pago por sesion).
    CONSTRAINT uk_payment_parking_session UNIQUE (parking_session_id),
    CONSTRAINT uk_payment_operation_number UNIQUE (operation_number)
);

CREATE TABLE cash_movement (
    id              CHAR(36)      NOT NULL PRIMARY KEY,
    cash_session_id CHAR(36)      NOT NULL REFERENCES cash_session (id),
    payment_id      CHAR(36) REFERENCES payment (id),
    type            VARCHAR(20)   NOT NULL CHECK (type IN ('PAYMENT', 'ADJUSTMENT')),
    amount          DECIMAL(12,2) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP     NOT NULL
);

CREATE TABLE configuration (
    id                  CHAR(36)     NOT NULL PRIMARY KEY,
    parking_name        VARCHAR(150) NOT NULL,
    nit                 VARCHAR(30),
    address             VARCHAR(255),
    phone               VARCHAR(30),
    max_capacity        INT,
    timezone            VARCHAR(60)  NOT NULL DEFAULT 'America/Bogota',
    currency            VARCHAR(10)  NOT NULL DEFAULT 'COP',
    receipt_format_json TEXT,
    updated_at          TIMESTAMP    NOT NULL,
    updated_by          CHAR(36) REFERENCES app_user (id)
);

CREATE TABLE audit_log (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    user_id     CHAR(36)     NOT NULL REFERENCES app_user (id),
    action      VARCHAR(100) NOT NULL,
    entity_name VARCHAR(60)  NOT NULL,
    entity_id   CHAR(36)     NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    occurred_at TIMESTAMP    NOT NULL
);

CREATE INDEX ix_audit_log_entity ON audit_log (entity_name, entity_id);
CREATE INDEX ix_audit_log_occurred_at ON audit_log (occurred_at);
