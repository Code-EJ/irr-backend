# Backend dependency register

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Current review: 2026-10-10. The resolved Maven graph contains 156 distinct coordinates across production and test scopes. OSV now reports two advisories against Spring MVC 6.2.19. The earlier zero-match scans below are historical observations. See the [current evidence](data/dependency-review-2026-10-10.json); this is a point-in-time coordinate scan, not a reachability proof, container OS scan or security guarantee.

## Decisions

Use Java 21 and Spring Boot 3.5.16 for the existing Spring 6 / Jackson 2 compatibility baseline. A separate Boot 4 migration must revisit framework behavior, security, Jackson integration and springdoc compatibility; it is not silently bundled into this domain overhaul. Version selection does not itself establish a support entitlement. Remove spring-boot-devtools from the application and delete the unused ReportPort/ReportService stub; reporting now has a real feature module.

| Explicit override | Version | Reason |
| --- | --- | --- |
| Jackson BOM | 2.21.7 | Current intentional POM family pin; preserve the aligned family |
| Netty family | 4.1.137.Final | Resolve advisories against 4.1.135.Final used by Redis transport |
| Embedded Tomcat | 10.1.60 | Published patched 10.1 line |
| PostgreSQL JDBC | 42.7.12 | Resolve the matched driver advisory |
| Log4j2 family | 2.25.5 | Resolve the matched API-family advisory |
| Commons Lang | 3.18.0 | Resolve transitive 3.17.0 advisory |
| Commons Compress | 1.28.0 | Resolve Testcontainers archive-extraction dependency advisory |

The initial post-Boot-upgrade scan found 14 coordinate/advisory matches. Pinning compatible families reduced the fresh scan to zero matches across 156 coordinates. The full functional suite and Docker workflow were rerun with these pins. Remove an override only when the inherited BOM supplies an equally reviewed version; do not independently downgrade sibling artifacts. Redis uses Boot-managed Spring Data/Lettuce. Testcontainers 1.21.4 retains Docker Engine 29 compatibility. No dependency is added solely for architectural naming.

## Direct resolved dependencies

| Coordinate | Version | Scope |
| --- | --- | --- |
| org.springframework.boot:spring-boot-starter-data-redis | 3.5.16 | compile |
| org.springframework.boot:spring-boot-starter-validation | 3.5.16 | compile |
| org.springframework.boot:spring-boot-starter-actuator | 3.5.16 | compile |
| org.springframework.boot:spring-boot-starter-data-jpa | 3.5.16 | compile |
| org.springframework.boot:spring-boot-starter-security | 3.5.16 | compile |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server | 3.5.16 | compile |
| org.springframework.security:spring-security-oauth2-jose | 6.5.11 | compile |
| org.springframework.boot:spring-boot-starter-web | 3.5.16 | compile |
| org.flywaydb:flyway-core | 11.7.2 | compile |
| org.flywaydb:flyway-database-postgresql | 11.7.2 | compile |
| org.postgresql:postgresql | 42.7.12 | runtime |
| org.projectlombok:lombok | 1.18.46 | compile |
| org.springframework.boot:spring-boot-starter-test | 3.5.16 | test |
| org.springframework.security:spring-security-test | 6.5.11 | test |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.9.1 | compile |
| org.testcontainers:postgresql | 1.21.4 | test |

## Repeat the scan

Run Java 21 and Node.js 22.6+ from the repository root:

~~~sh
./mvnw --batch-mode dependency:tree -DoutputType=json -DoutputFile=.local/audit/dependency-tree.json
node scripts/audit-dependencies.mjs
./mvnw clean verify
~~~

