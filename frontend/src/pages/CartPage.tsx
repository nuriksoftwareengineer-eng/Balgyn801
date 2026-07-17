import { useMutation, useQuery } from "@tanstack/react-query";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/app/auth-context";
import { useCart } from "@/app/use-cart";
import { isLegacyLine, isDesignLine } from "@/app/cart-context";
import {
  calculateCdekTariffByOrder,
  createOrder,
  getDeliveryCountries,
  getDeliveryMethods,
  getIntlQuote,
  initPayment,
  listCdekDeliveryPoints,
  searchCdekCities,
  validateCoupon,
} from "@/shared/api/backend-api";
import type {
  CdekCity,
  CdekDeliveryPoint,
  CdekOrderTariffResponse,
  CouponValidateResponse,
  CreateOrderRequest,
  DeliveryAddressRequest,
  DeliveryMethodResponse,
  DeliveryType,
  OrderResponse,
  PaymentProvider,
} from "@/shared/api/types";
import { ApiError } from "@/shared/api/http";
import { Price } from "@/shared/ui/price";
import { useCurrency } from "@/app/currency-context";
import { cn } from "@/shared/lib/cn";
import { Container } from "@/shared/ui/container";
import {
  savePendingPayment,
  loadPendingPayment,
  clearPendingPayment,
  saveLastPayment,
  type PendingPaymentRecord,
} from "@/shared/lib/pending-payment";
import { haptic } from "@/shared/lib/telegram";
import { useTelegramMainButton } from "@/shared/lib/telegram/hooks";

// DELIVERY_LABELS, DELIVERY_REGIONS, and STEP_LABELS are built inside the component via t()

// Флаг «намерения оформить заказ»: ставится при попытке checkout без авторизации,
// переживает редирект на /login и читается после возврата на /cart.
const CHECKOUT_INTENT_KEY = "balgyn_checkout_intent";

const inputClass =
  "w-full border-b border-[--color-border] bg-transparent py-3 text-sm text-black outline-none transition placeholder:text-[--color-muted] focus:border-black";

// ── Utils ──────────────────────────────────────────────────────────────────────

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), delayMs);
    return () => window.clearTimeout(id);
  }, [value, delayMs]);
  return debounced;
}

function buildCdekAddress(
  city: CdekCity,
  point: CdekDeliveryPoint,
  recipientName: string,
  recipientPhone: string,
): DeliveryAddressRequest {
  const cityLabel = city.region?.trim()
    ? `${city.city}, ${city.region}`
    : city.city;
  return {
    city: cityLabel,
    street: `СДЭК ПВЗ «${point.name}» [${point.code}]: ${point.address}`,
    apartment: "—",
    postalCode: "050000",
    recipientName: recipientName.trim(),
    recipientPhone: recipientPhone.trim(),
  };
}

// ── Icons ──────────────────────────────────────────────────────────────────────

function CheckIcon({ size = 10 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size * 0.8}
      viewBox="0 0 10 8"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M1 4l3 3 5-6"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

// ── Step indicator ─────────────────────────────────────────────────────────────

function StepIndicator({
  step,
  skipStep4,
}: {
  step: number;
  skipStep4: boolean;
}) {
  const { t } = useTranslation();
  const stepLabels = [
    t("cart.checkoutFlow.steps.contacts"),
    t("cart.checkoutFlow.steps.region"),
    t("cart.checkoutFlow.steps.delivery"),
    t("cart.checkoutFlow.steps.details"),
    t("cart.checkoutFlow.steps.summary"),
  ];
  return (
    <div className="flex items-start" role="list" aria-label={t("cart.checkoutFlow.title")}>
      {stepLabels.map((label, i) => {
        const id = i + 1;
        const isSkipped = id === 4 && skipStep4;
        const done = !isSkipped && id < step;
        const active = !isSkipped && id === step;

        return (
          <div
            key={id}
            role="listitem"
            className={cn(
              "flex flex-1 flex-col items-center",
              isSkipped && "opacity-25",
            )}
          >
            <div className="flex w-full items-center">
              {i > 0 && (
                <div
                  className={cn(
                    "h-px flex-1 transition-colors duration-300",
                    done || active ? "bg-black" : "bg-[--color-border]",
                  )}
                />
              )}
              <div
                className={cn(
                  "flex h-7 w-7 shrink-0 items-center justify-center text-[0.6rem] font-semibold transition-colors duration-300",
                  active
                    ? "bg-black text-white"
                    : done
                      ? "bg-black text-white"
                      : "border border-[--color-border] bg-white text-[--color-muted]",
                )}
              >
                {done ? <CheckIcon /> : id}
              </div>
              {i < stepLabels.length - 1 && (
                <div
                  className={cn(
                    "h-px flex-1 transition-colors duration-300",
                    done ? "bg-black" : "bg-[--color-border]",
                  )}
                />
              )}
            </div>
            <span
              className={cn(
                "mt-1.5 text-center text-[0.65rem] uppercase tracking-[0.08em] transition-colors duration-300",
                active
                  ? "font-semibold text-black"
                  : done
                    ? "text-black"
                    : "text-[--color-muted]",
              )}
            >
              {label}
            </span>
          </div>
        );
      })}
    </div>
  );
}

// ── Field label ────────────────────────────────────────────────────────────────

function FieldLabel({
  children,
  optional,
}: {
  children: React.ReactNode;
  optional?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
      {children}
      {optional ? (
        <span className="ml-1 normal-case font-normal">{t("cart.form.optional")}</span>
      ) : null}
    </span>
  );
}

// ── Summary sidebar (desktop) ──────────────────────────────────────────────────

type SidebarLine = {
  lineKey: string;
  title: string;
  meta: string | null;
  qty: number;
  price: number;
};

function toSidebarLine(l: ReturnType<typeof useCart>["lines"][number]): SidebarLine {
  if (isDesignLine(l)) {
    return {
      lineKey: l.lineKey,
      title: `${l.title} (${l.garmentLabel})`,
      meta: [l.colorName, l.sizeLabel].join(" · "),
      qty: l.qty,
      price: l.price,
    };
  }
  return {
    lineKey: l.lineKey,
    title: l.title,
    meta: [l.size, l.color].filter(Boolean).join(" · ") || null,
    qty: l.qty,
    price: l.price,
  };
}

function SummarySidebar({
  lines,
  subtotal,
  deliveryType,
  selectedMethod,
  intlFeeKzt,
  intlQuoteLoading,
  intlQuoteErrorMessage,
}: {
  lines: SidebarLine[];
  subtotal: number;
  deliveryType: DeliveryType | null;
  selectedMethod: DeliveryMethodResponse | null;
  intlFeeKzt?: number | null;
  intlQuoteLoading?: boolean;
  intlQuoteErrorMessage?: string | null;
}) {
  const { t } = useTranslation();
  // CDEK: delivery paid at pickup point — total is items only.
  // INTERNATIONAL: fee is the backend zone-tariff quote passed in as a prop.
  // Other methods: add the estimated delivery fee if known.
  const deliveryFeeForTotal =
    deliveryType === "INTERNATIONAL"
      ? (intlFeeKzt ?? 0)
      : deliveryType !== "CDEK" && selectedMethod?.estimatedFeeKzt != null
        ? selectedMethod.estimatedFeeKzt
        : 0;
  const grandTotal = subtotal + deliveryFeeForTotal;

  const deliveryLabel =
    selectedMethod?.labelRu ??
    (deliveryType ? t(`orders.delivery_type.${deliveryType}`) : null);

  return (
    <aside className="w-72 shrink-0">
      <div className="sticky top-[96px] border border-[--color-border] bg-[--color-surface] p-5">
        <p className="mb-4 text-[0.6rem] font-semibold uppercase tracking-[0.16em] text-black">
          {t("cart.summary.yourOrder")}
        </p>
        <ul className="m-0 flex flex-col gap-3 p-0 list-none">
          {lines.map((l) => (
            <li key={l.lineKey} className="flex items-start gap-3">
              <div className="min-w-0 flex-1">
                <p className="m-0 truncate text-xs font-medium text-black">
                  {l.title}
                </p>
                {l.meta ? (
                  <p className="m-0 text-[0.6rem] uppercase tracking-wider text-[--color-muted]">
                    {l.meta}
                  </p>
                ) : null}
              </div>
              <div className="shrink-0 text-right">
                <p className="m-0 text-[0.6rem] text-[--color-muted]">×{l.qty}</p>
                <p className="m-0 text-xs font-semibold text-black">
                  <Price kzt={l.price * l.qty} />
                </p>
              </div>
            </li>
          ))}
        </ul>

        <div className="mt-4 flex flex-col gap-2 border-t border-[--color-border] pt-4">
          <div className="flex justify-between text-sm">
            <span className="text-[--color-muted]">{t("cart.summary.items")}</span>
            <span className="font-medium text-black"><Price kzt={subtotal} /></span>
          </div>

          {deliveryType === "CDEK" ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{t("cart.summary.cdekDelivery")}</span>
              <span className="text-[--color-muted]">{t("cart.summary.atReceipt")}</span>
            </div>
          ) : deliveryType === "INTERNATIONAL" ? (
            <div className="flex justify-between gap-4 text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              {intlQuoteLoading ? (
                <span className="text-[--color-muted]">…</span>
              ) : intlQuoteErrorMessage ? (
                <span className="text-right text-red-600">{intlQuoteErrorMessage}</span>
              ) : intlFeeKzt != null ? (
                <span className="font-medium text-black"><Price kzt={intlFeeKzt} /></span>
              ) : (
                <span className="text-[--color-muted]">…</span>
              )}
            </div>
          ) : selectedMethod?.estimatedFeeKzt === 0 ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              <span className="font-medium text-emerald-600">{t("cart.form.free")}</span>
            </div>
          ) : selectedMethod?.estimatedFeeKzt != null ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              <span className="font-medium text-black">
                <Price kzt={selectedMethod.estimatedFeeKzt} />
              </span>
            </div>
          ) : deliveryLabel ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              <span className="text-[--color-muted]">{t("cart.summary.onAgreement")}</span>
            </div>
          ) : null}

          <div className="flex justify-between border-t border-[--color-border] pt-2">
            <span className="text-sm font-semibold text-black">{t("cart.total")}</span>
            <span className="text-base font-semibold text-black">
              <Price kzt={grandTotal} />
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
}

