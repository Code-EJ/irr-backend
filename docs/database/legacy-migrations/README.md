# Historical migration recovery

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

ADR-0014 removes duplicate unreleased SQL archives from the maintained tree. The prior active V1–V6 chain and older archive files remain available in Git at commit 3ca0cab. For example, use git show 3ca0cab:src/main/resources/db/migrations/V5__transactional_stock_operations.sql to inspect a historical migration. Never add those files to the final runtime migration location.

The final schema is src/main/resources/db/migrations/V1__initial_schema.sql; future changes start at V2. Old local volumes and private backups preserve development data without pretending that checksum repair is an upgrade.
