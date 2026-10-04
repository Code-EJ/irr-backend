-- Additive operational Routes model. Decision: ADR-0019; no reset or V1/V2/V3 rewrite.
-- Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
CREATE TABLE municipality (
    id uuid PRIMARY KEY,
    ibge_code varchar(7) NOT NULL UNIQUE CHECK (ibge_code ~ '^[0-9]{7}$'),
    name varchar(150) NOT NULL CHECK (length(btrim(name)) > 0),
    state_code varchar(2) NOT NULL CHECK (state_code IN ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    CONSTRAINT municipality_uf_prefix CHECK (substring(ibge_code from 1 for 2) = CASE state_code WHEN 'RO' THEN '11' WHEN 'AC' THEN '12' WHEN 'AM' THEN '13' WHEN 'RR' THEN '14' WHEN 'PA' THEN '15' WHEN 'AP' THEN '16' WHEN 'TO' THEN '17' WHEN 'MA' THEN '21' WHEN 'PI' THEN '22' WHEN 'CE' THEN '23' WHEN 'RN' THEN '24' WHEN 'PB' THEN '25' WHEN 'PE' THEN '26' WHEN 'AL' THEN '27' WHEN 'SE' THEN '28' WHEN 'BA' THEN '29' WHEN 'MG' THEN '31' WHEN 'ES' THEN '32' WHEN 'RJ' THEN '33' WHEN 'SP' THEN '35' WHEN 'PR' THEN '41' WHEN 'SC' THEN '42' WHEN 'RS' THEN '43' WHEN 'MS' THEN '50' WHEN 'MT' THEN '51' WHEN 'GO' THEN '52' WHEN 'DF' THEN '53' ELSE '00' END),
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE neighborhood (
    id uuid PRIMARY KEY,
    municipality_id uuid NOT NULL REFERENCES municipality(id),
    name varchar(150) NOT NULL CHECK (length(btrim(name)) > 0),
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (municipality_id,id)
);
CREATE UNIQUE INDEX neighborhood_name_unique ON neighborhood(municipality_id,lower(btrim(name)));
CREATE TABLE route (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organization(id),
    municipality_id uuid NOT NULL REFERENCES municipality(id),
    name varchar(150) NOT NULL CHECK (length(btrim(name)) > 0),
    status varchar(12) NOT NULL CHECK (status IN ('DRAFT','ACTIVE')),
    is_active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    planned_distance_km numeric(12,3) CHECK (planned_distance_km >= 0),
    planned_duration_minutes integer CHECK (planned_duration_minutes > 0),
    created_by uuid NOT NULL REFERENCES users(id),
    updated_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id,id),
    UNIQUE (organization_id,id,municipality_id)
);
CREATE UNIQUE INDEX route_active_name_unique ON route(organization_id,municipality_id,lower(btrim(name))) WHERE is_active;
CREATE INDEX route_scope_page ON route(organization_id,is_active,created_at,id);
CREATE TABLE route_area (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    route_id uuid NOT NULL,
    municipality_id uuid NOT NULL,
    neighborhood_id uuid NOT NULL,
    sequence integer NOT NULL CHECK (sequence > 0),
    street varchar(255) CHECK (street IS NULL OR length(btrim(street)) > 0),
    is_active boolean NOT NULL DEFAULT true,
    FOREIGN KEY (organization_id,route_id,municipality_id) REFERENCES route(organization_id,id,municipality_id),
    FOREIGN KEY (municipality_id,neighborhood_id) REFERENCES neighborhood(municipality_id,id),
    UNIQUE (organization_id,route_id,id)
);
CREATE UNIQUE INDEX route_area_sequence_unique ON route_area(route_id,sequence) WHERE is_active;
CREATE UNIQUE INDEX route_area_coverage_unique ON route_area(route_id,neighborhood_id,lower(btrim(COALESCE(street,'')))) WHERE is_active;
CREATE TABLE route_execution (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    route_id uuid NOT NULL,
    route_version bigint NOT NULL CHECK (route_version >= 0),
    route_name varchar(150) NOT NULL,
    municipality_id uuid NOT NULL,
    municipality_name varchar(150) NOT NULL,
    municipality_ibge_code varchar(7) NOT NULL,
    municipality_state_code varchar(2) NOT NULL,
    planned_distance_km numeric(12,3) CHECK (planned_distance_km >= 0),
    planned_duration_minutes integer CHECK (planned_duration_minutes > 0),
    planned_start_at timestamptz NOT NULL,
    status varchar(15) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    vehicle_id uuid NOT NULL,
    vehicle_license_plate varchar(20) NOT NULL,
    driver_id uuid NOT NULL,
    driver_name varchar(255) NOT NULL,
    started_at timestamptz,
    finished_at timestamptz,
    cancelled_at timestamptz,
    cancellation_reason varchar(500),
    distance_km numeric(12,3) CHECK (distance_km >= 0),
    created_by uuid NOT NULL REFERENCES users(id),
    updated_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id,route_id,municipality_id) REFERENCES route(organization_id,id,municipality_id),
    FOREIGN KEY (organization_id,vehicle_id) REFERENCES vehicle(organization_id,id),
    FOREIGN KEY (organization_id,driver_id) REFERENCES team_member(organization_id,id),
    UNIQUE (organization_id,id),
    UNIQUE (organization_id,id,route_id),
    CONSTRAINT route_execution_times CHECK (
      (status='PLANNED' AND started_at IS NULL AND finished_at IS NULL AND cancelled_at IS NULL) OR
      (status='IN_PROGRESS' AND started_at IS NOT NULL AND finished_at IS NULL AND cancelled_at IS NULL) OR
      (status='COMPLETED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND finished_at>=started_at AND cancelled_at IS NULL) OR
      (status='CANCELLED' AND finished_at IS NULL AND cancelled_at IS NOT NULL AND (started_at IS NULL OR cancelled_at>=started_at) AND cancellation_reason IS NOT NULL AND length(btrim(cancellation_reason))>0)
    )
);
CREATE INDEX route_execution_scope_page ON route_execution(organization_id,route_id,status,planned_start_at,id);
CREATE TABLE route_execution_area (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    execution_id uuid NOT NULL,
    route_id uuid NOT NULL,
    source_area_id uuid NOT NULL,
    neighborhood_id uuid NOT NULL REFERENCES neighborhood(id),
    neighborhood_name varchar(150) NOT NULL,
    sequence integer NOT NULL CHECK (sequence > 0),
    street varchar(255),
    covered boolean NOT NULL DEFAULT false,
    FOREIGN KEY (organization_id,execution_id,route_id) REFERENCES route_execution(organization_id,id,route_id),
    FOREIGN KEY (organization_id,route_id,source_area_id) REFERENCES route_area(organization_id,route_id,id),
    UNIQUE (execution_id,sequence)
);
CREATE TABLE route_execution_team (
    organization_id uuid NOT NULL,
    execution_id uuid NOT NULL,
    team_member_id uuid NOT NULL,
    member_name varchar(255) NOT NULL,
    member_role varchar(50),
    PRIMARY KEY (execution_id,team_member_id),
    FOREIGN KEY (organization_id,execution_id) REFERENCES route_execution(organization_id,id),
    FOREIGN KEY (organization_id,team_member_id) REFERENCES team_member(organization_id,id)
);
CREATE TABLE route_event (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    route_id uuid NOT NULL,
    execution_id uuid,
    actor_id uuid NOT NULL REFERENCES users(id),
    action varchar(20) NOT NULL CHECK (action IN ('CREATE','UPDATE','DEACTIVATE','PLAN','START','COMPLETE','CANCEL','COVERAGE')),
    version bigint NOT NULL CHECK (version >= 0),
    recorded_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id,route_id) REFERENCES route(organization_id,id),
    FOREIGN KEY (organization_id,execution_id,route_id) REFERENCES route_execution(organization_id,id,route_id)
);
CREATE INDEX route_event_history ON route_event(organization_id,route_id,recorded_at,id);
ALTER TABLE collection ADD COLUMN route_execution_id uuid;
ALTER TABLE collection ADD CONSTRAINT collection_route_execution_scope FOREIGN KEY (organization_id,route_execution_id) REFERENCES route_execution(organization_id,id);
CREATE INDEX collection_execution_idx ON collection(organization_id,route_execution_id) WHERE route_execution_id IS NOT NULL;

