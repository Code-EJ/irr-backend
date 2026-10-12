# ADR-0029: Upgrade the Spring platform through its supported open-source line

- Status: Accepted for implementation; release acceptance requires the verification below.
- Date: 2026-10-11.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- References: [ADR-0003](0003-architecture-and-dependencies.md), [ADR-0014](0014-final-preproduction-contract-and-baseline.md), [ADR-0028](0028-isolated-trial-homologation.md).

## Context

The owner confirms that the organization has no Spring Enterprise license and chooses the free supported line. The current Boot 3.5.16 / Framework 6.2.19 dependency audit fails on two Spring MVC advisories. Conditional-feature guards reduce observed exposure but are not dependency remediation. The vendor's 6.2.20 fixes require Enterprise access; the public 7.0.9 fixes provide an authorized route without procurement.

## Decision drivers

Remove the affected framework resolution without weakening security CI, introducing paid repositories, mixing incompatible framework generations, changing business rules, or rewriting the architecture. Preserve Java 21, Maven/Spotless, Docker entrypoints/cache work, PostgreSQL 16, Redis 8.2, retained V1–V4 migration files and the exclusively versioned `/api/v1` contract. Existing idempotency receipts, exact decimal strings, dates, authorization and OpenAPI consumers require behavioral regression verification.

## Considered options

| Option | Advantages | Disadvantages / disposition |
| --- | --- | --- |
| Licensed Framework 6.2.20 patch | Smaller framework change | Organization has no license; rejected by owner |
| Suppress advisories or keep conditional mitigations indefinitely | Minimal code edits | Does not fix the library; fails the existing release requirement; rejected |
| Override Framework 7 inside Boot 3.5 | Superficially small POM change | Unsupported platform mixture and difficult compatibility accounting; rejected |
| Boot 4.0 maintenance line with Framework 7.0.9 | Public supported platform; keeps the same application framework | Requires starter/package/serialization adaptation and full regression checks; selected |
| Rewrite domain/application architecture | None needed for these defects | Unrelated scope and risk; rejected under ADR-0003 |

## Decision

Start with Boot 4.0.8, whose published managed coordinates include Framework 7.0.9. Align springdoc to the documented Boot 4.0 / springdoc 3.0 line. Use the corresponding MVC, Flyway and test starters rather than rely on removed Boot 3 transitive auto-configuration. Adopt Boot's Jackson 3 mapper in application code and tests, retaining the public Jackson annotation API and explicit decimal-string serializer. Enable Jackson 2-compatible defaults where required for the existing wire contract; do not add a second runtime mapper as an unnecessary permanent bridge.

Remove obsolete platform-family overrides that would force Tomcat 10 or Netty 4.1 into the new platform. Audit the resulting dependency graph and apply any necessary compatible patch versions based on actual advisory evidence. Preserve the intent of existing security patches; removing an incompatible version pin is not permission to restore vulnerable packages. Adapt testing dependencies only as required by the supported platform and Docker API compatibility.

This supersedes only the earlier Spring 6.2.19 dependency baseline and the pending Enterprise-versus-OSS choice in the security register. It does not supersede ADR-0014's API/database decisions or authorize migration-history edits, new endpoints, cloud deployment, main merges or production release beyond existing authorizations.

## Verification and rollback

Require Java 21 `clean verify` including Spotless and all unit/integration tests against disposable PostgreSQL/Redis; current resolved Maven OSV audit; generated Swagger semantic comparison against the pinned frontend contract; real local application health and retained-schema startup; and matching frontend contract/types/build checks if executable contract output changes. Verify decimal strings, null/incomplete Analytics values, idempotent replay, date/time behavior, lifecycle/authorization and error responses through the maintained integration suite. The loaded MVC security guards remain defense in depth.

Do not mark implementation accepted solely because compilation succeeds. Record exact versions, commands, results and limitations after execution. Existing release gates for images, cloud volume locality, public pair, email and operator acceptance remain open. Roll back application source/image to the preceding reviewed commit without database rollback: this change introduces no schema migration or persisted-data conversion. Never deploy the candidate on an existing database if retained-schema and serialization checks fail.

## Consequences

The framework dependency can be patched without paid support or changing the modular monolith. More than a version-number edit is necessary, but changes stay bounded to platform integration and regression compatibility. Existing JSON receipts must remain readable and replayable; framework and mapper upgrades remain explicit reviewed dependencies rather than hidden implementation details.

Primary references checked 2026-10-11: [Boot managed Framework coordinates](https://docs.spring.io/spring-boot/4.0/appendix/dependency-versions/coordinates.html), [Boot migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [springdoc compatibility](https://springdoc.org/), [SSE fix availability](https://spring.io/security/cve-2026-47890/), [XSLT fix availability](https://spring.io/security/cve-2026-47884/).

## Local implementation evidence — 2026-10-11

Implemented in `ca8e8df` with the generated-schema correction in `02bc54d`. Java 21 `mvn --batch-mode --no-transfer-progress spotless:apply clean verify` passed 51 unit and 101 integration tests, with no failures/errors/skips. The new compatibility tests compare actual Jackson 3 payload bytes against an independent test-only Jackson 2 serializer and read a historical receipt retaining its original offset. Empty-password authentication remains rejected.

The fresh Maven dependency tree and `node scripts/audit-dependencies.mjs` pass across 185 coordinates with zero findings and no suppression; [dated evidence](../data/dependency-review-2026-10-11.json) records the POM digest and source. Compatible Jackson 3.1.7, Jackson 2.21.7 (Swagger transitive) and Tomcat 11.0.26 patches were required because inherited versions still had matches.

The real Docker candidate starts healthy against retained local PostgreSQL/Redis and uploads. Generated Swagger was compared recursively to the pre-upgrade executable contract: zero differences excluding only server origins, including all 116 v1 operations. Springdoc initially omitted eight numeric bounds; explicit version constraints and the existing planned-duration exclusive zero bound restore the exact schema. A maintained integration test prevents this regression. Frontend `npm run verify:homologation` passed all six read-only pair checks using `http://localhost:9292/api/v1` and `http://localhost:5173`. The frontend pinned contract was preserved because its executable content did not change. V1–V4 migration files are unchanged.

The [new sanitized runtime scan](../security/backend-spring-oss-image-2026-10-11.json) retains 288 raw matches (13 critical, 61 high, 155 medium, 50 low, nine negligible). Removal of the two Spring matches is not proof that the image is vulnerability-free. Earlier Ubuntu vendor dispositions require package/version comparison before being carried to this image; other runtime/build/infrastructure findings and Railway/mail/acceptance gates remain separate. No public workload was deployed or main branch merged.
