CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 8. Create tipologia table
CREATE TABLE tipologia
(
    id         SERIAL PRIMARY KEY,
    criador_id INT             NOT NULL,
    nome       VARCHAR(191)    NOT NULL,
    valor      DECIMAL(65, 30) NOT NULL,
    ativo      BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT tipologia_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 9. Create subtipologia table
CREATE TABLE subtipologia
(
    id           SERIAL PRIMARY KEY,
    criador_id   INT             NOT NULL,
    nome         VARCHAR(191)    NOT NULL,
    valor        DECIMAL(65, 30) NOT NULL,
    ativo        BOOLEAN         NOT NULL DEFAULT true,
    tipologia_id INT             NOT NULL,

    CONSTRAINT subtipologia_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT subtipologia_tipologia_id_fk FOREIGN KEY (tipologia_id)
        REFERENCES tipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE
);