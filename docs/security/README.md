# Pre-release security review — 2026-10-10

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Application dependencies: individual dispositions

The npm seven entries are two root advisories propagated through build dependencies. Production-only npm audit is zero, not a statement about images or build safety. The source-map-js 1.2.1 finding was corrected to 1.2.2 in frontend commit 511f461 with lint/types/build and 136 tests; it is outside the remaining seven entries.

| Entry | Current scope / cause | Disposition and evidence | Remaining action |
| --- | --- | --- | --- |
| braces 3.0.3, high | Tailwind content discovery; GHSA-vfj7-8cjw-p6xm, nested-pattern exhaustion | MITIGATED, not patched: trusted repository patterns and explicit extension paths; not exposed to HTTP input | No published compatible braces fix identified; upstream maintenance or deliberately verified toolchain migration |
| chokidar, high | Propagates braces through Tailwind file watching | MITIGATED with the same build input boundary; no independent patch claim | Resolve root and verify watching/build |
| micromatch, high | Propagates braces pattern parser | MITIGATED with the same boundary | Resolve root; do not suppress |
| fast-glob, high | Propagates micromatch/braces | MITIGATED with the same boundary | Resolve root and verify content discovery |
| postcss-selector-parser 6.1.4, moderate | GHSA-rj75-hqrm-r3gf, quadratic selector parsing; published fix 7.1.6 | MITIGATED: CSS/source under repository control, no user-provided stylesheet compilation. Upstream excludes ordinary trusted-source builds from vulnerable usage | Retained 6.x to 7.x requires compatibility review; not silently overridden |
| postcss-nested 6.2.0, moderate | Propagates selector-parser | MITIGATED with trusted CSS boundary | Compatible upstream dependency resolution, then styles/visual checks |
| tailwindcss 3.4.17, high | Aggregates both advisory chains | MITIGATED, no runtime Tailwind compiler; existing stack preserved | npm proposes Tailwind 4; deliberate migration and browser verification required before adoption |
| spring-webmvc 6.2.19, GHSA-j9f9-w8pj-32f8 / CVE-2026-47890 | SSE rendering prerequisite; scanner CRITICAL vs vendor LOW | MITIGATED conditional feature exposure. `MvcSecuritySurfaceIT` inspects loaded controllers, explicit return types and media declarations; two integration tests passed at 436b75b | Framework remains affected; future handlers/auto-configuration must keep guard passing; evaluate supported patch availability |
| spring-webmvc 6.2.19, GHSA-pc63-qcmh-9cmg / CVE-2026-47884 | XSLT implicit view resolution; scanner CRITICAL vs vendor MEDIUM | MITIGATED conditional feature exposure. Loaded context has no XsltView/XsltViewResolver; maintained application handlers are RestController, without View/ModelAndView returns | Not framework remediation; unsupported configurations/dynamic delegates are outside guard proof. Vendor 6.2.20 enterprise patch versus 7.0.9 OSS requires explicit compatibility/support decision |

Mitigation means bounded observed exposure, not vulnerability removal or release acceptance. No item is labelled not-applicable solely because grep found no direct call. The loaded-context tests cover current configuration and declared handlers; they are not whole-program reachability proof. Development/build pipelines must reject unreviewed source and avoid compiling customer-supplied CSS/globs. Owner security acceptance remains pending.

