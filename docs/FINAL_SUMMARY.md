# Delivery summary

This project implements the URL-shortener assignment as an original Java 17 / Spring Boot service, aligned with the requested Java role. It includes a same-origin browser console and REST API, persistent storage, an OpenAPI contract, Docker Compose configuration and CI. The supplied candidate repository was reference-only; no source code was copied and the internal assignment PDF is excluded.

## Delivered behavior

- Authenticated create, list, inspect, analytics and soft-disable operations; public GET/HEAD resolution.
- Full destination preservation, random codes, custom aliases, validation and collision handling.
- Expiry, non-reusable disabled codes, lifetime counts and 30 UTC daily buckets.
- Durable idempotency with concurrent retry handling and replay after expiry.
- Additive Flyway migrations, request limits, correlation IDs, safe error responses and process-local rate limits.
- A persistent H2 local demo, PostgreSQL deployment configuration and an executable verification workflow.

The code uses Java records, constructor injection, Spring Security, Bean Validation, Spring Data JPA and explicit transaction boundaries. It is a modular single service; no LLM, Redis or Kafka is needed at runtime.

## Engineering evidence

The three real scenario commits are `95bc345` (greenfield), `322873a` (brownfield lifecycle/analytics) and `ec57f00` (retry ambiguity and bounded requests). [SCENARIOS.md](SCENARIOS.md) maps the task, decisions, acceptance criteria and tests to each increment. [AI_WORK_LOG.md](AI_WORK_LOG.md) records AI assistance and the engineer's ownership boundary.

Final Maven verification passed **36 cases with no failures, errors or skips**, with zero Checkstyle violations. JaCoCo recorded 93.7% line and 69.3% branch coverage. Review regression tests exposed and drove fixes for encoded URL length, expiry precision, encoded-route limits and storage-start errors. The final OSV audit reported no findings across 100 resolved runtime packages after the Tomcat update.

The packaged JAR passed HTTP smoke checks, retained its link and idempotency result across an actual process restart, and omitted the tested token/destination values from logs. A local H2 run completed 100 redirects at concurrency eight with all 100 counted; its 192.8 requests/second result is not a production capacity claim. [VALIDATION.md](VALIDATION.md) records the measurements and remaining gaps.

## Remaining limits and acceptance

GitHub Actions completed H2 verification, the dependency audit and PostgreSQL integration tests successfully for the reviewed application. The final review also passed all 36 cases on PostgreSQL, built and exercised Docker Compose, and passed the desktop/mobile Chromium workflow. See the linked CI evidence in VALIDATION.md. Repository: [rakesh-kumar34/url-shortner-assignment](https://github.com/rakesh-kumar34/url-shortner-assignment). Human submission sign-off remains pending.

The prototype has one operator credential and process-local admission limits. Hot links serialize database writes; analytics count committed resolutions, not unique people or confirmed destination visits. It does not claim production capacity, an SLA, multi-tenancy or destination safety scanning. These are deliberate scope boundaries with follow-up paths in [ARCHITECTURE.md](ARCHITECTURE.md).

Before submission, complete the [review checklist](REVIEW_CHECKLIST.md), confirm the final verification record, and inspect the published repository. Before production use, separately review authentication, abuse handling, deployment, backup/recovery and load requirements.
