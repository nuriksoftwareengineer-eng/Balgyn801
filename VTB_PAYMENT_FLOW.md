# VTB Kazakhstan — Payment Flow Diagrams

**Date:** 2026-06-25  
**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html

---

## 1. Standard HPP Payment Flow (Hosted Payment Page)

This is the recommended flow for e-commerce. No card data touches the merchant server.
PCI DSS requirement: **SAQ-A only**.

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐     ┌────────────┐
│  Frontend   │     │   Backend   │     │   VTB Gateway       │     │  Customer  │
│ (React/TS)  │     │ (Spring)    │     │ payment.vtb.kz      │     │  Browser   │
└──────┬──────┘     └──────┬──────┘     └──────────┬──────────┘     └─────┬──────┘
       │                   │                        │                      │
       │  POST /payments/init                        │                      │
       │   { orderId, provider: VTB_KZ }             │                      │
       │──────────────────>│                        │                      │
       │                   │                        │                      │
       │                   │  POST /payment/rest/register.do               │
       │                   │   orderNumber, amount (tiyn),                 │
       │                   │   returnUrl, failUrl, currency=398,           │
       │                   │   dynamicCallbackUrl, userName, password       │
       │                   │───────────────────────>│                      │
       │                   │                        │                      │
       │                   │  { orderId: "UUID",    │                      │
       │                   │    formUrl: "https://vtbkz..." }               │
       │                   │<───────────────────────│                      │
       │                   │                        │                      │
       │  { paymentUrl: formUrl, status: PENDING }  │                      │
       │<──────────────────│                        │                      │
       │                   │                        │                      │
       │                   │                        │   Redirect browser   │
       │   Redirect to formUrl ──────────────────────────────────────────>│
       │                   │                        │                      │
       │                   │                        │   Customer enters    │
       │                   │                        │   card data on VTB   │
       │                   │                        │   hosted form        │
       │                   │                        │<─────────────────────│
       │                   │                        │                      │
       │                   │                        │   3DS2 challenge     │
       │                   │                        │<───────────────────> │
       │                   │                        │   (if required)      │
       │                   │                        │                      │
       │                   │                        │   Payment processed  │
       │                   │                        │                      │
       │                   │  GET {callbackUrl}     │                      │
       │                   │   ?mdOrder=...         │                      │
       │                   │   &operation=deposited │                      │
       │                   │   &status=1            │                      │
       │                   │   &checksum=...        │                      │
       │                   │<───────────────────────│                      │
       │                   │                        │                      │
       │                   │  [OPTIONAL] Verify     │                      │
       │                   │  SHA256 checksum       │                      │
       │                   │  (if VTB_HMAC_SECRET   │                      │
       │                   │   configured; does NOT │                      │
       │                   │   block if INVALID)    │                      │
       │                   │                        │                      │
       │                   │  POST /payment/rest/getOrderStatusExtended.do │
       │                   │   orderId: "UUID"      │                      │
       │                   │───────────────────────>│                      │
       │                   │                        │                      │
       │                   │  { orderStatus: 2,     │                      │
       │                   │    amount: ...,        │                      │
       │                   │    currency: 398 }     │                      │
       │                   │<───────────────────────│                      │
       │                   │                        │                      │
       │                   │  orderStatus==2:       │                      │
       │                   │  payment.status=SUCCEEDED                     │
       │                   │  order.status=CONFIRMED│                      │
       │                   │                        │                      │
       │                   │  HTTP 200 OK           │                      │
       │                   │───────────────────────>│                      │
       │                   │                        │                      │
       │                   │                        │  Redirect to returnUrl
       │                   │                        │─────────────────────>│
       │                   │                        │                      │
       │  GET /payment/success                      │                      │
       │<──────────────────────────────────────────────────────────────────│
```

---

## 2. Payment Failure Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
│  Frontend   │     │   Backend   │     │   VTB Gateway       │
└──────┬──────┘     └──────┬──────┘     └──────────┬──────────┘
       │                   │                        │
       │                   │  [customer declines or card declined]
       │                   │                        │
       │                   │  GET {callbackUrl}     │
       │                   │   ?operation=deposited │
       │                   │   &status=0 (failed)   │
       │                   │   &mdOrder=...         │
       │                   │<───────────────────────│
       │                   │                        │
       │                   │  getOrderStatusExtended→ orderStatus=3 (Declined)
       │                   │  payment.status=FAILED │
       │                   │  order.status stays PENDING_PAYMENT
       │                   │                        │
       │                   │                        │  Redirect to failUrl
       │  GET /payment/failed?error=DECLINED         │─────────────────────>
```

---