Primary references: [braces advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm), [selector-parser advisory](https://github.com/advisories/GHSA-rj75-hqrm-r3gf), [Spring SSE advisory](https://spring.io/security/cve-2026-47890/), [Spring XSLT advisory](https://spring.io/security/cve-2026-47884/). Exact npm/Maven resolutions and earlier dated observations remain in each repository's dependency documentation.

## Image scan evidence

Grype 0.120.1 official Windows archive SHA-256: `32e3c811f31822d17592908bafdc6288aaaca3d52583c479167a8dc8399ed65d`. Vulnerability database built 2026-10-10T06:30:01Z. Scout authentication did not prevent this alternative scan. Exit 0 means scan completed; no severity threshold was applied and it does not mean clean.

| Actual local image | Matches | Critical / high | State |
| --- | --- | --- | --- |
| Backend original | 298 | 15 / 62 | Reviewed inventory captured; individual OS/backport analysis pending |
| Frontend original | 260 | 29 / 121 | Compatible runtime patch implemented; before evidence retained |
| Frontend patched candidate | 4 | 0 / 0 | Nginx/Alpine corrections verified; four medium residual CPE matches pending |
| PostgreSQL 16 Alpine | 144 | 8 / 78 | Pending compatible image update and persisted-volume startup verification |
| Redis 8.2 Alpine | 31 | 0 / 9 | Pending individual vendor/package review |
| Local Mailpit | 39 | 1 / 16 | Pending local image update; not a production mail dependency |

JSON files alongside this runbook retain every match with package/version/type, scanner severity, namespace and advertised fix. Findings remain `pending` until individual evidence supports another state. No findings were suppressed. Grype JSON carries matched packages, not a complete SBOM; separate OS package lists provide backend/frontend inventories. Java resolution inventory is separate. Node/Maven build-base scanning remains pending. Original runtime images include their base packages, but this does not substitute for scanning discarded build stages.

Backend binary CPE matches for upstream curl/OpenSSL overlap Ubuntu versioned/backported packages. Do not count them as unique exploits or dismiss them collectively as false positives. Compare each identifier with Ubuntu package versions and vendor status before classification. Vendor `not-fixed`, `wont-fix` and empty fix states remain visible. The four patched frontend medium matches are CVE-2025-60876 on three BusyBox packages and CVE-2026-58055 on nghttp2-libs; their fix state is empty in this scan and applicability is pending.

## Reproduce without exposing image configuration

Download the official [Grype release](https://github.com/anchore/grype/releases/tag/v0.120.1), verify the published checksum and keep the executable under ignored `.local/tools/`. Run `grype docker:<actual-image-or-id> --output json --file <private-report.json>`. Keep raw reports private: image configuration may include sensitive values. Run `node scripts/summarize-image-scan.mjs <private-report.json> <review-inventory.json>` to project only finding/package metadata and source digests. Preserve scan logs, final image inspect IDs, tool/database versions and immutable release manifests. Runtime/build scans and npm/Maven audits answer different questions.

Before release, review all pending high/critical matches and actual applicability, patch compatible vendor packages, rebuild/re-scan and verify the matching API pair. This register is evidence of work and remaining risk, not a clean security certificate.

## Backend patch and build-base scan checkpoint

The subsequent backend build completed with overridable `MAVEN_OPTS=-Xmx1024m -XX:ActiveProcessorCount=2`, retaining existing Maven/Spotless goals and build-cache identifier. This bounds build resources; it does not prove the cause of prior Docker Desktop failures. Jammy updates changed libpng16-16 to 1.6.37-3ubuntu0.7 and libfreetype6 to 2.11.1+dfsg-1ubuntu0.4. Java 21 and runtime UID 10001:10001 remain.

Candidate image `sha256:549937278a937fc9d58eaa2d5ec7a80d75e9a8714cba4c97c6a8b4100275b8b5` returned **290 matches, 15 critical / 61 high**, versus 298 / 15 / 62 before. No high/critical matches remain with artifact type `deb`; upstream binary CPE and the two Spring findings remain. Startup with retained database/Redis/uploads passed readiness, six actual read-only frontend/backend pair checks passed and the 116-operation contract check passed. No migration reset or business command was executed. The scan is not clean.

Previously missing build-base scans now completed with the same Grype/database versions:

| Build base | Matches | Critical / high | State |
| --- | --- | --- | --- |
| Pinned Node 24 Alpine | 51 | 0 / 17 | Node 24.21.0/npm 11.19.0; npm-internal and OS findings need individual remediation |
| Maven 3.9.14 / Temurin 21 | 921 | 11 / 44 | Builder JDK, Maven archive dependencies and Ubuntu packages need vendor/backport review |

The current Node tag resolves to the same pinned digest; pulling it alone does not patch findings. Build bases are discarded from runtime but remain supply-chain exposure. Application npm lockfile audit does not cover npm's bundled dependencies. These are raw matches, not unique exploitable defects. Sanitized inventories are stored alongside this register. Build-base scanning is complete; remediation is not.

### Reviewed critical Ubuntu CPE entries

The candidate installs curl/libcurl **7.81.0-1ubuntu1.29** and OpenSSL/libssl **3.0.2-0ubuntu1.30**. Each row below has specific vendor evidence for the installed Jammy package. Raw scanner findings/counts remain preserved; no suppression or override is introduced.

| Identifier | Jammy vendor status | Disposition for candidate |
| --- | --- | --- |
| [CVE-2022-32207](https://ubuntu.com/security/CVE-2022-32207) | curl fixed 7.81.0-1ubuntu1.3 | CORRECTED by installed vendor backport |
| [CVE-2023-23914](https://ubuntu.com/security/CVE-2023-23914) | curl fixed 7.81.0-1ubuntu1.8 | CORRECTED by installed vendor backport |
| [CVE-2026-8924](https://ubuntu.com/security/CVE-2026-8924) | curl fixed 7.81.0-1ubuntu1.25 | CORRECTED by installed vendor backport |
| [CVE-2026-8927](https://ubuntu.com/security/CVE-2026-8927) | curl fixed 7.81.0-1ubuntu1.25 | CORRECTED by installed vendor backport |
| [CVE-2026-18924](https://ubuntu.com/security/CVE-2026-18924) | curl fixed 7.81.0-1ubuntu1.29 | CORRECTED by installed vendor backport |
| [CVE-2026-19931](https://ubuntu.com/security/CVE-2026-19931) | curl not affected | NOT APPLICABLE with vendor package evidence |
| [CVE-2026-34182](https://ubuntu.com/security/CVE-2026-34182) | OpenSSL fixed 3.0.2-0ubuntu1.25 | CORRECTED by installed vendor backport |
| [CVE-2026-31789](https://ubuntu.com/security/CVE-2026-31789) | OpenSSL fixed 3.0.2-0ubuntu1.23 | CORRECTED by installed vendor backport |
| [CVE-2026-75803](https://ubuntu.com/security/CVE-2026-75803) | OpenSSL fixed 3.0.2-0ubuntu1.29 | CORRECTED by installed vendor backport |

Remaining critical CPE entries CVE-2025-15467, CVE-2024-5535, CVE-2022-32221 and CVE-2026-11856 remain pending; exact vendor-page requests failed/timed out. No conclusion is extrapolated from other identifiers. Other high/severity findings also need individual review. Spring retains the conditional-feature mitigation and framework maintenance decision described above.

Reviewed dispositions in the dated backend-patched JSON are manual annotations supported by the vendor table above. Re-running summarize-image-scan.mjs regenerates pending states; write a new scan file rather than overwrite an annotated inventory. Preserve the original scanner hash/counts and the reviewed record when comparing later candidates.

### Storage-bootstrap candidate review

[The subsequent candidate scan](backend-storage-bootstrap-grype-2026-10-10.json) targets config image ID `54d71eedd701227275b7e8f485de39c4d89ead030f3b1bdeb6e83f47639b4d2a`, built from backend `89e9945`. Docker reports the running OCI image index as `a6f1b7d07f99d459b5c33afedf2e6bee18f40b7c2d863f62481d4ebff5ea3034`; these are different digest objects, not interchangeable identifiers. Grype 0.120.1 with the same dated database completed: 290 matches, including 15 critical and 61 high. The entrypoint change does not change package versions. All four installed curl/libcurl/OpenSSL/libssl versions were rechecked in the actual running container; nine existing exact finding/package vendor dispositions carry forward explicitly.

The four previously inaccessible vendor pages now have exact Ubuntu security-notice evidence:

| Identifier | Jammy fixed version | Installed candidate version | Disposition |
| --- | --- | --- | --- |
| [CVE-2025-15467 / USN-7980-1](https://ubuntu.com/security/notices/USN-7980-1) | OpenSSL 3.0.2-0ubuntu1.21 | 3.0.2-0ubuntu1.30 | CORRECTED vendor backport |
| [CVE-2024-5535 / USN-6937-1](https://ubuntu.com/security/notices/USN-6937-1) | libssl3 3.0.2-0ubuntu1.17 | 3.0.2-0ubuntu1.30 | CORRECTED vendor backport |
| [CVE-2022-32221 / USN-5702-1](https://ubuntu.com/security/notices/USN-5702-1) | curl/libcurl 7.81.0-1ubuntu1.6 | 7.81.0-1ubuntu1.29 | CORRECTED vendor backport |
| [CVE-2026-11856 / USN-8651-1](https://ubuntu.com/security/notices/USN-8651-1) | curl/libcurl 7.81.0-1ubuntu1.26 | 7.81.0-1ubuntu1.29 | CORRECTED vendor backport |

All 13 critical binary CPE entries now have individual backport/not-affected evidence in this candidate. The two critical-classified Spring findings retain their conditional-feature mitigation and pending framework support/release decision. Raw severity/counts are unchanged. The 61 high and other pending matches, infrastructure/build-image inventories and frontend build/runtime residual risks still require review. This is not a clean security certification. Earlier dated reports remain historical rather than overwritten.
