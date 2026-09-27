# URL Shortener design brief

## Purpose
An original Java 17 / Spring Boot URL shortener for the provided engineering assignment and Java role. The assignment is stack-neutral; the owner requested Java 17 and Spring Boot. The supplied candidate repository is reference-only. No reference code or internal assignment PDF is included.

## Architecture and alternatives
Use Spring MVC, Spring Data JPA, PostgreSQL, Flyway and Maven. Use H2 in PostgreSQL mode for an easy local demo and fast integration tests; run an additional PostgreSQL integration suite in CI. Use a same-origin vanilla browser console. Prefer a modular single service to microservice deployment overhead at prototype scale. No Redis/Kafka are required: transactionally updating total/daily counters is easier to validate. Trade-off: hot-link redirects serialize on the link row and depend on database write availability.

Node.js was initially considered for dependency-free setup; the owner supplied the Java requirement before implementation. Java is now the sole backend. Preserve this decision honestly in the execution record.

## Component boundaries
`api/`: controllers and DTOs. `service/`: URL policy and transactional service. `persistence/`: JPA entities/repositories. `config/`: configuration validation, security and rate limits. `resources/db/migration`: additive Flyway schema evolution. `resources/static`: web console. Constructor injection and a Clock bean make time-dependent policy testable.

## API contract
Management requires a bearer token. Public GET/HEAD `/s/{code}` resolves links. POST `/api/urls` accepts url, title, customAlias and expiresAt. Random codes use 72 bits of SecureRandom entropy (12 base64url characters); custom aliases are case-sensitive, 4-32 URL-safe characters. Unknown fields and invalid values are rejected. Duplicate aliases return 409. Duplicate destinations are permitted. List results have bounded page sizes. GET `/api/urls/{code}/stats` returns lifetime total and zero-filled UTC buckets for the last 30 days.

Expiry is an absolute timestamp, future and at most 365 days ahead. DELETE is an idempotent soft-disable; codes are never recycled. Expired/disabled redirects return 410; missing links return 404. Redirects use 302 and no-store. HEAD does not increment clicks. Analytics count committed GET resolutions including bots/repeats, not unique people or proof of visits. No visitor IP, agent, referrer or raw click event is stored.

Optional Idempotency-Key is retained for the resource lifetime. In the same database transaction, persist link and hashed key/fingerprint. Matching retries return the same resource with its current representation and 200; mismatches return 409. Retries of a successful creation work after the requested expiry. Random-code collisions retry in fresh transactions; custom-alias collisions never overwrite data.

## Reliability and security
Use row locks and one transaction for resolution, total increments and daily aggregation. All instances share database uniqueness and lock semantics. A failed analytics transaction rolls back and returns 503, not an uncounted redirect. Pessimistic locks trade hot-link throughput for clear consistency.

Use Spring Security stateless bearer authentication; no session cookies, so disable CSRF on the API only and allow no cross-origin API access. The token stays in browser memory. API keys are one operator credential, not user/tenant authorization. Require >=32 characters and HTTPS public origin in the production profile. Public creation and multi-tenancy are out of scope.

Only absolute HTTP(S) destinations; reject userinfo, controls, local-only hosts, private literal IPs and self-origin redirects. Never fetch destination URLs. This hygiene policy does not detect phishing or DNS changes. Bounded JSON bodies (8 KiB), pagination and process-local rate limiting; ignore X-Forwarded-For. Rate limits are per instance and require an edge policy for multi-instance production. Redact secrets and destination/query data from logs. Use request IDs, restrictive security headers and safe browser text rendering.

## Three real scenarios
1. Greenfield: persistent create/resolve/inspect baseline and tests.
2. Brownfield: additive migration adding expiry, disable and daily analytics while retaining existing rows and total counts.
3. Ambiguous: choose and implement retry/analytics semantics, then test concurrent/replayed requests, HEAD and boundaries.

## Validation and ownership
JUnit/MockMvc on real H2; PostgreSQL-backed tests in CI; migration tests; concurrency, collision, rollback, authentication and malformed input checks; JaCoCo coverage; style/static checks; packaged HTTP smoke and a local benchmark. Provide OpenAPI, Docker Compose, CI, setup, architecture, AI task record, scenario traceability and final summary. Record unexecuted checks explicitly. No model is used at application runtime. The owner controls acceptance; the completed delegated submission review is recorded in [REVIEW_CHECKLIST.md](REVIEW_CHECKLIST.md). Production approval remains separate. Never fabricate personal human review, production readiness or a multi-day timeline.
