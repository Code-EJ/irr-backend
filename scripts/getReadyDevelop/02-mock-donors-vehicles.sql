-- Dados base: Veículos, Doadores, Equipe
DO $$
DECLARE
    v_admin_id UUID;
BEGIN
    SELECT id INTO v_admin_id FROM users WHERE email = 'admin@irr.com' LIMIT 1;

    IF v_admin_id IS NULL THEN
        RAISE NOTICE 'Usuário admin não encontrado, pulando inserção de veículos e doadores.';
        RETURN;
    END IF;

    -- Veículos
    INSERT INTO vehicle (id, license_plate, model, is_active, creator_id) VALUES 
    (gen_random_uuid(), 'IRR-0001', 'Caminhão Baú VW', true, v_admin_id),
    (gen_random_uuid(), 'IRR-0002', 'Fiat Fiorino', true, v_admin_id);

    -- Doadores
    INSERT INTO donor (id, name, document, donor_type, is_active, creator_id) VALUES 
    (gen_random_uuid(), 'Supermercado Central', '12345678000199', 'LEGAL', true, v_admin_id),
    (gen_random_uuid(), 'João da Silva', '12345678901', 'PHYSICAL', true, v_admin_id);

    -- Equipe (Team Members)
    INSERT INTO team_member (id, name, role, is_active, creator_id) VALUES 
    (gen_random_uuid(), 'Carlos Motorista', 'MOTORISTA', true, v_admin_id),
    (gen_random_uuid(), 'José Coletor', 'COLETOR', true, v_admin_id);
END $$;
