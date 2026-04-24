CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 4. Create veiculo table
CREATE TABLE IF NOT EXISTS veiculo
(
    id         SERIAL PRIMARY KEY,
    placa      VARCHAR(191) NOT NULL UNIQUE,
    modelo     VARCHAR(191) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT true,
    criador_id UUID          NOT NULL,
    data_criacao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_atualizacao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT veiculo_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 5. Create motorista table
CREATE TABLE IF NOT EXISTS motorista
(
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(191) NOT NULL,
    cpf        VARCHAR(191) NOT NULL UNIQUE,
    criador_id UUID          NOT NULL,
    data_criacao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_atualizacao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT motorista_criador_id_fk
        FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);


