# Payment Audit Report

Date: 2026-06-20  
Test run: `./gradlew test --rerun-tasks` — **84 tests, 0 failures, 0 skipped**

---

## Freedom Pay

### Flow Overview

```
Frontend → POST /api/v1/payments/init
         → FreedomPayHttpClient.initPayment()
         → Freedom Pay API init_payment.php (or stub when merchantId blank)
         → Browser redirected to pg_redirect_url
         → Freedom Pay POSTs to pg_result_url (our callback)
         → POST /api/v1/payments/callback/freedom-pay
         → Browser redirected to pg_success_url / pg_failure_url
```

### Test Results

| Test | Result | Notes |
|---|---|---|
| `callback_withValidSignature_returnsOk` | ✅ PASS | MD5 sig verified, pg_status=ok |
| `callback_withMissingSignature_returnsRejected` | ✅ PASS | pg_sig absent → rejected |
| `callback_withWrongSignature_returnsRejected` | ✅ PASS | tampered sig → rejected |
| `callback_replayedPaymentId_isIdempotent_orderNotDoubleConfirmed` | ✅ PASS | second callback ignored |
| `callback_withWrongAmount_returnsRejected` | ✅ PASS | amount > tolerance → rejected |
| `callback_withCorrectAmount_succeeds` | ✅ PASS | amount within 0.01 KZT → ok |
| `initPayment_calledTwice_returnsSamePayment_noDuplicate` | ✅ PASS | idempotency via PENDING lookup |
| `callback_pgResult1_confirmsOrder` | ✅ PASS | pg_result=1 → SUCCEEDED, order CONFIRMED |
| `callback_pgResult0_paymentFailed` | ✅ PASS | pg_result=0 → FAILED |
| `callback_pgResult0_onAlreadyConfirmedOrder_orderStaysConfirmed` | ✅ PASS | late failure cannot un-confirm order |

### Security Observations

- **Signature verification**: MD5 HMAC using `pg_sig` field. Applied to every callback. Callbacks with blank or wrong signatures are rejected with `<pg_status>rejected</pg_status>`.
- **Replay protection**: `processed_webhook_events` table stores `(FREEDOM_PAY, pg_payment_id)` with a `UNIQUE` constraint. Duplicate `pg_payment_id` calls return early without reprocessing.
- **Amount validation**: Callback `pg_amount` is compared to stored payment amount within a 0.01 KZT tolerance. Mismatches are rejected.
- **Stub mode**: When `FREEDOMPAY_MERCHANT_ID` is blank, no real API call is made. The stub returns a fake redirect URL and no signature is set (so webhook tests with the real secret still work).
- **Secret key blank**: When `secretKey` is blank the callback is rejected immediately — no unsigned callbacks accepted.
- **Success/failure redirect**: Now configurable via `FRONTEND_BASE_URL` (default `http://localhost:5173`). Docker sets `FRONTEND_BASE_URL=http://localhost:5174`.

### Known Limitations

- Freedom Pay does not sign its callback with a nonce — replay protection relies entirely on the database unique constraint.
- `pg_result=2` (pending) maps to `PaymentStatus.PENDING` — no action taken. If Freedom Pay sends a pending callback before the final result, the order stays in `PENDING_PAYMENT` and the 60-minute expiry job will handle it.

---

## PayPal

### Flow Overview

```
Frontend → POST /api/v1/payments/paypal/create-order
         → PayPalOrdersClient.createOrder() → PayPal Orders API v2
         → Returns PayPal approval URL
         → Browser navigates to PayPal approval page
         → PayPal redirects to returnUrl (/payment/success?token=<id>)
         → Frontend → POST /api/v1/payments/paypal/capture/{paypalOrderId}
         → PayPalOrdersClient.captureOrder()
         → Order confirmed
         + PayPal POSTs webhook to /api/v1/payments/paypal/webhook (async confirmation)
```

### Test Results

| Test | Result | Notes |
|---|---|---|
| `createOrder_returnsApprovalUrl` | ✅ PASS | Provider=PAYPAL, status=PENDING, url present |
| `createOrder_idempotent_returnsSamePayment` | ✅ PASS | Second call returns same payment row |
| `captureOrder_completed_confirmsOrderAndPayment` | ✅ PASS | COMPLETED → SUCCEEDED, order CONFIRMED |
| `captureOrder_denied_failsPayment` | ✅ PASS | VOIDED → FAILED |
| `captureOrder_calledTwice_isIdempotent_noDoubleCapture` | ✅ PASS | Second capture skipped, order stays CONFIRMED |
| `captureOrder_onAlreadyConfirmedOrder_doesNotReConfirm` | ✅ PASS | Webhook arrives after capture — order stays CONFIRMED |
| `createOrder_onCancelledOrder_returns400` | ✅ PASS | Cancelled order rejects payment init with 400 |
| `webhook_captureCompleted_confirmsOrder` | ✅ PASS | PAYMENT.CAPTURE.COMPLETED → CONFIRMED |
| `webhook_duplicate_isIdempotent` | ✅ PASS | Same eventId processed once only |
| `webhook_invalidSignature_returns400` | ✅ PASS | Verifier returns false → 400 |

