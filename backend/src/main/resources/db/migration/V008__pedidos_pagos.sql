-- Etapa 03 §3.6/§3.7. Núcleo transaccional: pedido, ítems congelados,
-- historial de estados, pagos vía Mercado Pago y auditoría del webhook.

CREATE TABLE pedido (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id         CHAR(26) NOT NULL,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    estadia_id        BIGINT UNSIGNED NOT NULL,
    cliente_id        BIGINT UNSIGNED NOT NULL,
    -- Denormalizado igual que cliente_id (etapa 03 §3.6): es la clave del
    -- canal SSE del cliente. Evita que `ordering` dependa de `identity` solo
    -- para traducir id numérico -> ULID (ADR-002 no permite esa flecha).
    cliente_public_id CHAR(26) NOT NULL,
    ubicacion_id      BIGINT UNSIGNED NOT NULL,
    estado            VARCHAR(20) NOT NULL DEFAULT 'CREADO',
    idempotency_key   VARCHAR(80) NOT NULL,
    subtotal          DECIMAL(12,2) NOT NULL,
    descuento_total   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total             DECIMAL(12,2) NOT NULL,
    motivo_cancelacion VARCHAR(300) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_pedido_public_id UNIQUE (public_id),
    -- Idempotencia (etapa 04 §1.6): el reintento por mala señal en la playa
    -- NO duplica ni el pedido ni el cobro.
    CONSTRAINT uk_pedido_idempotency UNIQUE (balneario_id, idempotency_key),
    CONSTRAINT fk_pedido_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_pedido_estadia FOREIGN KEY (estadia_id) REFERENCES estadia (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id),
    CONSTRAINT fk_pedido_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicacion (id),
    CONSTRAINT ck_pedido_estado CHECK (estado IN
        ('CREADO', 'PAGO_PENDIENTE', 'PAGO_RECHAZADO', 'CONFIRMADO',
         'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cola operativa: pedidos activos por balneario, más antiguos primero.
CREATE INDEX idx_pedido_balneario_estado_creado ON pedido (balneario_id, estado, created_at);
-- Pedidos de una estadía (resumen de cierre, historial del cliente).
CREATE INDEX idx_pedido_estadia ON pedido (estadia_id, created_at);
CREATE INDEX idx_pedido_cliente ON pedido (cliente_id, created_at);

-- Snapshot inmutable: nombres y precios CONGELADOS al momento del pedido.
-- Las FK a producto/variante son referencias blandas (nullable): si el
-- catálogo cambia o se borra, el histórico sigue siendo legible.
CREATE TABLE pedido_item (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pedido_id             BIGINT UNSIGNED NOT NULL,
    balneario_id          BIGINT UNSIGNED NOT NULL,
    producto_id           BIGINT UNSIGNED NULL,
    producto_variante_id  BIGINT UNSIGNED NULL,
    nombre_producto       VARCHAR(120) NOT NULL,
    nombre_variante       VARCHAR(80) NULL,
    precio_unitario       DECIMAL(12,2) NOT NULL,
    cantidad              INT UNSIGNED NOT NULL,
    subtotal_linea        DECIMAL(12,2) NOT NULL,
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_pedido_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_pedido_item_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_pedido_item_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_pedido_item_variante FOREIGN KEY (producto_variante_id) REFERENCES producto_variante (id),
    CONSTRAINT ck_pedido_item_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_pedido_item_pedido ON pedido_item (pedido_id);

-- Auditoría de transiciones: quién movió el pedido, cuándo y por qué.
CREATE TABLE pedido_evento (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pedido_id         BIGINT UNSIGNED NOT NULL,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    estado_anterior   VARCHAR(20) NULL,
    estado_nuevo      VARCHAR(20) NOT NULL,
    actor_usuario_id  BIGINT UNSIGNED NULL,
    actor_tipo        VARCHAR(20) NOT NULL,
    motivo            VARCHAR(300) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_pedido_evento_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_pedido_evento_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT fk_pedido_evento_actor FOREIGN KEY (actor_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_pedido_evento_pedido ON pedido_evento (pedido_id, created_at);

-- 1:N con pedido (reintentos de pago); a lo sumo uno APROBADO.
CREATE TABLE pedido_pago (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pedido_id         BIGINT UNSIGNED NOT NULL,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    estado            VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE',
    monto             DECIMAL(12,2) NOT NULL,
    mp_preference_id  VARCHAR(80) NULL,
    mp_payment_id     VARCHAR(40) NULL,
    mp_status_detail  VARCHAR(80) NULL,
    metodo            VARCHAR(40) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_pedido_pago_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_pedido_pago_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT ck_pedido_pago_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Lookup por webhook de MP (llega con el payment_id, sin contexto de tenant).
CREATE INDEX idx_pedido_pago_mp_payment ON pedido_pago (mp_payment_id);
CREATE INDEX idx_pedido_pago_pedido ON pedido_pago (pedido_id);
-- Job de reconciliación: pagos pendientes con webhook perdido/demorado.
CREATE INDEX idx_pedido_pago_estado_creado ON pedido_pago (estado, created_at);

-- Idempotencia del webhook (ADR-004): una notificación repetida no se
-- re-procesa. El UK incluye el hash del payload para tolerar reenvíos de MP
-- con contenido distinto sobre el mismo pago (ej. cambios de estado).
CREATE TABLE mp_webhook_notificacion (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NULL,
    mp_payment_id     VARCHAR(40) NOT NULL,
    tipo              VARCHAR(40) NOT NULL,
    payload_hash      CHAR(64) NOT NULL,
    recibido_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    procesado         TINYINT(1) NOT NULL DEFAULT 0,
    resultado         VARCHAR(40) NULL,
    CONSTRAINT uk_mp_webhook UNIQUE (mp_payment_id, tipo, payload_hash),
    CONSTRAINT fk_mp_webhook_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Promociones aplicadas a un pedido: el descuento queda CONGELADO acá, así
-- una promo vencida o borrada después no altera pedidos históricos (etapa 14).
CREATE TABLE pedido_promocion (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pedido_id         BIGINT UNSIGNED NOT NULL,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    promocion_id      BIGINT UNSIGNED NULL,
    nombre_promocion  VARCHAR(120) NOT NULL,
    monto_descuento   DECIMAL(12,2) NOT NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_pedido_promocion_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_pedido_promocion_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_pedido_promocion_pedido ON pedido_promocion (pedido_id);
