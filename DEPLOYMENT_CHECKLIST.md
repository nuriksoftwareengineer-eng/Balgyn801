# Deployment Checklist — Balgyn

Work through this checklist before going live. Every unchecked item is a risk.

---

## Security

- [ ] `JWT_SECRET` is at least 64 random bytes (`openssl rand -hex 64`), NOT hardcoded in any file
- [ ] `POSTGRES_PASSWORD` is changed from default `postgres`
- [ ] `MINIO_ROOT_USER` is changed from `minioadmin`
- [ ] `MINIO_ROOT_PASSWORD` is changed from `minioadmin`
- [ ] `BOOTSTRAP_ADMIN_PASSWORD` is strong (12+ chars, mixed case, numbers, symbols)
- [ ] `.env.prod` is NOT committed to git (confirm with `git status`)
- [ ] `SPRING_PROFILES_ACTIVE=prod` is set (`docker-compose.prod.yml` does this automatically)
- [ ] `SWAGGER_ENABLED=false` is set (prod compose sets this automatically)
- [ ] MinIO console port 9001 is NOT publicly accessible (`docker-compose.prod.yml` doesn't expose it)
- [ ] Firewall allows only ports 80 and 443 from the internet (block 8080, 5432, 9000, 9001)
- [ ] HTTPS is configured (Let's Encrypt or Cloudflare)
- [ ] `ALLOWED_ORIGINS` contains only your production domain(s)

---

## Payments

- [ ] Freedom Pay: `FREEDOMPAY_MERCHANT_ID` set to your real merchant ID
- [ ] Freedom Pay: `FREEDOMPAY_SECRET_KEY` set to your real secret key
- [ ] Freedom Pay: `FREEDOMPAY_RESULT_URL` is publicly reachable (test with curl)
- [ ] Freedom Pay: `FREEDOMPAY_TESTING_MODE=false`
- [ ] PayPal: `PAYPAL_MODE=live`
- [ ] PayPal: `PAYPAL_CLIENT_ID` and `PAYPAL_CLIENT_SECRET` are **live** credentials (not sandbox)
- [ ] PayPal: `PAYPAL_WEBHOOK_ID` is configured in PayPal Developer Portal and matches `.env.prod`
- [ ] PayPal: Webhook URL `https://yourdomain.com/api/v1/payments/paypal/webhook` is registered in PayPal
- [ ] Freedom Pay: callback URL `https://yourdomain.com/api/v1/payments/callback/freedom-pay` is registered

---

## Delivery

- [ ] CDEK: `CDEK_BASE_URL=https://api.cdek.ru/v2` (NOT the sandbox URL)
- [ ] CDEK: `CDEK_CLIENT_ID` and `CDEK_CLIENT_SECRET` are production credentials
- [ ] CDEK: `CDEK_SENDER_CITY` is set to your actual dispatch city code (270 = Алматы)

---

## Frontend / URLs

- [ ] `FRONTEND_BASE_URL` is set to the real domain (e.g. `https://balgyn.kz`)
- [ ] `MINIO_PUBLIC_URL` is a publicly reachable URL for image loading
- [ ] All payment redirect URLs in Freedom Pay and PayPal dashboards point to production domain
- [ ] Test: visit `https://balgyn.kz/` → page loads without errors in browser console

---

## Infrastructure

- [ ] `docker-compose.prod.yml --env-file .env.prod up -d --build` starts without errors
- [ ] `curl https://balgyn.kz/api/v1/exchange-rates` returns JSON
- [ ] `curl -I https://balgyn.kz/` returns HTTP 200
- [ ] Flyway: all migrations applied (`SELECT version FROM flyway_schema_history`)
- [ ] PostgreSQL data is on a persistent volume (not lost on container restart)
- [ ] MinIO data is on a persistent volume
- [ ] Daily database backup is scheduled (cron or managed DB service)

---

## Admin Panel

- [ ] Log in to `https://balgyn.kz/admin` with bootstrap admin credentials
- [ ] Exchange rate is visible and updating
- [ ] Catalog groups, collections, designs are visible
- [ ] Upload at least one design image to verify MinIO is working
- [ ] Place a test order end-to-end (including payment)

---

## Monitoring

- [ ] Logs are accessible (`docker compose logs app`)
- [ ] Health check endpoint is responding: `GET /api/v1/exchange-rates`
- [ ] Set up uptime monitoring (UptimeRobot, Betterstack, or similar) on `/api/v1/exchange-rates`
- [ ] Alert on container restarts (Docker restart policy set to `unless-stopped`)

---

## Known Issues at Launch

These are tracked risks, not blockers — but address them soon after launch:

- [ ] **Refresh tokens are not server-side revocable** — stolen token is valid for 14 days. Fix: implement `refresh_tokens` table with rotation and revocation on logout.
- [ ] **~80% of frontend strings are hardcoded Russian** — language switcher shows RU strings in EN/KK mode. Fix: add `t()` to `CartPage`, `DesignPage`, `LoginPage`, `RegisterPage`, `SiteFooter`.
- [ ] **No payment refunds** — Freedom Pay and PayPal refund APIs are not implemented. Refunds must be initiated manually in the payment provider dashboards.
- [ ] **CDEK webhook not signature-verified** — any actor can POST to the CDEK webhook URL. Fix: verify CDEK's signature header or IP-allowlist their servers.
- [ ] **SEO is client-side only** — meta tags set via JS, invisible to crawlers without JS. Fix: add SSR or prerendering for key pages.
- [ ] **Design images are NULL** — all 28 seeded designs have `main_image_url = NULL`. Upload images via admin panel after launch.