## 3. Refund Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
│  Admin UI   │     │   Backend   │     │   VTB Gateway       │
└──────┬──────┘     └──────┬──────┘     └──────────┬──────────┘
       │                   │                        │
       │  POST /api/v1/payments/{id}/refund         │
       │   { amount: optional }                     │
       │──────────────────>│                        │
       │                   │                        │
       │                   │  POST /payment/rest/refund.do
       │                   │   orderId: "VTB-UUID", │
       │                   │   amount: tiyn (optional)
       │                   │───────────────────────>│
       │                   │                        │
       │                   │  { errorCode: 0 }      │
       │                   │<───────────────────────│
       │                   │                        │
       │                   │  payment.status=REFUNDED
       │                   │  order.status=REFUNDED │
       │                   │                        │
       │                   │  (VTB sends callback:  │
       │                   │   operation=refunded)  │
       │                   │<───────────────────────│
       │  { success: true }│                        │
       │<──────────────────│                        │
```

---

## 4. Two-Phase Payment (Pre-Authorization) Flow

Used for "hold and capture" scenarios (e.g., hold until item ships).

```
Phase 1 — AUTHORIZE (Hold)
──────────────────────────
Backend → POST /payment/rest/registerPreAuth.do → get orderId + formUrl
Customer → pays on HPP → authorization placed → orderStatus=1
Callback: operation=authorized, status=1

Phase 2 — CAPTURE (Deposit)
────────────────────────────
Backend → POST /payment/rest/depositorder.do (orderId)
→ orderStatus=2 (Deposited = confirmed charge)
Callback: operation=deposited, status=1

Cancel Pre-Auth (if needed before capture)
───────────────────────────────────────────
Backend → POST /payment/rest/reverse.do (orderId)
→ orderStatus=4 (Reversed)
Callback: operation=reversed, status=1
```

---

## 5. Cancel / Reverse Flow

**Only works on authorized (orderStatus=1) or registered (orderStatus=0) orders.**

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
│  Admin UI   │     │   Backend   │     │   VTB Gateway       │
└──────┬──────┘     └──────┬──────┘     └──────────┬──────────┘
       │                   │                        │
       │  POST /payments/{id}/cancel                │
       │──────────────────>│                        │
       │                   │                        │
       │                   │  orderStatus==0 → POST /cancel.do
       │                   │  orderStatus==1 → POST /reverse.do
       │                   │───────────────────────>│
       │                   │                        │
       │                   │  { errorCode: 0 }      │
       │                   │<───────────────────────│
       │                   │  order.status=CANCELLED│
       │  { success: true }│                        │
       │<──────────────────│                        │
```

---

## 6. Status Check Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
│  Admin/Job  │     │   Backend   │     │   VTB Gateway       │
└──────┬──────┘     └──────┬──────┘     └──────────┬──────────┘
       │                   │                        │
       │  GET /payments/{id}/status                 │
       │──────────────────>│                        │
       │                   │                        │
       │                   │  POST /payment/rest/getOrderStatusExtended.do
       │                   │   orderId: "VTB-UUID"  │
       │                   │───────────────────────>│
       │                   │                        │
       │                   │  { orderStatus, amount,│
       │                   │    currency, authCode, │
       │                   │    cardholderName... } │
       │                   │<───────────────────────│
       │                   │                        │
       │                   │  Map orderStatus       │
       │                   │  2 → SUCCEEDED         │
       │                   │  3 → FAILED            │
       │                   │  4 → REVERSED          │
       │                   │  5 → REFUNDED          │
       │  { payment }      │                        │
       │<──────────────────│                        │
```

---

## 7. Callback Signature Verification Algorithm

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html#callback-notifications  
**Source:** https://sandbox.vtb-bank.kz/ru/integration/mportal3/mp3.html

> ⚠️ **CRITICAL CORRECTION**: Earlier research (from Radar Payments docs) incorrectly documented
> HMAC-SHA256. The official VTB Kazakhstan docs describe a **SHA256 concatenation** approach —
> same family as FreedomPay's MD5, but with SHA256. Must be verified via sandbox before implementation.

### Algorithm (from official VTB Kazakhstan docs)

```
CALLBACK RECEIVED:
  mdOrder=abc123&orderNumber=ORDER-11&checksum=ABCDEF123&operation=deposited&status=1

STEP 1: Extract checksum value
  received_checksum = "ABCDEF123"

STEP 2: Remove checksum from parameter map
  params = { mdOrder: "abc123", orderNumber: "ORDER-11", operation: "deposited", status: "1" }

STEP 3: Sort remaining params alphabetically by KEY

STEP 4: Build semicolon-delimited string, append callback secret (password)
  Formula: SHA256(param1;param2;...;paramN;callbackSecret)
  → values joined by ";" then ";" + callbackSecret appended

STEP 5: Compute SHA256
  computed_hex = SHA256(verificationString).toUpperCase()

