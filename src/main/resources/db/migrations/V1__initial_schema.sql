-- Final pre-production schema baseline. Decision: ADR-0014.
-- Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
-- Equivalent to the verified final schema; future shared changes start at V2.
-- Create tables first, then keys/indexes/triggers so foreign-key ordering is explicit.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE FUNCTION reject_stock_history_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN RAISE EXCEPTION 'Stock history is immutable; post a reversal' USING ERRCODE='23514'; END;
$$;

CREATE FUNCTION validate_collection_team_scope() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE collection_scope UUID; member_scope UUID;
BEGIN
 SELECT organization_id INTO collection_scope FROM collection WHERE id=NEW.collection_id;
 SELECT organization_id INTO member_scope FROM team_member WHERE id=NEW.team_member_id;
 IF collection_scope IS DISTINCT FROM member_scope THEN
  RAISE EXCEPTION 'Collection team must belong to the collection organization' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;

CREATE TABLE attachment (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    file_name character varying(255) NOT NULL,
    file_type character varying(50) NOT NULL,
    storage_url text NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid
);

CREATE TABLE attachment_file_deletion (
    id uuid NOT NULL,
    storage_path text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_error character varying(300),
    CONSTRAINT attachment_file_deletion_attempts_check CHECK ((attempts >= 0))
);

CREATE TABLE buyer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    document character varying(20),
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid
);

CREATE TABLE collection (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    realization_date timestamp with time zone NOT NULL,
    total_weight_kg numeric(15,4) NOT NULL,
    vehicle_id uuid NOT NULL,
    driver_id uuid NOT NULL,
    mtr_generator_id uuid,
    mtr_destinator_id uuid,
    collection_diary_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    route_description character varying(2000),
    departure_at timestamp with time zone,
    arrival_at timestamp with time zone,
    distance_km numeric(12,3),
    CONSTRAINT ck_collection_distance CHECK (((distance_km IS NULL) OR (distance_km >= (0)::numeric))),
    CONSTRAINT ck_collection_schedule CHECK ((((departure_at IS NULL) AND (arrival_at IS NULL)) OR ((departure_at IS NOT NULL) AND (arrival_at IS NOT NULL) AND (arrival_at >= departure_at))))
);

CREATE TABLE collection_team (
    collection_id uuid NOT NULL,
    team_member_id uuid NOT NULL
);

CREATE TABLE command_receipt (
    id uuid NOT NULL,
    organization_id uuid NOT NULL,
    request_key character varying(128) NOT NULL,
    command_type character varying(50) NOT NULL,
    request_hash character varying(64) NOT NULL,
    response jsonb NOT NULL,
    actor_id uuid NOT NULL,
    recorded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE donation (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    donation_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    total_weight_kg numeric(15,4) NOT NULL,
    donor_id uuid NOT NULL,
    proof_attachment_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid
);

CREATE TABLE donor (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    document character varying(20) NOT NULL,
    donor_type character varying(10) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    address_line1 character varying(255),
    address_line2 character varying(255),
    address_city character varying(100),
    address_region character varying(100),
    address_postal_code character varying(20),
    address_country_code character varying(2),
    CONSTRAINT ck_donor_address_complete CHECK (((num_nonnulls(address_line1, address_line2, address_city, address_region, address_postal_code, address_country_code) = 0) OR ((address_line1 IS NOT NULL) AND (length(btrim((address_line1)::text)) > 0) AND (address_city IS NOT NULL) AND (length(btrim((address_city)::text)) > 0) AND (address_region IS NOT NULL) AND (length(btrim((address_region)::text)) > 0) AND (address_postal_code IS NOT NULL) AND (length(btrim((address_postal_code)::text)) > 0) AND (address_country_code IS NOT NULL) AND ((address_country_code)::text ~ '^[A-Z]{2}$'::text))))
);

CREATE TABLE input_item (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    collection_id uuid,
    donation_id uuid,
    material_subtype_id uuid NOT NULL,
    weight_kg numeric(15,4) NOT NULL,
    volume_m3 numeric(15,4) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    CONSTRAINT ck_input_scoped_source CHECK (((organization_id IS NULL) OR ((num_nonnulls(collection_id, donation_id) = 1) AND (weight_kg > (0)::numeric) AND (volume_m3 >= (0)::numeric))))
);

CREATE TABLE inventory_balance (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    material_subtype_id uuid NOT NULL,
    current_weight_kg numeric(15,4) DEFAULT 0 NOT NULL,
    current_volume_m3 numeric(15,4) DEFAULT 0 NOT NULL,
    last_updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    version bigint DEFAULT 0 NOT NULL,
    organization_id uuid,
    CONSTRAINT ck_inventory_balance_nonnegative CHECK (((current_weight_kg >= (0)::numeric) AND (current_volume_m3 >= (0)::numeric)))
);

CREATE TABLE inventory_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    material_subtype_id uuid NOT NULL,
    quantity_kg numeric(15,4) NOT NULL,
    quantity_m3 numeric(15,4) NOT NULL,
    operation_type character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid
);

