-- Additive intake details and explicit organization lifecycle auditing.
-- Author: Enzo Ribas (https://github.com/oEnzoRibas)
ALTER TABLE donor ADD COLUMN address_line1 VARCHAR(255), ADD COLUMN address_line2 VARCHAR(255),
 ADD COLUMN address_city VARCHAR(100), ADD COLUMN address_region VARCHAR(100),
 ADD COLUMN address_postal_code VARCHAR(20), ADD COLUMN address_country_code VARCHAR(2);
ALTER TABLE donor ADD CONSTRAINT ck_donor_address_complete CHECK
 (num_nonnulls(address_line1,address_line2,address_city,address_region,address_postal_code,address_country_code)=0
 OR (address_line1 IS NOT NULL AND length(btrim(address_line1))>0 AND address_city IS NOT NULL AND length(btrim(address_city))>0
 AND address_region IS NOT NULL AND length(btrim(address_region))>0 AND address_postal_code IS NOT NULL AND length(btrim(address_postal_code))>0
 AND address_country_code IS NOT NULL AND address_country_code ~ '^[A-Z]{2}$'));
ALTER TABLE collection ADD COLUMN route_description VARCHAR(2000), ADD COLUMN departure_at TIMESTAMPTZ,
 ADD COLUMN arrival_at TIMESTAMPTZ, ADD COLUMN distance_km NUMERIC(12,3);
ALTER TABLE collection ADD CONSTRAINT ck_collection_schedule CHECK
 ((departure_at IS NULL AND arrival_at IS NULL) OR (departure_at IS NOT NULL AND arrival_at IS NOT NULL AND arrival_at>=departure_at));
ALTER TABLE collection ADD CONSTRAINT ck_collection_distance CHECK (distance_km IS NULL OR distance_km>=0);
ALTER TABLE organization_access_audit DROP CONSTRAINT organization_access_audit_action_check;
ALTER TABLE organization_access_audit ADD CONSTRAINT organization_access_audit_action_check CHECK(action IN ('CREATE','GRANT','REVOKE','UPDATE','DEACTIVATE'));
ALTER TABLE organization_access_audit ADD COLUMN details JSONB NOT NULL DEFAULT '{}'::jsonb;
