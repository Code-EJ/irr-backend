-- Retired unsafe seed entrypoint. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
-- Development identities come from .env bootstrap; fixtures use the scoped HTTP API.
-- Previous SQL is retained in Git history, not executed against the new schema.
DO $$ BEGIN
    RAISE EXCEPTION 'Direct legacy seeding is retired. Start Docker Compose and run node scripts/smoke-development.mjs.';
END $$;
