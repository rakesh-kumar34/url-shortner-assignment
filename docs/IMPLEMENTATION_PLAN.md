# URL Shortener Java implementation plan

**Goal:** Original Java 17 / Spring Boot URL shortener and complete engineering evidence.
**Architecture:** REST/security adapter → transactional link service → JPA/PostgreSQL; H2 demo profile; same-origin console.
**Tech Stack:** Java 17, Spring Boot, Spring MVC/Security/Data JPA, Flyway, Maven, JUnit, JaCoCo.
**Spec:** [DESIGN.md](DESIGN.md)

## Global constraints
Java 17. Aliases 4-32 characters, random codes 12 characters. JSON 8 KiB. UTC expiry <=365 days. Authenticated management. No reference code, secrets, customer data or assignment PDF. Candidate reviewer and submission approver: Rakesh Kumar; see [REVIEW_CHECKLIST.md](REVIEW_CHECKLIST.md).

## Review focus
Replay after expiry; HEAD/disabled/expired counters; transaction rollback; populated v1 migration; concurrent idempotency and redirects.

## Task 1: Greenfield
Files: pom.xml, UrlShortenerApplication.java, api/UrlController.java, service/UrlService.java, service/UrlPolicy.java, persistence/*, config/*, V1__links.sql, CoreIntegrationTest.java.
Interfaces: UrlService.create(CreateUrlRequest, String), get(String), resolve(String, boolean), list(int,int); CreateResult(UrlResponse, boolean).
- [x] Write failing real-database/HTTP tests for create, redirect, validation, aliases, auth and pagination.
- [x] Run Maven tests; observe missing core behavior.
- [x] Implement core with dependency injection and explicit transactions.
- [x] Run all tests; commit greenfield baseline.

## Task 2: Brownfield
Files: V2__lifecycle_analytics.sql, entity/service changes, LifecycleIntegrationTest.java, MigrationTest.java.
Interfaces: disable(String), stats(String); expiry in CreateUrlRequest.
- [x] Add failing populated-v1 migration, expiry, disable, daily and concurrent-hit tests.
- [x] Implement additive migration, locked resolution and atomic counts.
- [x] Run complete suite; commit brownfield enhancement.

## Task 3: Ambiguity and hardening
Files: V3__idempotency.sql, Idempotency entity/repository, rate-limit/request filters, ReliabilityIntegrationTest.java.
- [x] Add failing replay/conflict/restart/HEAD/body-limit/rate-limit tests.
- [x] Implement durable fingerprinted retries, short independent transactions, bounded admission.
- [x] Run complete suite; commit decisions and behavior.

## Task 4: Reviewable delivery
Files: static UI, openapi.yaml, Dockerfile, compose.yaml, CI, tools, README and docs.
- [x] Build responsive create/list/stats/disable console.
- [x] Document architecture, assumptions, three scenarios, prompts, generated/edited/rejected choices, risks and human review gate.
- [x] Run full verify with coverage, packaged HTTP smoke, local benchmark. PostgreSQL runs in GitHub Actions; see [validation evidence](VALIDATION.md).
- [x] Fresh review; fix material findings with regression tests.
- [x] Publish to the owner-selected `rakesh-kumar34/url-shortner-assignment` repository; verify branch and file contents.
