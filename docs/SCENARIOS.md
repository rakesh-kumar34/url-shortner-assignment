# Three implemented scenarios

These scenarios correspond to actual commits in this repository. The brownfield work extends the greenfield baseline built in the same session; it is not presented as maintenance of an unrelated existing product.

| Scenario | Commit | Evidence |
| --- | --- | --- |
| Greenfield | `95bc345` — `feat: implement Java 17 Spring Boot greenfield URL shortener` | `CoreIntegrationTest`, V1 migration, controller/service/repositories |
| Brownfield | `322873a` — `feat: evolve link lifecycle and transactional daily analytics` | `LifecycleIntegrationTest`, `MigrationTest`, V2 migration |
| Ambiguous requirements | `ec57f00` — `feat: add durable idempotency and bounded request handling` | `ReliabilityIntegrationTest`, `RateLimiterTest`, V3 migration |

The verified local milestone snapshots were imported in sequence through the GitHub API; the commit IDs above identify their published counterparts. Inspect any increment with `git show <commit>`. Later delivery and review fixes build on these milestones.

## 1. Greenfield: a persistent short-link service

**Task.** Build a Java 17 service that creates, resolves, lists and inspects links, preserves the destination and protects management operations.

**Decomposition.** Define request/response records; create the V1 schema; implement URL validation and random-code generation; add transactional persistence and HTTP endpoints; exercise the public/authenticated boundary with real-database integration tests.

**Decisions.** Use Spring MVC, Security, JPA and Flyway in one service. Store the full destination, including query and fragment. Generate 12-character random codes and let database uniqueness settle conflicts. Assigned entity identifiers must insert new resources rather than merge over an existing alias.

**Acceptance.** Create returns 201, redirect returns 302, duplicate aliases return 409 without changing the original destination, unknown fields and malformed inputs are rejected, and management requires the bearer token. Pagination is bounded and deterministic.

**Observed result.** The initial tests could not run without an application baseline. After implementation, 17 test cases passed. The tests and commit preserve the inspectable result; this is not a fabricated multi-day development history.

## 2. Brownfield: lifecycle and daily analytics

**Task.** Add expiry, disable and daily reporting while retaining existing links and lifetime counts.

**Decomposition.** Add nullable lifecycle columns and the daily table in V2; inject a UTC clock; serialize link mutation with a database row lock; expose disable and analytics endpoints; test an actual populated V1-to-latest migration.

**Decisions.** Expiry takes effect at the exact boundary. Disable leaves a tombstone and never frees the alias. One transaction checks lifecycle and updates lifetime/daily counts; analytics failure rolls the whole resolution back. Historical daily activity is not reconstructed from an old lifetime total.

**Acceptance.** Existing V1 rows survive migration; expired/disabled resolutions return 410 and do not count; repeated disable returns 204; HEAD never counts; concurrent GETs lose no increments; daily buckets use UTC and include zero days.

**Observed result.** The added tests first exposed missing columns/endpoints and unsupported behavior. After the additive migration and implementation, all 24 cases passed. The rollback test deliberately forces a daily-write failure and checks that the lifetime counter remains unchanged.

## 3. Ambiguity: what does a retry mean?

**Task.** Resolve whether duplicate URLs should share a link, whether a timed-out creation may create another resource, and what a click count actually represents.

**Decision.** A destination is not the identity of a creation. Separate campaigns may use the same URL. An optional `Idempotency-Key` identifies the creation attempt; the service stores its hash and a fingerprint of normalized request fields in the same transaction as the link. Reusing a key with a changed request returns 409. Matching retries return 200 and the resource's current representation, including after expiry or disable.

**Reliability detail.** Database uniqueness coordinates simultaneous retries. A collision is handled after the failed transaction ends; random-code retries use fresh transactions and stop after five attempts. Analytics count committed GET resolutions, including repeated requests and bots, rather than unique people or proof that the destination loaded.

**Acceptance.** Concurrent matching retries produce one resource; changed payloads conflict; replay works after the original expiry; oversized bodies and invalid keys fail predictably; error responses carry a server-generated request ID; rate-limiter state is bounded.

**Observed result.** Six new integration cases first failed against the prior baseline. The limiter test initially failed to compile before its implementation existed. The completed increment passed 31 cases. Three additional review regression tests were subsequently added; their final result is tracked in [VALIDATION.md](VALIDATION.md).

## Human ownership

The engineer still needs to accept the trade-offs: one shared management credential, no destination reputation service, row-lock contention on hot links, and database write availability on the redirect path. [AI_WORK_LOG.md](AI_WORK_LOG.md) records AI-generated, edited and rejected choices. No human sign-off is claimed.
