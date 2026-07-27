-- Etapa 03 §3.8/§3.9. Servicios al carpero (ciclo simple, sin cobro en MVP)
-- y promociones básicas (descuento %, combo, happy hour).

CREATE TABLE tipo_servicio (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id  BIGINT UNSIGNED NOT NULL,
    nombre        VARCHAR(80) NOT NULL,
    activo        TINYINT(1) NOT NULL DEFAULT 1,
    orden         INT NOT NULL DEFAULT 0,
    deleted_at    DATETIME(3) NULL,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    -- Mismo truco de la etapa 11 para unicidad entre no borrados: MySQL no
    -- soporta UNIQUE parcial nativo.
    nombre_uk     VARCHAR(80) AS (IF(deleted_at IS NULL, nombre, NULL)) STORED,
    CONSTRAINT uk_tipo_servicio_nombre UNIQUE (balneario_id, nombre_uk),
    CONSTRAINT fk_tipo_servicio_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_tipo_servicio_balneario_activo_orden ON tipo_servicio (balneario_id, activo, orden);

CREATE TABLE solicitud_servicio (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id               CHAR(26) NOT NULL,
    balneario_id            BIGINT UNSIGNED NOT NULL,
    estadia_id              BIGINT UNSIGNED NOT NULL,
    -- Denormalizado (mismo patrón que pedido.cliente_public_id, etapa 13):
    -- es la clave del canal SSE del cliente sin que concierge dependa de identity.
    cliente_public_id       CHAR(26) NOT NULL,
    ubicacion_id            BIGINT UNSIGNED NOT NULL,
    tipo_servicio_id        BIGINT UNSIGNED NOT NULL,
    nota                    VARCHAR(300) NULL,
    estado                  VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE',
    atendida_por_usuario_id BIGINT UNSIGNED NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_solicitud_servicio_public_id UNIQUE (public_id),
    CONSTRAINT fk_solicitud_servicio_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_solicitud_servicio_estadia FOREIGN KEY (estadia_id) REFERENCES estadia (id),
    CONSTRAINT fk_solicitud_servicio_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicacion (id),
    CONSTRAINT fk_solicitud_servicio_tipo FOREIGN KEY (tipo_servicio_id) REFERENCES tipo_servicio (id),
    CONSTRAINT fk_solicitud_servicio_atendida_por FOREIGN KEY (atendida_por_usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_solicitud_servicio_estado CHECK (estado IN ('PENDIENTE', 'EN_CURSO', 'RESUELTA', 'CANCELADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cola del carpero: solo las activas, por antigüedad.
CREATE INDEX idx_solicitud_servicio_balneario_estado_creado ON solicitud_servicio (balneario_id, estado, created_at);
CREATE INDEX idx_solicitud_servicio_estadia ON solicitud_servicio (estadia_id, created_at);

CREATE TABLE promocion (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id        BIGINT UNSIGNED NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    tipo                VARCHAR(24) NOT NULL,
    estado              VARCHAR(16) NOT NULL DEFAULT 'ACTIVA',
    valor               DECIMAL(12,2) NOT NULL,
    vigencia_desde      DATE NULL,
    vigencia_hasta      DATE NULL,
    franja_hora_desde   TIME NULL,
    franja_hora_hasta   TIME NULL,
    dias_semana         VARCHAR(20) NULL,
    deleted_at          DATETIME(3) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_promocion_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT ck_promocion_tipo CHECK (tipo IN ('DESCUENTO_PORCENTUAL', 'COMBO', 'HAPPY_HOUR')),
    CONSTRAINT ck_promocion_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Consulta de promociones vigentes: menú público y aplicación en pedido.
CREATE INDEX idx_promocion_balneario_estado ON promocion (balneario_id, estado, deleted_at);

CREATE TABLE promocion_alcance (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    promocion_id    BIGINT UNSIGNED NOT NULL,
    balneario_id    BIGINT UNSIGNED NOT NULL,
    tipo_alcance    VARCHAR(12) NOT NULL,
    referencia_id   BIGINT UNSIGNED NOT NULL,
    CONSTRAINT fk_promocion_alcance_promocion FOREIGN KEY (promocion_id) REFERENCES promocion (id) ON DELETE CASCADE,
    CONSTRAINT fk_promocion_alcance_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT ck_promocion_alcance_tipo CHECK (tipo_alcance IN ('PRODUCTO', 'CATEGORIA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_promocion_alcance_promocion ON promocion_alcance (promocion_id);
-- Resolver "qué promos aplican a este producto/categoría" sin recorrer todas.
CREATE INDEX idx_promocion_alcance_referencia ON promocion_alcance (tipo_alcance, referencia_id);

CREATE TABLE promocion_combo_item (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    promocion_id    BIGINT UNSIGNED NOT NULL,
    balneario_id    BIGINT UNSIGNED NOT NULL,
    producto_id     BIGINT UNSIGNED NOT NULL,
    cantidad        INT UNSIGNED NOT NULL,
    CONSTRAINT fk_promocion_combo_promocion FOREIGN KEY (promocion_id) REFERENCES promocion (id) ON DELETE CASCADE,
    CONSTRAINT fk_promocion_combo_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_promocion_combo_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT ck_promocion_combo_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_promocion_combo_promocion ON promocion_combo_item (promocion_id);
