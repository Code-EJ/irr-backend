CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 7. Create Doador table
CREATE TABLE IF NOT EXISTS doador
(
    id                SERIAL PRIMARY KEY,
    nome              VARCHAR(191) NOT NULL,
    endereco          VARCHAR(191) NOT NULL,
    tipo              VARCHAR(30)  NOT NULL,
    cadastro_nacional VARCHAR(191) NOT NULL UNIQUE,

    CONSTRAINT documento_tipo_check CHECK ( tipo IN ('PESSOA_FISICA', 'PESSOA_JURIDICA') )
);