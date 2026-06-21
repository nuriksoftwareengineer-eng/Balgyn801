import type { PaymentProvider } from "@/shared/api/types";

// ─── Pending payment (balgyn_pending_payment_v1) ──────────────────────────────
// Saved when an order is created; read on /cart mount to show recovery banner.
// Cleared when: payment succeeds, user explicitly dismisses OrderSuccess screen.
// TTL mirrors the server-side expiry window (60 min) minus a safety margin.

export interface PendingItem {
  title: string;
  qty: number;
  price: number;
}

export interface PendingPaymentRecord {
  orderId: number;
  amount: number;
  items: PendingItem[];
  provider: PaymentProvider;
  expiresAt: number; // epoch ms
}

const PENDING_KEY = "balgyn_pending_payment_v1";
const PAYMENT_WINDOW_MS = 58 * 60 * 1000; // 58 min — slightly under server's 60 min

export function savePendingPayment(record: Omit<PendingPaymentRecord, "expiresAt">): void {
  try {
    const full: PendingPaymentRecord = { ...record, expiresAt: Date.now() + PAYMENT_WINDOW_MS };
    localStorage.setItem(PENDING_KEY, JSON.stringify(full));
  } catch {
    // storage quota exceeded — not fatal
  }
}

export function loadPendingPayment(): PendingPaymentRecord | null {
  try {
    const raw = localStorage.getItem(PENDING_KEY);
    if (!raw) return null;
    const record = JSON.parse(raw) as PendingPaymentRecord;
    if (Date.now() > record.expiresAt) {
      localStorage.removeItem(PENDING_KEY);
      return null;
    }
    return record;
  } catch {
    return null;
  }
}

export function clearPendingPayment(): void {
  localStorage.removeItem(PENDING_KEY);
}

// ─── Last payment info (balgyn_last_payment) ─────────────────────────────────
// Shown on /payment/success, /payment/cancelled, /payment/failed.
// Moved from sessionStorage → localStorage so it survives tab close.

export interface LastPaymentInfo {
  orderId: number;
  totalPrice: number;
  provider: string;
  cancelToken?: string; // PayPal only — HMAC-signed token required by /cancel endpoint
}

const LAST_KEY = "balgyn_last_payment";

export function saveLastPayment(info: LastPaymentInfo): void {
  try {
    localStorage.setItem(LAST_KEY, JSON.stringify(info));
  } catch {
    // storage quota exceeded — not fatal
  }
}

export function loadLastPayment(): LastPaymentInfo | null {
  try {
    // Check localStorage first; fall back to sessionStorage for in-flight sessions
    const local = localStorage.getItem(LAST_KEY);
    if (local) return JSON.parse(local) as LastPaymentInfo;
    const session = sessionStorage.getItem(LAST_KEY);
    if (session) return JSON.parse(session) as LastPaymentInfo;
    return null;
  } catch {
    return null;
  }
}

export function clearLastPayment(): void {
  localStorage.removeItem(LAST_KEY);
  sessionStorage.removeItem(LAST_KEY); // clean up old sessionStorage location
}