### Security Observations

- **Signature verification**: API-backed PayPal `verify-webhook-signature` endpoint (RSA-SHA256). The verifier is mocked in tests; real calls require `PAYPAL_WEBHOOK_ID`.
- **Double-capture prevention**: `captureOrder()` holds a `PESSIMISTIC_WRITE` lock on the payment row. If status is not `PENDING`, it returns the existing payment without calling PayPal.
- **Replay protection**: `processed_webhook_events` table with `UNIQUE(PAYPAL, eventId)`. Duplicate webhook events return 200 without reprocessing.
- **Order status guard**: Order is only moved to `CONFIRMED` when currently in `PENDING_PAYMENT` or `NEW`. An already-`CONFIRMED` order cannot be re-confirmed by a duplicate capture or webhook.
- **Idempotent create**: `createOrder()` reuses an existing `PENDING` PayPal payment for the same order. Prevents duplicate PayPal orders.
- **Currency conversion**: KZT → USD conversion uses `ExchangeRateService` (DB-backed, refreshed daily from NBK RSS). Rate changes between create and capture do not affect the already-fixed USD amount.

### Known Limitations

- **Buyer cancellation**: When a buyer cancels on the PayPal approval page, PayPal redirects to `cancelUrl`. The backend has no webhook for cancellation; the order remains in `PENDING_PAYMENT` until the 60-minute `OrderExpiryService` runs. No instant server-side cancellation.
- **PAYPAL_WEBHOOK_ID blank**: When blank, `PayPalWebhookVerifier` is configured in sandbox mode with no webhook ID. In this configuration verification may always return true or fail depending on the PayPal sandbox response. Set `PAYPAL_WEBHOOK_ID` in production.
- **No refund initiation**: `PAYMENT.CAPTURE.REFUNDED` webhook is handled (sets status to `REFUNDED`) but there is no admin UI or API endpoint to initiate a refund from our side.

---

## Webhook Security Summary

| Feature | Freedom Pay | PayPal |
|---|---|---|
| Signature verification | ✅ MD5 HMAC (pg_sig) | ✅ RSA-SHA256 (API-backed) |
| Replay protection | ✅ DB unique (provider, pg_payment_id) | ✅ DB unique (provider, eventId) |
| Idempotent order confirmation | ✅ Status guard (PENDING_PAYMENT/NEW only) | ✅ Status guard (PENDING_PAYMENT/NEW only) |
| Double-payment prevention | ✅ Idempotent init (reuse PENDING) | ✅ Idempotent create + pessimistic lock |
| Unsigned callback rejection | ✅ Returns rejected XML | ✅ Returns 400 |
| Amount validation | ✅ 0.01 KZT tolerance | N/A (amount fixed at capture) |

---

## MinIO Audit

### Configuration

| Setting | Docker value | Purpose |
|---|---|---|
| `MINIO_ENDPOINT` | `http://minio:9000` | Internal endpoint (app → MinIO container) |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | URL returned in image URLs (browser → MinIO) |
| Port | 9000 exposed to host | Browser can reach MinIO directly |
| Bucket | `balgyn-media` | Auto-created by `MinioBucketInitializer` on startup |
| Persistence | `minio_data` named volume | Survives `docker compose restart` and `up -d` |

### Verification

- `MinioBucketInitializer` (`@PostConstruct`) creates the bucket and applies a public-read bucket policy on every startup. Idempotent — no-ops if bucket already exists.
- Upload path: `products/<UUID>.<ext>` — UUID prevents naming collisions.
- Public URL: `{MINIO_PUBLIC_URL}/{bucket}/{key}` e.g. `http://localhost:9000/balgyn-media/products/abc123.jpg`
- Magic-byte validation prevents non-image uploads even if Content-Type is spoofed.
- Max file size: 8 MB (enforced in `MinioMediaStorageService` and at Spring multipart level: `spring.servlet.multipart.max-file-size=8MB`).

### Issue: MINIO_PUBLIC_URL is localhost

In production, `MINIO_PUBLIC_URL=http://localhost:9000` won't work if users access the site from any machine other than the server. Set it to the public MinIO domain or CDN URL.

---

## Test Suite Summary

```
Total tests:    84
Passed:         84
Failed:          0
Skipped:         0
Duration:       ~26s
```

### New tests added in this session

**PayPalPaymentIntegrationTest**:
- `captureOrder_calledTwice_isIdempotent_noDoubleCapture`
- `captureOrder_onAlreadyConfirmedOrder_doesNotReConfirm`
- `createOrder_onCancelledOrder_returns400`

**PaymentSecurityIntegrationTest**:
- `callback_pgResult0_onAlreadyConfirmedOrder_orderStaysConfirmed`
