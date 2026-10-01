-- RF-055 (arqueo: efectivo vs electronico vs otros) necesita saber a que categoria pertenece
-- cada medio de pago; sin esto habria que adivinar por el codigo del medio (fragil, y viola
-- BR-018 que exige que el catalogo sea parametrizable). Migracion nueva (no se edita V2 ya
-- aplicada) siguiendo la disciplina de Flyway.

ALTER TABLE payment_method ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'OTHER'
    CHECK (category IN ('CASH', 'ELECTRONIC', 'OTHER'));

UPDATE payment_method SET category = 'CASH' WHERE code = 'CASH';
UPDATE payment_method SET category = 'ELECTRONIC' WHERE code = 'TRANSFER';
