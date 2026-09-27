# Validation record

Evidence recorded on 2026-09-27. Results distinguish completed runs from checks that are configured or still pending. H2 in PostgreSQL mode is not a substitute for executing against PostgreSQL.

## Executed milestone checks

| Stage | Observed outcome | Relevant evidence |
| --- | --- | --- |
| Greenfield | 17 cases passed; no failures, errors or skips | `CoreIntegrationTest`, commit `95bc345` |
| Brownfield | 24 cases passed; no failures, errors or skips | Core, lifecycle and populated-V1 migration tests; commit `322873a` |
| Retry/hardening | 31 cases passed; no failures, errors or skips | Reliability and limiter tests added; commit `ec57f00` |
| Pre-review `verify` | Build succeeded with 31 passing cases and JaCoCo report generation | Maven verification log and generated reports |
| Review regression run | Three cases ran: two failed as expected before fixes; collision retry case passed | `ReviewRegressionTest` |
| Final `verify` | **Build succeeded: 34 cases, zero failures/errors/skips; zero Checkstyle violations** | Fresh verification output and Surefire reports after the review fixes and Tomcat update |
| Final dependency audit | 100 resolved runtime packages checked; no OSV findings | `target/dependency-audit.json` |
| Packaged runtime | HTTP smoke, actual process restart/replay and targeted log-redaction assertions passed | `python3 tools/verify_runtime.py` |

The initial brownfield tests observed five failures and two errors because the lifecycle behavior and schema did not yet exist. The initial reliability suite observed six failures. These red runs preceded their implementations; they are development evidence, not the current acceptance result.

## Review findings and response

1. **Encoded destination length.** A Unicode URL could fit the request character limit but exceed the database column once percent-encoded, producing 503. Validation now checks the encoded URL's 2,048-character limit and returns 422 before persistence, for both random and custom aliases.
2. **Expiry precision.** Nanosecond timestamps could be rounded by microsecond database storage, changing the lifecycle boundary. Creation now rejects precision finer than microseconds with 422.
3. **Collision behavior.** A regression test forces a random-code collision and verifies fresh-transaction retry plus bounded exhaustion. This case passed in the initial review regression run.
4. **Dependency findings.** An OSV scan of 100 resolved runtime coordinates reported three advisory IDs against `tomcat-embed-core:11.0.24`: `GHSA-9xv2-5v5q-p794`, `GHSA-gcx9-497g-6cp6` and `GHSA-h3x4-894j-xpx5`. The POM now pins Tomcat 11.0.25. Fresh dependency resolution and a final scan of 100 runtime packages returned no findings. This is a point-in-time advisory result, not a guarantee that dependencies contain no vulnerabilities.

All three review regression cases passed in the final suite.

## Coverage of the test suite

| Area | Assertions |
| --- | --- |
| HTTP/API contract | Destination query/fragment preservation, create/redirect/read, bearer boundary, malformed/unknown input, bounded pagination |
| URL and request policy | Unsafe hosts/literals, credential/control rejection, legacy numeric IP forms, oversized bodies, encoded length |
| Lifecycle | Exact expiry boundary, future/365-day limit, precision, repeated disable, retained tombstone, HEAD exclusion |
| Persistence | Populated V1 migration preserves links and lifetime counts; failed daily write rolls back total count |
| Concurrency | Parallel redirects retain counts; simultaneous matching idempotency keys create one resource |
| Retry semantics | Matching replay, changed-payload conflict, replay after expiry, random collision retry/exhaustion |
| Admission and errors | Monotonic rate-limit window, bounded client state, request correlation IDs |

Tests use real database transactions through Spring/MockMvc rather than replacing repositories with mocks. The populated-V1 migration test uses H2 explicitly; the CI PostgreSQL run exercises the integration tests against PostgreSQL through `TEST_DATABASE_*` configuration.

## Coverage and packaged runtime

JaCoCo recorded **278 of 298 lines covered (93.3%)** and **168 of 244 branches covered (68.9%)**. Coverage identifies unexercised paths; it does not replace behavioral assertions or the outstanding PostgreSQL run.

The packaged application passed create, matching retry, GET redirect, HEAD exclusion, analytics, disable and 410 checks. A real stop/start using the same H2 file retained the link and returned the same idempotent creation result. Runtime-log assertions confirmed that the specific test token and destination URLs were absent from the captured log.

The same local run measured one hot link with 100 requests at concurrency eight:

| Measurement | Result |
| --- | ---: |
| Elapsed time | 0.331 s |
| Throughput | 302.4 requests/s |
| Median latency | 19.17 ms |
| 95th-percentile latency | 61.21 ms |
| Recorded resolutions | 100 / 100 |

This was a short, warm-JVM, single-process H2 measurement with no external destination fetch. It does not establish sustained throughput, PostgreSQL performance, multi-instance behavior or a production SLA.

## Pending or unavailable checks

| Check | Status / reason |
| --- | --- |
| PostgreSQL and Docker Compose | Not executed locally: Docker is unavailable; installing PostgreSQL was blocked by the environment's package-management UID restriction |
| PostgreSQL CI | Passed in [GitHub Actions run 36294271325](https://github.com/rakesh-kumar34/url-shortner-assignment/actions/runs/36294271325) for delivery commit `aa6965d`; H2 verification and dependency auditing also passed |
| Browser interaction and responsive visual review | Not completed: browser installation failed while downloading its archive |
| Human submission review | Pending; no human sign-off claimed |

## Reproduce and inspect

```sh
./mvnw -B verify
python3 tools/verify_runtime.py
./mvnw -B dependency:tree -DoutputFile=target/dependencies.txt
python3 tools/audit_dependencies.py
```

Inspect `target/surefire-reports/`, `target/site/jacoco/index.html` and `target/dependency-audit.json`. These are generated files and are not committed. `verify_runtime.py` starts the package with a temporary H2 file and runs the smoke, benchmark, restart and redaction checks. To target an already running server instead, run `python3 tools/smoke.py` and `python3 tools/benchmark.py`. The scripts avoid following redirects to external destinations.

For PostgreSQL, set `TEST_DATABASE_URL`, `TEST_DATABASE_USER` and `TEST_DATABASE_PASSWORD` to a disposable test database and run `./mvnw -B test`. [README.md](../README.md) contains complete setup instructions. Record the command, outcome and environment for any newly completed check; do not convert a configured workflow into a claimed result.
