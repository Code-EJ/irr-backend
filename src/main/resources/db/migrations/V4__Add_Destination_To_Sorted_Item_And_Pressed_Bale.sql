-- ===================================================================
-- V4: Adiciona destination_type e destination_id em sorted_item
-- e pressed_bale para registrar o destino do material após a operação.
-- ===================================================================
ALTER TABLE sorted_item ADD COLUMN destination_type VARCHAR(50);
ALTER TABLE sorted_item ADD COLUMN destination_id UUID;
ALTER TABLE pressed_bale ADD COLUMN destination_type VARCHAR(50);
ALTER TABLE pressed_bale ADD COLUMN destination_id UUID;