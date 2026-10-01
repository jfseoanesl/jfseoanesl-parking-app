# ADR-007 — Arranque de una instalación de producción: datos de referencia y primer administrador

**Estado:** Aceptado (2026-09-29). Resuelve la decisión pendiente (a) de ADR-006. Opción elegida por el usuario.

## Contexto

La Fase 12 (ADR-006) mostró que un MySQL de producción recién creado quedaba **inutilizable**: 0 roles, 0 usuarios, 0 tipos de vehículo, 0 medios de pago y sin configuración. Nadie podía iniciar sesión. La causa es que `db/seed/V2` mezcla dos cosas:

- **Datos de prueba**, que nunca deben llegar a producción (`prompt.md` §33): los usuarios `admin/admin123` y `empleado/empleado123`, y tarifas de ejemplo.
- **Datos de referencia**, sin los que la aplicación no funciona: roles, tipos de vehículo, medios de pago (BR-018) y la fila de configuración.

## Decisión

1. **Datos de referencia en una migración común, `db/migration/V4__reference_data.sql`**, que corre en todos los perfiles (también en `production`). Contiene los roles ADMIN y EMPLOYEE, los tipos de vehículo Automóvil y Motocicleta, los medios de pago Efectivo, Transferencia y Otros con su categoría, y una configuración inicial genérica ("Mi parqueadero", America/Bogota, COP, sin capacidad) que el administrador completa después.
   - **Idempotente y portable:** usa `INSERT … SELECT` desde una tabla derivada con `WHERE NOT EXISTS` por código. En las bases que ya tienen la semilla V2 (desarrollo y pruebas) no inserta nada. Verificado en SQLite y MySQL.
   - **No incluye usuarios ni tarifas:** las tarifas son precios del negocio y las define el administrador.
   - `V2` no se modificó, porque su checksum ya está registrado en las bases existentes.
2. **Primer administrador creado al arrancar a partir de variables de entorno**, sin credenciales por defecto. Lo implementan `AdminBootstrapService` (capa de aplicación) y `AdminBootstrapRunner` (disparador de arranque en infraestructura):
   - `PARKING_BOOTSTRAP_ADMIN_USERNAME` y `PARKING_BOOTSTRAP_ADMIN_PASSWORD` (mínimo 8 caracteres, la misma política que el alta de usuarios por API); opcional `PARKING_BOOTSTRAP_ADMIN_FULL_NAME`.
   - Solo actúa si no existe ningún administrador. Una vez creado, reiniciar con las mismas variables no hace nada y el log recomienda retirarlas.
   - Contraseña guardada con BCrypt (BR-004). La creación queda en auditoría como `USER_CREATED` con `"bootstrap": true` y el propio administrador como actor, porque no existe otro usuario (BR-025). La contraseña nunca se escribe en el log.
   - Si falta la configuración y no hay administrador, la aplicación arranca y deja una advertencia con las instrucciones. Si los datos son inválidos (contraseña corta, o nombre de usuario ya tomado por un no-administrador), **el arranque falla** con un mensaje claro, para que el error no pase desapercibido.
   - No se modificó ninguna interfaz de dominio; se usan los repositorios existentes.

## Alternativas descartadas

- **Usuario administrador con contraseña fija en una migración:** viola `prompt.md` §33 (credenciales por defecto en producción).
- **Crear el administrador con SQL a mano:** exige generar un hash BCrypt fuera de la aplicación, es propenso a errores y no queda en auditoría.
- **Pantalla de "primer uso" abierta en la web:** quien llegue primero a la URL se convertiría en administrador; es un riesgo en un despliegue expuesto.

## Verificación (MySQL 8.4, stack `docker-compose.prod.yml`, sin semilla)

- Instalación nueva: Flyway aplica V1, V3, V3.1 y V4; quedan 2 roles, 2 tipos de vehículo, 3 medios de pago, 1 configuración y **un único administrador** creado desde el entorno. Las credenciales de desarrollo no existen (401).
- Primer uso completo por API: el administrador inicia sesión, crea un empleado y una tarifa, y el empleado abre caja, registra un ingreso y cobra (`OP-000001`). La auditoría muestra las tres operaciones.
- Reinicio con las mismas variables: no se crea un segundo administrador y el log pide retirarlas. Sin variables: arranca y deja la advertencia. Contraseña de 5 caracteres: el arranque falla con el mensaje esperado.
- Suite del backend: 62/62 en SQLite y 62/62 en MySQL (incluye 5 pruebas nuevas de `AdminBootstrapServiceTest`).

## Consecuencias

- **Procedimiento de primera instalación:** ver ADR-006 ("Procedimiento") y `README.md`. Tras el primer arranque hay que retirar `PARKING_BOOTSTRAP_ADMIN_PASSWORD` del entorno y cambiar la contraseña desde la aplicación.
- Después del primer inicio de sesión, el administrador debe crear las tarifas y completar la configuración (nombre, NIT, capacidad) antes de operar.
- **Recuperación de acceso fuera de alcance:** el mecanismo solo actúa si no existe *ningún* usuario con rol de administrador, esté activo o inactivo. Si se pierde la contraseña de todos los administradores, no sirve para recuperar el acceso. Se necesitaría un procedimiento de recuperación que hoy no existe.