CREATE TABLE material_category (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    version bigint DEFAULT 0 NOT NULL,
    organization_id uuid
);

CREATE TABLE material_subtype (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    type_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    version bigint DEFAULT 0 NOT NULL,
    organization_id uuid
);

CREATE TABLE material_type (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    category_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    version bigint DEFAULT 0 NOT NULL,
    organization_id uuid
);

CREATE TABLE organization (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    organization_type character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT organization_name_check CHECK ((length(btrim((name)::text)) > 0)),
    CONSTRAINT organization_organization_type_check CHECK ((length(btrim((organization_type)::text)) > 0))
);

CREATE TABLE organization_access_audit (
    id uuid NOT NULL,
    organization_id uuid NOT NULL,
    actor_id uuid NOT NULL,
    subject_user_id uuid,
    action character varying(20) NOT NULL,
    previous_role character varying(20),
    assigned_role character varying(20),
    recorded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    details jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT organization_access_audit_action_check CHECK (action IN ('CREATE','GRANT','REVOKE','UPDATE','DEACTIVATE'))
);

CREATE TABLE organization_membership (
    organization_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    granted_by uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT organization_membership_role_check CHECK (role IN ('MEMBER','MANAGER'))
);

CREATE TABLE pressed_bale (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    pressing_id uuid,
    sorted_item_id uuid,
    material_subtype_id uuid NOT NULL,
    weight_kg numeric(15,4) NOT NULL,
    initial_volume_m3 numeric(15,4) NOT NULL,
    final_volume_m3 numeric(15,4) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    destination_type character varying(50),
    destination_id uuid,
    organization_id uuid,
    CONSTRAINT ck_bale_scoped_source CHECK (((organization_id IS NULL) OR ((pressing_id IS NOT NULL) AND (sorted_item_id IS NOT NULL) AND (weight_kg > (0)::numeric) AND (initial_volume_m3 > final_volume_m3) AND (final_volume_m3 >= (0)::numeric))))
);

CREATE TABLE pressing (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    pressing_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    status character varying(20) DEFAULT 'POSTED'::character varying NOT NULL,
    CONSTRAINT pressing_status_check CHECK (status IN ('POSTED','REVERSED'))
);

CREATE TABLE sale (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sale_date timestamp with time zone NOT NULL,
    buyer_id uuid NOT NULL,
    nfe_attachment_id uuid,
    mtr_attachment_id uuid,
    cdf_attachment_id uuid,
    total_value numeric(15,2) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    currency character varying(3) DEFAULT 'BRL'::character varying NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_posted_sale_invoice CHECK (((organization_id IS NULL) OR ((status)::text = 'DRAFT'::text) OR (nfe_attachment_id IS NOT NULL))),
    CONSTRAINT sale_status_check CHECK (status IN ('DRAFT','POSTED','REVERSED'))
);

CREATE TABLE sale_item (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sale_id uuid,
    material_subtype_id uuid NOT NULL,
    weight_kg numeric(15,4) NOT NULL,
    volume_m3 numeric(15,4) NOT NULL,
    unit_price numeric(15,4) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    stock_lot_id uuid,
    CONSTRAINT ck_sale_item_scoped_lot CHECK (((organization_id IS NULL) OR (stock_lot_id IS NOT NULL))),
    CONSTRAINT ck_sale_item_scoped_parent CHECK (((organization_id IS NULL) OR ((sale_id IS NOT NULL) AND (weight_kg > (0)::numeric) AND (volume_m3 >= (0)::numeric) AND (unit_price >= (0)::numeric))))
);

