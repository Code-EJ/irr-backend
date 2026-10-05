# Relational schema and persistence topology

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Verified runtime: PostgreSQL 16, Flyway V1/V2/V3/V4, 38 application tables plus flyway_schema_history. The schema inventory below is generated from information_schema; authoritative checks, indexes and foreign keys are the immutable SQL under src/main/resources/db/migrations.

## Domain relationships

~~~mermaid
erDiagram
    organization ||--o{ organization_membership : grants
    users ||--o{ organization_membership : belongs
    users ||--o{ password_recovery_token : authorizes
    organization ||--o{ material_category : owns
    material_category ||--o{ material_type : classifies
    material_type ||--o{ material_subtype : classifies
    donor ||--o{ donation : supplies
    vehicle ||--o{ collection : transports
    collection ||--o{ input_item : receives
    donation ||--o{ input_item : receives
    input_item ||--o{ sorted_item : allocates
    sorted_item ||--o{ pressed_bale : compacts
    material_subtype ||--o{ stock_lot : identifies
    buyer ||--o{ sale : purchases
    sale ||--o{ sale_item : allocates
    stock_lot ||--o{ sale_item : supplies
    stock_operation ||--o{ stock_movement : records
~~~

The diagram summarizes relationships; it does not show every ownership/audit key. New business data always has organization_id. The final schema preserves predecessor nullability for structural equivalence; application writes always require explicit ownership. Any later import must validate ownership rather than infer it from creator_id. Input rows belong to exactly one donation or collection. Composite foreign keys prevent cross-organization references. Category/type/subtype normalization avoids repeated free-text material labels.

## Invariants and migration sequence

- Final V1 creates all 29 application tables with the complete verified constraints, indexes, functions and triggers.
- V2 adds nonnegative users.credential_version and users.must_change_password without changing existing hashes or data.
- V3 adds password_recovery_token with expiry validation, user foreign key and expiry/user indexes. Only token digests are stored.
- V1/V2/V3 remain immutable. V4 adds Routes and execution history without resetting data. Further shared changes start at V5.
- The unreleased predecessor chain is retained in Git and private backups, not replayed on fresh setup.
- FinalBaselineSchemaIT independently checks all 560 schema definitions against the predecessor.

Quantities use NUMERIC(15,4); financial totals retain their dedicated fixed precision. Raw intake creates provenance, sorting credits net output, pressing conserves mass and reduces volume, and sale posting debits allocated lots. Reversals compensate original operations; they never mutate/delete ledger history. inventory_balance is a constrained projection, not an independent stock source. inventory_log retains historical records but is not used for new ledger postings.

Shared JPA models remain in domain/models; new application modules may use JDBC records for query and audit boundaries. HTTP controllers return safe DTOs/value records. Attachment metadata never exposes storage paths. Soft-deactivated reference rows remain valid historical references and cannot be replaced by destructive cascades.


## attachment

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| file_name | character varying | NO |
| file_type | character varying | NO |
| storage_url | text | NO |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |

## attachment_file_deletion

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| storage_path | text | NO |
| created_at | timestamp with time zone | NO |
| attempts | integer | NO |
| next_attempt_at | timestamp with time zone | NO |
| last_error | character varying | YES |

## buyer

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| name | character varying | NO |
| document | character varying | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |

## collection

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| realization_date | timestamp with time zone | NO |
| total_weight_kg | numeric | NO |
| vehicle_id | uuid | NO |
| driver_id | uuid | NO |
| mtr_generator_id | uuid | YES |
| mtr_destinator_id | uuid | YES |
| collection_diary_id | uuid | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |
| route_description | character varying | YES |
| departure_at | timestamp with time zone | YES |
| arrival_at | timestamp with time zone | YES |
| distance_km | numeric | YES |

## collection_team

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| collection_id | uuid | NO |
| team_member_id | uuid | NO |

## command_receipt

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| request_key | character varying | NO |
| command_type | character varying | NO |
| request_hash | character varying | NO |
| response | jsonb | NO |
| actor_id | uuid | NO |
| recorded_at | timestamp with time zone | NO |

## donation

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| donation_date | timestamp with time zone | YES |
| total_weight_kg | numeric | NO |
| donor_id | uuid | NO |
| proof_attachment_id | uuid | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |

## donor

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| name | character varying | NO |
| document | character varying | NO |
| donor_type | character varying | NO |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |
| address_line1 | character varying | YES |
| address_line2 | character varying | YES |
| address_city | character varying | YES |
| address_region | character varying | YES |
| address_postal_code | character varying | YES |
| address_country_code | character varying | YES |

## input_item

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| collection_id | uuid | YES |
| donation_id | uuid | YES |
| material_subtype_id | uuid | NO |
| weight_kg | numeric | NO |
| volume_m3 | numeric | NO |
| is_active | boolean | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |

## inventory_balance

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| material_subtype_id | uuid | NO |
| current_weight_kg | numeric | NO |
| current_volume_m3 | numeric | NO |
| last_updated_at | timestamp with time zone | YES |
| version | bigint | NO |
| organization_id | uuid | YES |

## inventory_log

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| material_subtype_id | uuid | NO |
| quantity_kg | numeric | NO |
| quantity_m3 | numeric | NO |
| operation_type | character varying | NO |
| is_active | boolean | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |

## material_category

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| name | character varying | NO |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| version | bigint | NO |
| organization_id | uuid | YES |

## material_subtype

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| type_id | uuid | NO |
| name | character varying | NO |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| version | bigint | NO |
| organization_id | uuid | YES |

## material_type

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| category_id | uuid | NO |
| name | character varying | NO |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| version | bigint | NO |
| organization_id | uuid | YES |

## organization

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| name | character varying | NO |
| organization_type | character varying | NO |
| is_active | boolean | NO |
| created_by | uuid | NO |
| created_at | timestamp with time zone | NO |
| updated_at | timestamp with time zone | NO |

## organization_access_audit

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| actor_id | uuid | NO |
| subject_user_id | uuid | YES |
| action | character varying | NO |
| previous_role | character varying | YES |
| assigned_role | character varying | YES |
| recorded_at | timestamp with time zone | NO |
| details | jsonb | NO |

## organization_membership

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| organization_id | uuid | NO |
| user_id | uuid | NO |
| role | character varying | NO |
| is_active | boolean | NO |
| granted_by | uuid | NO |
| updated_at | timestamp with time zone | NO |

## pressed_bale

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| pressing_id | uuid | YES |
| sorted_item_id | uuid | YES |
| material_subtype_id | uuid | NO |
| weight_kg | numeric | NO |
| initial_volume_m3 | numeric | NO |
| final_volume_m3 | numeric | NO |
| is_active | boolean | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| destination_type | character varying | YES |
| destination_id | uuid | YES |
| organization_id | uuid | YES |

## pressing

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| pressing_date | timestamp with time zone | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |
| status | character varying | NO |

## sale

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| sale_date | timestamp with time zone | NO |
| buyer_id | uuid | NO |
| nfe_attachment_id | uuid | YES |
| mtr_attachment_id | uuid | YES |
| cdf_attachment_id | uuid | YES |
| total_value | numeric | NO |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |
| status | character varying | NO |
| currency | character varying | NO |
| version | bigint | NO |

## sale_item

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| sale_id | uuid | YES |
| material_subtype_id | uuid | NO |
| weight_kg | numeric | NO |
| volume_m3 | numeric | NO |
| unit_price | numeric | NO |
| is_active | boolean | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |
| stock_lot_id | uuid | YES |

## sorted_item

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| sorting_id | uuid | YES |
| input_item_id | uuid | YES |
| material_subtype_id | uuid | NO |
| weight_kg | numeric | NO |
| volume_m3 | numeric | NO |
| reject_weight_kg | numeric | YES |
| reject_volume_m3 | numeric | YES |
| is_active | boolean | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| destination_type | character varying | YES |
| destination_id | uuid | YES |
| organization_id | uuid | YES |

## sorting

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| sorting_date | timestamp with time zone | YES |
| sorting_type | character varying | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |
| status | character varying | NO |

## stock_lot

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| material_subtype_id | uuid | NO |
| sorted_item_id | uuid | YES |
| pressed_bale_id | uuid | YES |
| available_weight_kg | numeric | NO |
| available_volume_m3 | numeric | NO |
| original_weight_kg | numeric | NO |
| original_volume_m3 | numeric | NO |
| is_active | boolean | NO |
| created_at | timestamp with time zone | NO |

## stock_movement

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| operation_id | uuid | NO |
| material_subtype_id | uuid | NO |
| weight_delta_kg | numeric | NO |
| volume_delta_m3 | numeric | NO |

## stock_operation

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| actor_id | uuid | NO |
| kind | character varying | NO |
| sorting_id | uuid | YES |
| pressing_id | uuid | YES |
| sale_id | uuid | YES |
| reversal_of | uuid | YES |
| occurred_at | timestamp with time zone | NO |
| recorded_at | timestamp with time zone | NO |

## team_member

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| name | character varying | NO |
| role | character varying | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |

## users

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| email | character varying | NO |
| password_hash | text | NO |
| full_name | character varying | NO |
| user_role | character varying | NO |
| is_active | boolean | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| credential_version | bigint | NO |
| must_change_password | boolean | NO |

## vehicle

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| license_plate | character varying | NO |
| model | character varying | YES |
| is_active | boolean | NO |
| creator_id | uuid | NO |
| created_at | timestamp with time zone | YES |
| updated_at | timestamp with time zone | YES |
| organization_id | uuid | YES |


## password_recovery_token

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| token_hash | character(64), primary key | NO |
| user_id | uuid, users foreign key | NO |
| issued_email | character varying(255) | NO |
| credential_version | bigint, nonnegative | NO |
| created_at | timestamp with time zone | NO |
| expires_at | timestamp with time zone | NO |
| used_at | timestamp with time zone | YES |

Expiry must follow creation. Account then token row locks serialize confirmation with password changes; generation/address checks revoke outstanding authorization. Consumption and password replacement commit atomically. Hourly bounded cleanup removes sufficiently old expired authorization; tokens are not an audit ledger. [ADR-0018](adrs/0018-password-recovery-and-transactional-email.md) explains this boundary.

## Routes V4 operational schema

The additive [V4 migration](../src/main/resources/db/migrations/V4__routes_and_execution_history.sql) creates the following eight normalized tables. Column inventory was captured from the running PostgreSQL after V4 on 2026-10-04. Constraints/triggers, numeric precision and ownership are authoritative in SQL. Existing collection adds nullable UUID route_execution_id with a composite ownership FK and completed-execution/logistics/crew validation. No historical collection territory was guessed.

### municipality

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| ibge_code | varchar | NO |
| name | varchar | NO |
| state_code | varchar | NO |
| created_by | uuid | NO |
| created_at | timestamptz | NO |

### neighborhood

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| municipality_id | uuid | NO |
| name | varchar | NO |
| created_by | uuid | NO |
| created_at | timestamptz | NO |

### route

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| municipality_id | uuid | NO |
| name | varchar | NO |
| status | varchar | NO |
| is_active | bool | NO |
| version | int8 | NO |
| planned_distance_km | numeric | YES |
| planned_duration_minutes | int4 | YES |
| created_by | uuid | NO |
| updated_by | uuid | NO |
| created_at | timestamptz | NO |
| updated_at | timestamptz | NO |

### route_area

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| route_id | uuid | NO |
| municipality_id | uuid | NO |
| neighborhood_id | uuid | NO |
| sequence | int4 | NO |
| street | varchar | YES |
| is_active | bool | NO |

### route_event

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| route_id | uuid | NO |
| execution_id | uuid | YES |
| actor_id | uuid | NO |
| action | varchar | NO |
| version | int8 | NO |
| recorded_at | timestamptz | NO |

### route_execution

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| route_id | uuid | NO |
| route_version | int8 | NO |
| route_name | varchar | NO |
| municipality_id | uuid | NO |
| municipality_name | varchar | NO |
| municipality_ibge_code | varchar | NO |
| municipality_state_code | varchar | NO |
| planned_distance_km | numeric | YES |
| planned_duration_minutes | int4 | YES |
| planned_start_at | timestamptz | NO |
| status | varchar | NO |
| version | int8 | NO |
| vehicle_id | uuid | NO |
| vehicle_license_plate | varchar | NO |
| driver_id | uuid | NO |
| driver_name | varchar | NO |
| started_at | timestamptz | YES |
| finished_at | timestamptz | YES |
| cancelled_at | timestamptz | YES |
| cancellation_reason | varchar | YES |
| distance_km | numeric | YES |
| created_by | uuid | NO |
| updated_by | uuid | NO |
| created_at | timestamptz | NO |
| updated_at | timestamptz | NO |

### route_execution_area

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| id | uuid | NO |
| organization_id | uuid | NO |
| execution_id | uuid | NO |
| route_id | uuid | NO |
| source_area_id | uuid | NO |
| neighborhood_id | uuid | NO |
| neighborhood_name | varchar | NO |
| sequence | int4 | NO |
| street | varchar | YES |
| covered | bool | NO |

### route_execution_team

| Column | PostgreSQL type | Nullable |
| --- | --- | --- |
| organization_id | uuid | NO |
| execution_id | uuid | NO |
| team_member_id | uuid | NO |
| member_name | varchar | NO |
| member_role | varchar | YES |

See [Routes architecture](architecture/routes.md) for grains, scope, snapshots, lifecycle and collection linkage. Exact declared km has three fractional digits; raw input kg/m3 remains the existing four-digit contract. Missing distance is null. Geometry/GPS and metric formulas are not introduced.
