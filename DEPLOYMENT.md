# Balgyn — Deployment Guide

---

## Quick Start (local dev)

```bash
cp .env.example .env
# Edit .env — set JWT_SECRET and optionally BOOTSTRAP_ADMIN_EMAIL/PASSWORD
docker compose build app
docker compose up -d
# Optional: seed catalog on first run
APP_SEED_CATALOG=true docker compose up -d app
```

Services: backend on `:8080`, frontend on `:5174`, MinIO API on `:9000`.

---

## VPS Production Deployment

### 1. Server requirements

| Resource | Minimum |
|---|---|
| RAM | 2 GB |
| CPU | 2 vCPU |
| Disk | 20 GB SSD |
| OS | Ubuntu 22.04 / 24.04 |
| Domain | Pointed to VPS IP |

### 2. Install Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
docker compose version   # must be v2.x
```

### 3. Clone and configure

```bash
git clone https://github.com/your-org/balgynbol.git /opt/balgyn
cd /opt/balgyn
cp .env.example .env.prod
nano .env.prod    # fill in all required values
```

### 4. Required `.env.prod` values

```env
# Security
JWT_SECRET=<openssl rand -hex 64>
BOOTSTRAP_ADMIN_EMAIL=admin@balgyn.kz
BOOTSTRAP_ADMIN_PASSWORD=<strong password>

# Database
POSTGRES_PASSWORD=<strong unique password>

# MinIO
MINIO_ROOT_USER=<not "minioadmin">
MINIO_ROOT_PASSWORD=<strong password, min 8 chars>
MINIO_PUBLIC_URL=https://media.balgyn.kz

# Frontend
FRONTEND_BASE_URL=https://balgyn.kz
ALLOWED_ORIGINS=https://balgyn.kz,https://www.balgyn.kz

# Freedom Pay
FREEDOMPAY_MERCHANT_ID=<from Freedom Pay dashboard>
FREEDOMPAY_SECRET_KEY=<secret key>
FREEDOMPAY_RESULT_URL=https://balgyn.kz/api/v1/payments/callback/freedom-pay
FREEDOMPAY_TESTING_MODE=false

# PayPal
PAYPAL_MODE=live
PAYPAL_CLIENT_ID=<live client ID>
PAYPAL_CLIENT_SECRET=<live client secret>
PAYPAL_WEBHOOK_ID=<from PayPal Developer Portal>

# CDEK
CDEK_BASE_URL=https://api.cdek.ru/v2
CDEK_CLIENT_ID=<your CDEK client ID>
CDEK_CLIENT_SECRET=<your CDEK client secret>
CDEK_SENDER_CITY=270
CDEK_DEFAULT_TARIFF=136
```

### 5. Build and start production stack

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

This starts: PostgreSQL → MinIO → Spring Boot app → nginx-served frontend → nginx reverse proxy on `:80`.

First build takes 3–5 minutes (Java compilation + React build). Monitor:

```bash
docker compose -f docker-compose.prod.yml logs -f app
```

Verify:

```bash
curl http://localhost/api/v1/exchange-rates   # should return JSON
curl -I http://localhost/                     # should return 200
```

### 6. SSL with Let's Encrypt

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d balgyn.kz -d www.balgyn.kz
```

Certbot auto-renews certificates. Or use Cloudflare proxy (orange cloud) — free SSL, no certbot needed.

### 7. MinIO public access

For images to load in the browser, `MINIO_PUBLIC_URL` must be reachable from the internet.

**Option A — Subdomain:** Point `media.balgyn.kz` to the VPS, add to nginx:

```nginx
server {
    listen 80;
    server_name media.balgyn.kz;
    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host $host;
    }
}
```

Set `MINIO_PUBLIC_URL=https://media.balgyn.kz` in `.env.prod`.

**Option B — The `minio` service is not exposed publicly in `docker-compose.prod.yml`.** Expose it temporarily to create a public bucket policy, or use the internal API from the backend (already done automatically on startup).

### 8. Seed catalog (first deploy)

```bash
# Set APP_SEED_CATALOG=true in .env.prod for the first run only
docker compose -f docker-compose.prod.yml --env-file .env.prod \
  exec app curl -sf http://localhost:8080/api/v1/catalog/groups
```

