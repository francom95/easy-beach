-- Etapa 03 §3.3/§3.4. Catálogo (menú) y ubicaciones físicas del balneario.

CREATE TABLE ubicacion (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    tipo              VARCHAR(16) NOT NULL,
    identificador     VARCHAR(40) NOT NULL,
    estado            VARCHAR(16) NOT NULL DEFAULT 'ACTIVA',
    deleted_at        DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT ck_ubicacion_tipo CHECK (tipo IN ('CARPA', 'SOMBRILLA', 'MESA', 'SECTOR')),
    CONSTRAINT ck_ubicacion_estado CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT fk_ubicacion_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- UK solo entre no borradas: MySQL no soporta UNIQUE parcial nativo, se
-- aproxima con una columna generada que colapsa a un valor constante cuando
-- está borrada (así el UK real no choca entre bajas e identificadores reusados).
ALTER TABLE ubicacion
    ADD COLUMN identificador_uk VARCHAR(40) AS (IF(deleted_at IS NULL, identificador, NULL)) STORED,
    ADD CONSTRAINT uk_ubicacion_balneario_identificador UNIQUE (balneario_id, identificador_uk);

CREATE INDEX idx_ubicacion_balneario_estado ON ubicacion (balneario_id, estado);

CREATE TABLE categoria_menu (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    nombre            VARCHAR(80) NOT NULL,
    orden             INT NOT NULL DEFAULT 0,
    activa            TINYINT(1) NOT NULL DEFAULT 1,
    deleted_at        DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_categoria_menu_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_categoria_menu_balneario_activa_orden ON categoria_menu (balneario_id, activa, orden);

CREATE TABLE producto (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    categoria_id      BIGINT UNSIGNED NOT NULL,
    nombre            VARCHAR(120) NOT NULL,
    descripcion       VARCHAR(500) NULL,
    precio_base       DECIMAL(12,2) NOT NULL,
    foto_url          VARCHAR(500) NULL,
    disponible        TINYINT(1) NOT NULL DEFAULT 1,
    orden             INT NOT NULL DEFAULT 0,
    deleted_at        DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_producto_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria_menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_producto_balneario_categoria_disponible_orden
    ON producto (balneario_id, categoria_id, disponible, orden);

CREATE TABLE producto_variante (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    producto_id       BIGINT UNSIGNED NOT NULL,
    nombre            VARCHAR(80) NOT NULL,
    precio            DECIMAL(12,2) NOT NULL,
    disponible        TINYINT(1) NOT NULL DEFAULT 1,
    orden             INT NOT NULL DEFAULT 0,
    deleted_at        DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_producto_variante_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_producto_variante_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_producto_variante_producto_disponible ON producto_variante (producto_id, disponible);
