# ADR-0010: Authenticated Redis infrastructure in Docker Compose

- Status: Accepted and implemented infrastructure; business caches and distributed controls remain separate decisions.
- Date: 2026-09-27.
- Author and dependency documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Authority: the owner explicitly requested Redis, fully containerized, while continuing the master plan.
- Related: [Docker foundation](0005-preproduction-docker-foundation.md), [organization access policy](0009-organization-scope-and-catalog-integrity.md).

## Context and decision drivers

IRR already runs its API and PostgreSQL through Docker Compose. Redis is requested as a shared infrastructure service. It must start reproducibly, use credentials from the ignored development .env, survive container replacement, be reachable from the backend and have real integration coverage. It must not become a second authoritative database or silently weaken current-role and membership revocation checks.

## Considered options

| Option | Advantages | Disadvantages | Outcome |
| --- | --- | --- | --- |
| Host-installed Redis | Easy for one developer | Environment drift, separate setup and unsupported Windows-native path | Rejected |
| One authenticated Redis service in Compose | Reproducible local runtime with shared network/health/persistence | Single point of failure, no high availability | Selected for development |
| Kubernetes, Redis Cluster or Sentinel | Distributed availability features | Adds operating complexity without a deployment requirement | Deferred |
| Automatically cache catalog permissions and stock | Appears faster | Stale authorization, inconsistent balances and undefined invalidation | Rejected |
| Configure the adapter and health first | Tested integration without changing business consistency | Infrastructure precedes a dedicated feature consumer | Selected |

## Decision

Use the official redis:8.2-alpine image pinned to sha256:b51665e66f00759be7c3152ad5ac3c66fb2f619c13ef62dea7cc1f9914524635. The inspected image reports Redis 8.2.10. This is a reproducible tested selection, not a claim that every dependency advisory is resolved. Upgrades require an intentional image digest change and verification.

The redis service runs as the image's redis user with a read-only root filesystem, writable /data named volume, temporary /tmp, dropped capabilities and no-new-privileges. Port 6379 is available only inside the Compose network; no host port is published. REDIS_PASSWORD is required and independently generated in the actual ignored .env. Backend credentials are injected through environment configuration. Development infrastructure administrators can inspect container environment/process state; this is not a production secret-manager design.

Redis enables append-only persistence, appendfsync everysec, a 128-MiB data limit and noeviction, with a 384-MiB container memory limit. This avoids silent eviction of future coordination state; reaching the memory limit rejects writes and requires operational handling. AOF survives container replacement when the volume is preserved, but asynchronous fsync can lose recent writes during abrupt failures. Redis remains non-authoritative, and volume persistence is not a backup or high-availability guarantee.

The backend adds spring-boot-starter-data-redis and uses Boot-managed Spring Data Redis/Lettuce versions. Redis repository scanning is disabled because business entities remain JPA-managed. Connection and command timeouts are two seconds. StringRedisTemplate is available for deliberate future consumers; no Java native serialization contract, automatic Spring cache or custom public Redis endpoint is introduced.

Compose waits for authenticated Redis PING and PostgreSQL health before starting the API. Readiness includes Redis and PostgreSQL; Redis failure therefore makes the API unready, while liveness remains process-focused. This is an explicit stack-readiness policy: it does not prove that every current business operation needs Redis. Recovery should restore Redis rather than restart a healthy database or erase volumes.

## Key and consistency policy

Future keys must be namespaced (for example irr:feature:organization-id:resource-id), use explicit TTLs for transient state and avoid raw passwords/tokens or sensitive unbounded payloads. No stock ledger, balance, membership, token validity or permission result is currently stored in Redis. PostgreSQL remains authoritative for transactions, stock integrity and access checks. Rate limiting, cache invalidation, idempotency and session revocation each require their own failure semantics and acceptance tests before activation.

## Operations and migration

Set REDIS_PASSWORD in .env (already provisioned locally), then run docker compose up --build -d --wait. No relational migration is needed for Redis. Existing PostgreSQL/attachment volumes remain intact; Compose adds redis_data. A separate Docker environment file is unnecessary. Redis is accessed through docker compose exec, not a published host port. The README provides authenticated PING, health, restart and test steps without printing credentials.

Changing REDIS_PASSWORD requires recreating both redis and backend so the server and client agree. Do not use docker compose down -v as routine troubleshooting. A fresh empty Redis volume is acceptable only when feature-specific recovery semantics explicitly allow it; persistence testing uses a temporary namespaced key and removes that key afterward.

## Consequences and verification

The local stack gains another required healthy service and a small memory/storage budget. It does not gain Kubernetes or distributed availability. Integration fixtures create authenticated disposable Redis and PostgreSQL containers with independent test credentials and random ports. Redis tests verify authenticated read/write, TTL expiry, denied unauthenticated commands and health connectivity. Runtime verification must check .env-backed PING, no host port exposure, backend readiness, container recreation persistence and cleanup of the temporary key.

## References

- [Redis Docker operation guide](https://redis.io/tutorials/operate/orchestration/docker/).
- [Redis persistence semantics](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/).
- [Spring Data Redis 3.5 integration](https://docs.spring.io/spring-data-redis/reference/3.5/redis/getting-started.html).
- [Spring Boot Redis configuration properties](https://docs.spring.io/spring-boot/3.5/api/java/org/springframework/boot/autoconfigure/data/redis/RedisProperties.html).

## Executed evidence (2026-09-27)

- Full verification passed (87 unit/integration tests). The resolved graph uses Spring Data Redis 3.5.4 and Lettuce 6.6.0.RELEASE.
- Compose image build/start succeeded; PostgreSQL, Redis and API are healthy. Redis runs as UID 999 with no published host port.
- Authenticated PING using the actual .env returned PONG; unauthenticated PING returned NOAUTH. A temporary TTL key survived Redis container recreation with AOF enabled and was then removed.
- Stopping Redis made readiness return 503 while liveness remained 200; recreating Redis restored readiness without restarting the database.
- The Redis implementation/test commit is c5cc35c. Official Swagger remains the HTTP contract reference; Redis has no public API endpoint.
