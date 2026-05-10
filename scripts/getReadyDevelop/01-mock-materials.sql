-- Dados base: Materiais (Categorias, Tipos, Subtipos)
DO $$
DECLARE
    v_admin_id UUID;
    v_cat_plastico UUID := gen_random_uuid();
    v_cat_metal UUID := gen_random_uuid();
    v_cat_papel UUID := gen_random_uuid();
    
    v_type_pet UUID := gen_random_uuid();
    v_type_aluminio UUID := gen_random_uuid();
    v_type_papelao UUID := gen_random_uuid();
BEGIN
    SELECT id INTO v_admin_id FROM users WHERE email = 'admin@irr.com' LIMIT 1;
    
    IF v_admin_id IS NULL THEN
        RAISE NOTICE 'Usuário admin não encontrado, pulando inserção de materiais.';
        RETURN;
    END IF;

    -- Categorias
    INSERT INTO material_category (id, name, is_active, creator_id, version) VALUES 
    (v_cat_plastico, 'Plástico', true, v_admin_id, 0),
    (v_cat_metal, 'Metal', true, v_admin_id, 0),
    (v_cat_papel, 'Papel/Papelão', true, v_admin_id, 0);

    -- Tipos
    INSERT INTO material_type (id, category_id, name, is_active, creator_id, version) VALUES 
    (v_type_pet, v_cat_plastico, 'PET', true, v_admin_id, 0),
    (v_type_aluminio, v_cat_metal, 'Alumínio', true, v_admin_id, 0),
    (v_type_papelao, v_cat_papel, 'Papelão Ondulado', true, v_admin_id, 0);

    -- Subtipos
    INSERT INTO material_subtype (id, type_id, name, is_active, creator_id, version) VALUES 
    (gen_random_uuid(), v_type_pet, 'PET Cristal', true, v_admin_id, 0),
    (gen_random_uuid(), v_type_pet, 'PET Verde', true, v_admin_id, 0),
    (gen_random_uuid(), v_type_aluminio, 'Lata de Bebida', true, v_admin_id, 0),
    (gen_random_uuid(), v_type_papelao, 'Caixa de Papelão', true, v_admin_id, 0);
END $$;
