CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 11. Create Triagem table
CREATE TABLE IF NOT EXISTS triagem
(
    id              SERIAL PRIMARY KEY,
    criador_id      UUID             NOT NULL,
    data            TIMESTAMP(3) NOT NULL,
    tipo            VARCHAR(30)     NOT NULL,
    volume_total    DECIMAL(65, 30) NOT NULL,
    volume_rejeito  DECIMAL(65, 30) NOT NULL,
    tipo_origem     VARCHAR(30)     NOT NULL,
    tipo_destino    VARCHAR(30)     NOT NULL,
    subtipologia_id INT             NOT NULL,

    CONSTRAINT triagem_tipo_check CHECK ( tipo IN ('TOTAL', 'PARCIAL') ),
    CONSTRAINT triagem_tipo_oritem_check CHECK ( tipo IN ('DOACAO', 'COLETA') ),
    CONSTRAINT triagem_tipo_destino_check CHECK ( tipo IN ('DOACAO', 'COLETA') ),
    CONSTRAINT triagem_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT triagem_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 12. Create Prensagem table
CREATE TABLE IF NOT EXISTS prensagem
(
    id              SERIAL PRIMARY KEY,
    criador_id      UUID             NOT NULL,
    data            TIMESTAMP(3) NOT NULL,
    volume_total    DECIMAL(65, 30) NOT NULL,
    tipo_origem     VARCHAR(30)     NOT NULL,
    tipo_destino    VARCHAR(30)     NOT NULL,
    subtipologia_id INT             NOT NULL,

    CONSTRAINT prensagem_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT prensagem_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT prensagem_tipo_origem_check CHECK ( tipo_origem IN ('DOACAO', 'COLETA')),
    CONSTRAINT prensagem_tipo_destino_check CHECK ( tipo_destino IN ('DOACAO', 'COLETA'))
);