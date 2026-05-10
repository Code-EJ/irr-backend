-- ==========================================
-- V2: Adiciona coluna 'version' para Optimistic Locking (JPA @Version)
-- nas tabelas da árvore tipológica de materiais.
-- ==========================================

ALTER TABLE material_category ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE material_type ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE material_subtype ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
