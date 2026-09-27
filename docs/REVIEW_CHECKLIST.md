# Human review and acceptance

The implementation and evidence are prepared for review. The unchecked items below are intentionally pending; AI assistance does not constitute engineer approval.

## Submission review

- [ ] Run the README quick start on JDK 17 and review the browser create/list/analytics/disable flow.
- [ ] Confirm the final 34-case verification and dependency audit; reconcile results with [VALIDATION.md](VALIDATION.md).
- [ ] Run the integration suite on PostgreSQL and exercise Docker Compose.
- [ ] Review desktop/mobile layout, keyboard navigation, token handling and error states in a real browser.
- [ ] Inspect the three scenario commits and explain the migration, transaction and retry decisions without relying on generated prose.
- [ ] Confirm that the repository contains no credentials, customer URLs, private assignment PDF or reference implementation code.
- [ ] Verify the published repository URL, access and CI status, then record the submission decision below.

## Design acceptance

- [ ] Accept one shared operator token for this prototype and the boundary between public redirects and authenticated management.
- [ ] Accept URL-policy limits: no destination fetch or reputation scanning, and no guarantee against DNS changes or harmful public destinations.
- [ ] Accept counts as committed GET resolutions, with HEAD excluded, UTC days, and no unique-visitor claim.
- [ ] Accept atomic counting on the redirect path, database row-lock contention and 503 when storage cannot commit.
- [ ] Accept durable retry keys, current-state replays, non-reusable aliases and retained tombstones.
- [ ] Review the V1–V3 migrations and establish backup/recovery and rollback procedures before any real deployment.

## Sign-off record

| Field | Value |
| --- | --- |
| Reviewing engineer | Pending |
| Reviewed commit | Pending |
| Review date | Pending |
| Submission decision / exceptions | Pending |
| Public repository URL | [https://github.com/rakesh-kumar34/url-shortner-assignment](https://github.com/rakesh-kumar34/url-shortner-assignment) |
| Production deployment approval | Not granted; separate review required |
