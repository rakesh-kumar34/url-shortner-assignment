# AI task contracts and quality gates

AI supports analysis, implementation, tests, debugging and documentation within tasks. The candidate owns requirements, acceptance and the final submission. The contracts below are edited summaries of this project's tasks; they are not verbatim prompt transcripts.

## Greenfield contract

- **Intent:** produce a runnable Java 17 API that creates and resolves short URLs.
- **Context:** new Spring MVC service; JPA relational source of truth; no existing runtime behavior.
- **Constraints:** preserve query/fragment; use random codes and database uniqueness; protect management with bearer authentication; never fetch destinations.
- **Acceptance:** create 201, public redirect 302, missing 404, duplicate alias 409 without overwrite, unauthorized management 401, bounded pagination.
- **Requested AI output:** module/DTO/schema design, implementation and failing HTTP/database tests.
- **Disposition and iteration:** retained explicit DTOs and constructor injection; used a versioned entity to force inserts instead of merging assigned IDs; rejected sequential codes and imported reference code. The greenfield increment reached 17 passing cases.

## Brownfield contract

- **Intent:** add expiry, disable and daily analytics without losing existing records.
- **Context:** the working V1 baseline and its lifetime counter already exist; changes affect controller, service, entities and schema.
- **Constraints:** additive migration; no alias reuse; preserve lifetime totals; do not invent historic daily activity; one transaction for resolution and counters.
- **Acceptance:** populated V1 data survives upgrade, exact expiry returns 410, repeated disable 204, HEAD does not count, concurrent GETs retain all counts, a daily-write failure rolls back the total.
- **Requested AI output:** impact analysis, V2 migration, lifecycle implementation and boundary/concurrency tests.
- **Disposition and iteration:** retained row locks; rejected read/increment/save outside one transaction and a JVM-only correctness lock. Failing lifecycle/migration cases became a 24-case passing suite.

## Ambiguity contract

- **Intent:** make retries safe and define what an analytics count means.
- **Context:** POST may commit while the client times out; separate campaigns may share a destination; no visitor identity is collected.
- **Constraints:** atomic URL plus retry record; hash keys and length-prefix fingerprints; bounded collision retry in fresh transactions; no forced destination deduplication.
- **Acceptance:** concurrent matching retries return one resource, changed payload returns 409, replay still works after expiry, HEAD is excluded, request bodies/rate-limiter state are bounded.
- **Requested AI output:** alternatives, a chosen contract, V3 persistence and failure-focused tests.
- **Disposition and iteration:** chose current-state replay and committed GET resolutions, including repeats/bots. Rejected claims of unique visitors. This increment reached 31 cases before later review regressions.

## Final review iteration

An independent read-only code review found mismatched raw/decoded path classification and missing transaction-start error handling. A real HTTP probe showed an oversized `/a%70i/urls` request reaching the handler. New regressions first failed; the fixes use Spring's decoded path segments and return correlated 503 errors for transaction infrastructure failures. Browser checks cover disconnect cancellation so delayed private responses cannot restore cleared data. [Validation](VALIDATION.md) records current results.

The owner also requested consistent URL terminology. Application classes, API documentation, scripts and design files use the naming convention in [requirements](REQUIREMENTS.md#naming-and-compatibility); established database migrations and route aliases retain compatibility.

## Quality gates

| Gate | Executable evidence | Limit |
| --- | --- | --- |
| Analysis/review | Requirements map, architecture, independent code review, regression reproductions | AI-assisted verification supports the engineer's approval decision |
| Build/style | Java 17 enforcement and Checkstyle in `./mvnw verify` | Style checking is not a full static bug/security analyzer |
| Behavioral correctness | JUnit/MockMvc, real HTTP boundary test, H2 and PostgreSQL CI | Does not prove every failure mode |
| Schema safety | Populated-V1 migration and rollback tests; immutable migration checksums | Backup/restore and rolling deployment still need operational rehearsal |
| Dependency security | Resolved compile/runtime Maven coordinates checked against OSV | Point-in-time check; excludes JDK/container/build-tool advisories |
| Runtime/reliability | Packaged smoke, restart/replay, counter reconciliation and log assertions | Synthetic data and one local process |
| Browser/deployment | Docker Compose HTTP and Chromium desktop/mobile workflow | Chromium is not a cross-browser/accessibility audit |
| Performance | 100 redirects at concurrency eight with latency and count checks | A diagnostic sample, not sustained capacity or an SLA |

## Secure use and human control

Use synthetic URLs and test-only tokens in prompts, tests and screenshots. Do not provide production credentials, private URLs, customer data or the internal PDF to public artifacts. Treat generated source as a proposal, inspect the diff, reproduce failures and rerun the relevant gate. Record accepted, edited and rejected output with reasons.

Rakesh Kumar owns the engineering outcome and is the candidate reviewer and submission approver. [REVIEW_CHECKLIST.md](REVIEW_CHECKLIST.md) records the scope, evidence and decision; [AI_WORK_LOG.md](AI_WORK_LOG.md) records AI assistance. A production release, schema change against real data, credential change, destructive retention policy or change to authorization requires the relevant owner approval. No production deployment or approval is claimed here.
