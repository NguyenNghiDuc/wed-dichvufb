# W10 — Staging HTTPS, release controls and monitoring (#47)

Status: **not deployed**. The repository contains reusable configuration, not credentials, DNS ownership, a live HTTPS certificate or evidence that staging is online.

## Operator checklist — required before any exposure

1. Obtain explicit owner's approval for the staging host, trial data (synthetic), DNS, firewall, resource budget and release window. This guide never authorizes production.
2. Provision a dedicated staging VM/Docker project separate from production, with PostgreSQL 16 and encrypted persistent storage; do not reuse production data or keys. Ensure public TCP 80/443 (and optional UDP 443) can reach **only** approved Caddy, and no public DB port is open. Keep host Docker socket restricted to privileged operators.
3. Obtain reviewed repo commit SHA and independently generated staging \`POSTGRES_PASSWORD\` (20+ characters). Keep credentials out of Git, screenshots, \`docker compose config\` output and ticket comments; .env is gitignored but environment values are still visible to host/Docker administrators via \`docker inspect\`. For a real production release move to external managed secrets/credentials rotation.
4. Select \`STAGING_HOST\` as an owned DNS hostname (e.g., approved staging subdomain); it must resolve to staging, **not** a production server. Caddy requests managed certificates when public DNS/ACME reachability is ready. Verify ownership, TLS, HSTS and HTTPS redirects; never use \`curl -k\` or self-signed certificates for acceptance.
5. Run all commands manually with the team present; separate approval for public DNS/ports and separate approval for any real DB backup or restore.

## Configuration-only preflight (does NOT create containers)

From repo root, with approved **test** environment variables available in your shell:

\`\`\`bash
export STAGING_HOST="YOUR-APPROVED-STAGING-DNS"
# Retrieve password through a local secret manager or prompt; never type it in chat.
export POSTGRES_PASSWORD="YOUR-LOCALLY-GENERATED-STAGING-SECRET"
bash scripts/ops/preflight_staging.sh
docker compose -f compose.yaml -f compose.staging.yaml config --quiet
\`\`\`

\`config --quiet\` avoids printing interpolated secrets, unlike plain \`docker compose config\`. The script checks hostname, at least 20-character non-default DB password, webhook-secret length if present, Compose interpolation and required files. It **does not verify** that the DNS is owned, port 443 is reachable or a certificate is issued.

Validate Caddy config in a *one-off container*, without publishing ports or starting app/db:

\`\`\`bash
docker compose -f compose.yaml -f compose.staging.yaml \
  run --rm --no-deps proxy caddy validate --config /etc/caddy/Caddyfile
\`\`\`

## Manual staging rollout (ONLY after approval)

1. Check branch SHA, corresponding Java/PostgreSQL/Flyway/Docker CI and CodeQL alerts; no queued jobs treated as PASS. Store the last-known-good Git SHA and digest of the previously tested app image.
2. Run the synthetic backup/restore drill on **local disposable** DBs: \`bash scripts/ops/test_backup_restore.sh\`. Confirm no real database is involved.
3. With approval and **staging-only** credentials, start dependencies using the reviewed Compose overlay. \`proxy\` has published 80/443, so this step must NOT happen without owner authorization:

\`\`\`bash
docker compose -f compose.yaml -f compose.staging.yaml \
  up --build -d --wait db app proxy
\`\`\`

4. Base \`compose.yaml\` publishes Spring Boot only at \`127.0.0.1:8080\`. The staging overlay attaches \`db\` only to internal \`db_private\` network; app joins db and web networks; proxy joins web network only. The Postgres service publishes **no** host port. Proxy manages HTTPS (Caddyfile staging HSTS is limited to one day, no includeSubDomains). App container uses nonroot UID, read-only root FS, ephemeral /tmp, pids/RAM/CPU controls and log rotation. This is a staging baseline, not a production security certification.
5. From staging host: \`bash scripts/ops/smoke.sh --local\`. From a trusted TLS client with the approved \`STAGING_HOST\`: \`bash scripts/ops/smoke.sh --tls\`. Both only GET health and public login and do not generate real customer data.
6. Manual **authenticated** UAT remains necessary: registration/login/logout (stale session handling), CSRF, owner-only order access, role separation, invalid input, health/restart behavior, PostgreSQL migrations, rollback and restoration. Involve W1, W2/W3/W4, W6/W7 and W9 for their respective areas. Never configure live provider or payment keys without separate approvals.
7. Never call this production-ready until verified backup recoverability (incl. offsite encrypted copy), alerting, credential rotation, SLA/RPO/RTO and security review.

## Health and logging

- **Public-safe endpoint** \`GET /actuator/health\`: minimal status only. \`/actuator/health/liveness\` and \`/actuator/health/readiness\` are enabled for probes; do not expose detailed internals, arbitrary actuator env/metrics endpoints or datasource credentials.
- **Local read-only checks**: \`docker compose -f compose.yaml -f compose.staging.yaml ps\`, \`curl -fsS http://127.0.0.1:8080/actuator/health\`. Use operator-restricted \`docker stats --no-stream\` and host disk alerts.
- Containers rotate JSON-file logs at 10 MB × 3 files. Caddy access logs use JSON stdout; they may contain client IP, URL paths/query, timestamps and user agents. Store only in access-controlled retention, limit scraping. Application \`application-prod.properties\` disables Hibernate SQL/bind-value debug, Tomcat access logging, stacktraces and binding errors to clients. **These settings do not guarantee zero PII in application logs:** audit other modules and sanitize exports before enabling central collection.
- Configure approved external monitors/alerts, not done by this PR: alert on health failure for ≥3 consecutive checks (30s cadence), failed synthetic backup/restore, free disk below 20%, certificate expiry under 14 days, restart loop, PostgreSQL volume pressure, auth error spikes, and abnormal p95 latency (benchmark required). Escalation/on-call channels, retention, timeouts and silence policy must be designated by owner.
- Avoid unredacted \`docker compose logs\` in public GitHub issues. If diagnostic evidence is needed, scrub tokens, cookies, usernames, e-mails, paths and URLs containing query parameters first.

## Maintenance and rollback

- Before rollout, record staging Git SHA, immutable image digest, schema migration version, recent **verified** backup, and maintenance approver. Schema changes must be additive/backward-compatible before code rollback; Flyway migrates forward and cannot be assumed reversibly.
- To inspect an **inert** plan: \`bash scripts/ops/rollback_plan.sh --plan CURRENT_SHA PREVIOUS_GOOD_SHA\`. This prints a checklist, **never executes** Git/Docker/SQL changes. In a real incident obtain approval before redeploying a previously tested artifact, and **do not** automatically restore a database.
- Consider blue/green only after approved infrastructure, session store, migration compatibility, health checks and safe secret rotation. This PR does not implement a multi-node load balancer or production rollout.
- Recovery exercise and real incident authorization: [OPERATIONS_BACKUP_RESTORE.md](OPERATIONS_BACKUP_RESTORE.md).

## Security boundaries and blockers

- The base Compose app/DB pass passwords via environment variables. This is adequate for local proof-of-concept only; **Docker inspect exposes these to privileged host administrators**. A production design requires secret manager, least-privilege backup credentials, rest/encrypted transit, file retention and audited access.
- Dockerfile still builds from Maven image without pinning by digest. Before internet-facing rollout choose reviewed, SHA256-pinned base images and scan dependency/CVE data. A green GitHub workflow cannot substitute for image provenance review.
- HTTPS certificate issuance, registered domain, firewall controls, backup encryption/off-site storage, authorized roles, provider keys and real data were **not** configured. No production process or user accounts have been changed.
