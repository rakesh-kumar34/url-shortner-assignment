# Shortline Java implementation plan

> **For agentic workers:** Use superpowers:executing-plans. Check off actual evidence only.

**Goal:** Original Java 17 / Spring Boot URL shortener and complete engineering evidence.
**Architecture:** REST/security adapter → transactional link service → JPA/PostgreSQL; H2 demo profile; same-origin console.
**Tech Stack:** Java 17, Spring Boot, Spring MVC/Security/Data JPA, Flyway, Maven, JUnit, JaCoCo.
**Spec:** `docs/superpowers/specs/2026-09-27-shortline-design.md`

## Global constraints
Java 17. Aliases 4-32 characters, random codes 12 characters. JSON 8 KiB. UTC expiry <=365 days. Authenticated management. No reference code, secrets, customer data or assignment PDF. Human sign-off pending.

## Review focus
Replay after expiry; HEAD/disabled/expired counters; transaction rollback; populated v1 migration; concurrent idempotency and redirects.

## Task 1: Greenfield
Files: pom.xml, Application.java, api/LinkController.java, link/LinkService.java, link/UrlPolicy.java, persistence/*, config/*, V1__links.sql, CoreIntegrationTest.java.
Interfaces: LinkService.create(CreateLinkRequest, String), get(String), resolve(String, boolean), list(int,int); CreateResult(LinkResponse, boolean).
- [ ] Write failing real-database/HTTP tests for create, redirect, validation, aliases, auth and pagination.
- [ ] Run Maven tests; observe missing core behavior.
- [ ] Implement core with dependency injection and explicit transactions.
- [ ] Run all tests; commit greenfield baseline.

## Task 2: Brownfield
Files: V2__lifecycle_analytics.sql, entity/service changes, LifecycleIntegrationTest.java, MigrationTest.java.
Interfaces: disable(String), stats(String); expiry in CreateLinkRequest.
- [ ] Add failing populated-v1 migration, expiry, disable, daily and concurrent-hit tests.
- [ ] Implement additive migration, locked resolution and atomic counts.
- [ ] Run complete suite; commit brownfield enhancement.

## Task 3: Ambiguity and hardening
Files: V3__idempotency.sql, Idempotency entity/repository, rate-limit/request filters, ReliabilityIntegrationTest.java.
- [ ] Add failing replay/conflict/restart/HEAD/body-limit/rate-limit tests.
- [ ] Implement durable fingerprinted retries, short independent transactions, bounded admission.
- [ ] Run complete suite; commit decisions and behavior.

## Task 4: Reviewable delivery
Files: static UI, openapi.yaml, Dockerfile, compose.yaml, CI, tools, README and docs.
- [ ] Build responsive create/list/stats/disable console.
- [ ] Document architecture, assumptions, three scenarios, prompts, generated/edited/rejected choices, risks and human review gate.
- [ ] Run full verify with coverage, packaged HTTP smoke, local benchmark and PostgreSQL suite when available.
- [ ] Fresh review; fix material findings with regression tests.
- [ ] Publish a new repository under rakesh-kumar34 using authenticated GitHub access; verify link and contents.