CREATE TABLE sorted_item (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sorting_id uuid,
    input_item_id uuid,
    material_subtype_id uuid NOT NULL,
    weight_kg numeric(15,4) NOT NULL,
    volume_m3 numeric(15,4) NOT NULL,
    reject_weight_kg numeric(15,4) DEFAULT 0,
    reject_volume_m3 numeric(15,4) DEFAULT 0,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    destination_type character varying(50),
    destination_id uuid,
    organization_id uuid,
    CONSTRAINT ck_sorted_scoped_source CHECK (((organization_id IS NULL) OR ((sorting_id IS NOT NULL) AND (input_item_id IS NOT NULL) AND (weight_kg > (0)::numeric) AND (volume_m3 >= (0)::numeric) AND (reject_weight_kg >= (0)::numeric) AND (reject_weight_kg <= weight_kg) AND (reject_volume_m3 >= (0)::numeric) AND (reject_volume_m3 <= volume_m3))))
);

CREATE TABLE sorting (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sorting_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    sorting_type character varying(50),
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid,
    status character varying(20) DEFAULT 'POSTED'::character varying NOT NULL,
    CONSTRAINT sorting_status_check CHECK (status IN ('POSTED','REVERSED'))
);

CREATE TABLE stock_lot (
    id uuid NOT NULL,
    organization_id uuid NOT NULL,
    material_subtype_id uuid NOT NULL,
    sorted_item_id uuid,
    pressed_bale_id uuid,
    available_weight_kg numeric(15,4) NOT NULL,
    available_volume_m3 numeric(15,4) NOT NULL,
    original_weight_kg numeric(15,4) NOT NULL,
    original_volume_m3 numeric(15,4) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT stock_lot_available_volume_m3_check CHECK ((available_volume_m3 >= (0)::numeric)),
    CONSTRAINT stock_lot_available_weight_kg_check CHECK ((available_weight_kg >= (0)::numeric)),
    CONSTRAINT stock_lot_check CHECK ((num_nonnulls(sorted_item_id, pressed_bale_id) = 1)),
    CONSTRAINT stock_lot_check1 CHECK (((available_weight_kg <= original_weight_kg) AND (available_volume_m3 <= original_volume_m3))),
    CONSTRAINT stock_lot_original_volume_m3_check CHECK ((original_volume_m3 >= (0)::numeric)),
    CONSTRAINT stock_lot_original_weight_kg_check CHECK ((original_weight_kg >= (0)::numeric))
);

CREATE TABLE stock_movement (
    id uuid NOT NULL,
    organization_id uuid NOT NULL,
    operation_id uuid NOT NULL,
    material_subtype_id uuid NOT NULL,
    weight_delta_kg numeric(15,4) NOT NULL,
    volume_delta_m3 numeric(15,4) NOT NULL
);

CREATE TABLE stock_operation (
    id uuid NOT NULL,
    organization_id uuid NOT NULL,
    actor_id uuid NOT NULL,
    kind character varying(20) NOT NULL,
    sorting_id uuid,
    pressing_id uuid,
    sale_id uuid,
    reversal_of uuid,
    occurred_at timestamp with time zone NOT NULL,
    recorded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT stock_operation_check CHECK (((((kind)::text = 'SORTING'::text) AND (sorting_id IS NOT NULL) AND (num_nonnulls(pressing_id, sale_id, reversal_of) = 0)) OR (((kind)::text = 'PRESSING'::text) AND (pressing_id IS NOT NULL) AND (num_nonnulls(sorting_id, sale_id, reversal_of) = 0)) OR (((kind)::text = 'SALE'::text) AND (sale_id IS NOT NULL) AND (num_nonnulls(sorting_id, pressing_id, reversal_of) = 0)) OR (((kind)::text = 'REVERSAL'::text) AND (reversal_of IS NOT NULL) AND (num_nonnulls(sorting_id, pressing_id, sale_id) = 0)))),
    CONSTRAINT stock_operation_kind_check CHECK (kind IN ('SORTING','PRESSING','SALE','REVERSAL'))
);

CREATE TABLE team_member (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    role character varying(50),
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid
);

CREATE TABLE users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    full_name character varying(255) NOT NULL,
    user_role character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicle (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    license_plate character varying(20) NOT NULL,
    model character varying(100),
    is_active boolean DEFAULT true NOT NULL,
    creator_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    organization_id uuid
);

ALTER TABLE ONLY attachment_file_deletion
    ADD CONSTRAINT attachment_file_deletion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY attachment_file_deletion
    ADD CONSTRAINT attachment_file_deletion_storage_path_key UNIQUE (storage_path);