CREATE FUNCTION guard_route_execution_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Route execution history cannot be deleted' USING ERRCODE='23514'; END IF;
 IF OLD.status IN ('COMPLETED','CANCELLED') THEN RAISE EXCEPTION 'Terminal route executions are immutable' USING ERRCODE='23514'; END IF;
 IF (OLD.status='PLANNED' AND NEW.status NOT IN ('PLANNED','IN_PROGRESS','CANCELLED')) OR (OLD.status='IN_PROGRESS' AND NEW.status NOT IN ('IN_PROGRESS','COMPLETED','CANCELLED')) OR (OLD.started_at IS NOT NULL AND NEW.started_at IS DISTINCT FROM OLD.started_at)
 THEN RAISE EXCEPTION 'Invalid route execution transition' USING ERRCODE='23514'; END IF;
 IF ROW(NEW.organization_id,NEW.route_id,NEW.route_version,NEW.route_name,NEW.municipality_id,NEW.municipality_name,NEW.municipality_ibge_code,NEW.municipality_state_code,NEW.planned_distance_km,NEW.planned_duration_minutes,NEW.planned_start_at,NEW.vehicle_id,NEW.vehicle_license_plate,NEW.driver_id,NEW.driver_name,NEW.created_by,NEW.created_at)
 IS DISTINCT FROM ROW(OLD.organization_id,OLD.route_id,OLD.route_version,OLD.route_name,OLD.municipality_id,OLD.municipality_name,OLD.municipality_ibge_code,OLD.municipality_state_code,OLD.planned_distance_km,OLD.planned_duration_minutes,OLD.planned_start_at,OLD.vehicle_id,OLD.vehicle_license_plate,OLD.driver_id,OLD.driver_name,OLD.created_by,OLD.created_at)
 THEN RAISE EXCEPTION 'Route execution context is immutable' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER route_execution_history BEFORE UPDATE OR DELETE ON route_execution FOR EACH ROW EXECUTE FUNCTION guard_route_execution_history();