// ── Payment providers config ───────────────────────────────────────────────────

const PAYMENT_PROVIDERS = [
  {
    id: "FREEDOM_PAY" as PaymentProvider,
    titleKey: "payment.bankCard",
    descKey: "payment.bankCardDesc",
    footerKey: "payment.freedomPayNetworks",
    logo: (selected: boolean) => (
      <div className="flex items-center gap-1.5">
        <span className={cn(
          "flex h-[22px] w-[34px] items-center justify-center rounded border text-[9px] font-black italic",
          selected ? "border-white/30 bg-white/10 text-white" : "border-zinc-200 bg-white text-[#1434CB]",
        )}>VISA</span>
        <svg width="34" height="22" viewBox="0 0 34 22" fill="none" className={cn(
          "rounded border",
          selected ? "border-white/30" : "border-zinc-200",
        )}>
          <rect width="34" height="22" fill={selected ? "rgba(255,255,255,0.08)" : "white"} rx="2"/>
          <circle cx="13" cy="11" r="6" fill="#EB001B" fillOpacity="0.9"/>
          <circle cx="21" cy="11" r="6" fill="#F79E1B" fillOpacity="0.9"/>
          <path d="M17 6.2a6 6 0 0 1 0 9.6 6 6 0 0 1 0-9.6z" fill="#FF5F00" fillOpacity="0.8"/>
        </svg>
      </div>
    ),
  },
  {
    id: "PAYPAL" as PaymentProvider,
    titleKey: "payment.paypalLabel",
    descKey: "payment.paypalDesc",
    footerKey: "payment.paypalNetworks",
    logo: (selected: boolean) => (
      <div className={cn(
        "flex h-[22px] w-[56px] items-center justify-center rounded border text-[11px] font-extrabold tracking-tight",
        selected ? "border-white/30 bg-white/10 text-white" : "border-zinc-200 bg-[#003087] text-white",
      )}>
        <span className="text-[#009CDE]">Pay</span><span>Pal</span>
      </div>
    ),
  },
  {
    id: "VTB_KZ" as PaymentProvider,
    titleKey: "payment.vtbCard",
    descKey: "payment.vtbCardDesc",
    footerKey: "payment.vtbNetworks",
    logo: (selected: boolean) => (
      <div className={cn(
        "flex h-[22px] w-[42px] items-center justify-center rounded border text-[9px] font-extrabold tracking-widest",
        selected ? "border-white/30 bg-white/10 text-white" : "border-zinc-200 bg-[#003087] text-white",
      )}>
        VTB
      </div>
    ),
  },
];

// ── Order success ──────────────────────────────────────────────────────────────

function OrderSuccess({
  order,
  onContinue,
  onPay,
  paymentProvider,
  onPaymentProviderChange,
  paymentBusy,
  paymentError,
}: {
  order: OrderResponse;
  onContinue: () => void;
  onPay: () => void;
  paymentProvider: PaymentProvider;
  onPaymentProviderChange: (p: PaymentProvider) => void;
  paymentBusy: boolean;
  paymentError: string | null;
}) {
  const { t } = useTranslation();
  const deliveryLabel = t(`orders.delivery_type.${order.deliveryType}`) || order.deliveryType;
  const fee =
    order.deliveryFee != null && order.deliveryFee > 0 ? order.deliveryFee : null;
  const goodsTotal =
    fee != null
      ? (order.items?.length ?? 0) > 0
        ? order.items!.reduce((s, i) => s + i.unitPrice * i.quantity, 0)
        : Math.max(0, order.totalPrice - fee)
      : order.totalPrice;

  return (
    <motion.div
      className="mx-auto max-w-lg"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
    >
      <div className="border border-emerald-200 bg-emerald-50 px-8 py-10 text-center">
        <p className="m-0 text-[0.6rem] font-semibold uppercase tracking-[0.2em] text-emerald-700">
          {t("cart.order.accepted")}
        </p>
        <p className="mt-3 text-4xl font-semibold text-black">№ {order.id}</p>

        <div className="mt-5 flex flex-col gap-2 text-sm">
          {fee != null ? (
            <>
              <div className="flex justify-between">
                <span className="text-[--color-muted]">{t("cart.summary.items")}</span>
                <strong className="text-black"><Price kzt={goodsTotal} /></strong>
              </div>
              <div className="flex justify-between">
                <span className="text-[--color-muted]">{t("cart.order.delivery")}</span>
                <strong className="text-black"><Price kzt={fee} /></strong>
              </div>
              <div className="flex justify-between border-t border-emerald-200 pt-2">
                <span className="font-semibold text-black">{t("cart.total")}</span>
                <strong className="text-lg text-black">
                  <Price kzt={order.totalPrice} />
                </strong>
              </div>
            </>
          ) : (
            <>
              <div className="flex justify-between">
                <span className="text-[--color-muted]">{t("cart.order.amount")}</span>
                <strong className="text-black">
                  <Price kzt={order.totalPrice} />
                </strong>
              </div>
              {order.deliveryType === "CDEK" ? (
                <div className="flex justify-between">
                  <span className="text-[--color-muted]">{t("cart.order.cdekDelivery")}</span>
                  <span className="text-[--color-muted]">{t("cart.order.payOnReceipt")}</span>
                </div>
              ) : deliveryLabel ? (
                <div className="flex justify-between">
                  <span className="text-[--color-muted]">{t("cart.order.delivery")}</span>
                  <span className="text-black">{deliveryLabel}</span>
                </div>
              ) : null}
            </>
          )}
        </div>

        {order.address ? (
          <p className="mt-4 border border-emerald-200 bg-white px-4 py-3 text-left text-xs text-[--color-muted]">
            <span className="font-semibold text-black">{t("cart.order.address")}</span>
            {order.address.city}, {order.address.street}
            {order.address.apartment !== "—"
              ? `, ${t("cart.order.apt")} ${order.address.apartment}`
              : ""}
            . {order.address.recipientName},{" "}
            {order.address.recipientPhone}
          </p>
        ) : null}

        <p className="mt-4 text-sm text-[--color-muted]">
          {t("cart.order.contactNote")}
        </p>
      </div>

      <div className="mt-4 border border-[--color-border] bg-white px-6 py-5">
        <p className="mb-1 text-[0.6rem] font-semibold uppercase tracking-[0.16em] text-black">
          {t("cart.order.payment")}
        </p>
        <p className="mb-4 text-sm text-[--color-muted]">
          {t("cart.order.paymentDesc")}
        </p>
        <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          {PAYMENT_PROVIDERS.map((p) => {
            const selected = paymentProvider === p.id;
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => onPaymentProviderChange(p.id)}
                className={cn(
                  "relative flex flex-col gap-3 border p-4 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-black",
                  selected
                    ? "border-black bg-black text-white"
                    : "border-[--color-border] bg-white text-black hover:border-black/60",
                )}
                aria-pressed={selected}
              >
                {selected && (
                  <span className="absolute right-3 top-3 flex h-4 w-4 items-center justify-center rounded-full bg-white text-black">
                    <svg width="8" height="7" viewBox="0 0 8 7" fill="none" aria-hidden="true">
                      <path d="M1 3.5l2 2 4-4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </span>
                )}
                {p.logo(selected)}
                <div>
                  <p className="text-[13px] font-bold leading-tight">{t(p.titleKey)}</p>
                  <p className={cn(
                    "mt-0.5 text-[11px]",
                    selected ? "text-white/70" : "text-[--color-muted]",
                  )}>{t(p.descKey)}</p>
                </div>
                <p className={cn(
                  "text-[11px]",
                  selected ? "text-white/50" : "text-zinc-400",
                )}>
                  {t(p.footerKey)}
                </p>
              </button>
            );
          })}
        </div>
        <button
          type="button"
          disabled={paymentBusy}
          onClick={onPay}
          className="bg-black px-6 py-2.5 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800 disabled:opacity-50"
        >
          {paymentBusy ? t("payment.payingBtn") : t("payment.payBtn")}
        </button>
        {paymentError ? (
          <p className="mt-2 text-sm text-[--color-danger]">{paymentError}</p>
        ) : null}
      </div>

      <div className="mt-6 flex flex-wrap gap-3">
        <Link
          to="/catalog"
          onClick={onContinue}
          className="inline-flex h-11 items-center justify-center bg-black px-6 text-sm font-medium tracking-wide text-white transition hover:bg-zinc-800"
        >
          {t("cart.order.toCatalog")}
        </Link>
        <button
          type="button"
          onClick={onContinue}
          className="border border-[--color-border] px-6 py-2.5 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black"
        >
          {t("cart.order.close")}
        </button>
      </div>
    </motion.div>
  );
}

// ── Recovery banner ────────────────────────────────────────────────────────────
// Shown when the user returns to /cart after F5, tab close, or cancelled/failed
// payment. Uses the localStorage pending-payment record to restore context.

