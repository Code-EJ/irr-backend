# Backend dependency register

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Audit date: 2026-09-27T17:50:35.214Z. The resolved Maven graph contains 156 distinct coordinates across production and test scopes. OSV returned 0 matching advisories. This is a point-in-time coordinate scan, not a reachability proof, container OS scan or security guarantee.

## Decisions

Use Java 21 and Spring Boot 3.5.16 for the existing Spring 6 / Jackson 2 compatibility baseline. A separate Boot 4 migration must revisit framework behavior, security, Jackson integration and springdoc compatibility; it is not silently bundled into this domain overhaul. Version selection does not itself establish a support entitlement. Remove spring-boot-devtools from the application and delete the unused ReportPort/ReportService stub; reporting now has a real feature module.

| Explicit override | Version | Reason |
| --- | --- | --- |
| Jackson BOM | 2.21.5 | Resolve advisories found against 2.21.4; align the whole family |
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

On Windows use .\mvnw.cmd. The audit script queries the public [OSV API](https://google.github.io/osv.dev/api/), records coordinate/advisory evidence under .local/audit and exits nonzero on findings or API failures. It sends package coordinates only. Review advisories and upgrade safely; never suppress a scanner failure as an empty result. CI publishes this package-only report, never environment or backup files.

Official references: [Spring Boot 3.5 documentation](https://docs.spring.io/spring-boot/3.5/reference/index.html), [Java/system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Maven Central](https://repo.maven.apache.org/maven2/), [springdoc](https://springdoc.org/). Historical findings and rejected major-upgrade combinations remain in ADR-0003; current execution is ADR-0013.
