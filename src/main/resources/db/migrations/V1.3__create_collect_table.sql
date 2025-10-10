CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 6. Create Coleta table
CREATE TABLE coleta
(
    id                   SERIAL PRIMARY KEY,
    data_realizacao      DATETIME(3) NOT NULL,
    data_chegada         DATETIME(3) NOT NULL,
    data_saida           DATETIME(3) NOT NULL,
    pesagem              DECIMAL(65, 30) NOT NULL,
    quilometragem        DECIMAL(65, 30) NOT NULL,
    ativo                BOOLEAN         NOT NULL DEFAULT true,
    criador_id           INT             NOT NULL,
    veiculo_id           INT NULL,
    motorista_id         INT NULL,
    mtr_gerador_id       INT NULL,
    mtr_destinador_id    INT NULL,
    mtr_transportador_id INT NULL,
    diario_coleta_id     INT NULL,

    CONSTRAINT coleta_criador_id_fkey FOREIGN KEY (coleta_criador_id_fkey)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_veiculo_id_fkey FOREIGN KEY (coleta_veiculo_id_fkey)
        REFERENCES veiculo (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_motorista_id_fkey FOREIGN KEY (coleta_motorista_id_fkey)
        REFERENCES motorista (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_mtr_gerador_id_fkey FOREIGN KEY (coleta_mtr_gerador_id_fkey)
        REFERENCES documento (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_mtr_destinador_id_fkey FOREIGN KEY (coleta_mtr_destinador_id_fkey)
        REFERENCES documento (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_mtr_transportador_id_fkey FOREIGN KEY (coleta_mtr_transportador_id_fkey)
        REFERENCES documento (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_diario_coleta_id_fkey FOREIGN KEY (coleta_diario_coleta_id_fkey)
        REFERENCES documento (id) ON DELETE RESTRICT ON UPDATE CASCADE
);