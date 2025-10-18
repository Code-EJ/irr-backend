-- Tabela nota_fiscal
CREATE TABLE IF NOT EXISTS nota_fiscal
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    numero       VARCHAR(50)    NOT NULL,
    data_emissao TIMESTAMP(3)   NOT NULL,
    valor_total  DECIMAL(18, 2) NOT NULL,
    descricao    TEXT           NULL,
    coleta_id    INT            NOT NULL,
    emissor_id   UUID            NOT NULL,
    ativo        BOOLEAN        NOT NULL DEFAULT true,

    -- Foreign keys
    CONSTRAINT nota_fiscal_coleta_id_fk
        FOREIGN KEY (coleta_id)
            REFERENCES coleta (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT nota_fiscal_emissor_id_fk
        FOREIGN KEY (emissor_id)
            REFERENCES usuario (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

-- AddForeignKey venda_nota_fiscal_id_fk at Table Venda

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'venda_nota_fiscal_id_fk'
              AND table_name = 'venda'
        ) THEN
            ALTER TABLE venda
                ADD CONSTRAINT venda_nota_fiscal_id_fk
                    FOREIGN KEY (nota_fiscal_id)
                    REFERENCES nota_fiscal (id)
                    ON DELETE RESTRICT
                    ON UPDATE CASCADE;
        END IF;
END$$;

-- AddForeignKey material_saida_nota_fiscal_id_fk at Table material_saida

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'material_saida_nota_fiscal_id_fk'
              AND table_name = 'material_saida'
        ) THEN
            ALTER TABLE material_saida
                ADD CONSTRAINT material_saida_nota_fiscal_id_fk
                    FOREIGN KEY (nota_fiscal_id)
                    REFERENCES nota_fiscal (id)
                    ON DELETE RESTRICT
                    ON UPDATE CASCADE;
        END IF;
END$$
;
