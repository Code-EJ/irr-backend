SELECT definition FROM (
SELECT 'COLUMN '||c.relname||'.'||a.attname||' '||format_type(a.atttypid,a.atttypmod)||' NOTNULL='||a.attnotnull||' DEFAULT='||COALESCE(pg_get_expr(d.adbin,d.adrelid),'') definition
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum WHERE n.nspname='public' AND c.relkind='r' AND c.relname<>'flyway_schema_history'
UNION ALL SELECT 'CONSTRAINT '||c.relname||'.'||k.conname||' '||pg_get_constraintdef(k.oid) FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relname<>'flyway_schema_history'
UNION ALL SELECT 'INDEX '||indexname||' '||indexdef FROM pg_indexes WHERE schemaname='public' AND tablename<>'flyway_schema_history'
UNION ALL SELECT 'TRIGGER '||c.relname||'.'||t.tgname||' '||pg_get_triggerdef(t.oid) FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND NOT t.tgisinternal
UNION ALL SELECT 'FUNCTION '||p.proname||' '||pg_get_functiondef(p.oid) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.proname IN ('reject_stock_history_mutation','validate_collection_team_scope')
) definitions ORDER BY definition