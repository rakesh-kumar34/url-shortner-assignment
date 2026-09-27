# Risk register

| Risk / failure | Control and evidence | Remaining limit / next decision |
| --- | --- | --- |
| Collision overwrites a URL | Random code, unique primary key, forced insert and bounded fresh-transaction retries; collision regression | Codes are identifiers, not authorization secrets |
| Retry duplicates a resource | Atomic hashed key/fingerprint and URL creation; concurrent replay/conflict tests | Keys/tombstones retained for resource lifetime |
| Counter drift or lost updates | Database row lock; total/daily writes in one transaction; parallel and forced-rollback tests | Hot URLs serialize; decide on outbox/event semantics before scaling |
| Database or connection pool unavailable | Readiness includes DB; correlated 503 and Retry-After; transaction-start regression | Redirects depend on write availability; no availability SLA |
| Encoded routes bypass limits | Guard and router use decoded path segments; real HTTP body/rate-limit regression | Per-instance limits need a trusted edge for multiple replicas |
| Malicious destination or unsafe URL | HTTP(S)-only, bounds, local literal/credential/control rejection; no target fetching | Public destinations can still be harmful; no reputation/DNS safety guarantee |
| Private management data exposed | Bearer boundary, memory-only token, safe DOM rendering and cancellation on disconnect | One operator credential; add OIDC/ownership for multi-user deployment |
| Old data breaks after enhancement | Additive V1–V3 migrations, populated-V1 test, retained schema names and route aliases | Rehearse backups, restore and rolling upgrade for production |
| Dependency vulnerability | OSV audit; Tomcat family pinned to patched version; automated dependency updates | Audit is point-in-time and does not cover every software layer |
| AI-generated error or overstated evidence | Task contracts, independent review, red/green regressions, explicit limits | Owner-authorized delegated acceptance is recorded; the candidate retains ownership and must be able to explain the submission |
| Misleading analytics/performance | Counts explicitly mean committed GET resolutions; benchmark methodology recorded | Not unique visitors, delivery confirmation or production capacity |

Production priorities are separate from the interview prototype: distributed abuse prevention, identity/ownership, monitoring and alerting, tested backup/recovery, deployment TLS and load testing against agreed traffic. They should follow actual requirements rather than being added solely to increase component count.
