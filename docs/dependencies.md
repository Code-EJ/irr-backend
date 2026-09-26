# Backend dependency decisions

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Baseline and current changes

Java 21 and Spring Boot 3.5.6 remain the compatibility baseline. The historical complete inventory and advisory evidence are in [ADR-0003](adrs/0003-architecture-and-dependencies.md); its September 22/23 vulnerability counts are not a new scan or a claim of current safety. This slice is dependency convergence and verification infrastructure, not the entire security upgrade campaign.

| Dependency group | Responsibility | Decision |
| --- | --- | --- |
| Boot Web / Validation | HTTP, JSON and request constraints | Retain; characterize current contracts before major changes |
| Boot Data JPA / PostgreSQL driver | Relational persistence and transactions | Retain; use PostgreSQL integration tests, not H2 substitutions |
| Boot Security / OAuth2 resource server / JOSE | Authentication and signed tokens | Retain for now; redundant direct JOSE declaration is a later prune candidate after graph verification |
| Boot Actuator | Status-only liveness and readiness | Retain; expose health only |
| Flyway core / database-postgresql | Versioned SQL migration | Both resolve through the Boot BOM; remove the conflicting explicit 10.17.0 database module override |
| Lombok | Existing builders/accessors | Retain during module migration; do not combine broad Lombok removal with domain changes |
| Boot Devtools | Optional local development support | Retain existing optional runtime declaration; executable Boot packaging excludes development support by default |
| Boot Test / Security Test | JUnit, Mockito, MVC and security tests | Test scope only |
| Testcontainers PostgreSQL | Disposable real database for integration tests | Test scope; 1.21.4 BOM override addresses Docker 29 API compatibility; no test database fallback |
| springdoc-openapi-starter-webmvc-ui 2.9.1 | Official Swagger UI and generated OpenAPI | Current Spring Boot 3-compatible v2 release; verified through specification/UI integration tests |
| Maven Failsafe | Integration-test lifecycle and verify failure propagation | Boot-managed plugin version; *IT naming separates database tests from unit tests |

## Upgrade and prune procedure

Capture the resolved dependency tree, refresh advisory sources, determine reachable production/test scope, select a compatible patched family, then run unit/integration and container checks. Remove the Testcontainers override when Boot manages a compatible version. Do not infer exploitability from an advisory match or claim remediation from version alignment alone. The next dependency upgrade should cover the full Boot-managed family rather than independently forcing incompatible transitive versions.

The runtime container uses PostgreSQL 16 and Temurin Java 21, with Maven 3.9.14 in the build stage only. Pin validated image digests when a deployment/release process is introduced and establish refresh/scanning automation. The current image build proves reproducibility of this run, not indefinite immutability of upstream tags.

## Official API documentation

Swagger/OpenAPI is the official API reference, per owner instruction. Keep JavaDoc, controller/DTO signatures and OpenAPI customization in English and maintain the Enzo Ribas contact attribution. JSON/YAML output is generated from the application and must not diverge into an independently edited static specification.

## References

- [springdoc v2 documentation and compatibility](https://springdoc.org/v2/)
- [Testcontainers Docker 29 compatibility report](https://github.com/testcontainers/testcontainers-java/issues/11211)
- [Docker Engine API compatibility](https://docs.docker.com/reference/api/engine/)
- [Execution evidence](adrs/0006-backend-foundation-execution.md)

## Identity and attachment slice

ADR-0008 adds no dependency. It uses existing Spring Security, validation, BCrypt, JDBC transactions, PostgreSQL and scheduling. Enzo Ribas (https://github.com/oEnzoRibas) maintains these decisions. The dependency advisory/upgrade backlog in ADR-0003 remains open; passing functional tests is not a new vulnerability scan.
