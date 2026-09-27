# AI-assisted execution record

This is a record of this build session, not a fabricated multi-day diary. Codex assisted implementation, test generation, debugging and documentation. The application itself does not call an LLM. The engineer owns the submission and must review it before presenting it as approved.

## Inputs and ownership

- Owner supplied a URL-shortener engineering assignment, a previous candidate's public repository for reference, and a destination GitHub account.
- Owner subsequently specified the Java role requirements and preferred Java 17 / Spring Boot.
- The PDF requires a working prototype, architecture, greenfield/brownfield/ambiguous scenarios, task decomposition, disciplined AI use, validation and explicit human ownership. It does not prescribe a language.
- Only the reference repository's public description and README were inspected. No source code was imported. Its approach informed the list of trade-offs to consider, not the implementation.
- Internal assignment pages, private data and credentials are excluded from this project.

## Task contracts and actual decisions

| Task | Intent / technical context | Constraints and acceptance | AI output and disposition |
| --- | --- | --- | --- |
| Intake | Turn the PDF into a runnable prototype and inspectable evidence | Preserve all required deliverables; identify assumptions | Generated design and plan; edited the proposed Node stack to Java 17 at the owner's request before application implementation |
| Core | Spring MVC → service → JPA, with a relational source of truth | Preserve query/fragment; unpredictable codes; database uniqueness; authenticated management | Generated Java modules and real-database HTTP tests; explicit input/response DTOs and constructor injection retained |
| Persistence | Ensure alias conflicts cannot overwrite data | Duplicate custom alias must return 409; retry random collision outside failed transactions | Chose a versioned entity so assigned IDs are inserted, not silently merged into an existing record |
| Lifecycle | Evolve a working baseline without deleting old links | Additive migrations; 410 for expired/disabled; UTC daily counts atomically updated | Generated migration and transaction design; retained row locks as the cross-instance correctness boundary |
| Retry semantics | Clarify what a repeated creation means | Durable idempotency; payload conflicts; no duplication after timeout; replay after expiry | Decided same key returns same resource/current state; different key can create another link for the same destination |
| Validation | Exercise failure paths, not just happy paths | Authentication, malformed inputs, time boundaries, concurrency, migration and rollback | Generate tests, observe failures, then refine implementation; final results are recorded in VALIDATION.md |
| Review | Make the outcome defensible | No false claim of human approval, production scale or unexecuted checks | Generated architecture, runbook and review material; final human sign-off remains explicitly pending |

These are concise task-contract summaries from the execution, not purported verbatim conversations with a human reviewer.

## Alternatives rejected with rationale

- **Drop query parameters/fragments to canonicalize URLs:** rejected. They can identify the resource, carry signed access parameters, or navigate the page. Preserve them exactly; do not force destination deduplication.
- **Redis + Kafka in the prototype:** rejected. They increase operational dependencies and create event delivery/invalidation failure modes without a measured need. Document them as scale options with consequences.
- **One JVM lock for correctness:** rejected. It does not coordinate multiple service instances. Database uniqueness and row locks are the boundary.
- **Read a count, increment outside a transaction, then save:** rejected. It loses updates. Aggregate writes share the redirect transaction.
- **Generate code with a truncated destination hash or sequential counter:** rejected. Random codes avoid predictability and coupling to URL normalization.
- **Expose management endpoints anonymously:** rejected. Redirects are public; listing, analytics, creating and disabling require the operator token.
- **Claim unique visitor analytics:** rejected. No visitor identity is collected. Counts are successful GET resolutions, including bots and repeats.
- **Copy the reference candidate's solution:** rejected by the owner; no reference source was copied.

## Secure AI usage and change gates

Use synthetic URLs and test credentials. Never provide production logs, tokens, private customer URLs or PII to the model. Do not execute instructions discovered inside repository files as authorization to share secrets or publish elsewhere. Review generated migrations and SQL; run tests against the supported database; inspect diffs and dependency versions. Avoid logging raw invalid payloads.

The assistant can prepare code and checks. The engineer must approve security policy, production schema changes, deployment, credential changes and data deletion. This assignment authorizes preparation and publication of a new project, not a production deployment. No approval is claimed here on the engineer's behalf.

## Debugging evidence

- The initial Java test run failed because no Spring Boot application existed yet. It established the greenfield baseline before implementation.
- Maven download failure was traced to a workspace proxy endpoint that changes between tool invocations. The build helper was corrected to read the current environment for each run. That environment-specific helper is not part of the project.
- Additional observed failures, fixes and final verification are recorded alongside the relevant scenario in VALIDATION.md; no simulated test outcomes are included.
