-- Baseline del esquema (reescrita el 2026-09-30, antes del primer despliegue: ninguna base la había aplicado).
-- Desde el primer despliegue esta migración es inmutable; todo cambio va en una V2 nueva.
--
-- Esta migración NO crea usuarios. El primer ADMIN se define aparte (tarea TAR-13); mientras tanto
-- lo crea AdminBootstrap al arrancar desde ADMIN_DOCUMENT_NUMBER / ADMIN_EMAIL / ADMIN_INITIAL_PASSWORD.

CREATE TABLE users (
    id                       UUID PRIMARY KEY,
    email                    VARCHAR(200) NOT NULL UNIQUE,
    document_type            VARCHAR(20) NOT NULL,
    document_number          VARCHAR(20) NOT NULL,
    password_hash            VARCHAR(255) NOT NULL,
    role                     VARCHAR(20) NOT NULL,
    guest_id                 UUID NULL,
    must_change_password     BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts    SMALLINT NOT NULL DEFAULT 0,
    locked_until             TIMESTAMPTZ NULL,
    reset_token_hash         VARCHAR(255) NULL,
    reset_token_expires_at   TIMESTAMPTZ NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date             TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by               VARCHAR(200) NULL,
    last_modified_by         VARCHAR(200) NULL,
    CONSTRAINT uq_users_document UNIQUE (document_type, document_number)
);

COMMENT ON TABLE users IS 'Cuentas de acceso al sistema (ADMIN y huéspedes).';
COMMENT ON COLUMN users.document_type IS 'Tipo de documento con el que se inicia sesión (el login es por documento, RF-08).';
COMMENT ON COLUMN users.role IS 'ADMIN o GUEST.';
COMMENT ON COLUMN users.guest_id IS 'Huésped asociado; NULL para ADMIN.';
COMMENT ON COLUMN users.must_change_password IS 'TRUE hasta que el usuario cambia la contraseña inicial (RF-12).';
COMMENT ON COLUMN users.failed_login_attempts IS 'Intentos fallidos consecutivos. Reservado con locked_until para el bloqueo temporal (aún sin uso en el código).';
COMMENT ON COLUMN users.reset_token_hash IS 'Hash del token de recuperación de contraseña (aún sin uso); el token en claro nunca se guarda.';
COMMENT ON COLUMN users.status IS 'ACTIVE o INACTIVE; una cuenta INACTIVE no puede iniciar sesión.';
COMMENT ON COLUMN users.id IS 'Identificador de la cuenta (UUID generado por la aplicación).';
COMMENT ON COLUMN users.email IS 'Correo de la cuenta; único en todo el sistema.';
COMMENT ON COLUMN users.document_number IS 'Número del documento con el que se inicia sesión; único junto con document_type.';
COMMENT ON COLUMN users.password_hash IS 'Hash bcrypt de la contraseña; la contraseña en claro nunca se guarda.';
COMMENT ON COLUMN users.locked_until IS 'Hasta cuándo está bloqueada la cuenta tras demasiados intentos fallidos (aún sin uso en el código).';
COMMENT ON COLUMN users.reset_token_expires_at IS 'Vencimiento del token de recuperación de contraseña (aún sin uso).';
COMMENT ON COLUMN users.created_date IS 'Fecha de creación del registro (auditoría).';
COMMENT ON COLUMN users.last_modified_date IS 'Fecha de la última modificación del registro (auditoría).';
COMMENT ON COLUMN users.created_by IS 'Quién creó el registro (auditoría).';
COMMENT ON COLUMN users.last_modified_by IS 'Quién modificó el registro por última vez (auditoría).';
