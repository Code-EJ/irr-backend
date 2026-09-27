-- Test-only example of evolving the shared baseline without changing V1.
CREATE INDEX ix_example_inventory_updated ON inventory_balance(last_updated_at);
