# Backend contribution workflow

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

1. Work on a feature/refactor branch, never main/master. Preserve unrelated changes.
2. Read ADR-0001 and ADR-0005, the relevant issue acceptance criteria and current schema/contracts. Resolve domain ambiguity explicitly.
3. Define a small vertical slice, current-to-target API/schema mapping, affected frontend consumer and rollback/data implications.
4. Add behavior tests for business rules and negative authorization paths. Use PostgreSQL integration tests for SQL, uniqueness and concurrency. Do not use a developer database or real credentials.
5. Implement English-named DTOs, application use cases, rules and adapters. Keep transaction boundaries in application services and one owner for inventory posting. Use ports across feature boundaries.
6. Add a new Flyway migration for persistent changes; never edit an applied migration. Document preflight and invalid-data handling.
7. Update the API/entity/source inventories, architecture diagram if topology changed, and the relevant ADR. Identify planned versus implemented behavior.
8. Run ./mvnw clean verify and the container build/smoke checks affected by the change. Record actual results, failures and limitations.
9. Commit coherent changes with English semantic messages. PR descriptions explain the problem, resulting behavior, compatibility and validation. Keep unresolved product choices visible.

The current code is a migration baseline, not a template for entity serialization, global stock mutation, public partner creation or direct cross-feature repository access. See ADR-0004 for known deviations.

## JavaDoc and attribution

Document every new or modified Java type and public operation in English JavaDoc, including parameter/return/exception contracts where applicable. Credit this refactor with @author Enzo Ribas and the profile https://github.com/oEnzoRibas; preserve previous contributor credits. Dependency decisions identify Enzo Ribas as documentation maintainer, not as author of third-party libraries. Keep commit author identity consistent with repository Git configuration.