On Windows use .\mvnw.cmd and quote each Maven property argument: `'-DoutputType=json' '-DoutputFile=.local/audit/dependency-tree.json'`. This avoids PowerShell splitting the dotted path into a lifecycle argument. The audit script queries the public [OSV API](https://google.github.io/osv.dev/api/), records coordinate/advisory evidence under .local/audit and exits nonzero on findings or API failures. It sends package coordinates only. Review advisories and upgrade safely; never suppress a scanner failure as an empty result. CI publishes this package-only report, never environment or backup files.

Official references: [Spring Boot 3.5 documentation](https://docs.spring.io/spring-boot/3.5/reference/index.html), [Java/system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Maven Central](https://repo.maven.apache.org/maven2/), [springdoc](https://springdoc.org/). Historical findings and rejected major-upgrade combinations remain in ADR-0003; current execution is ADR-0013.

## Build formatting policy

Spotless Maven plugin 3.10.3 is intentional project tooling and runs check in the validate phase. Its configured Java steps shorten qualified types, organize/remove unused imports, apply Google Java Format, trim trailing whitespace and ensure a final newline. Use ./mvnw spotless:apply before ./mvnw clean verify. The Docker Maven build runs the same validation; it skips tests only after CI/local verification, not formatting. Application-coordinate OSV evidence does not claim to audit Maven plugin implementation dependencies.

## Identity delivery audit — 2026-10-03

Enzo Ribas (https://github.com/oEnzoRibas): no new backend library or version override was required for identity. Re-ran the resolved Maven graph audit: 156 coordinates, zero OSV matches at audit time. Existing Maven/Spotless and Docker changes were retained. This does not substitute for an image OS scan or future advisory monitoring.


## Identity completion — 2026-10-04

Maintainer: Enzo Ribas (https://github.com/oEnzoRibas). Recovery uses existing Java 21 HttpClient, Jackson, Spring transactions/task execution and Redis dependencies; no runtime library or provider SDK added. Local Mailpit is pinned to v1.31.3 and an image digest in Compose; no external email is sent in automated tests. Application package scans do not establish container OS vulnerability status.

## Synthetic benchmark tooling

The entries before this section retain their original audit dates; they do not override the current review below.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The report benchmark uses Node 24 built-ins, the existing Docker CLI and immutable locally available backend/PostgreSQL 16/Redis 8.2 images. It adds no npm or Maven dependency and no runtime reporting cache. Versions/image IDs actually measured are recorded in [the benchmark evidence](reporting-benchmarks.md); preparation does not pull images. This inventory entry does not replace the pending full dependency and container security audit.

## Local delivery review — 2026-10-10

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The current resolved graph was regenerated from the existing POM. OSV returned `GHSA-j9f9-w8pj-32f8` and `GHSA-pc63-qcmh-9cmg` against `spring-webmvc:6.2.19`; the audit exits nonzero and neither finding is suppressed. OSV/GitHub labels both critical, whereas the vendor labels the SSE issue low and the XSLT issue medium. Retain this severity discrepancy explicitly instead of substituting one source for the other.

The vendor describes prerequisites: [SSE stream corruption](https://spring.io/security/cve-2026-47890/) requires streaming view fragments; [XSLT path handling](https://spring.io/security/cve-2026-47884/) requires XsltView and wildcard implicit view rendering. Source inspection found no `SseEmitter`, `ServerSentEvent`, `FragmentsRendering`, `XsltView`, `ViewResolver`, `ModelAndView`, `text/event-stream`, XSL stylesheets or plain `@Controller` declarations in application source/resources. Controllers use REST response handling. This supports a conditional-feature disposition, not proof that the dependency is patched or every attack path is unreachable. Reassess if view/SSE support, view resolvers or routing conventions change.

The vendor lists Framework 6.2.20 as enterprise-only and Framework 7.0.9 as the OSS fix. No support entitlement is assumed, and a Framework 7/Boot 4 migration is not silently introduced into ADR-0003's current compatibility baseline. Before release, explicitly resolve the framework maintenance/support decision and these recorded findings. The existing Java/Spotless/Docker configuration and application contract remain unchanged.

Docker Scout 1.19.0 was present, but its scan of `local://irr-identity-backend` failed because Docker account authentication is required. There is no completed image OS scan and no zero-CVE claim. Use the intended release image IDs with an authenticated approved scanner, retain failures, and review all application and base-image findings before release. Package-only results do not cover Maven plugin implementations, JRE/OS packages or infrastructure images.

The selected local container configuration was inspected after restoring Docker Desktop and starting the existing database/Redis/backend containers. Backend/frontend are non-root, use read-only roots, drop all capabilities and set `no-new-privileges`; published application/Mailpit ports bind to loopback, and database/Redis ports are not published. Database/Redis/Mailpit retain their existing writable-root/image-entrypoint behavior; blank configured users do not prove the actual daemon UID. The selected verification backend/database/Redis have restart policy `no`, so restarting Docker Desktop required explicit `docker compose -f .local/identity/compose.json -p irr-identity-verification start --wait --wait-timeout 120`. No container was recreated, image pulled, secret changed or volume reset by this recovery. These are local configuration observations, not production hardening certification. The [current evidence](data/dependency-review-2026-10-10.json) records image IDs and non-secret configuration only.
