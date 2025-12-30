CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 3. Create Documento table
CREATE TABLE IF NOT EXISTS documento
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    nome           VARCHAR(191),
    data_documento TIMESTAMP,
    tipo           VARCHAR(30)  NOT NULL,
    media_id       INT UNIQUE,
    criador_id     UUID         NOT NULL,
    data_insercao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_remocao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deletado       BOOLEAN      NOT NULL    DEFAULT true

    CONSTRAINT documento_tipo_check CHECK (
        tipo IN ('NOTA_FISCAL', 'MTR_GERADOR', 'MTR_TRANSPORTADOR', 'MTR_DESTINADOR', 'DIARIO_COLETA')
        ),
    CONSTRAINT documento_media_id_fk
        FOREIGN KEY (media_id)
        REFERENCES media (id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,
    CONSTRAINT documento_criador_id_fk
        FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

