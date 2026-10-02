# Relational schema and persistence topology

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Verified runtime: PostgreSQL 16, the final Flyway V1, 29 application tables plus flyway_schema_history. The schema inventory below is generated from information_schema; authoritative checks, indexes and foreign keys are the immutable SQL under src/main/resources/db/migrations.

## Domain relationships

~~~mermaid
erDiagram
    organization ||--o{ organization_membership : grants
    users ||--o{ organization_membership : belongs
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
- Future shared changes start at V2; applied versions are immutable.
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

