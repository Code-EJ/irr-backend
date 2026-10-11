# Backend dependency register

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Current review: 2026-10-11. The resolved Maven graph contains 185 distinct production/test coordinates. The fresh OSV scan reports zero matched advisories after the supported open-source platform upgrade in [ADR-0029](adrs/0029-open-source-spring-security-upgrade.md). Historical findings remain below and in the dated evidence; coordinate scanning is not container or reachability certification.

## Decisions

Use Java 21, Spring Boot 4.0.8, Framework 7.0.9, Spring Security 7.0.7 and springdoc 3.0.3. The owner has no Enterprise license and explicitly selected the free supported line. Preserve the pragmatic modular monolith and ADR-0014's business contract/schema. Application JSON uses Boot's Jackson 3 mapper with compatible defaults and explicit exact-decimal string serialization; Swagger retains its own transitive Jackson 2 dependencies. MVC, security, Flyway and test starters follow Boot 4's module boundaries.

| Explicit override | Version | Reason |
| --- | --- | --- |
| Jackson 3 BOM | 3.1.7 | Compatible patch removes advisories in the inherited 3.1.5 family |
| Jackson 2 BOM | 2.21.7 | Patch Swagger's transitive 2.21.5 family; no second application mapper |
| Embedded Tomcat | 11.0.26 | Patched compatible Servlet 6.1 generation |
| PostgreSQL JDBC | 42.7.12 | Retain reviewed driver patch |
| Log4j2 family | 2.25.5 | Retain reviewed API-family patch |
| Commons Lang | 3.18.0 | Retain reviewed transitive patch |
| Commons Compress | 1.28.0 | Retain reviewed archive-extraction patch |

Obsolete Tomcat 10 and Netty 4.1 pins are removed because they conflict with this platform. Netty now follows Boot's 4.2.17.Final family; Testcontainers follows its managed 2.0.5 line, verified against Docker Engine 29.2.1. Do not remove compatible security overrides before auditing the replacement graph. Spotless 3.10.3 and the owner's Docker Maven cache/goals remain.

## Direct resolved dependencies

| Coordinate | Version | Scope |
| --- | --- | --- |
| org.springframework.boot:spring-boot-starter-data-redis | 4.0.8 | compile |
| org.springframework.boot:spring-boot-starter-validation | 4.0.8 | compile |
| org.springframework.boot:spring-boot-starter-actuator | 4.0.8 | compile |
| org.springframework.boot:spring-boot-starter-data-jpa | 4.0.8 | compile |
| org.springframework.boot:spring-boot-starter-security | 4.0.8 | compile |
| org.springframework.boot:spring-boot-starter-security-oauth2-resource-server | 4.0.8 | compile |
| org.springframework.security:spring-security-oauth2-jose | 7.0.7 | compile |
| org.springframework.boot:spring-boot-starter-webmvc | 4.0.8 | compile |
| org.springframework.boot:spring-boot-starter-flyway | 4.0.8 | compile |
| org.flywaydb:flyway-database-postgresql | 11.14.1 | compile |
| org.postgresql:postgresql | 42.7.12 | runtime |
| org.projectlombok:lombok | 1.18.46 | compile |
| org.springframework.boot:spring-boot-starter-webmvc-test | 4.0.8 | test |
| org.springframework.boot:spring-boot-starter-security-test | 4.0.8 | test |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 3.0.3 | compile |
| org.testcontainers:testcontainers-postgresql | 2.0.5 | test |

## Repeat the scan

Run Java 21 and Node.js 22.6+ from the repository root:

~~~sh
./mvnw --batch-mode dependency:tree -DoutputType=json -DoutputFile=.local/audit/dependency-tree.json
node scripts/audit-dependencies.mjs
./mvnw clean verify
~~~