STEP 6: Compare
  IF computed_hex == received_checksum.toUpperCase():
    → VALID: process payment
    → Call getOrderStatusExtended.do to confirm
    → Return HTTP 200
  ELSE:
    → INVALID: reject callback, log warning
    → Return HTTP 200 (to stop retries) but DO NOT confirm order
```

### Differences from HMAC-SHA256

| Aspect | HMAC-SHA256 | VTB SHA256 Concat |
|--------|-------------|-------------------|
| Key position | Separate key parameter | Appended to data string |
| Algorithm | HMAC(key, data) | SHA256(data + ";" + key) |
| Similar to | JWT signing | FreedomPay MD5 |
| Verification lib | `Mac.getInstance("HmacSHA256")` | `MessageDigest.getInstance("SHA-256")` |

### Callback Token Management

The callback secret is generated in the VTB Kazakhstan merchant portal:
- **Path:** Settings → Merchant → Callback Notifications
- **Method:** Click "Generate" button
- **Regenerate:** Allowed at any time; revert within 5 minutes
- **Asymmetric option:** Contact VTB KZ support for asymmetric key (certificate-based)
- **Portal URLs:**
  - TEST: `https://vtbkz.rbsuat.com/mportal3`
  - PROD: `https://payment.vtb.kz/generalmp3/auth/login`

### ⚠️ Open Questions (Must Verify in Sandbox)

1. Are params in Step 4 **values only** (like FreedomPay) or **key=value pairs**?  
   Example: either `"abc123;deposited;ORDER-11;1;secret"` OR `"mdOrder=abc123;operation=deposited;...;secret"`
2. Is sorting by **key name** alphabetically? Or by **value**?
3. Is the string encoding UTF-8 before SHA256?

These must be tested with a real sandbox callback before writing `VtbCallbackVerifier`.

---

## 8. Amount Conversion Rules

All amounts in VTB API are in **minor currency units**.

| Currency | Minor Unit | Example |
|----------|-----------|---------|
| KZT (398) | tiyn (1/100) | 12 000 KZT = `1200000` |
| RUB (643) | kopecks | 1 000 RUB = `100000` |
| USD (840) | cents | 10.50 USD = `1050` |
| EUR (978) | euro cents | 25.00 EUR = `2500` |

**Conversion for this project:**
```
vtbAmount = orderTotalPrice × 100  (as Long, rounded HALF_UP)
```

---

## 9. Sandbox Test Cards

**Source:** https://sandbox.vtb-bank.kz/ru/integration/structure/test-cards.html

> ⚠️ **CRITICAL FINDING**: VTB Kazakhstan sandbox test cards are **Mir payment system** (BIN 2201 38xx).
> Earlier research incorrectly listed `4000001111111118`. All VTB KZ sandbox testing must use Mir cards.

**Credentials (all cards):**
- CVC: `123`
- Expiry: `12/34`
- 3DS protocol: 3DS2

| Card Number | 3DS Type | Result |
|-------------|----------|--------|
| `2201 3820 0000 0021` | Full authentication | ✅ Success |
| `2201 3820 0000 0039` | Full authentication | ✅ Success |
| `2201 3820 0000 0013` | Full authentication | ✅ Success |
| `2201 3820 0000 0047` | Full authentication | ✅ Success |
| `2201 3820 0000 0054` | Full authentication | ✅ Success |
| `2201 3820 0000 0062` | Frictionless | ✅ Success |

**To simulate failed payment:**
- Use any of the above cards with **incorrect CVC** or **wrong expiry date**
- Error code returned: `71015` (Decline. Input error)

**Note:** No test cards for Visa/Mastercard/UnionPay provided in sandbox docs.
VTB KZ sandbox appears configured primarily for Mir cards.

---

## 10. Key Action Codes

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/action_codes.html

| Code | Category | Meaning |
|------|----------|---------|
| `0` / `000` | ✅ Success | Payment processed successfully |
| `20` | Card | Insufficient funds |
| `71015` | Input | Invalid payment data (wrong CVC/expiry) |
| `101` | Card | Card expired |
| `106` | Card | Card blocked (PIN exceeded) |
| `-2007` | Timeout | Payment timeout (20 min default) |
| `-2009` | 3DS | Cardholder didn't return from ACS |
| `-2006` | 3DS | 3DS verification failed |
| `2003` | 3DS | Payments require 3D Secure |
| `-20010` | Limits | Payment exceeds issuer limits |
| `121` | Limits | Daily transaction limit exceeded |
| `151` | Fraud | Fraud monitoring triggered |
| `8204` | Duplicate | Duplicate order number |
| `2002` | Status | Invalid operation for current order status |
| `2012` | Status | Order already cancelled |
| `-2014` | Abandon | Payer didn't return from payment page |
| `909` | System | System error (general) |
| `912` | System | Issuer bank unavailable |
