# Assignment requirements and evidence

This map covers the supplied PDF's eight core requirements and five deliverables. The PDF is stack-neutral; Java 17 and Spring Boot implement the owner's separate role preference. The internal PDF is not redistributed. Evidence describes actual work and does not claim that the session spanned the suggested 2–3 days.

## Core requirements

| Requirement | Implementation / evidence | Assessment |
| --- | --- | --- |
| 1. Understand intent and ambiguity | [Design](DESIGN.md), [scenarios](SCENARIOS.md): destination preservation, duplicate URLs, retry identity, expiry and the meaning of analytics are explicit | Covered |
| 2. Decompose tasks and sequence dependencies | [Implementation plan](IMPLEMENTATION_PLAN.md): baseline → additive lifecycle/schema work → durable retries → validation and delivery | Covered |
| 3. Reason about an existing codebase | Brownfield scenario identifies controller, service, entities, migration and analytics flow; `MigrationTest` upgrades populated V1 data | Covered |
| 4. Use AI with task contracts, iteration and traceability | [Execution record](AI_WORK_LOG.md), [task contracts and gates](AI_WORKFLOW.md): generated/edited/rejected choices, actual failures, fixes, secure context, and owner approval boundaries | Covered; human acceptance remains below |
| 5. Generate engineering artifacts | Java source, explicit API DTOs, [OpenAPI](../src/main/resources/static/openapi.yaml), Flyway migrations, tests, Maven wrapper, Docker Compose and CI | Covered |
| 6. Validate and control risks | [Validation](VALIDATION.md), [risk register](RISKS.md): transactions, migration, concurrency, abuse boundaries, dependency scan and measured performance | Covered within stated prototype scope |
| 7. Maintain engineer-led oversight | The owner selected the stack, reference-only approach and final terminology. [Review checklist](REVIEW_CHECKLIST.md) records decisions requiring candidate acceptance; no approval is fabricated | Candidate sign-off pending |
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

## Naming and compatibility

- Repository: `url-shortner-assignment` (the owner's existing repository name).
- README: **AI Assisted URL Shortener**; application: **URL Shortener**.
- Java package: `com.rakesh.urlshortener`; entry point: `UrlShortenerApplication`.
- Domain names: `UrlController`, `UrlService`, `UrlRepository`, `ShortUrl`, `CreateUrlRequest`, `UrlResponse`, `ShortCodeGenerator`.
- Canonical management API: `/api/urls`; public resolution: `/s/{code}`.
- Design/plan files: `docs/DESIGN.md` and `docs/IMPLEMENTATION_PLAN.md`; no tool-specific planning folder is shipped.

Earlier `/api/links` routes remain compatibility aliases. Already-applied V1–V3 SQL migrations and their physical table names are retained to preserve data and Flyway checksums. Historical commits retain the names they had at the time; history is not rewritten. For an existing local database or Compose volume, retain its original JDBC URL, database credentials and volume configuration when upgrading. A fresh checkout uses the neutral URL Shortener defaults.

## Submission decision

A successful technical validation does not substitute for the candidate's review. The remaining owner action is to review the design and AI-assisted changes, run or observe the demo, and record acceptance in [REVIEW_CHECKLIST.md](REVIEW_CHECKLIST.md). Production deployment requires separate operational and security decisions.