function RecoveryBanner({
  orderId,
  amount,
  provider,
  onProviderChange,
  onPay,
  onDismiss,
  busy,
  error,
}: {
  orderId: number;
  amount: number;
  provider: PaymentProvider;
  onProviderChange: (p: PaymentProvider) => void;
  onPay: () => void;
  onDismiss: () => void;
  busy: boolean;
  error: string | null;
}) {
  const { t } = useTranslation();

  return (
    <motion.div
      className="mx-auto max-w-lg"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {/* Status banner */}
      <div className="border border-amber-200 bg-amber-50 px-8 py-8">
        <p className="m-0 text-[0.6rem] font-semibold uppercase tracking-[0.2em] text-amber-700">
          {t("recovery.title")}
        </p>
        <p className="mt-2 text-3xl font-semibold text-black">
          {t("recovery.order", { id: orderId })}
        </p>
        {amount > 0 && (
          <p className="mt-1 text-sm font-medium text-black">
            <Price kzt={amount} />
          </p>
        )}
        <p className="mt-3 text-sm text-amber-700">
          {t("recovery.subtitle")}
        </p>
      </div>

      {/* Payment method + button */}
      <div className="mt-4 border border-[--color-border] bg-white px-6 py-5">
        <p className="mb-3 text-[0.6rem] font-semibold uppercase tracking-[0.16em] text-black">
          {t("cart.order.payment")}
        </p>
        <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          {PAYMENT_PROVIDERS.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => onProviderChange(p.id)}
              className={cn(
                "flex items-center gap-2 border px-4 py-3 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-black",
                provider === p.id
                  ? "border-black bg-black text-white"
                  : "border-[--color-border] bg-white text-black hover:border-black/60",
              )}
              aria-pressed={provider === p.id}
            >
              <span className="text-[12px] font-bold">{t(p.titleKey)}</span>
            </button>
          ))}
        </div>
        <button
          type="button"
          disabled={busy}
          onClick={onPay}
          className="bg-black px-6 py-2.5 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800 disabled:opacity-50"
        >
          {busy ? t("payment.payingBtn") : t("payment.payBtn")}
        </button>
        {error && (
          <p className="mt-2 text-sm text-[--color-danger]">{error}</p>
        )}
      </div>

      <button
        type="button"
        onClick={onDismiss}
        className="mt-4 text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-[--color-danger]"
      >
        {t("recovery.dismiss")}
      </button>
    </motion.div>
  );
}

// ── CartPage ───────────────────────────────────────────────────────────────────

