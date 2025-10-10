CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 6. Create Coleta table
CREATE TABLE coleta
(
    id                   SERIAL PRIMARY KEY,
    data_realizacao      TIMESTAMP(3) NOT NULL,
    data_chegada         TIMESTAMP(3) NOT NULL,
    data_saida           TIMESTAMP(3) NOT NULL,
    pesagem              DECIMAL(65, 30) NOT NULL,
    quilometragem        DECIMAL(65, 30) NOT NULL,
    ativo                BOOLEAN         NOT NULL DEFAULT true,
    criador_id           UUID             NOT NULL,
    veiculo_id           INT NULL,
    motorista_id         INT NULL,
    mtr_gerador_id       UUID NULL,
    mtr_destinador_id    UUID NULL,
    mtr_transportador_id UUID NULL,
    diario_coleta_id     UUID NULL,

    CONSTRAINT coleta_criador_id_fkey
        FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT coleta_veiculo_id_fkey
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculo (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT coleta_motorista_id_fkey
        FOREIGN KEY (motorista_id)
        REFERENCES motorista (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT coleta_mtr_gerador_id_fkey
        FOREIGN KEY (mtr_gerador_id)
        REFERENCES documento (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT coleta_mtr_destinador_id_fkey
        FOREIGN KEY (mtr_destinador_id)
        REFERENCES documento (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT coleta_mtr_transportador_id_fkey
        FOREIGN KEY (mtr_transportador_id)
        REFERENCES documento (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT coleta_diario_coleta_id_fkey
        FOREIGN KEY (diario_coleta_id)
        REFERENCES documento (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);