-- 1. Create Usuario table
CREATE TABLE usuario (
    id SERIAL PRIMARY KEY,
    email VARCHAR(191) NOT NULL UNIQUE,
    senha VARCHAR(191) NOT NULL,
    nome VARCHAR(191) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    criador_id INT NOT NULL,

    CONSTRAINT usuario_tipo_check CHECK (
        tipo IN ('ADMINISTRADOR', 'PREFEITURA', 'ORGANIZACAO', 'REPRESENTANTE')
    ),
    CONSTRAINT usuario_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 2. Create Media table
CREATE TABLE media (
    id SERIAL PRIMARY KEY,
    dados BYTEA NOT NULL
);

-- 3. Create Documento table
CREATE TABLE documento (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(191),
    data_documento DATE,
    data_insercao TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    tipo VARCHAR(30) NOT NULL,
    media_id INT UNIQUE,
    criador_id INT NOT NULL,

    CONSTRAINT documento_tipo_check CHECK (
        tipo IN ('NOTA_FISCAL', 'MTR_GERADOR', 'MTR_TRANSPORTADOR', 'MTR_DESTINADOR', 'DIARIO_COLETA')
    ),
    CONSTRAINT documento_media_id_fkey FOREIGN KEY (media_id)
        REFERENCES media(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT documento_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 4. Create Veiculo table
CREATE TABLE veiculo (
    id SERIAL PRIMARY KEY ,
    placa VARCHAR(191) NOT NULL UNIQUE,
    modelo VARCHAR(191) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criador_id INT NOT NULL,

    CONSTRAINT veiculo_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 5. Create Motorista table
CREATE TABLE motorista (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(191) NOT NULL,
    cpf VARCHAR(191) NOT NULL UNIQUE,
    criador_id INT NOT NULL,

    CONSTRAINT motorista_criador_id_fkey FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 6. Create Coleta table
CREATE TABLE coleta (
    id SERIAL PRIMARY KEY,
    data_realizacao DATETIME(3) NOT NULL,
    data_chegada DATETIME(3) NOT NULL,
    data_saida DATETIME(3) NOT NULL,
    pesagem DECIMAL(65, 30) NOT NULL,
    quilometragem DECIMAL(65, 30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criador_id INT NOT NULL,
    veiculo_id INT NULL,
    motorista_id INT NULL,
    mtr_gerador_id INT NULL,
    mtr_destinador_id INT NULL,
    mtr_transportador_id INT NULL,
    diario_coleta_id INT NULL,

    CONSTRAINT coleta_criador_id_fkey FOREIGN KEY (coleta_criador_id_fkey)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_veiculo_id_fkey FOREIGN KEY (coleta_veiculo_id_fkey)
        REFERENCES veiculo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_motorista_id_fkey FOREIGN KEY (coleta_motorista_id_fkey)
        REFERENCES motorista(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_mtr_gerador_id_fkey FOREIGN KEY (coleta_mtr_gerador_id_fkey)
        REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_mtr_destinador_id_fkey FOREIGN KEY (coleta_mtr_destinador_id_fkey)
        REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_mtr_transportador_id_fkey FOREIGN KEY (coleta_mtr_transportador_id_fkey)
        REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT coleta_diario_coleta_id_fkey FOREIGN KEY (coleta_diario_coleta_id_fkey)
        REFERENCES documento(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 7. Create Doador table
CREATE TABLE doador (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(191) NOT NULL,
    endereco VARCHAR(191) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cadastro_nacional VARCHAR(191) NOT NULL UNIQUE,

    CONSTRAINT documento_tipo_check CHECK ( tipo IN ('PESSOA_FISICA', 'PESSOA_JURIDICA') )
);

-- 8. Create tipologia table
CREATE TABLE tipologia (
    id SERIAL PRIMARY KEY,
    criador_id INT NOT NULL,
    nome VARCHAR(191) NOT NULL,
    valor DECIMAL(65, 30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT tipologia_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 9. Create subtipologia table
CREATE TABLE subtipologia (
    id SERIAL PRIMARY KEY,
    criador_id INT NOT NULL,
    nome VARCHAR(191) NOT NULL,
    valor DECIMAL(65, 30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    tipologia_id INT NOT NULL,

    CONSTRAINT subtipologia_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT subtipologia_tipologia_id_fk FOREIGN KEY (tipologia_id)
        REFERENCES tipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 10. Create Doacao table
CREATE TABLE doacao (
    id SERIAL PRIMARY KEY,
    pesagem DECIMAL(65, 30) NOT NULL,
    tipologia_id INT NOT NULL,
    subtipologia_id INT NOT NULL,
    doador_id INT NOT NULL,
    criador_id INT NOT NULL,

    CONSTRAINT doacao_tipologia_id_fk FOREIGN KEY (tipologia_id)
        REFERENCES tipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_doador_id_fk FOREIGN KEY (doador_id)
        REFERENCES doador(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT doacao_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 11. Create Triagem table
CREATE TABLE triagem (
    id SERIAL PRIMARY KEY,
    criador_id INT NOT NULL,
    data DATETIME(3) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    volume_total DECIMAL(65, 30) NOT NULL,
    volume_rejeito DECIMAL(65, 30) NOT NULL,
    tipo_origem VARCHAR(30) NOT NULL,
    tipo_destino VARCHAR(30) NOT NULL,
    subtipologia_id INT NOT NULL,

    CONSTRAINT triagem_tipo_check CHECK ( tipo IN ('TOTAL', 'PARCIAL') ),
    CONSTRAINT triagem_tipo_oritem_check CHECK ( tipo IN ('DOACAO', 'COLETA') ),
    CONSTRAINT triagem_tipo_destino_check CHECK ( tipo IN ('DOACAO', 'COLETA') ),
    CONSTRAINT triagem_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT triagem_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtiplogia(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 12. Create Prensagem table
CREATE TABLE prensagem (
    id SERIAL PRIMARY KEY,
    criador_id INT NOT NULL,
    data DATETIME(3) NOT NULL,
    volume_total DECIMAL(65, 30) NOT NULL,
    tipo_origem VARCHAR(30) NOT NULL,
    tipo_destino VARCHAR(30) NOT NULL,
    subtipologia_id INT NOT NULL,

    CONSTRAINT prensagem_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT prensagem_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT prensagem_tipo_origem_check CHECK ( tipo_origem IN ('DOACAO', 'COLETA')),
    CONSTRAINT prensagem_tipo_destino_check CHECK ( tipo_destino IN ('DOACAO', 'COLETA'))
);

-- 13. Create Material_Entrada table
CREATE TABLE material_entrada (
    id SERIAL PRIMARY KEY,
    criador_id INT NOT NULL,
    quantidade DECIMAL(65, 30) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    subtipologia_id INT NOT NULL,

    CONSTRAINT material_entrada_criador_id_fk FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT material_entrada_subtipologia_id_fk FOREIGN KEY (subtipologia_id)
        REFERENCES subtipologia(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT material_entrada_tipo_fk CHECK ( tipo IN ('DOACAO', 'COLETA'))
);

-- 14. Create Material_Saida table
CREATE TABLE material_saida (
    id SERIAL PRIMARY KEY,
    criador_id INT NOT NULL,
    quantidade DECIMAL(65, 30) NOT NULL,
    nota_fiscal_id INT NOT NULL,
    subtipologia_id INT NOT NULL,

    CONSTRAINT material_saida_criador_id FOREIGN KEY (criador_id)
        REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
--        AQUI: Colocar corretamente a nota fiscal, ela ainda nao existe
    CONSTRAINT material_saida_nota_fiscal_id_fk FOREIGN KEY (nota_fiscal_id)
        REFERENCES nota_fiscal(id)
);

-- AddForeignKey
ALTER TABLE `MaterialSaida` ADD CONSTRAINT `MaterialSaida_criadorId_fkey` FOREIGN KEY (`criadorId`) REFERENCES `Usuario`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `MaterialSaida` ADD CONSTRAINT `MaterialSaida_notaFiscalId_fkey` FOREIGN KEY (`notaFiscalId`) REFERENCES `Documento`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `MaterialSaida` ADD CONSTRAINT `MaterialSaida_subtipologiaId_fkey` FOREIGN KEY (`subtipologiaId`) REFERENCES `Subtipologia`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;





-- CreateTable
CREATE TABLE `Venda` (
    `id` VARCHAR(191) NOT NULL,
    `criadorId` VARCHAR(191) NOT NULL,
    `comprador` VARCHAR(191) NOT NULL,
    `data` DATETIME(3) NOT NULL,
    `valor` DECIMAL(65, 30) NOT NULL,
    `notaFiscalId` VARCHAR(191) NOT NULL,
    `mtrGeradorId` VARCHAR(191) NOT NULL,
    `mtrTransportadorId` VARCHAR(191) NOT NULL,
    `mtrDestinadorId` VARCHAR(191) NOT NULL,

    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `Venda` ADD CONSTRAINT `Venda_criadorId_fkey` FOREIGN KEY (`criadorId`) REFERENCES `Usuario`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Venda` ADD CONSTRAINT `Venda_notaFiscalId_fkey` FOREIGN KEY (`notaFiscalId`) REFERENCES `Documento`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Venda` ADD CONSTRAINT `Venda_mtrGeradorId_fkey` FOREIGN KEY (`mtrGeradorId`) REFERENCES `Documento`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Venda` ADD CONSTRAINT `Venda_mtrTransportadorId_fkey` FOREIGN KEY (`mtrTransportadorId`) REFERENCES `Documento`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Venda` ADD CONSTRAINT `Venda_mtrDestinadorId_fkey` FOREIGN KEY (`mtrDestinadorId`) REFERENCES `Documento`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
