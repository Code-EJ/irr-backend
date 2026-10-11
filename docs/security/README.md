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