CREATE FUNCTION guard_execution_area_history() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE state varchar;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Execution coverage history cannot be deleted' USING ERRCODE='23514'; END IF;
 SELECT status INTO state FROM route_execution WHERE id=NEW.execution_id;
 IF TG_OP='INSERT' THEN
  IF state<>'PLANNED' THEN RAISE EXCEPTION 'Snapshot areas require a planned execution' USING ERRCODE='23514'; END IF;
 ELSE
  IF state<>'IN_PROGRESS' OR ROW(NEW.id,NEW.organization_id,NEW.execution_id,NEW.route_id,NEW.source_area_id,NEW.neighborhood_id,NEW.neighborhood_name,NEW.sequence,NEW.street)
  IS DISTINCT FROM ROW(OLD.id,OLD.organization_id,OLD.execution_id,OLD.route_id,OLD.source_area_id,OLD.neighborhood_id,OLD.neighborhood_name,OLD.sequence,OLD.street)
  THEN RAISE EXCEPTION 'Only in-progress coverage state can change' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER execution_area_history BEFORE INSERT OR UPDATE OR DELETE ON route_execution_area FOR EACH ROW EXECUTE FUNCTION guard_execution_area_history();
CREATE FUNCTION reject_route_history_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Route historical records cannot be rewritten or deleted' USING ERRCODE='23514'; END; $$;
CREATE TRIGGER execution_team_history BEFORE UPDATE OR DELETE ON route_execution_team FOR EACH ROW EXECUTE FUNCTION reject_route_history_change();
CREATE FUNCTION validate_execution_team_insert() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM route_execution WHERE id=NEW.execution_id AND organization_id=NEW.organization_id AND status='PLANNED' AND driver_id<>NEW.team_member_id)
 THEN RAISE EXCEPTION 'Crew snapshot requires a planned execution and a distinct driver' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER execution_team_insert BEFORE INSERT ON route_execution_team FOR EACH ROW EXECUTE FUNCTION validate_execution_team_insert();
CREATE TRIGGER route_event_history BEFORE UPDATE OR DELETE ON route_event FOR EACH ROW EXECUTE FUNCTION reject_route_history_change();
CREATE FUNCTION validate_collection_execution() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE execution route_execution%ROWTYPE;
BEGIN
 IF TG_OP='UPDATE' AND OLD.route_execution_id IS NOT NULL AND NEW.route_execution_id IS DISTINCT FROM OLD.route_execution_id
 THEN RAISE EXCEPTION 'Collection execution reference is immutable' USING ERRCODE='23514'; END IF;
 IF NEW.route_execution_id IS NULL THEN RETURN NEW; END IF;
 SELECT * INTO execution FROM route_execution WHERE id=NEW.route_execution_id AND organization_id=NEW.organization_id;
 IF NOT FOUND OR execution.status<>'COMPLETED' OR NEW.vehicle_id<>execution.vehicle_id OR NEW.driver_id<>execution.driver_id
 OR NEW.departure_at IS DISTINCT FROM execution.started_at OR NEW.arrival_at IS DISTINCT FROM execution.finished_at OR NEW.distance_km IS DISTINCT FROM execution.distance_km
 THEN RAISE EXCEPTION 'Collection must retain matching completed execution facts' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER collection_execution_integrity BEFORE INSERT OR UPDATE ON collection FOR EACH ROW EXECUTE FUNCTION validate_collection_execution();
CREATE FUNCTION validate_collection_execution_team() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_collection_id uuid; v_execution_id uuid;
BEGIN
 IF TG_TABLE_NAME='collection' THEN v_collection_id=NEW.id;
 ELSIF TG_OP='DELETE' THEN v_collection_id=OLD.collection_id;
 ELSE v_collection_id=NEW.collection_id; END IF;
 SELECT c.route_execution_id INTO v_execution_id FROM collection c WHERE c.id=v_collection_id;
 IF v_execution_id IS NOT NULL AND (
 EXISTS(SELECT ct.team_member_id FROM collection_team ct WHERE ct.collection_id=v_collection_id EXCEPT SELECT t.team_member_id FROM route_execution_team t WHERE t.execution_id=v_execution_id)
 OR EXISTS(SELECT t.team_member_id FROM route_execution_team t WHERE t.execution_id=v_execution_id EXCEPT SELECT ct.team_member_id FROM collection_team ct WHERE ct.collection_id=v_collection_id))
 THEN RAISE EXCEPTION 'Collection crew must match its execution snapshot' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END; $$;
CREATE CONSTRAINT TRIGGER collection_execution_team_integrity AFTER INSERT OR UPDATE ON collection DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION validate_collection_execution_team();
CREATE CONSTRAINT TRIGGER collection_team_execution_integrity AFTER INSERT OR UPDATE OR DELETE ON collection_team DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION validate_collection_execution_team();
