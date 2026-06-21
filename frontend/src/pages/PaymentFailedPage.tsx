import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";
import { loadLastPayment } from "@/shared/lib/pending-payment";

function XIcon() {
  return (
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#ef4444" />
      <path d="M11 11l10 10M21 11L11 21" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}

export function PaymentFailedPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [payment, setPayment] = useState<ReturnType<typeof loadLastPayment>>(null);

  useEffect(() => {
    // Read from localStorage (survives tab close); falls back to sessionStorage
    // Keep record — user needs it for retry via cart recovery banner
    const info = loadLastPayment();
    if (info) setPayment(info);
  }, []);

  const orderId = payment?.orderId ?? params.get("orderId") ?? null;
  const errorMsg = params.get("error") ?? null;

  function handleRetry() {
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
          <XIcon />
          <h1 className="font-display text-3xl font-bold tracking-tight text-black md:text-4xl">
            {t("payment.failed.title")}
          </h1>
        </div>

        <p className="mb-8 text-[15px] text-[--color-muted]">
          {t("payment.failed.subtitle")}
        </p>

        {(orderId || errorMsg) && (
          <div className="mb-8 border border-[--color-border] bg-white divide-y divide-[--color-border]">
            {orderId && (
              <div className="flex items-center justify-between px-5 py-4">
                <span className="text-[12px] font-semibold uppercase tracking-[0.12em] text-[--color-muted]">
                  {t("payment.failed.orderNumber")}
                </span>
                <span className="text-[15px] font-bold text-black">#{orderId}</span>
              </div>
            )}
            {errorMsg && (
              <div className="px-5 py-4">
                <span className="text-[13px] text-red-500">{errorMsg}</span>
              </div>
            )}
          </div>
        )}

        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            onClick={handleRetry}
            className="inline-flex items-center justify-center bg-black px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
          >
            {t("payment.failed.retry")}
          </button>
          <Link
            to="/catalog"
            className="inline-flex items-center justify-center border border-[--color-border] bg-white px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-black transition hover:border-black/60"
          >
            {t("payment.failed.toCatalog")}
          </Link>
        </div>
      </Container>
    </div>
  );
}
