-- Insere um administrador padrao com a senha '123456' ($2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK)
INSERT INTO users (id, email, password_hash, full_name, user_role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@irr.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK', 'Administrador Root', 'ADMINISTRADOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
