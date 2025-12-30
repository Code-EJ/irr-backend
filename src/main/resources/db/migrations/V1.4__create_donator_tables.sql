CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 7. Create doador table
CREATE TABLE IF NOT EXISTS doador
(
    id                SERIAL PRIMARY KEY,
    nome              VARCHAR(191) NOT NULL,
    endereco          VARCHAR(191) NOT NULL,
    tipo              VARCHAR(30)  NOT NULL,
    cadastro_nacional VARCHAR(191) NOT NULL UNIQUE,
    criador_id           UUID             NOT NULL,
    data_criacao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_atualizacao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT doador_tipo_check
        CHECK ( tipo IN ('PESSOA_FISICA', 'PESSOA_JURIDICA') ),

    CONSTRAINT doador_criador_id_fk
    FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);