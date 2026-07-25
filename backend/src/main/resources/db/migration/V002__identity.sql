-- Etapa 03 §3.2 (usuario, rol, usuario_balneario_rol) + `sesion_refresh`,
-- infraestructura de autenticación de la etapa 09 / etapa 05 §1.1 (refresh
-- token opaco, persistido con hash, revocable) que no estaba anticipada como
-- tabla propia en el plan de migraciones original de la etapa 03.

CREATE TABLE usuario (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id         CHAR(26) NOT NULL,
    email             VARCHAR(160) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    nombre            VARCHAR(120) NOT NULL,
    tipo              VARCHAR(20)  NOT NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_usuario_public_id UNIQUE (public_id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_tipo CHECK (tipo IN ('CLIENTE', 'STAFF', 'SUPER_ADMIN')),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'BAJA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rol (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    codigo            VARCHAR(30) NOT NULL,
    nombre            VARCHAR(80) NOT NULL,
    CONSTRAINT uk_rol_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO rol (codigo, nombre) VALUES
    ('CLIENTE', 'Cliente'),
    ('CARPERO', 'Carpero'),
    ('OPERADOR', 'Operador de barra/cocina'),
    ('ADMIN_BALNEARIO', 'Admin de balneario'),
    ('SUPER_ADMIN', 'Super Admin');

CREATE TABLE usuario_balneario_rol (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id        BIGINT UNSIGNED NOT NULL,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    rol_id            BIGINT UNSIGNED NOT NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_usuario_balneario_rol UNIQUE (usuario_id, balneario_id, rol_id),
    CONSTRAINT fk_ubr_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_ubr_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_ubr_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_ubr_balneario ON usuario_balneario_rol (balneario_id);

-- Sesiones de refresh token (etapa 05 §1.1): opaco, hash persistido,
-- agrupado por familia para poder revocar toda la cadena ante reuso.
CREATE TABLE sesion_refresh (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id        BIGINT UNSIGNED NOT NULL,
    familia_id        CHAR(26) NOT NULL,
    token_hash        CHAR(64) NOT NULL,
    estado            VARCHAR(16) NOT NULL DEFAULT 'ACTIVA',
    expira_at         DATETIME(3) NOT NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_sesion_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_sesion_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_sesion_refresh_estado CHECK (estado IN ('ACTIVA', 'ROTADA', 'REVOCADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_sesion_refresh_familia ON sesion_refresh (familia_id);
CREATE INDEX idx_sesion_refresh_usuario_estado ON sesion_refresh (usuario_id, estado);
