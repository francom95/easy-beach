-- Etapa 03 (docs/especificacion/03-modelo-de-datos.md) §3.1
-- Tenancy / plataforma. `balneario` es el tenant; NO es tenant-scoped.

CREATE TABLE balneario (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    slug              VARCHAR(60)  NOT NULL,
    nombre            VARCHAR(120) NOT NULL,
    email_contacto    VARCHAR(160) NOT NULL,
    telefono          VARCHAR(40)  NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_balneario_slug UNIQUE (slug),
    CONSTRAINT ck_balneario_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE configuracion_visual (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    theme_version     INT NOT NULL DEFAULT 1,
    tokens            JSON NOT NULL,
    logo_url          VARCHAR(500) NULL,
    portada_url       VARCHAR(500) NULL,
    splash_url        VARCHAR(500) NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_configuracion_visual_balneario UNIQUE (balneario_id),
    CONSTRAINT fk_configuracion_visual_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE plan (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(80)  NOT NULL,
    descripcion       VARCHAR(400) NULL,
    precio            DECIMAL(12,2) NOT NULL,
    activo            TINYINT(1)   NOT NULL DEFAULT 1,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE temporada (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(60) NOT NULL,
    fecha_inicio      DATE NOT NULL,
    fecha_fin         DATE NOT NULL,
    estado            VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADA',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT ck_temporada_estado CHECK (estado IN ('PLANIFICADA', 'EN_CURSO', 'CERRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE suscripcion_temporada (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    plan_id           BIGINT UNSIGNED NOT NULL,
    temporada_id      BIGINT UNSIGNED NOT NULL,
    estado            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_alta        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_suscripcion_balneario_temporada UNIQUE (balneario_id, temporada_id),
    CONSTRAINT fk_suscripcion_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_suscripcion_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT fk_suscripcion_temporada FOREIGN KEY (temporada_id) REFERENCES temporada (id),
    CONSTRAINT ck_suscripcion_estado CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'SUSPENDIDA', 'FINALIZADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_suscripcion_balneario_estado ON suscripcion_temporada (balneario_id, estado);
