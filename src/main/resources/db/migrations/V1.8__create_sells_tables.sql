CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- 15. Create Venda table
CREATE TABLE Venda
(
    id                 VARCHAR(191)    NOT NULL,
    criadorId          VARCHAR(191)    NOT NULL,
    comprador          VARCHAR(191)    NOT NULL,
    data               DATETIME(3) NOT NULL,
    valor              DECIMAL(65, 30) NOT NULL,
    notaFiscalId       VARCHAR(191)    NOT NULL,
    mtrGeradorId       VARCHAR(191)    NOT NULL,
    mtrTransportadorId VARCHAR(191)    NOT NULL,
    mtrDestinadorId    VARCHAR(191)    NOT NULL,

    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE Venda
    ADD CONSTRAINT Venda_criadorId_fkey FOREIGN KEY (criadorId)
        REFERENCES Usuario (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE Venda
    ADD CONSTRAINT Venda_notaFiscalId_fkey FOREIGN KEY (notaFiscalId)
        REFERENCES Documento (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE Venda
    ADD CONSTRAINT Venda_mtrGeradorId_fkey FOREIGN KEY (mtrGeradorId)
        REFERENCES Documento (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE Venda
    ADD CONSTRAINT Venda_mtrTransportadorId_fkey FOREIGN KEY (mtrTransportadorId)
        REFERENCES Documento (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE Venda
    ADD CONSTRAINT Venda_mtrDestinadorId_fkey FOREIGN KEY (mtrDestinadorId)
        REFERENCES Documento (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;