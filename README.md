# AI Assisted URL Shortener

Repository: [rakesh-kumar34/url-shortner-assignment](https://github.com/rakesh-kumar34/url-shortner-assignment)

An original Java 17 / Spring Boot URL shortener with a browser console, persistent links and transactional analytics. Built for the supplied engineering assignment and Java role requirements. The supplied candidate repository was used only as a reference; no reference source code or assignment PDF is included.

The project demonstrates three real increments: a greenfield service, a backward-compatible lifecycle enhancement, and an explicit resolution of retry and analytics ambiguity. AI assisted the engineering work; the application does not call an LLM.

## Run locally

Install **JDK 17**. The Maven wrapper downloads Maven and dependencies on its first run.

```sh
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`. Open [http://localhost:8080](http://localhost:8080) and connect with the local demo token:

```text
local-demo-token-change-before-deployment
```

This token comes from `src/main/resources/application-local.yaml`. The default `local` profile uses a persistent H2 file under `./data/shortline`; links survive a normal restart. Set `API_TOKEN` to override the demo token. H2 is the convenient demo database; PostgreSQL is the intended deployment database.

## Run with PostgreSQL and Docker Compose

Install Docker with Compose, then:

```sh
cp .env.example .env
# Edit .env: replace API_TOKEN and DATABASE_PASSWORD with random values.
# API_TOKEN must contain at least 32 characters.
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080) and enter the token from `.env`. Compose binds the application to localhost and persists PostgreSQL data in a named volume. Stop it with `docker compose down`; this retains the data. Do not commit `.env`.

For a hosted deployment, configure the `prod` profile, database credentials and an HTTPS `PUBLIC_ORIGIN`, and terminate TLS at a trusted edge. Deployment and production policy review remain separate from this prototype.

## Try the API

With the local server running:

```sh
export API_TOKEN='local-demo-token-change-before-deployment'

# Create: 201 initially, 200 for an identical retry with this key.
curl -i http://localhost:8080/api/links \
  -H "Authorization: Bearer $API_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: readme-example-v1' \
  -d '{"url":"https://example.com/docs?q=demo#intro","title":"Documentation","customAlias":"docs-demo"}'

# Public GET: inspect the 302 response without following the destination.
curl -i http://localhost:8080/s/docs-demo

# Public HEAD resolves without incrementing analytics.
curl -I http://localhost:8080/s/docs-demo

curl http://localhost:8080/api/links/docs-demo/stats \
  -H "Authorization: Bearer $API_TOKEN"

# Soft-disable: 204. Subsequent redirects return 410.
curl -i -X DELETE http://localhost:8080/api/links/docs-demo \
  -H "Authorization: Bearer $API_TOKEN"
```

Use the configured token for Compose. The example alias remains reserved after disable; use a new alias and idempotency key for a new resource.

| Endpoint | Access | Behavior |
| --- | --- | --- |
| `POST /api/links` | Bearer token | Create; optional title, alias, expiry and `Idempotency-Key` |
| `GET /api/links?page=0&size=20` | Bearer token | List; size 1–100 |
| `GET /api/links/{code}` | Bearer token | Inspect destination, state and lifetime count |
| `GET /api/links/{code}/stats` | Bearer token | Lifetime count and 30 zero-filled UTC days |
| `DELETE /api/links/{code}` | Bearer token | Idempotent soft-disable |
| `GET /s/{code}` | Public | 302 plus `Cache-Control: no-store`; records one resolution |
| `HEAD /s/{code}` | Public | Same lifecycle checks; records no resolution |
| `GET /actuator/health/readiness` | Public | Readiness including database availability |

See the [OpenAPI contract](src/main/resources/static/openapi.yaml), also served at `/openapi.yaml`.

## Behavioral guarantees and limits

- HTTP(S) destinations retain query parameters and fragments. The encoded URL must fit 2,048 characters. Credentials, controls, local hosts, private literal IPs and self-origin redirects are rejected; the service never fetches a destination.
- Random codes contain 72 bits of entropy. Custom aliases use 4–32 case-sensitive letters, numbers, `_` or `-`; conflicts return 409 and never overwrite links.
- Optional `expiresAt` is an absolute timestamp, in the future and within 365 days, with microsecond precision or coarser. Expired or disabled links return 410; unknown links return 404. Codes are never recycled.
- A durable idempotency key identifies one creation. Matching retries return the same resource's current state, including after expiry; changed payloads return 409. Different keys may create separate links for the same destination.
- Analytics count committed GET resolutions, including bots and repeats. They do not measure unique visitors or confirmed destination visits. Lifetime and daily counts commit together; a storage failure returns 503 instead of an uncounted redirect.
- Management uses one operator token. Rate limits are process-local. Multi-tenancy, destination reputation scanning, distributed rate limiting and a production availability SLA are outside this prototype.

## Verify

```sh
./mvnw -B verify
# Reports: target/surefire-reports/ and target/site/jacoco/index.html

# Start the packaged JAR in isolation, run smoke/benchmark, then verify restart:
python3 tools/verify_runtime.py

# With a server running; Python 3 is required:
python3 tools/smoke.py
python3 tools/benchmark.py

# Resolve and audit runtime dependencies against OSV:
./mvnw -B dependency:tree -DoutputFile=target/dependencies.txt
python3 tools/audit_dependencies.py
```

`verify` runs Checkstyle, Java-version enforcement, tests, packaging and JaCoCo reporting. `verify_runtime.py` starts the packaged JAR with a temporary H2 database, exercises HTTP behavior, restarts the process and checks persistent retry behavior and log redaction. The benchmark makes 100 requests at concurrency eight and verifies the final count; it is a local measurement, not a capacity guarantee. The standalone smoke and benchmark tools accept `BASE_URL` and `API_TOKEN` environment variables.

To exercise the integration suite against a disposable PostgreSQL database:

```sh
export TEST_DATABASE_URL='jdbc:postgresql://localhost:5432/shortline_test'
export TEST_DATABASE_USER='shortline'
export TEST_DATABASE_PASSWORD='your-test-password'
./mvnw -B test
```

The suite writes test data and includes a schema-constraint rollback test; use a dedicated test database. CI is configured to run H2 verification, dependency auditing and PostgreSQL tests. See [actual validation status](docs/VALIDATION.md) for executed checks and remaining gaps.

## Engineering evidence

- [Architecture and trade-offs](docs/ARCHITECTURE.md)
- [Three implementation scenarios and commit traceability](docs/SCENARIOS.md)
- [AI-assisted execution record](docs/AI_WORK_LOG.md)
- [Validation evidence and limitations](docs/VALIDATION.md)
- [Delivery summary](docs/FINAL_SUMMARY.md)
- [Human review checklist](docs/REVIEW_CHECKLIST.md)

Human submission sign-off and production approval are pending.