ALTER TABLE ONLY attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY buyer
    ADD CONSTRAINT buyer_pkey PRIMARY KEY (id);

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY collection_team
    ADD CONSTRAINT collection_team_pkey PRIMARY KEY (collection_id, team_member_id);

ALTER TABLE ONLY command_receipt
    ADD CONSTRAINT command_receipt_organization_id_request_key_key UNIQUE (organization_id, request_key);

ALTER TABLE ONLY command_receipt
    ADD CONSTRAINT command_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY donation
    ADD CONSTRAINT donation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY donor
    ADD CONSTRAINT donor_pkey PRIMARY KEY (id);

ALTER TABLE ONLY input_item
    ADD CONSTRAINT input_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY inventory_balance
    ADD CONSTRAINT inventory_balance_pkey PRIMARY KEY (id);

ALTER TABLE ONLY inventory_log
    ADD CONSTRAINT inventory_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY material_category
    ADD CONSTRAINT material_category_pkey PRIMARY KEY (id);

ALTER TABLE ONLY material_subtype
    ADD CONSTRAINT material_subtype_pkey PRIMARY KEY (id);

ALTER TABLE ONLY material_type
    ADD CONSTRAINT material_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY organization_access_audit
    ADD CONSTRAINT organization_access_audit_pkey PRIMARY KEY (id);

ALTER TABLE ONLY organization_membership
    ADD CONSTRAINT organization_membership_pkey PRIMARY KEY (organization_id, user_id);

