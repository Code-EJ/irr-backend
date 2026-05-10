CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- 1. SISTEMA E ACESSOS (IAM)
-- ==========================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL, 
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 2. HIERARQUIA DE MATERIAIS (FF-5)
-- ==========================================

CREATE TABLE IF NOT EXISTS material_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES material_category(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_subtype (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_id UUID NOT NULL REFERENCES material_type(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 3. CADASTROS BASE E DOCUMENTOS
-- ==========================================

CREATE TABLE IF NOT EXISTS attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL, 
    storage_url TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    license_plate VARCHAR(20) NOT NULL,
    model VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50), 
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS donor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    document VARCHAR(20) NOT NULL, 
    donor_type VARCHAR(10) NOT NULL, 
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 4. OPERACIONAL (COLETA E DOAÇÃO)
-- ==========================================

CREATE TABLE IF NOT EXISTS collection (
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

-- Tabela Associativa: Equipe na Coleta (Se apagar a coleta, limpa a lista de membros aqui)
CREATE TABLE IF NOT EXISTS collection_team (
    collection_id UUID REFERENCES collection(id) ON UPDATE CASCADE ON DELETE CASCADE,
    team_member_id UUID REFERENCES team_member(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    PRIMARY KEY (collection_id, team_member_id)
);

CREATE TABLE IF NOT EXISTS donation (
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

CREATE TABLE IF NOT EXISTS input_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id UUID REFERENCES collection(id) ON UPDATE CASCADE ON DELETE CASCADE,
    donation_id UUID REFERENCES donation(id) ON UPDATE CASCADE ON DELETE CASCADE,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    weight_kg NUMERIC(15, 4) NOT NULL,
    volume_m3 NUMERIC(15, 4) NOT NULL, -- Volume de entrada (a granel)
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. MERCADO E VENDAS (MARKET)
-- ==========================================

CREATE TABLE IF NOT EXISTS buyer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    document VARCHAR(20), 
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sale (
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

CREATE TABLE IF NOT EXISTS sale_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id UUID REFERENCES sale(id) ON UPDATE CASCADE ON DELETE CASCADE,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    weight_kg NUMERIC(15, 4) NOT NULL,
    volume_m3 NUMERIC(15, 4) NOT NULL, -- Volume vendido (importante para frete)
    unit_price NUMERIC(15, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 6. LOG DE INVENTÁRIO (ESTOQUE)
-- ==========================================

CREATE TABLE IF NOT EXISTS inventory_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    quantity_kg NUMERIC(15, 4) NOT NULL,
    quantity_m3 NUMERIC(15, 4) NOT NULL, -- Delta de volume (Pode ser negativo em transformações)
    operation_type VARCHAR(50) NOT NULL, 
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. TRATAMENTO (TRIAGEM E PRENSAGEM)
-- ==========================================

CREATE TABLE IF NOT EXISTS sorting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sorting_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sorting_type VARCHAR(50), -- GROSS, PRIMARY, FINE
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sorted_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sorting_id UUID REFERENCES sorting(id) ON UPDATE CASCADE ON DELETE CASCADE,
    input_item_id UUID REFERENCES input_item(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    weight_kg NUMERIC(15, 4) NOT NULL,
    volume_m3 NUMERIC(15, 4) NOT NULL, -- Volume resultante após separação
    reject_weight_kg NUMERIC(15, 4) DEFAULT 0,
    reject_volume_m3 NUMERIC(15, 4) DEFAULT 0, -- Volume do que foi descartado
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pressing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pressing_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_id UUID NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pressed_bale (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pressing_id UUID REFERENCES pressing(id) ON UPDATE CASCADE ON DELETE CASCADE,
    sorted_item_id UUID REFERENCES sorted_item(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    
    weight_kg NUMERIC(15, 4) NOT NULL,
    
    -- Registro da transformação de volume (Compactação)
    initial_volume_m3 NUMERIC(15, 4) NOT NULL, -- Volume do material solto
    final_volume_m3 NUMERIC(15, 4) NOT NULL,   -- Volume do fardo prensado
    
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- TABELA DE PERFORMANCE (RETRATO DO ESTOQUE ATUAL)
-- ==========================================

CREATE TABLE IF NOT EXISTS inventory_balance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_subtype_id UUID NOT NULL REFERENCES material_subtype(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    current_weight_kg NUMERIC(15, 4) NOT NULL DEFAULT 0,
    current_volume_m3 NUMERIC(15, 4) NOT NULL DEFAULT 0, -- Foto do espaço ocupado hoje
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);