export function CartPage() {
  const { t, i18n } = useTranslation();
  const { format } = useCurrency();

  const STEP_LABELS = [
    t("cart.checkoutFlow.steps.contacts"),
    t("cart.checkoutFlow.steps.region"),
    t("cart.checkoutFlow.steps.delivery"),
    t("cart.checkoutFlow.steps.details"),
    t("cart.checkoutFlow.steps.summary"),
  ];

  const DELIVERY_REGIONS = [
    { iso2: "KZ", label: t("cart.regions.KZ"), hint: t("cart.regions.KZ_hint") },
    { iso2: "RU", label: t("cart.regions.RU"), hint: t("cart.regions.RU_hint") },
    { iso2: "OTHER", label: t("cart.regions.OTHER"), hint: t("cart.regions.OTHER_hint") },
  ];

  const navigate = useNavigate();
  const location = useLocation();
  const { lines, totalQty, subtotal, increment, decrement, removeLine, clear } =
    useCart();
  const { user, token } = useAuth();
  const reduceMotion = useReducedMotion();

  // ── UI phase ─────────────────────────────────────────────────────────────────
  const [phase, setPhase] = useState<"cart" | "checkout">("cart");
  const [step, setStep] = useState(1);
  const [formError, setFormError] = useState<string | null>(null);

  // ── Step 1: Contacts ─────────────────────────────────────────────────────────
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [telegramUsername, setTelegramUsername] = useState("");

  // ── Step 2: Country ──────────────────────────────────────────────────────────
  const [countryIso2, setCountryIso2] = useState("KZ");
  // «Другие страны»: выбранный регион-псевдокод и поиск. Международная доставка — всегда
  // авиа (единственный способ), выбор типа перевозки покупателю не предлагается.
  const [regionChoice, setRegionChoice] = useState("KZ");
  const [countrySearch, setCountrySearch] = useState("");

  // ── Step 3: Delivery type ────────────────────────────────────────────────────
  const [deliveryType, setDeliveryType] = useState<DeliveryType>("PICKUP");

  // ── Step 4 — address-based (TAXI, POSTAL, INTERNATIONAL) ────────────────────
  const [addrCity, setAddrCity] = useState("");
  const [addrStreet, setAddrStreet] = useState("");
  const [addrApartment, setAddrApartment] = useState("");
  const [addrPostal, setAddrPostal] = useState("");
  const [addrRecipientName, setAddrRecipientName] = useState("");
  const [addrRecipientPhone, setAddrRecipientPhone] = useState("");

  // ── Step 4 — CDEK ─────────────────────────────────────────────────────────
  const [cdekCitySearch, setCdekCitySearch] = useState("");
  const [selectedCity, setSelectedCity] = useState<CdekCity | null>(null);
  const [selectedPoint, setSelectedPoint] = useState<CdekDeliveryPoint | null>(
    null,
  );
  // Локальный фильтр по уже загруженному списку ПВЗ (без доп. запросов к API).
  const [pvzFilter, setPvzFilter] = useState("");
  const [pvzOpen, setPvzOpen] = useState(false);
  const [cdekTariff, setCdekTariff] = useState<CdekOrderTariffResponse | null>(
    null,
  );
  const [cdekCalcPending, setCdekCalcPending] = useState(false);
  const [cdekCalcError, setCdekCalcError] = useState<string | null>(null);
  const [cdekRecipientName, setCdekRecipientName] = useState("");
  const [cdekRecipientPhone, setCdekRecipientPhone] = useState("");

  // ── Step 5: Comment ──────────────────────────────────────────────────────────
  const [comment, setComment] = useState("");

  // ── Payment ──────────────────────────────────────────────────────────────────
  const [completedOrder, setCompletedOrder] = useState<OrderResponse | null>(
    null,
  );
  const [paymentProvider, setPaymentProvider] =
    useState<PaymentProvider>("FREEDOM_PAY");
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  // ── Coupon ────────────────────────────────────────────────────────────────────
  const [couponInput, setCouponInput] = useState("");
  const [appliedCoupon, setAppliedCoupon] = useState<CouponValidateResponse | null>(null);
  const [couponError, setCouponError] = useState<string | null>(null);
  const [couponBusy, setCouponBusy] = useState(false);

  // ── Recovery: pending payment from localStorage / navigation state ────────────
  const [pendingRecord, setPendingRecord] = useState<PendingPaymentRecord | null>(null);
  const [recoveryProvider, setRecoveryProvider] = useState<PaymentProvider>("FREEDOM_PAY");

  // ── Delivery methods from backend ────────────────────────────────────────────
  // Prefetch on step 2 so step 3 loads instantly after country selection.
  const deliveryMethodsQuery = useQuery({
    queryKey: ["delivery", "methods", countryIso2],
    queryFn: () => getDeliveryMethods(countryIso2),
    enabled: phase === "checkout" && step >= 2 && countryIso2.length > 0,
    staleTime: 5 * 60 * 1000,
  });

  // ── Международная доставка: страны + расчёт ──────────────────────────────────
  const countriesQuery = useQuery({
    queryKey: ["delivery", "countries"],
    queryFn: getDeliveryCountries,
    enabled: phase === "checkout" && regionChoice === "OTHER",
    staleTime: 10 * 60 * 1000,
  });

  // ── Международная доставка: позиции корзины (для расчёта веса на бэкенде) ────
  // linesSig/intlQuoteItems перенесены сюда (выше intlQuoteQuery, которая на них
  // ссылается) — иначе const, объявленная ниже по коду, недоступна на момент вызова.
  const linesSig = useMemo(
    () => lines.map((l) => `${l.lineKey}:${l.qty}`).join("|"),
    [lines],
  );

  const intlQuoteItems = useMemo(
    () =>
      lines
        .filter(isDesignLine)
        .map((l) => ({ designGarmentId: l.designGarmentId, quantity: l.qty })),
    [lines],
  );

  const intlQuoteQuery = useQuery({
    queryKey: ["delivery", "intl-quote", countryIso2, linesSig],
    queryFn: () => getIntlQuote(countryIso2, intlQuoteItems),
    enabled:
      phase === "checkout" &&
      regionChoice === "OTHER" &&
      countryIso2.length > 0,
    staleTime: 5 * 60 * 1000,
  });

  // ── CDEK queries ─────────────────────────────────────────────────────────────
  const debouncedCityQuery = useDebouncedValue(cdekCitySearch.trim(), 400);

  const citiesQuery = useQuery({
    queryKey: ["cdek", "cities", debouncedCityQuery],
    queryFn: () => searchCdekCities(debouncedCityQuery, 15),
    enabled: deliveryType === "CDEK" && debouncedCityQuery.length >= 2,
  });

  const pointsQuery = useQuery({
    queryKey: ["cdek", "points", selectedCity?.code],
    queryFn: () => listCdekDeliveryPoints(selectedCity!.code),
    enabled: deliveryType === "CDEK" && selectedCity != null,
  });

  // ── Derived ───────────────────────────────────────────────────────────────────

  const selectedMethod =
    deliveryMethodsQuery.data?.find((m) => m.type === deliveryType) ?? null;

  // When selectedMethod isn't loaded yet, fall back to the known PICKUP=free rule.
  const requiresAddress = selectedMethod
    ? selectedMethod.requiresAddress
    : deliveryType !== "PICKUP";

  // CDEK: delivery paid at pickup point — order total is items only.
  // Other methods: add the estimated delivery fee if known.
  const deliveryFeeForTotal =
    deliveryType === "INTERNATIONAL"
      ? (intlQuoteQuery.data?.priceKzt ?? 0)
      : deliveryType !== "CDEK" && selectedMethod?.estimatedFeeKzt != null
        ? selectedMethod.estimatedFeeKzt
        : 0;
  const grandTotal = subtotal + deliveryFeeForTotal;

  // ── Effects ───────────────────────────────────────────────────────────────────

  useEffect(() => {
    const st = location.state as {
      retryOrderId?: number;
      retryAmount?: number;
    } | null;

    // Restore pending record from OrderHistory "Pay" button or payment cancelled/failed pages
    if (st?.retryOrderId && !completedOrder) {
      const record: PendingPaymentRecord = {
        orderId: st.retryOrderId,
        amount: st.retryAmount ?? 0,
        items: [],
        provider: "FREEDOM_PAY",
        expiresAt: Date.now() + 58 * 60 * 1000,
      };
      setPendingRecord(record);
      setRecoveryProvider("FREEDOM_PAY");
    }

    if (st?.retryOrderId) {
      navigate(location.pathname, { replace: true, state: {} });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname, location.state, navigate]);

  // Check localStorage for a pending payment record on first mount.
  // Only runs once; location-state-based recovery is handled in the effect above.
  useEffect(() => {
    if (completedOrder) return;
    const record = loadPendingPayment();
    if (record) {
      // Don't overwrite if location state already set pendingRecord (handled synchronously above)
      setPendingRecord((prev) => prev ?? record);
      setRecoveryProvider(record.provider);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Возврат в checkout после авторизации: гость, нажавший «Оформить заказ»,
  // был отправлен на /login с выставленным флагом. После успешного входа он
  // возвращается на /cart — здесь подхватываем намерение и открываем оформление.
  useEffect(() => {
    if (!user) return;
    if (sessionStorage.getItem(CHECKOUT_INTENT_KEY)) {
      sessionStorage.removeItem(CHECKOUT_INTENT_KEY);
      if (lines.length > 0) {
        setPhase("checkout");
        setStep(1);
      }
    }
  }, [user, lines.length]);

  // Reset delivery type when country changes and the current type is unavailable.
  useEffect(() => {
    const methods = deliveryMethodsQuery.data;
    if (!methods) return;
    if (!methods.find((m) => m.type === deliveryType)) {
      setDeliveryType(methods[0]?.type ?? "PICKUP");
    }
  }, [deliveryMethodsQuery.data, deliveryType]);

  useEffect(() => {
    if (deliveryType !== "CDEK") {
      setCdekCitySearch("");
      setSelectedCity(null);
      setSelectedPoint(null);
      setCdekTariff(null);
      setCdekCalcError(null);
      setPvzFilter("");
      setPvzOpen(false);
    }
  }, [deliveryType]);

  // Auto-calculate CDEK tariff whenever city/PVZ/cart changes — no button needed
  useEffect(() => {
    setCdekTariff(null);
    setCdekCalcError(null);

    if (
      deliveryType !== "CDEK" ||
      !selectedCity ||
      !selectedPoint ||
      lines.length === 0
    ) {
      setCdekCalcPending(false);
      return;
    }

    let cancelled = false;
    setCdekCalcPending(true);

    calculateCdekTariffByOrder({
      toCityCode: selectedCity.code,
      items: lines.map((l) =>
        isDesignLine(l)
          ? { designGarmentId: l.designGarmentId, quantity: l.qty }
          : { productId: l.productId, quantity: l.qty },
      ),
    })
      .then((t) => {
        if (!cancelled) setCdekTariff(t);
      })
      .catch((err) => {
        if (!cancelled)
          setCdekCalcError(
            err instanceof ApiError
              ? err.message
              : t("cart.errors.cdekDeliveryError"),
          );
      })
      .finally(() => {
        if (!cancelled) setCdekCalcPending(false);
      });

    return () => {
      cancelled = true;
    };
    // linesSig tracks lines changes; lines itself used inside via closure
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deliveryType, selectedPoint?.code, selectedCity?.code, linesSig]);

  useEffect(() => {
    if (step === 4) {
      if (requiresAddress && !selectedMethod?.requiresCitySearch) {
        setAddrRecipientName((n) => n || customerName);
        setAddrRecipientPhone((p) => p || customerPhone);
      }
      if (selectedMethod?.requiresCitySearch) {
        setCdekRecipientName((n) => n || customerName);
        setCdekRecipientPhone((p) => p || customerPhone);
      }
    }
  }, [
    step,
    requiresAddress,
    selectedMethod?.requiresCitySearch,
    customerName,
    customerPhone,
  ]);

  // ── Validation ────────────────────────────────────────────────────────────────

  function canAdvance(): boolean {
    switch (step) {
      case 1:
        return (
          customerName.trim().length > 0 && customerPhone.trim().length > 0
        );
      case 2:
        return countryIso2.length > 0;
      case 3:
        return true;
      case 4:
        if (!requiresAddress) return true;
        if (selectedMethod?.requiresCitySearch) {
          return (
            !!selectedCity &&
            !!selectedPoint &&
            !cdekCalcPending &&
            cdekRecipientName.trim().length > 0 &&
            cdekRecipientPhone.trim().length > 0
          );
        }
        return [
          addrCity,
          addrStreet,
          addrApartment,
          addrRecipientName,
          addrRecipientPhone,
        ].every((f) => f.trim().length > 0);
      case 5:
        return true;
      default:
        return false;
    }
  }

  // ── Navigation ────────────────────────────────────────────────────────────────

  function handleNext() {
    if (!canAdvance()) {
      setFormError(
        step === 4 && selectedMethod?.requiresCitySearch && cdekCalcPending
          ? t("cart.errors.calculating")
          : step === 4 && selectedMethod?.requiresCitySearch && !selectedPoint
            ? t("cart.errors.selectCdekPoint")
            : t("cart.errors.fillRequired"),
      );
      return;
    }
    setFormError(null);
    if (step === 3 && !requiresAddress) {
      setStep(5);
      return;
    }
    if (step < 5) setStep((s) => s + 1);
  }

  function handleBack() {
    setFormError(null);
    if (step === 1) {
      setPhase("cart");
      return;
    }
    if (step === 5 && !requiresAddress) {
      setStep(3);
      return;
    }
    setStep((s) => Math.max(s - 1, 1));
  }

  // ── Order mutation ────────────────────────────────────────────────────────────

  const orderMutation = useMutation({
    mutationFn: (body: CreateOrderRequest) => createOrder(body, token),
    onSuccess: (order) => {
      haptic("success");
      setFormError(null);
      setPaymentError(null);
      setPaymentBusy(false);
      clear();
      setCompletedOrder(order);
      setPendingRecord(null); // clear any previous recovery banner
      // Persist to localStorage so F5 / tab-close shows recovery banner
      savePendingPayment({
        orderId: order.id,
        amount: order.totalPrice,
        provider: paymentProvider,
        items: (order.items ?? []).map((i) => ({
          title: i.productTitle ?? "",
          qty: i.quantity,
          price: i.unitPrice,
        })),
      });
      setPhase("cart");
      setStep(1);
      setCustomerName("");
      setCustomerPhone("");
      setTelegramUsername("");
      setCountryIso2("KZ");
      setDeliveryType("PICKUP");
      setComment("");
      setAppliedCoupon(null);
      setCouponInput("");
    },
    onError: (err: unknown) => {
      haptic("error");
      setFormError(
        err instanceof ApiError ? err.message : t("cart.errors.orderFailed"),
      );
    },
  });

  function handleSubmitOrder() {
    setFormError(null);

    let address: DeliveryAddressRequest | null = null;
    let pvzCode: string | null = null;

    if (selectedMethod?.requiresCitySearch) {
      if (!selectedCity || !selectedPoint) {
        setFormError(t("cart.errors.cdekAddressRequired"));
        return;
      }
      address = buildCdekAddress(
        selectedCity,
        selectedPoint,
        cdekRecipientName,
        cdekRecipientPhone,
      );
      pvzCode = selectedPoint.code;
    } else if (requiresAddress) {
      address = {
        city: addrCity.trim(),
        street: addrStreet.trim(),
        apartment: addrApartment.trim(),
        postalCode: addrPostal.trim() || "000000",
        recipientName: addrRecipientName.trim(),
        recipientPhone: addrRecipientPhone.trim(),
      };
    }

    const tg = telegramUsername.trim().replace(/^@/, "");
    const countryIso2ForOrder = requiresAddress ? countryIso2 : null;

    const payload: CreateOrderRequest = {
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      telegramUsername: tg || null,
      deliveryType,
      comment: comment.trim() || null,
      countryIso2: countryIso2ForOrder,
      pvzCode,
      couponCode: appliedCoupon?.code ?? null,
      items: lines.map((l) =>
        isDesignLine(l)
          ? {
              designGarmentId: l.designGarmentId,
              colorId: l.colorId,
              sizeId: l.sizeId,
              quantity: l.qty,
              currency: "KZT",
            }
          : {
              productId: l.productId,
              quantity: l.qty,
              size: l.size ?? null,
              color: l.color ?? null,
            },
      ),
      address,
    };

    orderMutation.mutate(payload);
  }

  // Mirrors the on-screen Continue/Submit button inside Telegram's native Main Button.
  // Hidden entirely while browsing the cart (phase === "cart") or outside Telegram.
  useTelegramMainButton(
    phase === "checkout"
      ? step < 5
        ? { text: t("cart.checkoutFlow.continue"), onClick: handleNext }
        : {
            text: orderMutation.isPending
              ? t("cart.checkoutFlow.submitting")
              : t("cart.checkoutFlow.submit"),
            onClick: handleSubmitOrder,
            isEnabled: !orderMutation.isPending,
            isLoaderVisible: orderMutation.isPending,
          }
      : null,
  );

  async function handleApplyCoupon() {
    const code = couponInput.trim();
    if (!code) return;
    setCouponError(null);
    setCouponBusy(true);
    try {
      const result = await validateCoupon(code, subtotal);
      setAppliedCoupon(result);
    } catch (err) {
      setCouponError(
        err instanceof ApiError
          ? err.message
          : t("cart.coupon.invalid", "Промокод недействителен или не применим"),
      );
    } finally {
      setCouponBusy(false);
    }
  }

  async function handleInitPayment(override?: {
    orderId: number;
    provider: PaymentProvider;
    amount: number;
  }) {
    const oid = override?.orderId ?? completedOrder?.id;
    const prov = override?.provider ?? paymentProvider;
    if (!oid) return;

    setPaymentError(null);
    setPaymentBusy(true);
    try {
      const returnUrl = `${window.location.origin}/payment-return`;
      const cancelUrl = `${window.location.origin}/payment/cancelled`;

      const payment = await initPayment({
        orderId: oid,
        provider: prov,
        returnUrl,
        cancelUrl,
      });
      if (!payment.paymentUrl)
        throw new Error(t("cart.errors.paymentUrlError"));
      const paymentUrl = payment.paymentUrl;
      // Defense-in-depth: only follow absolute http(s) gateway URLs — never javascript:/data:.
      if (!/^https?:\/\//i.test(paymentUrl))
        throw new Error(t("cart.errors.paymentUrlError"));
      saveLastPayment({
        orderId: oid,
        totalPrice: override?.amount ?? completedOrder?.totalPrice ?? 0,
        provider: prov,
        cancelToken: payment.cancelToken ?? undefined,
      });

      window.location.href = paymentUrl;
    } catch (err: unknown) {
      setPaymentError(
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : t("cart.errors.paymentFailed"),
      );
    } finally {
      setPaymentBusy(false);
    }
  }

  // ── Step content ──────────────────────────────────────────────────────────────

  function renderStep() {
    switch (step) {
      // ── Step 1: Contacts ────────────────────────────────────────────────────
      case 1:
        return (
          <div className="flex flex-col gap-4">
            <p className="text-sm text-[--color-muted]">
              {t("cart.form.contactsDesc")}
            </p>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>{t("cart.form.name")}</FieldLabel>
              <input
                required
                autoComplete="name"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>{t("cart.form.phone")}</FieldLabel>
              <input
                required
                type="tel"
                autoComplete="tel"
                placeholder="+7 …"
                value={customerPhone}
                onChange={(e) => setCustomerPhone(e.target.value)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <FieldLabel optional>Telegram</FieldLabel>
              <input
                autoComplete="off"
                placeholder="@username"
                value={telegramUsername}
                onChange={(e) => setTelegramUsername(e.target.value)}
                className={inputClass}
              />
            </label>
          </div>
        );

      // ── Step 2: Delivery region ──────────────────────────────────────────────
      case 2:
        return (
          <div className="flex flex-col gap-3">
            <p className="text-sm text-[--color-muted]">
              {t("cart.form.regionDesc")}
            </p>
            {DELIVERY_REGIONS.map((r) => (
              <button
                key={r.iso2}
                type="button"
                onClick={() => {
                  setRegionChoice(r.iso2);
                  setCountryIso2(r.iso2 === "OTHER" ? "" : r.iso2);
                }}
                className={cn(
                  "flex items-center justify-between border px-5 py-4 text-left transition-colors duration-150",
                  regionChoice === r.iso2
                    ? "border-black bg-black text-white"
                    : "border-[--color-border] bg-white hover:border-zinc-400",
                )}
              >
                <div>
                  <p className="m-0 text-sm font-semibold">{r.label}</p>
                  <p
                    className={cn(
                      "m-0 mt-0.5 text-xs",
                      regionChoice === r.iso2
                        ? "text-white/70"
                        : "text-[--color-muted]",
                    )}
                  >
                    {r.hint}
                  </p>
                </div>
                {regionChoice === r.iso2 && (
                  <span className="ml-4 shrink-0">
                    <CheckIcon size={12} />
                  </span>
                )}
              </button>
            ))}

            {regionChoice === "OTHER" && (
              <div className="mt-2 flex flex-col gap-2">
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-black">
                  {t("cart.intl.country")}
                </p>
                <input
                  value={countrySearch}
                  onChange={(e) => setCountrySearch(e.target.value)}
                  placeholder={t("cart.intl.searchCountry")}
                  className="border border-[--color-border] px-4 py-3 text-sm outline-none focus:border-black"
                />
                <div className="max-h-56 overflow-y-auto border border-[--color-border]">
                  {(countriesQuery.data ?? [])
                    .filter((c) => {
                      const q = countrySearch.trim().toLowerCase();
                      if (!q) return true;
                      return (
                        c.nameRu.toLowerCase().includes(q) ||
                        c.nameEn.toLowerCase().includes(q)
                      );
                    })
                    .map((c) => (
                      <button
                        key={c.iso2}
                        type="button"
                        onClick={() => setCountryIso2(c.iso2)}
                        className={cn(
                          "flex w-full items-center justify-between px-4 py-2.5 text-left text-sm transition-colors",
                          countryIso2 === c.iso2
                            ? "bg-black text-white"
                            : "bg-white hover:bg-[--color-surface]",
                        )}
                      >
                        <span>{i18n.language.startsWith("en") ? c.nameEn : c.nameRu}</span>
                        {countryIso2 === c.iso2 && <CheckIcon size={10} />}
                      </button>
                    ))}
                  {countriesQuery.isFetching && (
                    <p className="px-4 py-2.5 text-xs text-[--color-muted]">…</p>
                  )}
                </div>
                {countryIso2 && (
                  <div className="mt-3 flex items-center justify-between border border-[--color-border] bg-[--color-surface] px-5 py-4">
                    <div>
                      <p className="m-0 text-sm font-semibold text-black">{t("cart.intl.air")}</p>
                      <p className="m-0 mt-0.5 text-xs text-[--color-muted]">{t("cart.intl.airDays")}</p>
                    </div>
                    <p className="m-0 text-sm font-medium text-black">
                      {intlQuoteQuery.isFetching ? (
                        "…"
                      ) : intlQuoteQuery.error ? (
                        <span className="text-red-600">
                          {(intlQuoteQuery.error as Error).message}
                        </span>
                      ) : intlQuoteQuery.data ? (
                        <Price kzt={intlQuoteQuery.data.priceKzt} />
                      ) : null}
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        );

      // ── Step 3: Delivery method ──────────────────────────────────────────────
      case 3: {
        const methods = deliveryMethodsQuery.data;
        const loading = deliveryMethodsQuery.isFetching;
        const fetchError = deliveryMethodsQuery.error;

        return (
          <div className="flex flex-col gap-3">
            <p className="text-sm text-[--color-muted]">
              {t("cart.form.methodDesc")}
            </p>
            {loading ? (
              <p className="text-sm text-[--color-muted]">
                {t("cart.form.loadingMethods")}
              </p>
            ) : fetchError ? (
              <p className="text-sm text-[--color-danger]">
                {t("cart.form.loadMethodsError")}
              </p>
            ) : methods && methods.length > 0 ? (
              methods.map((method) => (
                <button
                  key={method.type}
                  type="button"
                  onClick={() => setDeliveryType(method.type)}
                  className={cn(
                    "flex items-center justify-between border px-5 py-4 text-left transition-colors duration-150",
                    deliveryType === method.type
                      ? "border-black bg-black text-white"
                      : "border-[--color-border] bg-white hover:border-zinc-400",
                  )}
                >
                  <div>
                    <p className="m-0 text-sm font-semibold">{method.labelRu}</p>
                    {method.type === "INTERNATIONAL" ? (
                      <p
                        className={cn(
                          "m-0 mt-0.5 text-xs",
                          deliveryType === method.type
                            ? "text-white/70"
                            : intlQuoteQuery.error
                              ? "text-red-600"
                              : "text-[--color-muted]",
                        )}
                      >
                        {intlQuoteQuery.isFetching
                          ? "…"
                          : intlQuoteQuery.error
                            ? (intlQuoteQuery.error as Error).message
                            : intlQuoteQuery.data
                              ? format(intlQuoteQuery.data.priceKzt)
                              : "…"}
                      </p>
                    ) : method.cityRestriction ? (
                      <p
                        className={cn(
                          "m-0 mt-0.5 text-xs",
                          deliveryType === method.type
                            ? "text-white/70"
                            : "text-[--color-muted]",
                        )}
                      >
                        {t("cart.form.availableIn")} {method.cityRestriction}
                      </p>
                    ) : method.estimatedFeeKzt === 0 ? (
                      <p
                        className={cn(
                          "m-0 mt-0.5 text-xs",
                          deliveryType === method.type
                            ? "text-white/70"
                            : "text-emerald-600",
                        )}
                      >
                        {t("cart.form.free")}
                      </p>
                    ) : method.estimatedFeeKzt != null ? (
                      <p
                        className={cn(
                          "m-0 mt-0.5 text-xs",
                          deliveryType === method.type
                            ? "text-white/70"
                            : "text-[--color-muted]",
                        )}
                      >
                        {t("cart.form.fromPrice", { price: format(method.estimatedFeeKzt) })}
                      </p>
                    ) : (
                      <p
                        className={cn(
                          "m-0 mt-0.5 text-xs",
                          deliveryType === method.type
                            ? "text-white/70"
                            : "text-[--color-muted]",
                        )}
                      >
                        {t("cart.form.onAgreement")}
                      </p>
                    )}
                  </div>
                  {deliveryType === method.type && (
                    <span className="ml-4 shrink-0">
                      <CheckIcon size={12} />
                    </span>
                  )}
                </button>
              ))
            ) : (
              <p className="text-sm text-amber-600">
                {t("cart.form.noMethods")}
              </p>
            )}
          </div>
        );
      }

      // ── Step 4: Delivery details ─────────────────────────────────────────────
      case 4:
        if (!requiresAddress) {
          return (
            <div className="border border-[--color-border] bg-[--color-surface] px-5 py-5">
              <p className="text-sm font-semibold text-black">
                {selectedMethod?.labelRu ?? t("orders.delivery_type.PICKUP")}
              </p>
              <p className="mt-1 text-sm text-[--color-muted]">
                {t("cart.form.pickupDesc")}
              </p>
            </div>
          );
        }

        if (selectedMethod?.requiresCitySearch) {
          return (
            <div className="flex flex-col gap-5">
              <p className="text-sm text-[--color-muted]">
                {t("cart.form.cdekDesc")}
              </p>

              <div className="flex flex-col gap-2">
                <label className="flex flex-col gap-1.5">
                  <FieldLabel>{t("cart.form.citySearch")}</FieldLabel>
                  <input
                    value={cdekCitySearch}
                    onChange={(e) => {
                      setCdekCitySearch(e.target.value);
                      if (selectedCity) setSelectedCity(null);
                    }}
                    placeholder={t("cart.form.cityPlaceholder")}
                    autoComplete="off"
                    className={inputClass}
                  />
                </label>
                {citiesQuery.isFetching ? (
                  <p className="text-xs text-[--color-muted]">
                    {t("cart.form.searchingCities")}
                  </p>
                ) : null}
                {citiesQuery.data &&
                citiesQuery.data.length > 0 &&
                !selectedCity ? (
                  <ul className="m-0 max-h-48 overflow-y-auto border border-[--color-border] bg-white p-1 list-none">
                    {citiesQuery.data.map((c) => {
                      const label = c.region?.trim()
                        ? `${c.city}, ${c.region}`
                        : c.city;
                      return (
                        <li key={c.code}>
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedCity(c);
                              setCdekCitySearch(label);
                              setPvzFilter("");
                              setPvzOpen(false);
                            }}
                            className="w-full px-3 py-2.5 text-left text-sm text-black transition hover:bg-[--color-surface]"
                          >
                            {label}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                ) : null}
              </div>

              {selectedCity ? (
                <div className="flex flex-col gap-3">
                  {pointsQuery.isFetching ? (
                    <p className="text-xs text-[--color-muted]">
                      {t("cart.form.loadingPvz")}
                    </p>
                  ) : null}
                  {pointsQuery.data && pointsQuery.data.length > 0 ? (
                    (() => {
                      const all = pointsQuery.data ?? [];
                      const f = pvzFilter.trim().toLowerCase();
                      // Локальная фильтрация по уже загруженному списку: код / адрес / имя.
                      // Без обращений к API — список обновляется мгновенно при вводе.
                      const filtered = f
                        ? all.filter(
                            (p) =>
                              p.code.toLowerCase().includes(f) ||
                              (p.address ?? "").toLowerCase().includes(f) ||
                              (p.name ?? "").toLowerCase().includes(f),
                          )
                        : all;
                      return (
                        <div className="flex flex-col gap-2">
                          <FieldLabel>
                            {t("cart.form.pvzCount", { count: filtered.length, total: all.length })}
                          </FieldLabel>
                          {/* Custom click-to-open PVZ dropdown */}
                          <div className="relative">
                            <button
                              type="button"
                              onClick={() => {
                                setPvzOpen((o) => !o);
                                setPvzFilter("");
                              }}
                              className="flex w-full items-center justify-between border border-[--color-border] bg-white px-3 py-2.5 text-sm text-left transition focus:border-black focus:outline-none focus:ring-1 focus:ring-black"
                            >
                              <span className={selectedPoint ? "text-black" : "text-[--color-muted]"}>
                                {selectedPoint
                                  ? `${selectedPoint.code} — ${selectedPoint.address || selectedPoint.name}`
                                  : t("cart.form.pvzPlaceholder")}
                              </span>
                              <svg width="8" height="5" viewBox="0 0 8 5" fill="none" aria-hidden="true"
                                className={cn("shrink-0 ml-2 transition-transform duration-150", pvzOpen && "rotate-180")}>
                                <path d="M1 1l3 3 3-3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
                              </svg>
                            </button>
                            {pvzOpen && (
                              <div className="absolute z-20 left-0 right-0 top-full mt-0.5 border border-[--color-border] bg-white shadow-md">
                                <div className="border-b border-[--color-border] p-2">
                                  <input
                                    type="text"
                                    value={pvzFilter}
                                    onChange={(e) => setPvzFilter(e.target.value)}
                                    placeholder={t("cart.form.pvzFilter")}
                                    autoComplete="off"
                                    autoFocus
                                    className="w-full border border-[--color-border] px-3 py-2 text-sm outline-none focus:border-black focus:ring-1 focus:ring-black"
                                  />
                                </div>
                                <ul className="m-0 max-h-52 overflow-y-auto list-none p-0">
                                  {filtered.length === 0 ? (
                                    <li className="px-3 py-2.5 text-xs text-amber-600">{t("cart.form.pvzNotFound")}</li>
                                  ) : filtered.map((p) => (
                                    <li key={p.code}>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          setSelectedPoint(p);
                                          setPvzOpen(false);
                                          setPvzFilter("");
                                        }}
                                        className={cn(
                                          "w-full px-3 py-2.5 text-left text-sm transition hover:bg-[--color-surface]",
                                          selectedPoint?.code === p.code ? "bg-[--color-surface] font-medium text-black" : "text-black",
                                        )}
                                      >
                                        <span className="font-medium">{p.code}</span>
                                        {" — "}
                                        {p.address || p.name}
                                      </button>
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })()
                  ) : pointsQuery.isSuccess ? (
                    <p className="text-sm text-amber-600">
                      {t("cart.form.pvzEmpty")}
                    </p>
                  ) : null}

                  {selectedPoint ? (
                    <div className="flex flex-col gap-2">
                      {cdekCalcPending ? (
                        <p className="text-xs text-[--color-muted]">
                          {t("cart.errors.calculating")}
                        </p>
                      ) : cdekCalcError ? (
                        <p className="text-xs text-amber-600">{cdekCalcError}</p>
                      ) : cdekTariff ? (
                        <div className="border border-[--color-border] bg-[--color-surface] px-4 py-3">
                          <div className="flex items-center justify-between">
                            <span className="text-sm text-black">
                              {t("cart.form.cdekCost")}{" "}
                              <span className="font-semibold">
                                <Price kzt={cdekTariff.deliveryPrice} />
                              </span>
                            </span>
                            <span className="text-[0.65rem] uppercase tracking-[0.08em] text-[--color-muted]">
                              {t("cart.form.cdekAtReceipt")}
                            </span>
                          </div>
                          {cdekTariff.minDays && cdekTariff.maxDays ? (
                            <p className="mt-1 text-[0.65rem] text-[--color-muted]">
                              {t("cart.form.deliveryDays", { min: cdekTariff.minDays, max: cdekTariff.maxDays })}
                            </p>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              ) : null}

              <div className="flex flex-col gap-3 border-t border-[--color-border] pt-4">
                <p className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
                  {t("cart.form.recipientSection")}
                </p>
                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="flex flex-col gap-1.5">
                    <FieldLabel>{t("cart.form.recipientName")}</FieldLabel>
                    <input
                      required
                      autoComplete="name"
                      value={cdekRecipientName}
                      onChange={(e) => setCdekRecipientName(e.target.value)}
                      className={inputClass}
                    />
                  </label>
                  <label className="flex flex-col gap-1.5">
                    <FieldLabel>{t("cart.form.recipientPhone")}</FieldLabel>
                    <input
                      required
                      type="tel"
                      autoComplete="tel"
                      value={cdekRecipientPhone}
                      onChange={(e) => setCdekRecipientPhone(e.target.value)}
                      className={inputClass}
                    />
                  </label>
                </div>
              </div>
            </div>
          );
        }

        // TAXI, POSTAL, INTERNATIONAL — generic address form
        return (
          <div className="flex flex-col gap-4">
            <p className="text-sm text-[--color-muted]">
              {t("cart.form.addressDesc")}
              {selectedMethod?.cityRestriction ? (
                <>
                  {" "}
                  {t("cart.form.availableIn")}{" "}
                  <strong>{selectedMethod.cityRestriction}</strong>.
                </>
              ) : null}
              {deliveryType !== "INTERNATIONAL" && <> {t("cart.form.onAgreement")}</>}
            </p>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>{t("cart.form.cityLabel")}</FieldLabel>
              <input
                required
                value={addrCity}
                onChange={(e) => setAddrCity(e.target.value)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>{t("cart.form.street")}</FieldLabel>
              <input
                required
                value={addrStreet}
                onChange={(e) => setAddrStreet(e.target.value)}
                className={inputClass}
              />
            </label>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-1.5">
                <FieldLabel>{t("cart.form.apt")}</FieldLabel>
                <input
                  required
                  placeholder={t("cart.form.aptPlaceholder")}
                  value={addrApartment}
                  onChange={(e) => setAddrApartment(e.target.value)}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <FieldLabel optional>{t("cart.form.postal")}</FieldLabel>
                <input
                  inputMode="numeric"
                  value={addrPostal}
                  onChange={(e) => setAddrPostal(e.target.value)}
                  className={inputClass}
                />
              </label>
            </div>
            <div className="h-px bg-[--color-border]" />
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-1.5">
                <FieldLabel>{t("cart.form.recipient")}</FieldLabel>
                <input
                  required
                  autoComplete="name"
                  value={addrRecipientName}
                  onChange={(e) => setAddrRecipientName(e.target.value)}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <FieldLabel>{t("cart.form.recipientPhone")}</FieldLabel>
                <input
                  required
                  type="tel"
                  autoComplete="tel"
                  value={addrRecipientPhone}
                  onChange={(e) => setAddrRecipientPhone(e.target.value)}
                  className={inputClass}
                />
              </label>
            </div>
          </div>
        );

      // ── Step 5: Summary ──────────────────────────────────────────────────────
      case 5: {
        const regionObj = DELIVERY_REGIONS.find((r) => r.iso2 === countryIso2);
        const deliveryMethodLabel =
          selectedMethod?.labelRu ??
          (deliveryType ? t(`orders.delivery_type.${deliveryType}`) : deliveryType);

        return (
          <div className="flex flex-col gap-5">
            <p className="text-sm text-[--color-muted]">
              {t("cart.form.summaryDesc")}
            </p>

            <section className="border border-[--color-border] bg-white">
              <div className="flex items-center justify-between border-b border-[--color-border] px-5 py-3">
                <p className="text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                  {t("cart.form.contactsSection")}
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setStep(1);
                    setFormError(null);
                  }}
                  className="text-[0.55rem] uppercase tracking-[0.1em] text-[--color-muted] hover:text-black transition"
                >
                  {t("cart.form.edit")}
                </button>
              </div>
              <dl className="flex flex-col gap-2 px-5 py-4 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">{t("cart.form.nameLabel")}</dt>
                  <dd className="m-0 font-medium text-black">{customerName}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">{t("cart.form.phoneLabel")}</dt>
                  <dd className="m-0 font-medium text-black">{customerPhone}</dd>
                </div>
                {telegramUsername ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">Telegram</dt>
                    <dd className="m-0 font-medium text-black">
                      @{telegramUsername.replace(/^@/, "")}
                    </dd>
                  </div>
                ) : null}
              </dl>
            </section>

            <section className="border border-[--color-border] bg-white">
              <div className="flex items-center justify-between border-b border-[--color-border] px-5 py-3">
                <p className="text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                  {t("cart.form.deliverySection")}
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setStep(3);
                    setFormError(null);
                  }}
                  className="text-[0.55rem] uppercase tracking-[0.1em] text-[--color-muted] hover:text-black transition"
                >
                  {t("cart.form.edit")}
                </button>
              </div>
              <dl className="flex flex-col gap-2 px-5 py-4 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">{t("cart.form.regionLabel")}</dt>
                  <dd className="m-0 font-medium text-black">
                    {regionObj?.label ?? countryIso2}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">{t("cart.form.methodLabel")}</dt>
                  <dd className="m-0 font-medium text-black">
                    {deliveryMethodLabel}
                  </dd>
                </div>
                {requiresAddress &&
                !selectedMethod?.requiresCitySearch &&
                addrCity ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">{t("cart.form.addressLabel")}</dt>
                    <dd className="m-0 text-right font-medium text-black">
                      {addrCity}, {addrStreet}, {addrApartment}
                    </dd>
                  </div>
                ) : null}
                {selectedMethod?.requiresCitySearch &&
                selectedCity &&
                selectedPoint ? (
                  <>
                    <div className="flex justify-between gap-4">
                      <dt className="text-[--color-muted]">{t("cart.form.cdekCity")}</dt>
                      <dd className="m-0 font-medium text-black">
                        {selectedCity.region?.trim()
                          ? `${selectedCity.city}, ${selectedCity.region}`
                          : selectedCity.city}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-4">
                      <dt className="text-[--color-muted]">{t("cart.form.pvzLabel")}</dt>
                      <dd className="m-0 text-right font-medium text-black">
                        {selectedPoint.name}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-4">
                      <dt className="text-[--color-muted]">{t("cart.form.recipientLabel")}</dt>
                      <dd className="m-0 font-medium text-black">
                        {cdekRecipientName}, {cdekRecipientPhone}
                      </dd>
                    </div>
                  </>
                ) : null}
                {selectedMethod?.requiresCitySearch ? (
                  <div className="flex justify-between gap-4 border-t border-[--color-border] pt-2">
                    <dt className="text-[--color-muted]">{t("cart.form.cdekDeliveryLabel")}</dt>
                    <dd className="m-0 text-right text-[--color-muted]">
                      {cdekTariff
                        ? <><Price kzt={cdekTariff.deliveryPrice} /> · {t("cart.form.cdekAtReceipt")}</>
                        : t("cart.form.cdekAtReceipt")}
                    </dd>
                  </div>
                ) : deliveryType === "INTERNATIONAL" ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">{t("cart.form.costLabel")}</dt>
                    <dd className={cn("m-0", intlQuoteQuery.error ? "text-right text-red-600" : "font-medium text-black")}>
                      {intlQuoteQuery.isFetching
                        ? "…"
                        : intlQuoteQuery.error
                          ? (intlQuoteQuery.error as Error).message
                          : intlQuoteQuery.data
                            ? <Price kzt={intlQuoteQuery.data.priceKzt} />
                            : "…"}
                    </dd>
                  </div>
                ) : selectedMethod?.estimatedFeeKzt === 0 ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">{t("cart.form.costLabel")}</dt>
                    <dd className="m-0 font-medium text-emerald-600">
                      {t("cart.form.free")}
                    </dd>
                  </div>
                ) : selectedMethod?.estimatedFeeKzt != null ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">{t("cart.form.costLabel")}</dt>
                    <dd className="m-0 font-medium text-black">
                      <Price kzt={selectedMethod.estimatedFeeKzt} />
                    </dd>
                  </div>
                ) : (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">{t("cart.form.costLabel")}</dt>
                    <dd className="m-0 text-[--color-muted]">
                      {t("cart.form.onAgreement")}
                    </dd>
                  </div>
                )}
              </dl>
            </section>

            <section className="border border-[--color-border] bg-white">
              <div className="border-b border-[--color-border] px-5 py-3">
                <p className="text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                  {t("cart.form.goodsSection")}
                </p>
              </div>
              <ul className="m-0 flex flex-col gap-0 p-0 list-none divide-y divide-[--color-border]">
                {lines.map((l) => (
                  <li
                    key={l.lineKey}
                    className="flex items-center gap-4 px-5 py-3 text-sm"
                  >
                    <div className="min-w-0 flex-1">
                      <span className="font-medium text-black">
                        {l.title}
                        {isDesignLine(l) ? ` (${l.garmentLabel})` : ""}
                      </span>
                      {isDesignLine(l) ? (
                        <span className="ml-2 text-[0.6rem] uppercase tracking-wider text-[--color-muted]">
                          {l.colorName} · {l.sizeLabel}
                        </span>
                      ) : isLegacyLine(l) && (l.size || l.color) ? (
                        <span className="ml-2 text-[0.6rem] uppercase tracking-wider text-[--color-muted]">
                          {[l.size, l.color].filter(Boolean).join(" · ")}
                        </span>
                      ) : null}
                    </div>
                    <span className="shrink-0 text-[--color-muted]">
                      ×{l.qty}
                    </span>
                    <span className="shrink-0 font-semibold text-black">
                      <Price kzt={l.price * l.qty} />
                    </span>
                  </li>
                ))}
              </ul>
              <div className="flex flex-col gap-2 border-t border-[--color-border] px-5 py-4 text-sm">
                <div className="flex justify-between">
                  <span className="text-[--color-muted]">{t("cart.summary.items")}</span>
                  <span className="font-medium text-black">
                    <Price kzt={subtotal} />
                  </span>
                </div>
                {deliveryType === "CDEK" ? (
                  <div className="flex justify-between">
                    <span className="text-[--color-muted]">{t("cart.summary.cdekDelivery")}</span>
                    <span className="text-[--color-muted]">{t("cart.summary.atReceipt")}</span>
                  </div>
                ) : null}
                <div className="flex justify-between border-t border-[--color-border] pt-2">
                  <span className="font-semibold text-black">{t("cart.total")}</span>
                  <span className="text-base font-semibold text-black">
                    <Price kzt={grandTotal} />
                  </span>
                </div>
              </div>
            </section>

            <label className="flex flex-col gap-1.5">
              <FieldLabel optional>{t("cart.checkoutFlow.comment")}</FieldLabel>
              <textarea
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder={t("cart.checkoutFlow.commentPlaceholder")}
                className={cn(inputClass, "resize-y")}
              />
            </label>
          </div>
        );
      }

      default:
        return null;
    }
  }

  // ── Render: Success ───────────────────────────────────────────────────────────

  if (completedOrder) {
    return (
      <div className="py-14">
        <Container>
          <OrderSuccess
            order={completedOrder}
            onContinue={() => {
              setCompletedOrder(null);
              clearPendingPayment();
            }}
            onPay={() => void handleInitPayment()}
            paymentProvider={paymentProvider}
            onPaymentProviderChange={(p) => {
              setPaymentProvider(p);
              setPaymentError(null);
            }}
            paymentBusy={paymentBusy}
            paymentError={paymentError}
          />
        </Container>
      </div>
    );
  }

  // ── Render: Recovery banner (pending payment, F5 / tab-close / cancelled) ──────

  if (pendingRecord && !completedOrder) {
    return (
      <div className="py-14">
        <Container>
          <RecoveryBanner
            orderId={pendingRecord.orderId}
            amount={pendingRecord.amount}
            provider={recoveryProvider}
            onProviderChange={(p) => {
              setRecoveryProvider(p);
              setPaymentError(null);
            }}
            onPay={() =>
              void handleInitPayment({
                orderId: pendingRecord.orderId,
                provider: recoveryProvider,
                amount: pendingRecord.amount,
              })
            }
            onDismiss={() => {
              // Clear locally; the backend expiry job will mark the order EXPIRED in ~58 min.
              clearPendingPayment();
              setPendingRecord(null);
            }}
            busy={paymentBusy}
            error={paymentError}
          />
        </Container>
      </div>
    );
  }

  // ── Render: Cart review ───────────────────────────────────────────────────────

  if (phase === "cart") {
    return (
      <div className="py-12 md:py-16">
        <Container>
          <h1 className="display mb-8 text-[40px] uppercase text-black md:text-[56px]">
            {t("cart.title")}
          </h1>

          {lines.length === 0 ? (
            <div className="mx-auto flex max-w-md flex-col items-center gap-6 py-20 text-center">
              <div className="flex h-16 w-16 items-center justify-center bg-[--color-surface] text-3xl select-none">
                🧵
              </div>
              <p className="m-0 text-[15px] leading-relaxed text-[--color-muted]">
                {t("cart.emptyAction")}
              </p>
              <button
                type="button"
                onClick={() => navigate("/catalog")}
                className="inline-flex items-center justify-center bg-black px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
              >
                {t("cart.toCatalog")}
              </button>
            </div>
          ) : (
            <div className="mx-auto max-w-2xl">
              <ul className="m-0 flex flex-col gap-2 p-0 list-none">
                {lines.map((line) => (
                  <motion.li
                    key={line.lineKey}
                    layout
                    initial={{ opacity: 0, x: reduceMotion ? 0 : -6 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="flex flex-wrap items-center gap-4 border border-[--color-border] bg-white px-4 py-4 sm:flex-nowrap"
                  >
                    <div className="min-w-0 flex-1">
                      <Link
                        to={
                          isDesignLine(line)
                            ? `/catalog/${line.groupSlug}/${line.collectionSlug}/${line.designSlug}`
                            : `/catalog/${line.productId}`
                        }
                        className="text-sm font-semibold text-black hover:text-[--color-muted]"
                      >
                        {line.title}
                        {isDesignLine(line) ? ` (${line.garmentLabel})` : ""}
                      </Link>
                      {isDesignLine(line) ? (
                        <p className="mt-0.5 text-[0.65rem] uppercase tracking-wider text-[--color-muted]">
                          {line.colorName} · {line.sizeLabel}
                        </p>
                      ) : isLegacyLine(line) && (line.size || line.color) ? (
                        <p className="mt-0.5 text-[0.65rem] uppercase tracking-wider text-[--color-muted]">
                          {[line.size, line.color].filter(Boolean).join(" · ")}
                        </p>
                      ) : null}
                      <p className="mt-1 text-sm text-[--color-muted]">
                        <Price kzt={line.price} /> × {line.qty}{" "}
                        <span className="text-[--color-border]">·</span>{" "}
                        <strong className="text-black">
                          <Price kzt={line.price * line.qty} />
                        </strong>
                      </p>
                    </div>
                    <div className="flex items-center gap-px border border-[--color-border]">
                      <button
                        type="button"
                        onClick={() => decrement(line.lineKey)}
                        aria-label={t("cart.decreaseQty")}
                        className="flex h-8 w-8 items-center justify-center text-sm transition hover:bg-[--color-surface]"
                      >
                        −
                      </button>
                      <span className="flex h-8 w-8 items-center justify-center text-xs font-semibold tabular-nums">
                        {line.qty}
                      </span>
                      <button
                        type="button"
                        onClick={() => increment(line.lineKey)}
                        aria-label={t("cart.increaseQty")}
                        className="flex h-8 w-8 items-center justify-center text-sm transition hover:bg-[--color-surface]"
                      >
                        +
                      </button>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        haptic("light");
                        removeLine(line.lineKey);
                      }}
                      className="text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-[--color-danger]"
                    >
                      {t("cart.remove")}
                    </button>
                  </motion.li>
                ))}
              </ul>

              {/* Coupon input */}
              <div className="mt-3 border border-[--color-border] bg-white px-4 py-4">
                {appliedCoupon ? (
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <p className="text-[0.6rem] font-semibold uppercase tracking-[0.1em] text-emerald-600">{t("cart.coupon.applied", "Промокод применён")}</p>
                      <p className="mt-0.5 font-mono text-sm font-semibold text-black">{appliedCoupon.code} — -<Price kzt={appliedCoupon.discountAmount} /></p>
                    </div>
                    <button type="button" onClick={() => { setAppliedCoupon(null); setCouponInput(""); }}
                      className="shrink-0 text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-[--color-danger]">
                      {t("cart.coupon.remove", "Убрать")}
                    </button>
                  </div>
                ) : (
                  <div className="flex gap-2">
                    <input
                      className="min-w-0 flex-1 border-b border-[--color-border] bg-transparent py-2 font-mono text-sm uppercase placeholder:text-[--color-muted] outline-none focus:border-black transition"
                      placeholder={t("cart.coupon.placeholder", "ПРОМОКОД")}
                      value={couponInput}
                      onChange={e => setCouponInput(e.target.value.toUpperCase())}
                      onKeyDown={e => { if (e.key === "Enter") void handleApplyCoupon(); }}
                    />
                    <button type="button" onClick={() => void handleApplyCoupon()} disabled={couponBusy || !couponInput.trim()}
                      className="shrink-0 bg-black px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800 disabled:opacity-40">
                      {couponBusy ? "..." : t("cart.coupon.apply", "Применить")}
                    </button>
                  </div>
                )}
                {couponError && <p className="mt-2 text-xs text-[--color-danger]">{couponError}</p>}
              </div>

              <div className="mt-4 flex flex-wrap items-center justify-between gap-4 border border-[--color-border] bg-[--color-surface] px-5 py-5">
                <div>
                  <p className="m-0 text-[0.6rem] uppercase tracking-[0.16em] text-[--color-muted]">
                    {t("cart.items", { count: totalQty })}
                  </p>
                  {appliedCoupon && (
                    <p className="m-0 mt-0.5 text-xs text-emerald-600">
                      {t("cart.coupon.discount", "Скидка")}: -<Price kzt={appliedCoupon.discountAmount} />
                    </p>
                  )}
                  <p className="mt-1 text-2xl font-semibold text-black">
                    <Price kzt={appliedCoupon ? Math.max(0, subtotal - appliedCoupon.discountAmount) : subtotal} />
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                  <button
                    type="button"
                    onClick={clear}
                    className="text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-[--color-danger]"
                  >
                    {t("cart.clearCart")}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      // Checkout доступен только авторизованным. Гостя отправляем
                      // на вход/регистрацию и возвращаем в checkout после входа.
                      if (!user) {
                        sessionStorage.setItem(CHECKOUT_INTENT_KEY, "1");
                        navigate("/login", { state: { from: "/cart" } });
                        return;
                      }
                      setPhase("checkout");
                      setStep(1);
                    }}
                    className="bg-black px-8 py-4 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
                  >
                    {t("cart.checkout")}
                  </button>
                </div>
              </div>

              <Link
                to="/catalog"
                className="mt-8 inline-block text-[0.65rem] uppercase tracking-[0.12em] text-[--color-muted] transition hover:text-black"
              >
                {t("cart.backToCatalog")}
              </Link>
            </div>
          )}
        </Container>
      </div>
    );
  }

  // ── Render: Checkout ──────────────────────────────────────────────────────────

  return (
    <div className="py-10 md:py-16">
      <Container>
        <div className="mb-3">
          <h1 className="display text-[32px] uppercase text-black md:text-[44px]">
            {t("cart.checkoutFlow.title")}
          </h1>
          <p className="mt-2 text-[13px] text-[--color-muted]">
            {t("cart.goods", { count: totalQty })}
            {" "}· <Price kzt={subtotal} />
          </p>
        </div>

        <div className="mb-10 max-w-md">
          <StepIndicator step={step} skipStep4={!requiresAddress} />
        </div>

        <div className="flex gap-10 lg:gap-14">
          <div className="min-w-0 flex-1">
            <AnimatePresence mode="wait">
              <motion.div
                key={step}
                initial={{ opacity: 0, x: reduceMotion ? 0 : 18 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: reduceMotion ? 0 : -18 }}
                transition={{ duration: reduceMotion ? 0 : 0.2, ease: "easeOut" }}
              >
                <div className="mb-5">
                  <p className="text-[0.55rem] font-semibold uppercase tracking-[0.18em] text-[--color-muted]">
                    {t("cart.checkoutFlow.stepOf", { step })}
                  </p>
                  <h2 className="mt-0.5 text-xl font-semibold text-black">
                    {STEP_LABELS[step - 1]}
                  </h2>
                </div>

                {renderStep()}

                {formError ? (
                  <p
                    className="mt-4 text-sm font-medium text-[--color-danger]"
                    role="alert"
                  >
                    {formError}
                  </p>
                ) : null}

                <div className="mt-8 flex flex-wrap items-center gap-4">
                  <button
                    type="button"
                    onClick={handleBack}
                    className="text-[0.65rem] font-medium uppercase tracking-[0.12em] text-[--color-muted] transition hover:text-black"
                  >
                    {step === 1 ? t("cart.checkoutFlow.backToCart") : t("cart.checkoutFlow.back")}
                  </button>

                  {step < 5 ? (
                    <button
                      type="button"
                      onClick={handleNext}
                      className="bg-black px-8 py-4 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
                    >
                      {t("cart.checkoutFlow.continue")}
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={orderMutation.isPending}
                      onClick={handleSubmitOrder}
                      className="bg-black px-8 py-4 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800 disabled:opacity-50"
                    >
                      {orderMutation.isPending
                        ? t("cart.checkoutFlow.submitting")
                        : t("cart.checkoutFlow.submit")}
                    </button>
                  )}
                </div>
              </motion.div>
            </AnimatePresence>
          </div>

          <div className="hidden lg:block">
            <SummarySidebar
              lines={lines.map(toSidebarLine)}
              subtotal={subtotal}
              deliveryType={deliveryType}
              selectedMethod={selectedMethod}
              intlFeeKzt={intlQuoteQuery.data?.priceKzt ?? null}
              intlQuoteLoading={intlQuoteQuery.isFetching}
              intlQuoteErrorMessage={
                intlQuoteQuery.error ? (intlQuoteQuery.error as Error).message : null
              }
            />
          </div>
        </div>
      </Container>
    </div>
  );
}
