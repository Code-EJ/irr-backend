CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Create Usuario table
CREATE TABLE usuario
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email         VARCHAR(191) NOT NULL UNIQUE,
    senha         VARCHAR(191) NOT NULL,
    nome          VARCHAR(191) NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    criador_id    UUID         NOT NULL,
    criado_em     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    atualizado_em TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT usuario_tipo_check CHECK (
        tipo IN ('ADMINISTRADOR', 'PREFEITURA', 'ORGANIZACAO', 'REPRESENTANTE')
        ),
    CONSTRAINT usuario_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 2. Create Media table
CREATE TABLE media
(
    id    SERIAL PRIMARY KEY,
    dados BYTEA NOT NULL
);
