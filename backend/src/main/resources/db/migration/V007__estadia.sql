-- Etapa 03 §3.5. Estadía activa: el vínculo cliente-balneario-ubicación que
-- persiste hasta cierre explícito (puede durar un día o toda la temporada).

CREATE TABLE estadia (
    id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id                 CHAR(26) NOT NULL,
    balneario_id              BIGINT UNSIGNED NOT NULL,
    cliente_id                BIGINT UNSIGNED NOT NULL,
    ubicacion_id              BIGINT UNSIGNED NOT NULL,
    estado                    VARCHAR(24) NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
    -- = cliente_id mientras la estadía ocupa cupo (PENDIENTE_VALIDACION|ACTIVA);
    -- NULL en estados terminales. Ver UK más abajo.
    activa_uk                 BIGINT UNSIGNED NULL,
    fecha_solicitud           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    validada_por_usuario_id   BIGINT UNSIGNED NULL,
    fecha_validacion          DATETIME(3) NULL,
    fecha_cierre              DATETIME(3) NULL,
    motivo_rechazo            VARCHAR(300) NULL,
    created_at                DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_estadia_public_id UNIQUE (public_id),
    -- Unicidad "una estadía activa por cliente y por balneario" (etapa 01),
    -- IMPOSIBLE de violar por concurrencia: MySQL trata múltiples NULL como
    -- distintos, así que las estadías terminales (activa_uk=NULL) no colisionan
    -- entre sí, pero solo puede existir UNA fila con activa_uk=cliente_id por
    -- balneario. El mismo cliente SÍ puede tener estadías en balnearios
    -- distintos (el UK incluye balneario_id).
    CONSTRAINT uk_estadia_activa_por_cliente_balneario UNIQUE (balneario_id, activa_uk),
    CONSTRAINT fk_estadia_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_estadia_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id),
    CONSTRAINT fk_estadia_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicacion (id),
    CONSTRAINT fk_estadia_validador FOREIGN KEY (validada_por_usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_estadia_estado CHECK (estado IN
        ('PENDIENTE_VALIDACION', 'ACTIVA', 'RECHAZADA', 'CERRADA', 'CERRADA_POR_SISTEMA', 'EXPIRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cola de validación del carpero (etapa 17) y expiración por TTL (job).
CREATE INDEX idx_estadia_balneario_estado_solicitud ON estadia (balneario_id, estado, fecha_solicitud);
-- "Mis estadías" del cliente, en todos los balnearios.
CREATE INDEX idx_estadia_cliente_estado ON estadia (cliente_id, estado);
-- Estadías por ubicación (regla: no desactivar una ubicación con estadía activa).
CREATE INDEX idx_estadia_ubicacion_estado ON estadia (ubicacion_id, estado);

CREATE TABLE estadia_ubicacion_historial (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    estadia_id        BIGINT UNSIGNED NOT NULL,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    ubicacion_id      BIGINT UNSIGNED NOT NULL,
    desde             DATETIME(3) NOT NULL,
    hasta             DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_estadia_hist_estadia FOREIGN KEY (estadia_id) REFERENCES estadia (id) ON DELETE CASCADE,
    CONSTRAINT fk_estadia_hist_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_estadia_hist_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicacion (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_estadia_hist_estadia_desde ON estadia_ubicacion_historial (estadia_id, desde);
