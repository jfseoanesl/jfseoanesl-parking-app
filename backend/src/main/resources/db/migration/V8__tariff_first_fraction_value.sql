-- BR-011: valor propio para la primera fraccion de una tarifa por tiempo ("primera hora $4.000, cada hora
-- adicional o fraccion $1.000"). Portable SQLite/MySQL. NULL = todas las fracciones valen `value`, asi que
-- las tarifas existentes siguen cobrando igual.

ALTER TABLE tariff ADD COLUMN first_fraction_value DECIMAL(12,2);
