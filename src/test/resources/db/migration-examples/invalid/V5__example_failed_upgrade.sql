-- Test-only failure proves transactional DDL rollback and preservation of V1 data.
CREATE TABLE failed_migration_marker (id INTEGER PRIMARY KEY);
SELECT 1 / 0;
