-- ===================================================================
-- V3: Adiciona coluna 'version' para Optimistic Locking (JPA @Version)
-- na tabela inventory_balance.
-- ===================================================================
ALTER TABLE inventory_balance ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