ALTER TABLE ONLY organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT pressed_bale_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pressing
    ADD CONSTRAINT pressing_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT sale_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT sorted_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sorting
    ADD CONSTRAINT sorting_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_organization_id_id_key UNIQUE (organization_id, id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_pressed_bale_id_key UNIQUE (pressed_bale_id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_sorted_item_id_key UNIQUE (sorted_item_id);

ALTER TABLE ONLY stock_movement
    ADD CONSTRAINT stock_movement_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_id_key UNIQUE (organization_id, id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_pressing_id_key UNIQUE (organization_id, pressing_id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_sale_id_key UNIQUE (organization_id, sale_id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_sorting_id_key UNIQUE (organization_id, sorting_id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_reversal_of_key UNIQUE (reversal_of);

ALTER TABLE ONLY team_member
    ADD CONSTRAINT team_member_pkey PRIMARY KEY (id);

ALTER TABLE ONLY attachment
    ADD CONSTRAINT uq_attachment_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY buyer
    ADD CONSTRAINT uq_buyer_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY collection
    ADD CONSTRAINT uq_collection_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY donation
    ADD CONSTRAINT uq_donation_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY donor
    ADD CONSTRAINT uq_donor_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY input_item
    ADD CONSTRAINT uq_input_item_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY inventory_balance
    ADD CONSTRAINT uq_inventory_balance_material_subtype UNIQUE (material_subtype_id);

ALTER TABLE ONLY inventory_balance
    ADD CONSTRAINT uq_inventory_balance_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY inventory_log
    ADD CONSTRAINT uq_inventory_log_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY material_category
    ADD CONSTRAINT uq_material_category_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY material_subtype
    ADD CONSTRAINT uq_material_subtype_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY material_type
    ADD CONSTRAINT uq_material_type_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT uq_pressed_bale_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY pressing
    ADD CONSTRAINT uq_pressing_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT uq_sale_item_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY sale
    ADD CONSTRAINT uq_sale_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT uq_sorted_item_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY sorting
    ADD CONSTRAINT uq_sorting_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY team_member
    ADD CONSTRAINT uq_team_member_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY vehicle
    ADD CONSTRAINT uq_vehicle_scope UNIQUE (organization_id, id);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY vehicle
    ADD CONSTRAINT vehicle_pkey PRIMARY KEY (id);

CREATE INDEX ix_attachment_file_deletion_due ON attachment_file_deletion USING btree (next_attempt_at, created_at);

CREATE INDEX ix_attachment_organization ON attachment USING btree (organization_id);

CREATE INDEX ix_buyer_organization ON buyer USING btree (organization_id);

CREATE INDEX ix_collection_organization ON collection USING btree (organization_id);

CREATE INDEX ix_donation_organization ON donation USING btree (organization_id);

CREATE INDEX ix_donor_organization ON donor USING btree (organization_id);

CREATE INDEX ix_input_item_organization ON input_item USING btree (organization_id);

CREATE INDEX ix_inventory_balance_organization ON inventory_balance USING btree (organization_id);

CREATE INDEX ix_inventory_log_organization ON inventory_log USING btree (organization_id);

CREATE INDEX ix_material_category_organization ON material_category USING btree (organization_id);

CREATE INDEX ix_material_subtype_organization ON material_subtype USING btree (organization_id);

CREATE INDEX ix_material_type_organization ON material_type USING btree (organization_id);

CREATE INDEX ix_organization_access_audit_org ON organization_access_audit USING btree (organization_id, recorded_at);

CREATE INDEX ix_organization_membership_user ON organization_membership USING btree (user_id, organization_id) WHERE is_active;

CREATE INDEX ix_pressed_bale_organization ON pressed_bale USING btree (organization_id);

CREATE INDEX ix_pressing_organization ON pressing USING btree (organization_id);

CREATE INDEX ix_sale_item_organization ON sale_item USING btree (organization_id);

CREATE INDEX ix_sale_organization ON sale USING btree (organization_id);

CREATE INDEX ix_sorted_item_organization ON sorted_item USING btree (organization_id);

CREATE INDEX ix_sorting_organization ON sorting USING btree (organization_id);

CREATE INDEX ix_stock_lot_scope ON stock_lot USING btree (organization_id, material_subtype_id, created_at);

CREATE INDEX ix_stock_movement_scope_material ON stock_movement USING btree (organization_id, material_subtype_id);

CREATE INDEX ix_team_member_organization ON team_member USING btree (organization_id);

CREATE INDEX ix_vehicle_organization ON vehicle USING btree (organization_id);

CREATE UNIQUE INDEX uq_buyer_document_scope ON buyer USING btree (organization_id, document) WHERE ((organization_id IS NOT NULL) AND (document IS NOT NULL));

CREATE UNIQUE INDEX uq_category_name_scope ON material_category USING btree (organization_id, lower(btrim((name)::text))) WHERE (organization_id IS NOT NULL);

CREATE UNIQUE INDEX uq_donor_document_scope ON donor USING btree (organization_id, document) WHERE (organization_id IS NOT NULL);

CREATE UNIQUE INDEX uq_subtype_name_scope ON material_subtype USING btree (organization_id, type_id, lower(btrim((name)::text))) WHERE (organization_id IS NOT NULL);

CREATE UNIQUE INDEX uq_type_name_scope ON material_type USING btree (organization_id, category_id, lower(btrim((name)::text))) WHERE (organization_id IS NOT NULL);

CREATE UNIQUE INDEX uq_vehicle_plate_scope ON vehicle USING btree (organization_id, upper((license_plate)::text)) WHERE (organization_id IS NOT NULL);

CREATE TRIGGER collection_team_scope BEFORE INSERT OR UPDATE ON collection_team FOR EACH ROW EXECUTE FUNCTION validate_collection_team_scope();

CREATE TRIGGER stock_movement_immutable BEFORE DELETE OR UPDATE ON stock_movement FOR EACH ROW EXECUTE FUNCTION reject_stock_history_mutation();

CREATE TRIGGER stock_operation_immutable BEFORE DELETE OR UPDATE ON stock_operation FOR EACH ROW EXECUTE FUNCTION reject_stock_history_mutation();

ALTER TABLE ONLY attachment
    ADD CONSTRAINT attachment_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY attachment
    ADD CONSTRAINT attachment_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY buyer
    ADD CONSTRAINT buyer_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY buyer
    ADD CONSTRAINT buyer_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_collection_diary_id_fkey FOREIGN KEY (collection_diary_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES team_member(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_mtr_destinator_id_fkey FOREIGN KEY (mtr_destinator_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_mtr_generator_id_fkey FOREIGN KEY (mtr_generator_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection_team
    ADD CONSTRAINT collection_team_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES collection(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY collection_team
    ADD CONSTRAINT collection_team_team_member_id_fkey FOREIGN KEY (team_member_id) REFERENCES team_member(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT collection_vehicle_id_fkey FOREIGN KEY (vehicle_id) REFERENCES vehicle(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY command_receipt
    ADD CONSTRAINT command_receipt_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES users(id);

ALTER TABLE ONLY command_receipt
    ADD CONSTRAINT command_receipt_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ONLY donation
    ADD CONSTRAINT donation_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY donation
    ADD CONSTRAINT donation_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY donation
    ADD CONSTRAINT donation_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY donation
    ADD CONSTRAINT donation_proof_attachment_id_fkey FOREIGN KEY (proof_attachment_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY donor
    ADD CONSTRAINT donor_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY donor
    ADD CONSTRAINT donor_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT fk_collection_collection_diary_id_scope FOREIGN KEY (organization_id, collection_diary_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT fk_collection_driver_id_scope FOREIGN KEY (organization_id, driver_id) REFERENCES team_member(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT fk_collection_mtr_destinator_id_scope FOREIGN KEY (organization_id, mtr_destinator_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT fk_collection_mtr_generator_id_scope FOREIGN KEY (organization_id, mtr_generator_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY collection
    ADD CONSTRAINT fk_collection_vehicle_id_scope FOREIGN KEY (organization_id, vehicle_id) REFERENCES vehicle(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY donation
    ADD CONSTRAINT fk_donation_donor_id_scope FOREIGN KEY (organization_id, donor_id) REFERENCES donor(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY donation
    ADD CONSTRAINT fk_donation_proof_attachment_id_scope FOREIGN KEY (organization_id, proof_attachment_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT fk_input_item_collection_id_scope FOREIGN KEY (organization_id, collection_id) REFERENCES collection(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT fk_input_item_donation_id_scope FOREIGN KEY (organization_id, donation_id) REFERENCES donation(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT fk_input_item_material_subtype_id_scope FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY inventory_balance
    ADD CONSTRAINT fk_inventory_balance_material_subtype_id_scope FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY inventory_log
    ADD CONSTRAINT fk_inventory_log_material_subtype_id_scope FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY material_subtype
    ADD CONSTRAINT fk_material_subtype_type_id_scope FOREIGN KEY (organization_id, type_id) REFERENCES material_type(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY material_type
    ADD CONSTRAINT fk_material_type_category_id_scope FOREIGN KEY (organization_id, category_id) REFERENCES material_category(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT fk_pressed_bale_material_subtype_id_scope FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT fk_pressed_bale_pressing_id_scope FOREIGN KEY (organization_id, pressing_id) REFERENCES pressing(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT fk_pressed_bale_sorted_item_id_scope FOREIGN KEY (organization_id, sorted_item_id) REFERENCES sorted_item(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT fk_sale_buyer_id_scope FOREIGN KEY (organization_id, buyer_id) REFERENCES buyer(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT fk_sale_cdf_attachment_id_scope FOREIGN KEY (organization_id, cdf_attachment_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT fk_sale_item_lot_scope FOREIGN KEY (organization_id, stock_lot_id) REFERENCES stock_lot(organization_id, id);

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT fk_sale_item_material_subtype_id_scope FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT fk_sale_item_sale_id_scope FOREIGN KEY (organization_id, sale_id) REFERENCES sale(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT fk_sale_mtr_attachment_id_scope FOREIGN KEY (organization_id, mtr_attachment_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT fk_sale_nfe_attachment_id_scope FOREIGN KEY (organization_id, nfe_attachment_id) REFERENCES attachment(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT fk_sorted_item_input_item_id_scope FOREIGN KEY (organization_id, input_item_id) REFERENCES input_item(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT fk_sorted_item_material_subtype_id_scope FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT fk_sorted_item_sorting_id_scope FOREIGN KEY (organization_id, sorting_id) REFERENCES sorting(organization_id, id) ON DELETE RESTRICT;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT input_item_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES collection(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT input_item_donation_id_fkey FOREIGN KEY (donation_id) REFERENCES donation(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT input_item_material_subtype_id_fkey FOREIGN KEY (material_subtype_id) REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY input_item
    ADD CONSTRAINT input_item_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY inventory_balance
    ADD CONSTRAINT inventory_balance_material_subtype_id_fkey FOREIGN KEY (material_subtype_id) REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY inventory_balance
    ADD CONSTRAINT inventory_balance_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY inventory_log
    ADD CONSTRAINT inventory_log_material_subtype_id_fkey FOREIGN KEY (material_subtype_id) REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY inventory_log
    ADD CONSTRAINT inventory_log_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY material_category
    ADD CONSTRAINT material_category_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY material_category
    ADD CONSTRAINT material_category_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY material_subtype
    ADD CONSTRAINT material_subtype_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY material_subtype
    ADD CONSTRAINT material_subtype_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY material_subtype
    ADD CONSTRAINT material_subtype_type_id_fkey FOREIGN KEY (type_id) REFERENCES material_type(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY material_type
    ADD CONSTRAINT material_type_category_id_fkey FOREIGN KEY (category_id) REFERENCES material_category(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY material_type
    ADD CONSTRAINT material_type_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY material_type
    ADD CONSTRAINT material_type_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization_access_audit
    ADD CONSTRAINT organization_access_audit_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization_access_audit
    ADD CONSTRAINT organization_access_audit_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization_access_audit
    ADD CONSTRAINT organization_access_audit_subject_user_id_fkey FOREIGN KEY (subject_user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization
    ADD CONSTRAINT organization_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization_membership
    ADD CONSTRAINT organization_membership_granted_by_fkey FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization_membership
    ADD CONSTRAINT organization_membership_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY organization_membership
    ADD CONSTRAINT organization_membership_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT pressed_bale_material_subtype_id_fkey FOREIGN KEY (material_subtype_id) REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT pressed_bale_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT pressed_bale_pressing_id_fkey FOREIGN KEY (pressing_id) REFERENCES pressing(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY pressed_bale
    ADD CONSTRAINT pressed_bale_sorted_item_id_fkey FOREIGN KEY (sorted_item_id) REFERENCES sorted_item(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY pressing
    ADD CONSTRAINT pressing_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY pressing
    ADD CONSTRAINT pressing_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_buyer_id_fkey FOREIGN KEY (buyer_id) REFERENCES buyer(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_cdf_attachment_id_fkey FOREIGN KEY (cdf_attachment_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT sale_item_material_subtype_id_fkey FOREIGN KEY (material_subtype_id) REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT sale_item_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY sale_item
    ADD CONSTRAINT sale_item_sale_id_fkey FOREIGN KEY (sale_id) REFERENCES sale(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_mtr_attachment_id_fkey FOREIGN KEY (mtr_attachment_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_nfe_attachment_id_fkey FOREIGN KEY (nfe_attachment_id) REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sale
    ADD CONSTRAINT sale_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT sorted_item_input_item_id_fkey FOREIGN KEY (input_item_id) REFERENCES input_item(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT sorted_item_material_subtype_id_fkey FOREIGN KEY (material_subtype_id) REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT sorted_item_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY sorted_item
    ADD CONSTRAINT sorted_item_sorting_id_fkey FOREIGN KEY (sorting_id) REFERENCES sorting(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY sorting
    ADD CONSTRAINT sorting_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sorting
    ADD CONSTRAINT sorting_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_organization_id_material_subtype_id_fkey FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_organization_id_pressed_bale_id_fkey FOREIGN KEY (organization_id, pressed_bale_id) REFERENCES pressed_bale(organization_id, id);

ALTER TABLE ONLY stock_lot
    ADD CONSTRAINT stock_lot_organization_id_sorted_item_id_fkey FOREIGN KEY (organization_id, sorted_item_id) REFERENCES sorted_item(organization_id, id);

ALTER TABLE ONLY stock_movement
    ADD CONSTRAINT stock_movement_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ONLY stock_movement
    ADD CONSTRAINT stock_movement_organization_id_material_subtype_id_fkey FOREIGN KEY (organization_id, material_subtype_id) REFERENCES material_subtype(organization_id, id);

ALTER TABLE ONLY stock_movement
    ADD CONSTRAINT stock_movement_organization_id_operation_id_fkey FOREIGN KEY (organization_id, operation_id) REFERENCES stock_operation(organization_id, id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES users(id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_pressing_id_fkey FOREIGN KEY (organization_id, pressing_id) REFERENCES pressing(organization_id, id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_reversal_of_fkey FOREIGN KEY (organization_id, reversal_of) REFERENCES stock_operation(organization_id, id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_sale_id_fkey FOREIGN KEY (organization_id, sale_id) REFERENCES sale(organization_id, id);

ALTER TABLE ONLY stock_operation
    ADD CONSTRAINT stock_operation_organization_id_sorting_id_fkey FOREIGN KEY (organization_id, sorting_id) REFERENCES sorting(organization_id, id);

ALTER TABLE ONLY team_member
    ADD CONSTRAINT team_member_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY team_member
    ADD CONSTRAINT team_member_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;

ALTER TABLE ONLY vehicle
    ADD CONSTRAINT vehicle_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY vehicle
    ADD CONSTRAINT vehicle_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE RESTRICT;
