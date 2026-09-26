# Current schema and entity inventory

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

The current relational schema is defined by versioned SQL, not Hibernate auto-DDL. One fresh V1 now creates the complete current schema, including versions, destinations and unique/nonnegative balances. Historical migrations are archived outside the runtime path; ADR-0007 defines future V2+ evolution. See [target topology and ERD](adrs/0002-database-schema-redesign.md) for the planned redesign and [operations](operations.md) for upgrade preflight.

## JPA entity mapping

| Entity | SQL table | Source |
| --- | --- | --- |
| Attachment | `attachment` | [src/main/java/org/code/api/domain/models/base/Attachment.java](../src/main/java/org/code/api/domain/models/base/Attachment.java) |
| Donor | `donor` | [src/main/java/org/code/api/domain/models/base/Donor.java](../src/main/java/org/code/api/domain/models/base/Donor.java) |
| TeamMember | `team_member` | [src/main/java/org/code/api/domain/models/base/TeamMember.java](../src/main/java/org/code/api/domain/models/base/TeamMember.java) |
| Vehicle | `vehicle` | [src/main/java/org/code/api/domain/models/base/Vehicle.java](../src/main/java/org/code/api/domain/models/base/Vehicle.java) |
| Collection | `collection` | [src/main/java/org/code/api/domain/models/collection/Collection.java](../src/main/java/org/code/api/domain/models/collection/Collection.java) |
| InputItem | `input_item` | [src/main/java/org/code/api/domain/models/collection/InputItem.java](../src/main/java/org/code/api/domain/models/collection/InputItem.java) |
| Donation | `donation` | [src/main/java/org/code/api/domain/models/donation/Donation.java](../src/main/java/org/code/api/domain/models/donation/Donation.java) |
| InventoryBalance | `inventory_balance` | [src/main/java/org/code/api/domain/models/inventory/InventoryBalance.java](../src/main/java/org/code/api/domain/models/inventory/InventoryBalance.java) |
| InventoryLog | `inventory_log` | [src/main/java/org/code/api/domain/models/inventory/InventoryLog.java](../src/main/java/org/code/api/domain/models/inventory/InventoryLog.java) |
| MaterialCategory | `material_category` | [src/main/java/org/code/api/domain/models/material/MaterialCategory.java](../src/main/java/org/code/api/domain/models/material/MaterialCategory.java) |
| MaterialSubtype | `material_subtype` | [src/main/java/org/code/api/domain/models/material/MaterialSubtype.java](../src/main/java/org/code/api/domain/models/material/MaterialSubtype.java) |
| MaterialType | `material_type` | [src/main/java/org/code/api/domain/models/material/MaterialType.java](../src/main/java/org/code/api/domain/models/material/MaterialType.java) |
| PressedBale | `pressed_bale` | [src/main/java/org/code/api/domain/models/pressing/PressedBale.java](../src/main/java/org/code/api/domain/models/pressing/PressedBale.java) |
| Pressing | `pressing` | [src/main/java/org/code/api/domain/models/pressing/Pressing.java](../src/main/java/org/code/api/domain/models/pressing/Pressing.java) |
| Buyer | `buyer` | [src/main/java/org/code/api/domain/models/sale/Buyer.java](../src/main/java/org/code/api/domain/models/sale/Buyer.java) |
| Sale | `sale` | [src/main/java/org/code/api/domain/models/sale/Sale.java](../src/main/java/org/code/api/domain/models/sale/Sale.java) |
| SaleItem | `sale_item` | [src/main/java/org/code/api/domain/models/sale/SaleItem.java](../src/main/java/org/code/api/domain/models/sale/SaleItem.java) |
| SortedItem | `sorted_item` | [src/main/java/org/code/api/domain/models/sorting/SortedItem.java](../src/main/java/org/code/api/domain/models/sorting/SortedItem.java) |
| Sorting | `sorting` | [src/main/java/org/code/api/domain/models/sorting/Sorting.java](../src/main/java/org/code/api/domain/models/sorting/Sorting.java) |
| User | `users` | [src/main/java/org/code/api/domain/models/user/User.java](../src/main/java/org/code/api/domain/models/user/User.java) |

