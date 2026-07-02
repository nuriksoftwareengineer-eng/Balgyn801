# VTB Kazakhstan — Multi-Currency Design

**Date:** 2026-06-25

---

## 1. Problem Statement

The Balgyn store currently supports multiple display currencies (KZT, USD, EUR, RUB). Orders are stored with a `totalPrice` and the frontend can display prices in different currencies.

VTB Kazakhstan API accepts the `currency` parameter as ISO 4217 **numeric** code. The default for Kazakhstan merchants is KZT (398).

**Goal:** When a customer selects a currency and proceeds to pay via VTB KZ, the payment should be initiated in that same currency — **with no manual configuration changes**.

---

## 2. Currency Support Reality Check

**What VTB Kazakhstan officially supports for merchants:**

| Currency | ISO Numeric | Status |
|----------|------------|--------|
| KZT | 398 | ✅ Confirmed — primary merchant currency |
| USD | 840 | ⚠️ Platform supports, but merchant agreement required |
| EUR | 978 | ⚠️ Platform supports, but merchant agreement required |
| RUB | 643 | ❌ **NOT confirmed** — requires separate investigation |

**ARCHITECTURAL DECISION:** Design for automatic currency selection, but implement safe fallback to KZT. The system should not crash if the merchant only has a KZT acquiring account.

---

## 3. Proposed Architecture

### 3.1 Currency Mapping Layer

A dedicated component `VtbCurrencyMapper` maps display currencies to VTB ISO numeric codes and determines which ones are enabled.

```
┌──────────────────────────────────────────────────┐
│             VtbCurrencyMapper                    │
│                                                  │
│  Input:  order.currency (String: "KZT","USD"...) │
│  Output: VtbCurrency { isoCode, enabled }        │
│                                                  │
│  Rules:                                          │
│  1. Look up ISO numeric from currency string     │
│  2. Check if this currency is enabled in         │
│     VtbProperties (merchant agreement scope)     │
│  3. If enabled → use that currency               │
│  4. If NOT enabled → fallback to KZT (398)       │
│     + log warning                                │
└──────────────────────────────────────────────────┘
```

### 3.2 VtbProperties Currency Configuration

```yaml
# In VtbProperties:

# Which currencies this merchant account supports (comma-separated ISO numeric codes)
# Default: KZT only (398)
# Set to "398,840,978" if merchant has multi-currency agreement with VTB KZ
vtb.supported-currencies: ${VTB_SUPPORTED_CURRENCIES:398}

# Whether to fall back to KZT when order currency is unsupported
# Default: FALSE — throw BusinessRuleException if currency not in supported list
# Rationale: silent currency conversion (e.g. USD→KZT) charges customer in wrong currency
# Only set to true if you explicitly want silent fallback AND have informed the customer
vtb.fallback-to-kzt: ${VTB_FALLBACK_TO_KZT:false}
```

### 3.3 Currency Selection Logic

```
GIVEN: order.currency = "USD"

STEP 1: Map "USD" → ISO 840
STEP 2: Is 840 in vtbProperties.supportedCurrencies?
  YES → vtbCurrency = 840, proceed in USD
  NO  → vtbFallback = true? → vtbCurrency = 398 (KZT)
         → log WARN: "Order in USD, VTB merchant not configured for USD — charging in KZT"
         → This should only happen in prod if merchant hasn't signed multi-currency agreement
```

---

## 4. Order Amount in Merchant's Currency

The project's `ExchangeRateService` already handles conversions between currencies. The order's `totalPrice` is stored in the order's currency. For VTB payment:

```
IF order.currency == vtbChargeCurrency:
  vtbAmount = order.totalPrice × 100 (tiyn/cents)

IF order.currency != vtbChargeCurrency (fallback to KZT):
  Need to convert order amount to KZT first
  kztAmount = exchangeRateService.convert(order.totalPrice, order.currency → KZT)
  vtbAmount = kztAmount × 100
```

