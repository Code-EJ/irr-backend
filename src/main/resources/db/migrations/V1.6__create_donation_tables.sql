CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 10. Create Doacao table
CREATE TABLE IF NOT EXISTS doacao
(
    id              SERIAL PRIMARY KEY,
    pesagem         DECIMAL(65, 30) NOT NULL,
    tipologia_id    INT             NOT NULL,
    subtipologia_id INT             NOT NULL,
    doador_id       INT             NOT NULL,
    criador_id      UUID             NOT NULL,

    CONSTRAINT doacao_tipologia_id_fk FOREIGN KEY (tipologia_id)
        REFERENCES tipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_doador_id_fk FOREIGN KEY (doador_id)
        REFERENCES doador (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);