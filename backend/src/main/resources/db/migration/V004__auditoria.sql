-- Etapa 03 §3.10. Auditoría de acciones de Super Admin y otras operaciones
-- sensibles (mitigación de repudio, etapa 05 §7 amenaza #12).

CREATE TABLE auditoria_plataforma (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_usuario_id  BIGINT UNSIGNED NOT NULL,
    accion            VARCHAR(60) NOT NULL,
    entidad_tipo      VARCHAR(40) NOT NULL,
    entidad_id        BIGINT UNSIGNED NULL,
    balneario_id      BIGINT UNSIGNED NULL,
    detalle           JSON NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_auditoria_actor FOREIGN KEY (actor_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_auditoria_balneario_fecha ON auditoria_plataforma (balneario_id, created_at);
