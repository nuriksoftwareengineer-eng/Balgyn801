# OPERATIONS_REVIEW.md
> Balgyn — Pre-Launch Operations Review · 2026-06-21

---

## 1. Deployment Model

**Production:** Single-host Docker Compose (`docker-compose.prod.yml`).

```
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

| Service | Image | Port (internal/external) | Restart |
|---------|-------|--------------------------|---------|
| `nginx` | nginx:1.27-alpine | 80:80 (public) | unless-stopped |
| `frontend` | custom (React SPA, nginx:1.27-alpine) | 80 (internal) | unless-stopped |
| `app` | custom (Spring Boot Java 21) | 8080 (internal) | unless-stopped |
| `db` | postgres:16 | 5432 (internal) | unless-stopped |
| `minio` | minio/minio (2024-12-18) | 9000 internal / 9001 console (internal) | unless-stopped |

**Network isolation:** All services share the default Compose network. There are no explicit networks separating the frontend-tier from the backend-tier. All containers can reach each other by service name.

---

## 2. HTTPS / TLS — CRITICAL ABSENCE

**File:** `nginx/nginx.conf`

nginx listens only on port 80. There is no:
- HTTPS (443) listener
- TLS certificate configuration
- HTTP → HTTPS redirect
- Let's Encrypt integration

**Impact:** All customer traffic — JWT tokens, payment provider redirects, customer addresses, and all API calls — is transmitted in plain text over HTTP. Freedom Pay and PayPal callbacks arrive over HTTP.

**Required fix before launch:**
1. Obtain an SSL certificate (Let's Encrypt via certbot, or supply a certificate from the load balancer)
2. Add a port `443:443` mapping to the nginx service in docker-compose.prod.yml
3. Add HTTPS server block to nginx.conf with SSL certificate paths
4. Add HTTP → HTTPS redirect (301) in the HTTP server block
5. If behind a cloud load balancer that terminates TLS, ensure `proxy_set_header X-Forwarded-Proto https` is set and `COOKIE_SECURE=true` is confirmed

---

## 3. Health Checks

| Service | Health Check | Quality |
|---------|-------------|---------|
| `db` | `pg_isready -U postgres -d balgynbol-spring` | ✓ Correct — tests actual PostgreSQL readiness |
| `minio` | `curl -sf .../minio/health/live` | ✓ Correct — MinIO health endpoint |
| `app` | `curl -sf http://localhost:8080/api/v1/exchange-rates` | ⚠ Business endpoint, not a proper health probe |
| `frontend` | None | — nginx container; acceptable |
| `nginx` | None | — load balancer container; acceptable |

**Issue:** The `app` healthcheck uses the `/api/v1/exchange-rates` endpoint. This endpoint queries the `exchange_rates` table. If the exchange rate data is misconfigured or the table is empty, the endpoint may return 200 even with a broken application, or return 500 for business reasons while the application is otherwise healthy. This creates false positives and false negatives in health reporting.

**Fix:** Add Spring Actuator (`spring-boot-starter-actuator`) and use `/actuator/health` as the health check endpoint. This properly checks DB connectivity and application readiness. (See Operations section 6.)

---

## 4. Environment Variables

### Required in production (enforced by `:?` in docker-compose.prod.yml)
`JWT_SECRET`, `POSTGRES_PASSWORD`, `CDEK_CLIENT_ID`, `CDEK_CLIENT_SECRET`, `FRONTEND_BASE_URL`, `FREEDOMPAY_MERCHANT_ID`, `FREEDOMPAY_SECRET_KEY`, `FREEDOMPAY_RESULT_URL`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_WEBHOOK_ID`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_PUBLIC_URL`, `ALLOWED_ORIGINS`

**Status: Good.** If any of these are missing, `docker compose up` fails before starting any service.

### Issues

| Variable | Issue |
|----------|-------|
| `SPRING_DATASOURCE_PASSWORD` | Falls back to `1234` if not set (not enforced by docker-compose.prod.yml, only by app) |
| `PAYPAL_MODE` | Defaults to `sandbox` — must be set to `live` in production |
| `FREEDOMPAY_TESTING_MODE` | Defaults to `false` in prod — correct |
| `COOKIE_SECURE` | Set to `true` by `application-prod.properties` — correct |
| `TRUST_PROXY` | Set to `true` by `application-prod.properties` — correct |

---

## 5. MinIO / Media Storage

**Configuration:**
- MinIO runs as an internal service (port 9000 not exposed publicly)
- Public media URLs served via `MINIO_PUBLIC_URL` (e.g., `https://media.balgyn.kz`)
- This implies a separate nginx or CDN proxy in front of MinIO's internal port — **this proxy is not in this repository and is not documented**

**Issues:**
1. The MinIO bucket must be pre-configured as public-read. No code enforces or documents this. If the bucket is created without the correct policy, all image URLs return 403.
2. No lifecycle policy: uploaded images have no TTL or automatic cleanup. Storage grows indefinitely.
3. No pre-signed URLs: all media URLs are permanent public links. There is no mechanism to revoke access to specific files.

**Required operational step before launch:** Document the MinIO bucket creation command and public-read policy configuration. Add it to the deployment checklist.

---

## 6. Spring Actuator — ABSENT

