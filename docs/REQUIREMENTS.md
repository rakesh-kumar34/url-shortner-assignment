# Assignment requirements and evidence

This map covers the supplied PDF's eight core requirements and five deliverables. The PDF is stack-neutral; Java 17 and Spring Boot implement the owner's separate role preference. The internal PDF is not redistributed. Evidence describes the actual build session and follow-up reviews. The PDF describes a 2–3-day working period; this repository does not assert that 2–3 full days elapsed.

## Core requirements

| Requirement | Implementation / evidence | Assessment |
| --- | --- | --- |
| 1. Understand intent and ambiguity | [Design](DESIGN.md), [scenarios](SCENARIOS.md): destination preservation, duplicate URLs, retry identity, expiry and the meaning of analytics are explicit | Covered |
| 2. Decompose tasks and sequence dependencies | [Implementation plan](IMPLEMENTATION_PLAN.md): baseline → additive lifecycle/schema work → durable retries → validation and delivery | Covered |
| 3. Reason about an existing codebase | Brownfield scenario identifies controller, service, entities, migration and analytics flow; `MigrationTest` upgrades populated V1 data | Covered |
| 4. Use AI with task contracts, iteration and traceability | [Execution record](AI_WORK_LOG.md), [task contracts and gates](AI_WORKFLOW.md): generated/edited/rejected choices, actual failures, fixes, secure context, and owner approval boundaries | Covered; candidate acceptance recorded |
| 5. Generate engineering artifacts | Java source, explicit API DTOs, [OpenAPI](../src/main/resources/static/openapi.yaml), Flyway migrations, tests, Maven wrapper, Docker Compose and CI | Covered |
| 6. Validate and control risks | [Validation](VALIDATION.md), [risk register](RISKS.md): transactions, migration, concurrency, abuse boundaries, dependency scan and measured performance | Covered within stated prototype scope |
| 7. Maintain engineer-led oversight | Rakesh Kumar selected the stack, reference-only approach and terminology and is the candidate reviewer/submission approver. AI assistance and technical verification are recorded separately from [candidate acceptance](REVIEW_CHECKLIST.md) | Engineer ownership and candidate acceptance recorded |
| 8. Summarize the engineering outcome | [Final summary](FINAL_SUMMARY.md): artifacts, rationale, assumptions, validation, trade-offs and limits | Covered |

## Required deliverables

| Deliverable | Where to review |
| --- | --- |
| Runnable end-to-end prototype | [README quick start](../README.md#run-locally); authenticated management console; public redirects; HTTP/browser smoke scripts |
| Architecture, components, tools, control flow and decisions | [Architecture](ARCHITECTURE.md), [design](DESIGN.md), [AI workflow](AI_WORKFLOW.md) |
| Three scenarios with decomposition, execution and validation | [Scenarios](SCENARIOS.md); linked greenfield, brownfield and ambiguity milestone commits |
| Setup instructions | [README](../README.md), `.env.example`, `compose.yaml`, Maven wrapper |
| Testing approach, limitations and trade-offs | [Validation](VALIDATION.md), [risks](RISKS.md), [architecture](ARCHITECTURE.md) |

## Reference comparison

The reference repository uses a Spring Boot/Maven layout, migrations, a setup README, architecture/decision documents, scenario evidence, an AI workflow and a review checklist. This submission follows those useful categories with original implementation and project-specific evidence. It adds a browser console, executable H2/PostgreSQL verification, dependency auditing, packaged-runtime checks and focused regressions for issues found during review.

The reference's Java 21, Redis, Kafka, URL query stripping, geographic analytics and numbered template folders are implementation choices, not PDF requirements. This project uses Java 17, preserves complete destinations and keeps transactional analytics in a single service. [Architecture](ARCHITECTURE.md) explains the hot-URL and availability costs. More infrastructure would not itself establish a better submission.

The reference also separates engineer sign-off from AI assistance. Its [Engineer Review and Sign-Off template](https://github.com/laharichoudary5lp/assignment/blob/main/docs/ai-assisted-engineering/10-review-signoff/ENGINEER-SIGNOFF.md) has blank Engineer and Date fields in the version inspected on 2026-09-27. Its [engineering playbook](https://github.com/laharichoudary5lp/assignment/blob/main/docs/ai-assisted-engineering/README.md) assigns final review to the engineer. This submission uses that ownership model with a completed candidate acceptance record; a blank template alone is not treated as evidence of approval.

## Naming and compatibility

- Repository: `url-shortner-assignment` (the owner's existing repository name).
- README: **AI Assisted URL Shortener**; application: **URL Shortener**.
- Java package: `com.rakesh.urlshortener`; entry point: `UrlShortenerApplication`.
- Domain names: `UrlController`, `UrlService`, `UrlRepository`, `ShortUrl`, `CreateUrlRequest`, `UrlResponse`, `ShortCodeGenerator`.
- Canonical management API: `/api/urls`; public resolution: `/s/{code}`.
- Design/plan files: `docs/DESIGN.md` and `docs/IMPLEMENTATION_PLAN.md`; no tool-specific planning folder is shipped.

Earlier `/api/links` routes remain compatibility aliases. Already-applied V1–V3 SQL migrations and their physical table names are retained to preserve data and Flyway checksums. Historical commits retain the names they had at the time; history is not rewritten. For an existing local database or Compose volume, retain its original JDBC URL, database credentials and volume configuration when upgrading. A fresh checkout uses the neutral URL Shortener defaults.

## Submission decision

**Candidate reviewer and submission approver: Rakesh Kumar.** The submission is accepted within the documented prototype scope on the basis of the requirement map, scenario history and validation evidence. [REVIEW_CHECKLIST.md](REVIEW_CHECKLIST.md) records the decision; [AI_WORK_LOG.md](AI_WORK_LOG.md) records AI assistance. Production deployment requires separate operational and security decisions.