Or add `APP_SEED_CATALOG=true` to `.env.prod` → restart → remove it.

### 9. Updates

```bash
cd /opt/balgyn
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build app frontend
```

Flyway runs new migrations on startup automatically.

---

## Environment Variables Reference

### Mandatory

| Variable | Description |
|---|---|
| `JWT_SECRET` | HS256 signing key (32+ bytes). App refuses to start if blank. |
| `POSTGRES_PASSWORD` | PostgreSQL password. |

### Admin Bootstrap

| Variable | Description |
|---|---|
| `BOOTSTRAP_ADMIN_EMAIL` | First admin email. Created once on startup. Leave blank to skip. |
| `BOOTSTRAP_ADMIN_PASSWORD` | First admin password. |

### Frontend URLs

| Variable | Default | Description |
|---|---|---|
| `FRONTEND_BASE_URL` | `http://localhost:5174` | Used to build Freedom Pay redirect URLs. |
| `ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:5174` | CORS allowed origins. |

### Freedom Pay

| Variable | Default | Description |
|---|---|---|
| `FREEDOMPAY_MERCHANT_ID` | _(blank)_ | **Blank = stub mode** (no real charges). |
| `FREEDOMPAY_SECRET_KEY` | _(blank)_ | MD5 signature key. |
| `FREEDOMPAY_RESULT_URL` | _(blank)_ | Publicly reachable callback URL. |
| `FREEDOMPAY_TESTING_MODE` | `false` | `true` = sandbox. |

### PayPal

| Variable | Default | Description |
|---|---|---|
| `PAYPAL_MODE` | `sandbox` | `sandbox` or `live`. |
| `PAYPAL_CLIENT_ID` | _(blank)_ | **Blank = PayPal stub mode.** |
| `PAYPAL_CLIENT_SECRET` | _(blank)_ | OAuth2 client secret. |
| `PAYPAL_WEBHOOK_ID` | _(blank)_ | Required for webhook signature verification. |

### MinIO

| Variable | Default | Description |
|---|---|---|
| `MINIO_ENDPOINT` | `http://minio:9000` | Internal (container-to-container) endpoint. |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | Public URL returned in image URLs. |
| `MINIO_ROOT_USER` | `minioadmin` | Change in production. |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | Change in production. |
| `MINIO_BUCKET` | `balgyn-media` | Auto-created on startup. |

### CDEK

| Variable | Default | Description |
|---|---|---|
| `CDEK_BASE_URL` | `https://api.edu.cdek.ru/v2` | Sandbox. Change to `https://api.cdek.ru/v2` for production. |
| `CDEK_CLIENT_ID` | _(blank)_ | Blank = mock mode. |
| `CDEK_CLIENT_SECRET` | _(blank)_ | CDEK API secret. |
| `CDEK_SENDER_CITY` | `270` | CDEK city code for Almaty. |

---

## Docker Services

| Service | Port (prod) | Description |
|---|---|---|
| `nginx` | `:80` | Reverse proxy — single public entry point |
| `frontend` | internal `:80` | nginx serving React build |
| `app` | internal `:8080` | Spring Boot backend |
| `db` | internal | PostgreSQL 16 |
| `minio` | internal `:9000` | MinIO object storage (console not exposed) |

---

## Backups

```bash
# PostgreSQL
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U postgres balgynbol-spring > backup_$(date +%Y%m%d).sql

# MinIO data volume
docker run --rm \
  -v balgyn_minio_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/minio_$(date +%Y%m%d).tar.gz /data
```

Recommended: add a daily cron job for both.

---

## Logs

```bash
docker compose -f docker-compose.prod.yml logs -f app       # Spring Boot
docker compose -f docker-compose.prod.yml logs -f nginx     # reverse proxy
docker compose -f docker-compose.prod.yml logs -f frontend  # frontend nginx
```

---

## Flyway Migrations

Migrations run automatically on startup. Current schema: **V24**.

```bash
docker compose exec db psql -U postgres -d balgynbol-spring \
  -c "SELECT version, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
```