**IMPORTANT:** If falling back from USD to KZT, the displayed price and charged amount are in different currencies. This must be logged and the customer must be informed. Ideally, do NOT fall back silently — reject the payment and ask customer to set currency to KZT.

**Recommended approach for fallback:**
- Throw `BusinessRuleException("Currency USD not supported by VTB KZ merchant configuration")`
- Return HTTP 422 to frontend
- Frontend shows: "Pay in KZT" option

This is cleaner than silent currency conversion.

---

## 5. Multi-Currency Flow End-to-End

### Scenario A: Customer chose KZT (standard)

```
Customer selects KZT in checkout
↓
Order stored with totalPrice=12000.00, currency="KZT"
↓
POST /payments/init { orderId: 11, provider: VTB_KZ }
↓
VtbCurrencyMapper: "KZT" → 398 (enabled → OK)
↓
register.do: amount=1200000, currency=398
↓
Payment proceeds in KZT
```

### Scenario B: Customer chose USD (merchant has USD agreement)

```
Customer selects USD in checkout
↓
Order stored with totalPrice=25.00, currency="USD"
↓
POST /payments/init { orderId: 12, provider: VTB_KZ }
↓
VtbCurrencyMapper: "USD" → 840 (enabled → OK)
↓
register.do: amount=2500, currency=840
↓
VTB charges customer $25.00 USD
```

### Scenario C: Customer chose RUB (merchant DOES NOT have RUB agreement)

```
Customer selects RUB in checkout
↓
Order stored with totalPrice=1200.00, currency="RUB"
↓
POST /payments/init { orderId: 13, provider: VTB_KZ }
↓
VtbCurrencyMapper: "RUB" → 643 (NOT enabled)
↓
VTB_FALLBACK_TO_KZT=false → throw BusinessRuleException
  → "VTB KZ does not support RUB payments. Please pay in KZT."
↓
Frontend shows: "This payment method doesn't support RUB. Please use FreedomPay or select KZT."
```

---

## 6. Currency ISO Numeric Code Table

| Display Currency | ISO Numeric | Minor Unit |
|-----------------|-------------|-----------|
| KZT | 398 | tiyn (×100) |
| RUB | 643 | kopecks (×100) |
| USD | 840 | cents (×100) |
| EUR | 978 | euro cents (×100) |

Implementation:
```java
// In VtbCurrencyMapper
static final Map<String, Integer> CURRENCY_ISO_NUMERIC = Map.of(
    "KZT", 398,
    "RUB", 643,
    "USD", 840,
    "EUR", 978
);
```

---

## 7. Preventing Double-Currency Confusion

**Rule:** The currency used in `register.do` MUST match the currency stored on the `Payment` entity.

**Design invariant:**
```
payment.currency = order.currency
vtbRequest.currency = VtbCurrencyMapper.map(payment.currency)
```

When the callback arrives and we call `getOrderStatusExtended`, we verify:
- `response.currency` matches the ISO code we sent
- `response.amount` matches the amount we sent (in minor units)

If amounts differ → reject, log, flag for manual review.

---

## 8. ENV Variables for Multi-Currency

```properties
# Currency codes this merchant can charge (comma-separated ISO numeric codes)
# "398" = KZT only (default and safest)
# "398,840,978" = KZT + USD + EUR
VTB_SUPPORTED_CURRENCIES=398

# Whether to fall back to KZT for unsupported currencies (true=yes, false=throw error)
VTB_FALLBACK_TO_KZT=false
```

---

## 9. Summary

| Requirement | Solution |
|-------------|---------|
| Customer selects KZT → VTB charges KZT | VtbCurrencyMapper maps "KZT"→398, register.do uses 398 |
| Customer selects USD → VTB charges USD | VtbCurrencyMapper maps "USD"→840, only if 840 in supported list |
| Customer selects RUB → no agreement | Throw BusinessRuleException, frontend shows error message |
| No manual switches needed | VtbCurrencyMapper reads from order.currency automatically |
| Safe default | If no supported currencies configured, KZT is always the fallback |
