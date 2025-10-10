CREATE
    EXTENSION IF NOT EXISTS "pgcrypto";

-- 15. Create Venda table
CREATE TABLE venda
(
    id                   SERIAL PRIMARY KEY,
    criador_id           UUID            NOT NULL,
    comprador            VARCHAR(191)   NOT NULL,
    data                 TIMESTAMP(3)   NOT NULL,
    valor                DECIMAL(18, 2) NOT NULL,
    nota_fiscal_id       UUID            NOT NULL,
    mtr_gerador_id       UUID            NOT NULL,
    mtr_transportador_id UUID            NOT NULL,
    mtr_destinador_id    UUID            NOT NULL
);

-- AddForeignKey
ALTER TABLE venda
    ADD CONSTRAINT venda_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE;

-- AddForeignKey
-- ALTER TABLE venda
--     ADD CONSTRAINT venda_nota_fiscal_id_fk FOREIGN KEY (nota_fiscal_id)
--         REFERENCES nota_fiscal (id)
--         ON DELETE RESTRICT
--         ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE venda
    ADD CONSTRAINT venda_mtr_gerador_id_fk FOREIGN KEY (mtr_gerador_id)
        REFERENCES documento (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE venda
    ADD CONSTRAINT venda_mtr_transportador_id_fk FOREIGN KEY (mtr_transportador_id)
        REFERENCES documento (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE venda
    ADD CONSTRAINT venda_mtr_destinador_id_fk FOREIGN KEY (mtr_destinador_id)
        REFERENCES documento (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE;