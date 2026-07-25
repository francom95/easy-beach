-- Etapa 05 §1.1: "cambio obligatorio en primer login" para staff creado con
-- password temporal (etapa 10: alta de balneario crea su primer admin).
ALTER TABLE usuario
    ADD COLUMN debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 0 AFTER estado;

-- Etapa 05 §5: password con argon2id (etapa 09 usaba bcrypt por default; se
-- corrige acá). El hash encodeado de argon2id es más largo que el de bcrypt
-- (~95-100+ chars según parámetros) - VARCHAR(100) quedaba muy justo.
ALTER TABLE usuario
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;
