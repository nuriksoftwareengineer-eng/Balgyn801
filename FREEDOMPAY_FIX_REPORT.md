# FreedomPay Fix Report

**Date:** 2026-06-24
**Verdict:** FREEDOMPAY WORKING

---

## Root Cause

FreedomPay redirects the user's browser to `pg_success_url` = `http://localhost:5174/payment-return`
after a successful payment. The previous implementation called `check_payment.php` to verify
the payment, but FreedomPay returned HTTP 403 — merchant 587060 has no access to this endpoint.
Order stayed `PENDING_PAYMENT`.

---

## Fix: Local pg_sig Verification

FreedomPay signs the browser success redirect with:

```
pg_sig = MD5( scriptName ; param_values_sorted_by_key ; secretKey )
```

Where:
- `scriptName` = last path segment of `pg_success_url` = `"payment-return"`
- All params except `pg_sig` are sorted alphabetically by key (TreeMap order)
- `pg_salt` IS included; `pg_sig` is excluded
- `secretKey` is appended last

This signature is cryptographic proof of payment success — no API call to FreedomPay needed.

---

## Changes Made

### Backend

| File | Change |
|------|--------|
| `FreedomPayProperties.java` | Added `getSuccessScriptName()` — derives `"payment-return"` from `pg_success_url` |
| `FreedomPayHttpClient.java` | `postForm()` now logs HTTP 4xx/5xx response body |
| `PaymentService.java` | Replaced `checkFreedomPayPaymentStatus` with `verifyFreedomPayRedirect` |
| `PaymentServiceImpl.java` | Implemented `verifyFreedomPayRedirect` — verifies pg_sig, logs invalid sig details, injects pg_result=1, delegates to `handleFreedomPayCallback` |
| `FreedomPayCheckController.java` | Replaced `/check-return` with `POST /verify-redirect` — accepts all redirect params as JSON body |
| `SecurityConfig.java` | Permits `POST /api/v1/payments/freedom-pay/verify-redirect` without auth |

### Frontend

| File | Change |
|------|--------|
| `backend-api.ts` | Replaced `checkFreedomPayReturn(orderId)` with `verifyFreedomPayRedirect(params: Record<string,string>)` |
| `PaymentReturnPage.tsx` | Sends ALL URL params via Object.fromEntries to `verify-redirect` |

---

## Payment Flow (Fixed)

```
User pays on FreedomPay page
    |
    v
FreedomPay redirects browser:
  /payment-return?pg_payment_id=xxx&pg_order_id=yyy&pg_amount=zzz
                 &pg_sig=HASH&pg_salt=...&pg_result=1
    |
    v
PaymentReturnPage.tsx — detects FP params (pg_payment_id | pg_order_id | pg_result)
    |
    v
POST /api/v1/payments/freedom-pay/verify-redirect
  Body: { "pg_payment_id": "xxx", "pg_order_id": "yyy", "pg_sig": "HASH", ... }
    |
    v
PaymentServiceImpl.verifyFreedomPayRedirect():
  1. Logs all pg_* params
  2. scriptName = "payment-return"
  3. expected = MD5("payment-return" ; sorted_values ; secretKey)
  4. received pg_sig != expected → BusinessRuleException → HTTP 400
  5. Signature valid → inject pg_result=1 → handleFreedomPayCallback()
    |
    v
handleFreedomPayCallback():
  pg_result=1 → PaymentStatus.SUCCEEDED
  order.status = CONFIRMED
  ProcessedWebhookEvent saved (replay protection)
    |
    v
Frontend: navigate("/payment/success")
```

---

## Exact Signature Formula

```
MD5( "payment-return"
     ; pg_amount_value
     ; pg_currency_value
     ; pg_order_id_value
     ; pg_payment_id_value
     ; pg_result_value
     ; pg_salt_value
     ; ... (all other pg_* except pg_sig, sorted by key)
     ; secretKey )
```

Implementation in `FreedomPaySignature.sign()` — uses `TreeMap` for alphabetical key sort.

---

## Security Analysis

**Q: Can a user forge the redirect URL to confirm a different order?**

No. The secretKey is only known to the server. A forged URL with:
- Different `pg_order_id` — MD5 mismatch → 400
- Different `pg_amount` — MD5 mismatch → 400
- Invented `pg_payment_id` — MD5 mismatch → 400
- Replayed valid URL — blocked by `ProcessedWebhookEvent` (pg_payment_id dedup)

---

## Server Callback

`POST /api/v1/payments/callback/freedom-pay` (pg_result_url) still works when ngrok is running.
Both paths are idempotent — order confirmed exactly once regardless of which arrives first.

---

## Test Results

**134 / 0 / 0** (passed / failed / errors) — BUILD SUCCESSFUL, 0 TypeScript errors
