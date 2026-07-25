-- Etapa 03 §3.7 (adelantada a la etapa 10: la vinculación OAuth ocurre en el
-- onboarding del balneario, aunque el módulo `payments` que la CONSUME recién
-- se construye en la etapa 13). ADR-004 / etapa 05 §4.2: tokens cifrados en
-- reposo, columnas VARBINARY, nunca en claro.

CREATE TABLE balneario_mp_credencial (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id            BIGINT UNSIGNED NOT NULL,
    mp_user_id              VARCHAR(40) NOT NULL,
    access_token_cifrado    VARBINARY(1024) NOT NULL,
    refresh_token_cifrado   VARBINARY(1024) NOT NULL,
    token_expira_at         DATETIME(3) NOT NULL,
    scope                   VARCHAR(120) NULL,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'VINCULADA',
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_balneario_mp_credencial_balneario UNIQUE (balneario_id),
    CONSTRAINT fk_balneario_mp_credencial_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id),
    CONSTRAINT ck_balneario_mp_credencial_estado CHECK (estado IN ('VINCULADA', 'DESVINCULADA', 'EXPIRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Estado transitorio de la solicitud OAuth (anti-CSRF): el `state` se genera
-- al iniciar el flujo y se valida al volver del callback de Mercado Pago.
CREATE TABLE mp_oauth_solicitud (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    balneario_id      BIGINT UNSIGNED NOT NULL,
    state             CHAR(43) NOT NULL,
    usado             TINYINT(1) NOT NULL DEFAULT 0,
    expira_at         DATETIME(3) NOT NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_mp_oauth_solicitud_state UNIQUE (state),
    CONSTRAINT fk_mp_oauth_solicitud_balneario FOREIGN KEY (balneario_id) REFERENCES balneario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
