CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 13. Create Material_Entrada table
CREATE TABLE material_entrada
(
    id              SERIAL PRIMARY KEY,
    criador_id      INT             NOT NULL,
    quantidade      DECIMAL(65, 30) NOT NULL,
    tipo            VARCHAR(30)     NOT NULL,
    subtipologia_id INT             NOT NULL,

    CONSTRAINT material_entrada_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT material_entrada_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT material_entrada_tipo_fk CHECK ( tipo IN ('DOACAO', 'COLETA'))
);

-- 14. Create Material_Saida table
CREATE TABLE material_saida
(
    id              SERIAL PRIMARY KEY,
    criador_id      INT             NOT NULL,
    quantidade      DECIMAL(65, 30) NOT NULL,
    nota_fiscal_id  INT             NOT NULL,
    subtipologia_id INT             NOT NULL,

    CONSTRAINT material_saida_criador_id FOREIGN KEY (criador_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE,
--        AQUI: Colocar corretamente a nota fiscal, ela ainda nao existe
    CONSTRAINT material_saida_nota_fiscal_id_fk FOREIGN KEY (nota_fiscal_id)
        REFERENCES nota_fiscal (id)
);

-- AddForeignKey
ALTER TABLE MaterialSaida
    ADD CONSTRAINT MaterialSaida_criadorId_fkey FOREIGN KEY (criadorId)
        REFERENCES Usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE MaterialSaida
    ADD CONSTRAINT MaterialSaida_notaFiscalId_fkey FOREIGN KEY (notaFiscalId)
        REFERENCES Documento (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE MaterialSaida
    ADD CONSTRAINT MaterialSaida_subtipologiaId_fkey FOREIGN KEY (subtipologiaId)
        REFERENCES Subtipologia (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;