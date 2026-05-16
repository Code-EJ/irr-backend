-- Insere um administrador padrao com a senha '123456' ($2a$10$wMx4v6kD6YgqyQZeIXbCg.mozjTEA4ZWTHs5Ekluh8Ez.6fATOXWq)
INSERT INTO users (id, email, password_hash, full_name, user_role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@irr.com', '$2a$10$wMx4v6kD6YgqyQZeIXbCg.mozjTEA4ZWTHs5Ekluh8Ez.6fATOXWq', 'Administrador Root', 'ADMINISTRATOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;