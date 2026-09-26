-- Fail rather than silently merge duplicate balances or discard negative quantities.
-- Existing development databases must reconcile invalid rows before applying this migration.
ALTER TABLE inventory_balance
    ADD CONSTRAINT uq_inventory_balance_material_subtype UNIQUE (material_subtype_id),
    ADD CONSTRAINT ck_inventory_balance_nonnegative
        CHECK (current_weight_kg >= 0 AND current_volume_m3 >= 0);