On Windows use .\mvnw.cmd and quote each Maven property argument: `'-DoutputType=json' '-DoutputFile=.local/audit/dependency-tree.json'`. This avoids PowerShell splitting the dotted path into a lifecycle argument. The audit script queries the public [OSV API](https://google.github.io/osv.dev/api/), records coordinate/advisory evidence under .local/audit and exits nonzero on findings or API failures. It sends package coordinates only. Review advisories and upgrade safely; never suppress a scanner failure as an empty result. CI publishes this package-only report, never environment or backup files.

Official references: [Spring Boot 4.0 documentation](https://docs.spring.io/spring-boot/4.0/reference/index.html), [Java/system requirements](https://docs.spring.io/spring-boot/4.0/system-requirements.html), [Maven Central](https://repo.maven.apache.org/maven2/), [springdoc](https://springdoc.org/). Historical findings and rejected major-upgrade combinations remain in ADR-0003; current execution is ADR-0013.

## Build formatting policy

Spotless Maven plugin 3.10.3 is intentional project tooling and runs check in the validate phase. Its configured Java steps shorten qualified types, organize/remove unused imports, apply Google Java Format, trim trailing whitespace and ensure a final newline. Use ./mvnw spotless:apply before ./mvnw clean verify. The Docker Maven build runs the same validation; it skips tests only after CI/local verification, not formatting. Application-coordinate OSV evidence does not claim to audit Maven plugin implementation dependencies.

## Identity delivery audit — 2026-10-03

Enzo Ribas (https://github.com/oEnzoRibas): no new backend library or version override was required for identity. Re-ran the resolved Maven graph audit: 156 coordinates, zero OSV matches at audit time. Existing Maven/Spotless and Docker changes were retained. This does not substitute for an image OS scan or future advisory monitoring.


## Identity completion — 2026-10-04

Maintainer: Enzo Ribas (https://github.com/oEnzoRibas). Recovery uses existing Java 21 HttpClient, Jackson, Spring transactions/task execution and Redis dependencies; no runtime library or provider SDK added. Local Mailpit is pinned to v1.31.3 and an image digest in Compose; no external email is sent in automated tests. Application package scans do not establish container OS vulnerability status.

## Synthetic benchmark tooling

The entries before this section retain their original audit dates; they do not override the current review below.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The report benchmark uses Node 24 built-ins, the existing Docker CLI and immutable locally available backend/PostgreSQL 16/Redis 8.2 images. It adds no npm or Maven dependency and no runtime reporting cache. Versions/image IDs actually measured are recorded in [the benchmark evidence](reporting-benchmarks.md); preparation does not pull images. This inventory entry does not replace the pending full dependency and container security audit.

## Historical local delivery review — 2026-10-10

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The current resolved graph was regenerated from the existing POM. OSV returned `GHSA-j9f9-w8pj-32f8` and `GHSA-pc63-qcmh-9cmg` against `spring-webmvc:6.2.19`; the audit exits nonzero and neither finding is suppressed. OSV/GitHub labels both critical, whereas the vendor labels the SSE issue low and the XSLT issue medium. Retain this severity discrepancy explicitly instead of substituting one source for the other.

The vendor describes prerequisites: [SSE stream corruption](https://spring.io/security/cve-2026-47890/) requires streaming view fragments; [XSLT path handling](https://spring.io/security/cve-2026-47884/) requires XsltView and wildcard implicit view rendering. Source inspection found no `SseEmitter`, `ServerSentEvent`, `FragmentsRendering`, `XsltView`, `ViewResolver`, `ModelAndView`, `text/event-stream`, XSL stylesheets or plain `@Controller` declarations in application source/resources. Controllers use REST response handling. This supports a conditional-feature disposition, not proof that the dependency is patched or every attack path is unreachable. Reassess if view/SSE support, view resolvers or routing conventions change.

The vendor lists Framework 6.2.20 as enterprise-only and Framework 7.0.9 as the OSS fix. No support entitlement is assumed, and a Framework 7/Boot 4 migration is not silently introduced into ADR-0003's current compatibility baseline. Before release, explicitly resolve the framework maintenance/support decision and these recorded findings. The existing Java/Spotless/Docker configuration and application contract remain unchanged.

Docker Scout 1.19.0 was present, but its scan of `local://irr-identity-backend` failed because Docker account authentication is required. There is no completed image OS scan and no zero-CVE claim. Use the intended release image IDs with an authenticated approved scanner, retain failures, and review all application and base-image findings before release. Package-only results do not cover Maven plugin implementations, JRE/OS packages or infrastructure images.

The selected local container configuration was inspected after restoring Docker Desktop and starting the existing database/Redis/backend containers. Backend/frontend are non-root, use read-only roots, drop all capabilities and set `no-new-privileges`; published application/Mailpit ports bind to loopback, and database/Redis ports are not published. Database/Redis/Mailpit retain their existing writable-root/image-entrypoint behavior; blank configured users do not prove the actual daemon UID. The selected verification backend/database/Redis have restart policy `no`, so restarting Docker Desktop required explicit `docker compose -f .local/identity/compose.json -p irr-identity-verification start --wait --wait-timeout 120`. No container was recreated, image pulled, secret changed or volume reset by this recovery. These are local configuration observations, not production hardening certification. The [current evidence](data/dependency-review-2026-10-10.json) records image IDs and non-secret configuration only.

## Subsequent verified container maintenance — 2026-10-10

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The earlier Scout-authentication limitation is superseded by actual Grype runtime and build-base scans. See [individual dispositions, counts and package inventories](security/README.md). Compatible Jammy security updates and a bounded overridable Maven build succeeded; retained cache/goals, Java 21 and non-root runtime remain. The resulting backend image is healthy against the existing local volumes and passed six actual pair checks plus the 116-operation contract comparison. No business/API/schema feature changed.

The Dockerfile base tag and OS repositories are mutable. Preserve the approved final image digest and inventory; rebuild, re-scan and verify before later promotion. Outstanding CPE/vendor, framework and builder findings remain explicit. Local success does not close deployment security or operator acceptance.
