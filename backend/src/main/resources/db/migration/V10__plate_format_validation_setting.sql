-- BR-036: el administrador decide si se valida el formato de placa por tipo de vehiculo.
-- Activada por defecto para conservar el comportamiento actual.
ALTER TABLE configuration ADD COLUMN plate_format_validation BOOLEAN NOT NULL DEFAULT TRUE;
