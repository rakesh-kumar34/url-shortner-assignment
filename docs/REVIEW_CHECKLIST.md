# Review and acceptance

Acceptance review completed on **2026-09-27** by **Codex, acting on behalf of Rakesh Kumar at his explicit request**. The decision is **accepted for interview submission within the documented prototype scope**. This record identifies the actual reviewer; it does not claim that the candidate personally ran the checks or performed an independent review.

## Completed technical review

- [x] Map the eight PDF requirements and five deliverables to inspectable artifacts in [REQUIREMENTS.md](REQUIREMENTS.md).
- [x] Compare the reference repository structure and AI workflow; retain original code and project-specific decisions.
- [x] Use consistent URL Shortener terminology, Java domain names and standard documentation folders; document compatibility exceptions.
- [x] Run a clean JDK 17 verification: 36 cases, zero failures/errors/skips and zero Checkstyle violations.
- [x] Audit 100 runtime dependency coordinates: no current OSV findings.
- [x] Exercise the packaged JAR, restart persistence, durable retries, request-log redaction and a bounded local benchmark.
- [x] Re-review the request-boundary, storage-error and disconnect fixes; no remaining critical or important code-review findings.
- [x] Verify the published source tree and record passing PostgreSQL, Docker Compose and desktop/mobile browser CI results in [VALIDATION.md](VALIDATION.md).

## Candidate acceptance before submission — completed by delegated review

- [x] Observe the executed API/browser demo and review create, list, redirect, analytics, disable, invalid credentials and disconnect behavior. The Docker/PostgreSQL smoke and Chromium flows passed; desktop/mobile screenshots were inspected during the final review.
- [x] Review the three published scenario commits, their parent sequence, current implementation and the migration, transaction and retry rationale below.
- [x] Accept the documented choices for this interview prototype: one operator token, process-local limits, committed GET counts, permanent aliases and retained tombstones. These limits are explicit and proportionate to the assignment.
- [x] Review AI-assisted changes and repository contents, verify the current application revision and successful CI, and record the submission decision. The tracked-file inventory excludes the internal PDF and local credentials; a common credential-pattern scan found no matches. Explicit demo/CI credentials are test-only.

## Reviewed decisions

| Decision | Rationale and evidence |
| --- | --- |
| Additive migrations | V2 adds nullable lifecycle fields and daily aggregates; V3 adds durable retry records. Existing URLs and lifetime totals survive. Historical daily counts are not invented. `MigrationTest` exercises populated V1 data. Applied migration names/checksums remain unchanged. |
| Transactional resolution | The database row lock coordinates concurrent mutations across instances. Lifecycle checks and lifetime/daily increments share one transaction; a failed write rolls back and returns 503. This favors accurate counts over hot-URL throughput and write-outage availability. |
| Durable retries | URL creation and the hashed retry key/fingerprint commit together. Matching retries return the same resource's current state; changed requests return 409. Uniqueness settles races, and collision retries start after the failed transaction ends. |
| Deliberate prototype scope | Java 17/Spring Boot matches the requested role. One relational service keeps behavior testable. Multi-tenancy, distributed rate limits, destination reputation, production capacity and operational approval remain outside this submission. |

## Acceptance record

| Field | Value |
| --- | --- |
| Candidate / repository owner | Rakesh Kumar |
| Reviewing delegate | Codex (AI assistant), at the candidate's explicit request |
| Authorization | Candidate requested completion of this acceptance review on his behalf on 2026-09-27 |
| Reviewed application commit | [`054cc9594078db3858a8dc989e4065aa87a89e0d`](https://github.com/rakesh-kumar34/url-shortner-assignment/commit/054cc9594078db3858a8dc989e4065aa87a89e0d) |
| Review date | 2026-09-27 (America/New_York) |
| Verification evidence | [Successful CI run 36327265545](https://github.com/rakesh-kumar34/url-shortner-assignment/actions/runs/36327265545): 36 cases on each database, dependency audit, Docker Compose and browser smoke |
| Scenario commits reviewed | Greenfield `95bc345`; brownfield `322873a`; ambiguity `ec57f00` |
| Submission decision | **Accepted for interview submission; no technical acceptance blockers found** |
| Scope / exceptions | Documented prototype limits accepted for the assignment; production deployment requires separate approval |
| Public repository URL | [rakesh-kumar34/url-shortner-assignment](https://github.com/rakesh-kumar34/url-shortner-assignment) |

This acceptance follow-up changes documentation only and preserves the tested application, test suite and CI configuration. The candidate retains ownership of the submitted work. Production use requires separate review of authentication, abuse handling, deployment, backups/recovery and sustained load requirements.