`spring-boot-starter-actuator` is not in `build.gradle`.

**Impact:**
- No `/actuator/health` endpoint for proper health checking
- No `/actuator/metrics` for application monitoring
- No `/actuator/info` for deployment metadata
- Docker healthcheck falls back to a business endpoint

**Recommendation:** Add `implementation 'org.springframework.boot:spring-boot-starter-actuator'` with management endpoints restricted to localhost:
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
management.server.port=8081
management.server.address=127.0.0.1
```

---

## 7. Logging

**Current setup:** Spring Boot default logback (plain-text console output to Docker stdout).

| Issue | Severity |
|-------|----------|
| No structured (JSON) logging — log lines are plain text, hard to aggregate with ELK/Loki/Grafana | MEDIUM |
| No log rotation — Docker's default `json-file` driver has no size limit configured; logs grow indefinitely | MEDIUM |
| No log level management at runtime — changing log verbosity requires application restart | LOW |
| No request ID / correlation ID in logs — cannot trace a single request across log lines | LOW |

**Quick fix for log size:** Add to docker-compose.prod.yml:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "50m"
    max-file: "5"
```

**For structured logging:** Add `logstash-logback-encoder` and configure `logback-spring.xml` with a JSON appender.

---

## 8. Backups — ABSENT

No backup strategy exists anywhere in the repository.

**What is at risk:**
- `postgres_data` Docker volume — entire database (orders, customers, payments, catalog)
- `minio_data` Docker volume — all product images

**There are no:**
- Automated `pg_dump` scripts
- Cron jobs for backups
- MinIO data backup configuration
- Off-site backup destination documentation
- Backup retention policy
- Recovery procedure documentation

The `dumps/` directory exists with a `.gitignore` that excludes `*.sql` — suggesting manual dumps have been taken, but no automation.

**Minimum required before launch:**
1. Daily `pg_dump` cron job with output to a separate volume or remote storage
2. Daily MinIO snapshot or `mc mirror` to a remote S3 bucket
3. Document restore procedure and test it

---

## 9. Scheduled Jobs

| Job | Schedule | Mechanism |
|-----|----------|-----------|
| Order expiry | `fixedDelay=5min` (after previous run completes) | Spring `@Scheduled` |
| Exchange rate refresh | Every hour (`0 0 * * * *`) | Spring `@Scheduled` |

**Multi-instance concern:** Both jobs use in-JVM scheduling with no distributed lock. In a multi-pod deployment (horizontal scaling), both pods run these jobs simultaneously. The expiry job re-reads order status fresh from DB before expiring, which prevents double-expiry under READ COMMITTED isolation. The exchange rate job does idempotent upserts, so concurrent runs are safe but wasteful.

**For future horizontal scaling:** Consider `ShedLock` or a distributed scheduler to ensure exactly-one execution per scheduled window.

---

## 10. Rate Limiting

**Implementation:** Two in-memory Spring filters (`SensitiveEndpointRateLimiterFilter`, `PaymentRateLimiterFilter`) using `ConcurrentHashMap<String, Deque<Instant>>` (sliding 60-second window, per-IP).

**Coverage:**
- Auth (login, register): 10/min
- Custom design submit: 5/min
- Order creation: 15/min
- Media upload: 5/min
- Payment init: 10/min (configurable)
- Webhooks: 100/min (configurable)

**Issues:**
1. In-memory only — each pod has an independent counter. Rate limit can be bypassed by distributing requests across pods. Requires Redis or a distributed cache for multi-instance production.
2. No memory bound on the IP → timestamp map. A DDoS with millions of unique IPs exhausts heap without any eviction of stale entries.
3. No nginx-level rate limiting as a first line of defense.

---

## 11. CD/CI

No CI/CD pipeline configuration found in the repository (no `.github/workflows`, no `Jenkinsfile`, no `Dockerfile` pipeline).

**Test validation:** `./gradlew test` runs 84 integration tests.
**Frontend build validation:** `npm run build` in `frontend/` produces the production bundle.

---

## 12. Pre-Launch Operational Checklist

- [ ] Configure HTTPS (Let's Encrypt or load-balancer TLS)
- [ ] Rotate all credentials in `.env.example` to placeholder values
- [ ] Create `.env.prod` with all required production values
- [ ] Set `PAYPAL_MODE=live`
- [ ] Set `FREEDOMPAY_TESTING_MODE=false`
- [ ] Configure MinIO bucket with public-read policy and document the steps
- [ ] Configure `MINIO_PUBLIC_URL` pointing to a publicly accessible MinIO endpoint
- [ ] Add Docker log rotation to all services in `docker-compose.prod.yml`
- [ ] Set up daily automated database backup (pg_dump + offsite storage)
- [ ] Set up MinIO data backup (mc mirror or snapshot)
- [ ] Add Spring Actuator and update Docker healthcheck
- [ ] Test full production build: `docker compose -f docker-compose.prod.yml up --build`
- [ ] Verify Freedom Pay callback URL is publicly accessible (not ngrok)
- [ ] Verify PayPal webhook URL is registered in PayPal Developer Dashboard
- [ ] Verify CDEK production credentials work (not sandbox)
- [ ] Run `./gradlew test` — all 84 tests green
- [ ] Run `npm run build` in frontend — zero TypeScript errors
