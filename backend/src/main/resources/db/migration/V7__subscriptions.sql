-- RF-074 a RF-077 / BR-028 a BR-031: mensualidades (clientes con pago mensual y placas autorizadas).
-- Portable SQLite/MySQL: las tablas nuevas declaran sus FK como restricciones de tabla (MySQL descarta
-- las FK "en linea", ver V3_1) y sus fechas como DATETIME(6) (en SQLite tiene la misma afinidad que
-- TIMESTAMP). La FK de la columna nueva de parking_session se agrega en MySQL con V7_1.

CREATE TABLE subscription (
    id            CHAR(36)      NOT NULL PRIMARY KEY,
    customer_name VARCHAR(150)  NOT NULL,
    document_id   VARCHAR(30),
    phone         VARCHAR(30),
    monthly_fee   DECIMAL(12,2) NOT NULL,
    cut_off_day   INT           NOT NULL CHECK (cut_off_day BETWEEN 1 AND 28),
    paid_until    DATE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    notes         VARCHAR(255),
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    created_by    CHAR(36),
    CONSTRAINT fk_subscription_created_by FOREIGN KEY (created_by) REFERENCES app_user (id)
);

CREATE TABLE subscription_vehicle (
    id              CHAR(36)    NOT NULL PRIMARY KEY,
    subscription_id CHAR(36)    NOT NULL,
    plate           VARCHAR(20) NOT NULL,
    vehicle_type_id CHAR(36)    NOT NULL,
    -- Una placa pertenece a una sola mensualidad (BR-029: el ingreso la reconoce sin ambigüedad).
    CONSTRAINT uk_subscription_vehicle_plate UNIQUE (plate),
    CONSTRAINT fk_subscription_vehicle_subscription FOREIGN KEY (subscription_id) REFERENCES subscription (id),
    CONSTRAINT fk_subscription_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (id)
);

CREATE TABLE subscription_payment (
    id                CHAR(36)      NOT NULL PRIMARY KEY,
    subscription_id   CHAR(36)      NOT NULL,
    cash_session_id   CHAR(36)      NOT NULL,
    payment_method_id CHAR(36)      NOT NULL,
    user_id           CHAR(36)      NOT NULL,
    amount            DECIMAL(12,2) NOT NULL,
    period_from       DATE          NOT NULL,
    period_to         DATE          NOT NULL,
    -- Mismo consecutivo OP- que los cobros de salida (contador PAYMENT de V5): nunca se repiten.
    operation_number  VARCHAR(30)   NOT NULL,
    paid_at           DATETIME(6)   NOT NULL,
    CONSTRAINT uk_subscription_payment_operation_number UNIQUE (operation_number),
    CONSTRAINT fk_subscription_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscription (id),
    CONSTRAINT fk_subscription_payment_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    CONSTRAINT fk_subscription_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method (id),
    CONSTRAINT fk_subscription_payment_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX ix_subscription_payment_cash_session ON subscription_payment (cash_session_id);
CREATE INDEX ix_subscription_payment_paid_at ON subscription_payment (paid_at);

ALTER TABLE parking_session ADD COLUMN subscription_id CHAR(36) REFERENCES subscription (id);

CREATE INDEX ix_parking_session_subscription ON parking_session (subscription_id, status);

ALTER TABLE configuration ADD COLUMN subscription_grace_days INT NOT NULL DEFAULT 3;
