CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 13. Create Material_Entrada table
CREATE TABLE IF NOT EXISTS material_entrada
(
    id              SERIAL PRIMARY KEY,
    quantidade      DECIMAL(65, 30) NOT NULL,
    tipo            VARCHAR(30)     NOT NULL,
    subtipologia_id INT             NOT NULL,
    criador_id      UUID             NOT NULL,
    data_criacao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_atualizacao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT material_entrada_criador_id_fk
        FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT material_entrada_subtipologia_id_fk
        FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,
    CONSTRAINT material_entrada_tipo_fk CHECK ( tipo IN ('DOACAO', 'COLETA'))
);

-- 14. Create Material_Saida table
CREATE TABLE IF NOT EXISTS material_saida
(
    id              SERIAL PRIMARY KEY,
    quantidade      DECIMAL(65, 30) NOT NULL,
    nota_fiscal_id  UUID             NOT NULL,
    subtipologia_id INT             NOT NULL,
    criador_id      UUID             NOT NULL,
    data_criacao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_atualizacao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT material_saida_criador_id_fk
        FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT material_saida_subtipologia_id_fk
        FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);
