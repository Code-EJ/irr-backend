-- One immutable stock ledger; raw intake and saleable stock are separate quantities.
-- Author: Enzo Ribas (https://github.com/oEnzoRibas)
CREATE TABLE command_receipt (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id),
 request_key VARCHAR(128) NOT NULL, command_type VARCHAR(50) NOT NULL,
 request_hash VARCHAR(64) NOT NULL, response JSONB NOT NULL,
 actor_id UUID NOT NULL REFERENCES users(id), recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(organization_id,request_key)
);
CREATE TABLE stock_operation (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id),
 actor_id UUID NOT NULL REFERENCES users(id), kind VARCHAR(20) NOT NULL CHECK(kind IN ('SORTING','PRESSING','SALE','REVERSAL')),
 sorting_id UUID, pressing_id UUID, sale_id UUID, reversal_of UUID,
 occurred_at TIMESTAMPTZ NOT NULL, recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(organization_id,id), UNIQUE(organization_id,sorting_id), UNIQUE(organization_id,pressing_id), UNIQUE(organization_id,sale_id), UNIQUE(reversal_of),
 FOREIGN KEY(organization_id,sorting_id) REFERENCES sorting(organization_id,id),
 FOREIGN KEY(organization_id,pressing_id) REFERENCES pressing(organization_id,id),
 FOREIGN KEY(organization_id,sale_id) REFERENCES sale(organization_id,id),
 FOREIGN KEY(organization_id,reversal_of) REFERENCES stock_operation(organization_id,id),
 CHECK ((kind='SORTING' AND sorting_id IS NOT NULL AND num_nonnulls(pressing_id,sale_id,reversal_of)=0)
 OR (kind='PRESSING' AND pressing_id IS NOT NULL AND num_nonnulls(sorting_id,sale_id,reversal_of)=0)
 OR (kind='SALE' AND sale_id IS NOT NULL AND num_nonnulls(sorting_id,pressing_id,reversal_of)=0)
 OR (kind='REVERSAL' AND reversal_of IS NOT NULL AND num_nonnulls(sorting_id,pressing_id,sale_id)=0))
);
CREATE TABLE stock_movement (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id),
 operation_id UUID NOT NULL, material_subtype_id UUID NOT NULL,
 weight_delta_kg NUMERIC(15,4) NOT NULL, volume_delta_m3 NUMERIC(15,4) NOT NULL,
 FOREIGN KEY(organization_id,operation_id) REFERENCES stock_operation(organization_id,id),
 FOREIGN KEY(organization_id,material_subtype_id) REFERENCES material_subtype(organization_id,id)
);
CREATE INDEX ix_stock_movement_scope_material ON stock_movement(organization_id,material_subtype_id);
CREATE TABLE stock_lot (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organization(id),
 material_subtype_id UUID NOT NULL, sorted_item_id UUID, pressed_bale_id UUID,
 available_weight_kg NUMERIC(15,4) NOT NULL CHECK(available_weight_kg>=0),
 available_volume_m3 NUMERIC(15,4) NOT NULL CHECK(available_volume_m3>=0),
 original_weight_kg NUMERIC(15,4) NOT NULL CHECK(original_weight_kg>=0),
 original_volume_m3 NUMERIC(15,4) NOT NULL CHECK(original_volume_m3>=0),
 is_active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(organization_id,id), UNIQUE(sorted_item_id), UNIQUE(pressed_bale_id),
 CHECK(num_nonnulls(sorted_item_id,pressed_bale_id)=1),
 CHECK(available_weight_kg<=original_weight_kg AND available_volume_m3<=original_volume_m3),
 FOREIGN KEY(organization_id,material_subtype_id) REFERENCES material_subtype(organization_id,id),
 FOREIGN KEY(organization_id,sorted_item_id) REFERENCES sorted_item(organization_id,id),
 FOREIGN KEY(organization_id,pressed_bale_id) REFERENCES pressed_bale(organization_id,id)
);
CREATE INDEX ix_stock_lot_scope ON stock_lot(organization_id,material_subtype_id,created_at);
ALTER TABLE sale ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','POSTED','REVERSED'));
ALTER TABLE sale ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'BRL';
ALTER TABLE sale ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sale ADD CONSTRAINT ck_posted_sale_invoice CHECK(organization_id IS NULL OR status='DRAFT' OR nfe_attachment_id IS NOT NULL);
ALTER TABLE sale_item ALTER COLUMN unit_price TYPE NUMERIC(15,4);
ALTER TABLE sale_item ADD COLUMN stock_lot_id UUID;
ALTER TABLE sale_item ADD CONSTRAINT fk_sale_item_lot_scope FOREIGN KEY(organization_id,stock_lot_id) REFERENCES stock_lot(organization_id,id);
ALTER TABLE sale_item ADD CONSTRAINT ck_sale_item_scoped_lot CHECK(organization_id IS NULL OR stock_lot_id IS NOT NULL);
CREATE FUNCTION reject_stock_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Stock history is immutable; post a reversal' USING ERRCODE='23514'; END;
$$;
CREATE TRIGGER stock_operation_immutable BEFORE UPDATE OR DELETE ON stock_operation FOR EACH ROW EXECUTE FUNCTION reject_stock_history_mutation();
CREATE TRIGGER stock_movement_immutable BEFORE UPDATE OR DELETE ON stock_movement FOR EACH ROW EXECUTE FUNCTION reject_stock_history_mutation();

ALTER TABLE sorting ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK(status IN ('POSTED','REVERSED'));
ALTER TABLE pressing ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK(status IN ('POSTED','REVERSED'));
