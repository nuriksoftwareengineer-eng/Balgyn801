import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";
import { getApiBaseUrl } from "@/shared/api/http";
import { loadLastPayment } from "@/shared/lib/pending-payment";

function CancelIcon() {
  return (
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#a1a1aa" />
      <path d="M16 10v7M16 20v2" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}

export function PaymentCancelledPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [payment, setPayment] = useState<ReturnType<typeof loadLastPayment>>(null);
  const cancelledRef = useRef(false);

  useEffect(() => {
    // Read from localStorage (survives tab close); falls back to sessionStorage
    const info = loadLastPayment();
    if (info) setPayment(info);

    // PayPal redirects to cancel URL with ?token=PAYPAL_ORDER_ID.
    // Notify the backend so the payment row is marked CANCELLED instead of staying PENDING.
    // cancelToken (HMAC-signed) is required by the backend to prevent unauthenticated cancellation.
    const token = params.get("token");
    if (token && !cancelledRef.current) {
      cancelledRef.current = true;
      const cancelToken = info?.cancelToken ?? "";
      const qs = cancelToken ? `?cancelToken=${encodeURIComponent(cancelToken)}` : "";
      fetch(`${getApiBaseUrl()}/payments/paypal/cancel/${token}${qs}`, { method: "POST" }).catch(
        () => { /* best-effort — backend expiry job handles PENDING cleanup anyway */ }
      );
    }
  }, [params]);

  const orderId = payment?.orderId ?? params.get("orderId") ?? null;

  function handleRetry() {
    // Pass orderId via navigation state so CartPage recovery banner activates immediately
    navigate("/cart", {
      state: orderId != null
        ? { retryOrderId: Number(orderId), retryAmount: payment?.totalPrice }
        : undefined,
    });
  }

  return (
    <div className="py-16 md:py-24">
      <Container className="max-w-xl">
        <div className="mb-8 flex items-center gap-4">
          <CancelIcon />
          <h1 className="font-display text-3xl font-bold tracking-tight text-black md:text-4xl">
            {t("payment.cancelled.title")}
          </h1>
        </div>

        <p className="mb-8 text-[15px] text-[--color-muted]">
          {t("payment.cancelled.subtitle")}
        </p>

        {orderId && (
          <div className="mb-8 border border-[--color-border] bg-white">
            <div className="flex items-center justify-between px-5 py-4">
              <span className="text-[12px] font-semibold uppercase tracking-[0.12em] text-[--color-muted]">
                {t("payment.cancelled.orderNumber")}
              </span>
              <span className="text-[15px] font-bold text-black">#{orderId}</span>
            </div>
          </div>
        )}

        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            onClick={handleRetry}
            className="inline-flex items-center justify-center bg-black px-6 py-3 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
          >
            {t("payment.cancelled.retry")}
          </button>
          <Link
            to="/catalog"
            className="inline-flex items-center justify-center border border-[--color-border] bg-white px-6 py-3 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black/60"
          >
            {t("payment.cancelled.toCatalog")}
          </Link>
        </div>
      </Container>
    </div>
  );
}
