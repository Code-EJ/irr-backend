CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 3. Create Documento table
CREATE TABLE documento
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    nome           VARCHAR(191),
    data_documento DATE,
    data_insercao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    criado_em      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    tipo           VARCHAR(30)  NOT NULL,
    media_id       INT UNIQUE,
    criador_id     UUID         NOT NULL,
    atualizado_em  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT documento_tipo_check CHECK (
        tipo IN ('NOTA_FISCAL', 'MTR_GERADOR', 'MTR_TRANSPORTADOR', 'MTR_DESTINADOR', 'DIARIO_COLETA')
        ),
    CONSTRAINT documento_media_id_fkey FOREIGN KEY (media_id)
        REFERENCES media (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT documento_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

