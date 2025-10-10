CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 4. Create Veiculo table
CREATE TABLE veiculo
(
    id         SERIAL PRIMARY KEY,
    placa      VARCHAR(191) NOT NULL UNIQUE,
    modelo     VARCHAR(191) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT true,
    criador_id INT          NOT NULL,

    CONSTRAINT veiculo_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 5. Create Motorista table
CREATE TABLE motorista
(
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(191) NOT NULL,
    cpf        VARCHAR(191) NOT NULL UNIQUE,
    criador_id INT          NOT NULL,

    CONSTRAINT motorista_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);