## Current baseline DDL

[Active V1](../src/main/resources/db/migrations/V1__initial_schema.sql). This is the implemented schema, not the completed target ledger/org redesign.

~~~sql
-- Initial pre-production baseline for a new, empty PostgreSQL database.
-- Author: Enzo Ribas (https://github.com/oEnzoRibas).
-- Future shared schema changes use V2+; never clean or baseline an existing database automatically.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE material_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES material_category(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE material_subtype (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_id UUID NOT NULL REFERENCES material_type(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    storage_url TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    license_plate VARCHAR(20) NOT NULL,
    model VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE donor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    document VARCHAR(20) NOT NULL,
    donor_type VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realization_date TIMESTAMP WITH TIME ZONE NOT NULL,
    total_weight_kg NUMERIC(15, 4) NOT NULL,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    driver_id UUID NOT NULL REFERENCES team_member(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    mtr_generator_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    mtr_destinator_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    collection_diary_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collection_team (
    collection_id UUID REFERENCES collection(id) ON UPDATE CASCADE ON DELETE CASCADE,
    team_member_id UUID REFERENCES team_member(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    PRIMARY KEY (collection_id, team_member_id)
);

CREATE TABLE donation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    total_weight_kg NUMERIC(15, 4) NOT NULL,
    donor_id UUID NOT NULL REFERENCES donor(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    proof_attachment_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE input_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id UUID REFERENCES collection(id) ON UPDATE CASCADE ON DELETE CASCADE,
    donation_id UUID REFERENCES donation(id) ON UPDATE CASCADE ON DELETE CASCADE,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    weight_kg NUMERIC(15, 4) NOT NULL,
    volume_m3 NUMERIC(15, 4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE buyer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    document VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sale (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_date TIMESTAMP WITH TIME ZONE NOT NULL,
    buyer_id UUID NOT NULL REFERENCES buyer(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    nfe_attachment_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    mtr_attachment_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    cdf_attachment_id UUID REFERENCES attachment(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    total_value NUMERIC(15, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sale_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id UUID REFERENCES sale(id) ON UPDATE CASCADE ON DELETE CASCADE,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    weight_kg NUMERIC(15, 4) NOT NULL,
    volume_m3 NUMERIC(15, 4) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    quantity_kg NUMERIC(15, 4) NOT NULL,
    quantity_m3 NUMERIC(15, 4) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sorting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sorting_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sorting_type VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sorted_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sorting_id UUID REFERENCES sorting(id) ON UPDATE CASCADE ON DELETE CASCADE,
    input_item_id UUID REFERENCES input_item(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    weight_kg NUMERIC(15, 4) NOT NULL,
    volume_m3 NUMERIC(15, 4) NOT NULL,
    reject_weight_kg NUMERIC(15, 4) DEFAULT 0,
    reject_volume_m3 NUMERIC(15, 4) DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    destination_type VARCHAR(50),
    destination_id UUID
);

CREATE TABLE pressing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pressing_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pressed_bale (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pressing_id UUID REFERENCES pressing(id) ON UPDATE CASCADE ON DELETE CASCADE,
    sorted_item_id UUID REFERENCES sorted_item(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,

    weight_kg NUMERIC(15, 4) NOT NULL,

    initial_volume_m3 NUMERIC(15, 4) NOT NULL,
    final_volume_m3 NUMERIC(15, 4) NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    destination_type VARCHAR(50),
    destination_id UUID
);

CREATE TABLE inventory_balance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    current_weight_kg NUMERIC(15, 4) NOT NULL DEFAULT 0,
    current_volume_m3 NUMERIC(15, 4) NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_balance_material_subtype UNIQUE (material_subtype_id),
    CONSTRAINT ck_inventory_balance_nonnegative CHECK (current_weight_kg >= 0 AND current_volume_m3 >= 0)
);
~~~
