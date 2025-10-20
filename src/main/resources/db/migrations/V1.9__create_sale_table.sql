CREATE
    EXTENSION IF NOT EXISTS "pgcrypto";

-- 15. Create Venda table
CREATE TABLE IF NOT EXISTS venda
(
    id                   SERIAL PRIMARY KEY,
    comprador            VARCHAR(191)   NOT NULL,
    data                 TIMESTAMP(3)   NOT NULL,
    valor                DECIMAL(18, 2) NOT NULL,
    nota_fiscal_id       UUID            NOT NULL,
    mtr_gerador_id       UUID            NOT NULL,
    mtr_transportador_id UUID            NOT NULL,
    mtr_destinador_id    UUID            NOT NULL,
    criador_id           UUID            NOT NULL,
    data_criacao      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    data_atualizacao  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

-- AddForeignKey venda_criador_id_fk

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'venda_criador_id_fk'
              AND table_name = 'venda'
        ) THEN
            ALTER TABLE venda
                ADD CONSTRAINT venda_criador_id_fk
                FOREIGN KEY (criador_id)
                    REFERENCES usuario (id)
                    ON DELETE RESTRICT
                    ON UPDATE CASCADE;
        END IF;
END$$;

-- AddForeignKey venda_mtr_gerador_id_fk
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'venda_mtr_gerador_id_fk'
              AND table_name = 'venda'
        ) THEN
            ALTER TABLE venda
                ADD CONSTRAINT venda_mtr_gerador_id_fk
                    FOREIGN KEY (mtr_gerador_id)
                    REFERENCES documento (id)
                    ON DELETE RESTRICT
                    ON UPDATE CASCADE;
        END IF;
END$$;

-- AddForeignKey venda_mtr_transportador_id_fk
DO $$
    BEGIN
        IF NOT EXISTS(
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'venda_mtr_transportador_id_fk'
              AND table_name = 'venda'
        ) THEN
            ALTER TABLE venda
                ADD CONSTRAINT venda_mtr_transportador_id_fk
                    FOREIGN KEY (mtr_transportador_id)
                    REFERENCES documento (id)
                    ON DELETE RESTRICT
                    ON UPDATE CASCADE;
        END IF;
END $$;

-- AddForeignKey venda_mtr_destinador_id_fk
DO $$
    BEGIN
        IF NOT EXISTS(
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'venda_mtr_destinador_id_fk'
              AND table_name = 'venda'
        ) THEN
            ALTER TABLE venda
                ADD CONSTRAINT venda_mtr_destinador_id_fk
                    FOREIGN KEY (mtr_destinador_id)
                    REFERENCES documento (id)
                    ON DELETE RESTRICT
                    ON UPDATE CASCADE;
        END IF;
    END $$;