import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";
import { loadLastPayment, clearLastPayment, clearPendingPayment } from "@/shared/lib/pending-payment";

function CheckIcon() {
  return (
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#000" />
      <path d="M9 16.5l5 5 9-9" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function PaymentSuccessPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const [payment, setPayment] = useState<ReturnType<typeof loadLastPayment>>(null);

  useEffect(() => {
    const info = loadLastPayment();
    if (info) setPayment(info);
    // Clear both records: payment succeeded, no recovery needed
    clearLastPayment();
    clearPendingPayment();
  }, []);

  const orderId = payment?.orderId ?? params.get("orderId") ?? null;
  const provider = payment?.provider ?? params.get("provider") ?? null;
  const totalPrice = payment?.totalPrice ?? null;

  function fmt(n: number) {
    return new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 0 }).format(n) + " ₸";
  }

  const providerLabel =
    provider === "PAYPAL" ? "PayPal" :
    provider === "FREEDOM_PAY" ? "Freedom Pay" :
    provider === "VTB_KZ" ? "VTB Kazakhstan" :
    provider ?? "—";

  return (
    <div className="py-16 md:py-24">
      <Container className="max-w-xl">
        <div className="mb-8 flex items-center gap-4">
          <CheckIcon />
          <h1 className="font-display text-3xl font-bold tracking-tight text-black md:text-4xl">
            {t("payment.success.title")}
          </h1>
        </div>

        <p className="mb-8 text-[15px] text-[--color-muted]">
          {t("payment.success.subtitle")}
        </p>

        {(orderId || totalPrice || provider) && (
          <div className="mb-8 border border-[--color-border] bg-white divide-y divide-[--color-border]">
            {orderId && (
              <div className="flex items-center justify-between px-5 py-4">
                <span className="text-[12px] font-semibold uppercase tracking-[0.12em] text-[--color-muted]">
                  {t("payment.success.orderNumber")}
                </span>
                <span className="text-[15px] font-bold text-black">#{orderId}</span>
              </div>
            )}
            {totalPrice != null && totalPrice > 0 && (
              <div className="flex items-center justify-between px-5 py-4">
                <span className="text-[12px] font-semibold uppercase tracking-[0.12em] text-[--color-muted]">
                  {t("payment.success.amount")}
                </span>
                <span className="text-[15px] font-bold text-black">{fmt(totalPrice)}</span>
              </div>
            )}
            {provider && (
              <div className="flex items-center justify-between px-5 py-4">
                <span className="text-[12px] font-semibold uppercase tracking-[0.12em] text-[--color-muted]">
                  {t("payment.success.method")}
                </span>
                <span className="text-[14px] font-medium text-black">{providerLabel}</span>
              </div>
            )}
          </div>
        )}

        <div className="flex flex-wrap gap-3">
          <Link
            to="/orders"
            className="inline-flex items-center justify-center bg-black px-6 py-3 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
          >
            {t("payment.success.toOrders")}
          </Link>
          <Link
            to="/catalog"
            className="inline-flex items-center justify-center border border-[--color-border] bg-white px-6 py-3 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black/60"
          >
            {t("payment.success.toCatalog")}
          </Link>
        </div>
      </Container>
    </div>
  );
}
