CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 7. Create Doador table
CREATE TABLE doador
(
    id                SERIAL PRIMARY KEY,
    nome              VARCHAR(191) NOT NULL,
    endereco          VARCHAR(191) NOT NULL,
    tipo              VARCHAR(30)  NOT NULL,
    cadastro_nacional VARCHAR(191) NOT NULL UNIQUE,

    CONSTRAINT documento_tipo_check CHECK ( tipo IN ('PESSOA_FISICA', 'PESSOA_JURIDICA') )
);

-- 10. Create Doacao table
CREATE TABLE doacao
(
    id              SERIAL PRIMARY KEY,
    pesagem         DECIMAL(65, 30) NOT NULL,
    tipologia_id    INT             NOT NULL,
    subtipologia_id INT             NOT NULL,
    doador_id       INT             NOT NULL,
    criador_id      INT             NOT NULL,

    CONSTRAINT doacao_tipologia_id_fk FOREIGN KEY (tipologia_id)
        REFERENCES tipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_doador_id_fk FOREIGN KEY (doador_id)
        REFERENCES doador (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);