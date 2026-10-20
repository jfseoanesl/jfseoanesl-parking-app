-- Exclusiva de MySQL (ver V3_1): la FK "en linea" de parking_session.subscription_id que agrega V7
-- la aplica SQLite pero MySQL la descarta en silencio; aqui se crea como restriccion de tabla.

ALTER TABLE parking_session
    ADD CONSTRAINT fk_parking_session_subscription FOREIGN KEY (subscription_id) REFERENCES subscription (id);
