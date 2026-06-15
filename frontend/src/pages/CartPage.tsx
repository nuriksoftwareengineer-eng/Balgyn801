import { useMutation, useQuery } from "@tanstack/react-query";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { useCart } from "@/app/use-cart";
import { isLegacyLine, isDesignLine } from "@/app/cart-context";
import {
  calculateCdekTariffByOrder,
  createOrder,
  getDeliveryMethods,
  initPayment,
  listCdekDeliveryPoints,
  searchCdekCities,
} from "@/shared/api/backend-api";
import type {
  CdekCity,
  CdekDeliveryPoint,
  CdekOrderTariffResponse,
  CreateOrderRequest,
  DeliveryAddressRequest,
  DeliveryMethodResponse,
  DeliveryType,
  OrderResponse,
  PaymentProvider,
} from "@/shared/api/types";
import { ApiError } from "@/shared/api/http";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Container } from "@/shared/ui/container";

// Static display-only labels — used after order is placed and cart state is cleared.
const DELIVERY_LABELS: Record<DeliveryType, string> = {
  PICKUP: "Самовывоз",
  TAXI: "Такси / курьер",
  CDEK: "СДЭК",
  POSTAL: "Казпочта",
  INTERNATIONAL: "Международная доставка",
};

// Delivery regions shown at checkout. Customers pick a region, not a country.
// Each region maps to a representative ISO-2 code; the backend resolves the
// shipping zone from it and returns the allowed delivery methods — no method
// availability, pricing or restrictions are decided client-side.
const DELIVERY_REGIONS: { iso2: string; label: string; hint: string }[] = [
  { iso2: "KZ", label: "Казахстан", hint: "Доставка по всему Казахстану" },
  { iso2: "RU", label: "РФ / СНГ", hint: "Россия, Беларусь и другие страны СНГ" },
  { iso2: "US", label: "Другие страны", hint: "Международная доставка по всему миру" },
];

const STEP_LABELS = ["Контакты", "Регион", "Доставка", "Детали", "Итог"] as const;

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
  return (
    <div className="flex items-start" role="list" aria-label="Шаги оформления">
      {STEP_LABELS.map((label, i) => {
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
              {i < STEP_LABELS.length - 1 && (
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
                "mt-1.5 text-center text-[0.5rem] uppercase tracking-[0.08em] transition-colors duration-300",
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
  return (
    <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
      {children}
      {optional ? (
        <span className="ml-1 normal-case font-normal">(необязательно)</span>
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
}: {
  lines: SidebarLine[];
  subtotal: number;
  deliveryType: DeliveryType | null;
  selectedMethod: DeliveryMethodResponse | null;
}) {
  // CDEK: delivery at pickup — show items total only
  const grandTotal = subtotal;

  const deliveryLabel =
    selectedMethod?.labelRu ??
    (deliveryType ? DELIVERY_LABELS[deliveryType] : null);

  return (
    <aside className="w-72 shrink-0">
      <div className="sticky top-[96px] border border-[--color-border] bg-[--color-surface] p-5">
        <p className="mb-4 text-[0.6rem] font-semibold uppercase tracking-[0.16em] text-black">
          Ваш заказ
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
                  {formatMoney(l.price * l.qty)} ₸
                </p>
              </div>
            </li>
          ))}
        </ul>

        <div className="mt-4 flex flex-col gap-2 border-t border-[--color-border] pt-4">
          <div className="flex justify-between text-sm">
            <span className="text-[--color-muted]">Товары</span>
            <span className="font-medium text-black">{formatMoney(subtotal)} ₸</span>
          </div>

          {deliveryType === "CDEK" ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">Доставка СДЭК</span>
              <span className="text-[--color-muted]">при получении</span>
            </div>
          ) : selectedMethod?.estimatedFeeKzt === 0 ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              <span className="font-medium text-emerald-600">Бесплатно</span>
            </div>
          ) : selectedMethod?.estimatedFeeKzt != null ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              <span className="font-medium text-black">
                {formatMoney(selectedMethod.estimatedFeeKzt)} ₸
              </span>
            </div>
          ) : deliveryLabel ? (
            <div className="flex justify-between text-sm">
              <span className="text-[--color-muted]">{deliveryLabel}</span>
              <span className="text-[--color-muted]">по договорённости</span>
            </div>
          ) : null}

          <div className="flex justify-between border-t border-[--color-border] pt-2">
            <span className="text-sm font-semibold text-black">Итого</span>
            <span className="text-base font-semibold text-black">
              {formatMoney(grandTotal)} ₸
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
}

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
  const deliveryLabel = DELIVERY_LABELS[order.deliveryType] ?? order.deliveryType;
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
          Заказ принят
        </p>
        <p className="mt-3 text-4xl font-semibold text-black">№ {order.id}</p>

        <div className="mt-5 flex flex-col gap-2 text-sm">
          {fee != null ? (
            <>
              <div className="flex justify-between">
                <span className="text-[--color-muted]">Товары</span>
                <strong className="text-black">{formatMoney(goodsTotal)} ₸</strong>
              </div>
              <div className="flex justify-between">
                <span className="text-[--color-muted]">Доставка</span>
                <strong className="text-black">{formatMoney(fee)} ₸</strong>
              </div>
              <div className="flex justify-between border-t border-emerald-200 pt-2">
                <span className="font-semibold text-black">Итого</span>
                <strong className="text-lg text-black">
                  {formatMoney(order.totalPrice)} ₸
                </strong>
              </div>
            </>
          ) : (
            <>
              <div className="flex justify-between">
                <span className="text-[--color-muted]">Сумма</span>
                <strong className="text-black">
                  {formatMoney(order.totalPrice)} ₸
                </strong>
              </div>
              {order.deliveryType === "CDEK" ? (
                <div className="flex justify-between">
                  <span className="text-[--color-muted]">Доставка СДЭК</span>
                  <span className="text-[--color-muted]">оплата при получении</span>
                </div>
              ) : deliveryLabel ? (
                <div className="flex justify-between">
                  <span className="text-[--color-muted]">Доставка</span>
                  <span className="text-black">{deliveryLabel}</span>
                </div>
              ) : null}
            </>
          )}
        </div>

        {order.address ? (
          <p className="mt-4 border border-emerald-200 bg-white px-4 py-3 text-left text-xs text-[--color-muted]">
            <span className="font-semibold text-black">Адрес: </span>
            {order.address.city}, {order.address.street}
            {order.address.apartment !== "—"
              ? `, кв./оф. ${order.address.apartment}`
              : ""}
            . Получатель: {order.address.recipientName},{" "}
            {order.address.recipientPhone}
          </p>
        ) : null}

        <p className="mt-4 text-sm text-[--color-muted]">
          Мы свяжемся с вами по указанному телефону.
        </p>
      </div>

      <div className="mt-4 border border-[--color-border] bg-white px-6 py-5">
        <p className="mb-1 text-[0.6rem] font-semibold uppercase tracking-[0.16em] text-black">
          Оплата
        </p>
        <p className="mb-4 text-sm text-[--color-muted]">
          Выберите способ оплаты и перейдите к платёжной форме.
        </p>
        <div className="flex flex-wrap items-center gap-3">
          <select
            value={paymentProvider}
            onChange={(e) =>
              onPaymentProviderChange(e.target.value as PaymentProvider)
            }
            className="rounded-none border border-[--color-border] bg-white px-3 py-2.5 text-sm text-black outline-none transition focus:border-black"
          >
            <option value="KASPI">Kaspi</option>
            <option value="YOOKASSA">YooKassa</option>
            <option value="PAYPAL">PayPal</option>
          </select>
          <button
            type="button"
            disabled={paymentBusy}
            onClick={onPay}
            className="bg-black px-6 py-2.5 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800 disabled:opacity-50"
          >
            {paymentBusy ? "Переходим…" : "Оплатить"}
          </button>
        </div>
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
          В каталог
        </Link>
        <button
          type="button"
          onClick={onContinue}
          className="border border-[--color-border] px-6 py-2.5 text-[13px] font-bold uppercase tracking-[0.14em] text-black transition hover:border-black"
        >
          Закрыть
        </button>
      </div>
    </motion.div>
  );
}

// ── CartPage ───────────────────────────────────────────────────────────────────

export function CartPage() {
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
  const [justAddedTitle, setJustAddedTitle] = useState<string | null>(null);

  // ── Step 1: Contacts ─────────────────────────────────────────────────────────
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [telegramUsername, setTelegramUsername] = useState("");

  // ── Step 2: Country ──────────────────────────────────────────────────────────
  const [countryIso2, setCountryIso2] = useState("KZ");

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
    useState<PaymentProvider>("KASPI");
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  // ── Delivery methods from backend ────────────────────────────────────────────
  // Prefetch on step 2 so step 3 loads instantly after country selection.
  const deliveryMethodsQuery = useQuery({
    queryKey: ["delivery", "methods", countryIso2],
    queryFn: () => getDeliveryMethods(countryIso2),
    enabled: phase === "checkout" && step >= 2 && countryIso2.length > 0,
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
  const linesSig = useMemo(
    () => lines.map((l) => `${l.lineKey}:${l.qty}`).join("|"),
    [lines],
  );

  const selectedMethod =
    deliveryMethodsQuery.data?.find((m) => m.type === deliveryType) ?? null;

  // When selectedMethod isn't loaded yet, fall back to the known PICKUP=free rule.
  const requiresAddress = selectedMethod
    ? selectedMethod.requiresAddress
    : deliveryType !== "PICKUP";

  // CDEK: delivery paid at pickup — order total is items only
  const grandTotal = subtotal;

  // ── Effects ───────────────────────────────────────────────────────────────────

  useEffect(() => {
    const st = location.state as { justAdded?: string } | null;
    if (st?.justAdded) {
      setJustAddedTitle(st.justAdded);
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.pathname, location.state, navigate]);

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
              : "Не удалось рассчитать сроки доставки СДЭК",
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
          ? "Подождите, рассчитываем сроки доставки…"
          : step === 4 && selectedMethod?.requiresCitySearch && !selectedPoint
            ? "Выберите пункт выдачи СДЭК"
            : "Заполните все обязательные поля",
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
      setFormError(null);
      setPaymentError(null);
      setPaymentBusy(false);
      clear();
      setCompletedOrder(order);
      setPhase("cart");
      setStep(1);
      setCustomerName("");
      setCustomerPhone("");
      setTelegramUsername("");
      setCountryIso2("KZ");
      setDeliveryType("PICKUP");
      setComment("");
    },
    onError: (err: unknown) => {
      setFormError(
        err instanceof ApiError ? err.message : "Не удалось оформить заказ",
      );
    },
  });

  function handleSubmitOrder() {
    setFormError(null);

    let address: DeliveryAddressRequest | null = null;
    let pvzCode: string | null = null;

    if (selectedMethod?.requiresCitySearch) {
      if (!selectedCity || !selectedPoint) {
        setFormError("Выберите город и пункт выдачи СДЭК");
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

  async function handleInitPayment() {
    if (!completedOrder) return;
    setPaymentError(null);
    setPaymentBusy(true);
    try {
      const returnUrl = `${window.location.origin}/payment-return`;
      const payment = await initPayment({
        orderId: completedOrder.id,
        provider: paymentProvider,
        returnUrl,
      });
      if (!payment.paymentUrl)
        throw new Error("Платёжный URL не получен. Попробуйте позже.");
      window.location.href = payment.paymentUrl;
    } catch (err: unknown) {
      setPaymentError(
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : "Не удалось инициализировать оплату",
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
              Укажите контактные данные — мы свяжемся с вами после получения
              заказа.
            </p>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>Имя *</FieldLabel>
              <input
                required
                autoComplete="name"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>Телефон *</FieldLabel>
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
              Выберите регион доставки.
            </p>
            {DELIVERY_REGIONS.map((r) => (
              <button
                key={r.iso2}
                type="button"
                onClick={() => setCountryIso2(r.iso2)}
                className={cn(
                  "flex items-center justify-between border px-5 py-4 text-left transition-colors duration-150",
                  countryIso2 === r.iso2
                    ? "border-black bg-black text-white"
                    : "border-[--color-border] bg-white hover:border-zinc-400",
                )}
              >
                <div>
                  <p className="m-0 text-sm font-semibold">{r.label}</p>
                  <p
                    className={cn(
                      "m-0 mt-0.5 text-xs",
                      countryIso2 === r.iso2
                        ? "text-white/70"
                        : "text-[--color-muted]",
                    )}
                  >
                    {r.hint}
                  </p>
                </div>
                {countryIso2 === r.iso2 && (
                  <span className="ml-4 shrink-0">
                    <CheckIcon size={12} />
                  </span>
                )}
              </button>
            ))}
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
              Выберите способ получения заказа.
            </p>
            {loading ? (
              <p className="text-sm text-[--color-muted]">
                Загружаем методы доставки…
              </p>
            ) : fetchError ? (
              <p className="text-sm text-[--color-danger]">
                Не удалось загрузить методы доставки. Попробуйте ещё раз.
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
                    {method.cityRestriction ? (
                      <p
                        className={cn(
                          "m-0 mt-0.5 text-xs",
                          deliveryType === method.type
                            ? "text-white/70"
                            : "text-[--color-muted]",
                        )}
                      >
                        Доступно только в: {method.cityRestriction}
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
                        Бесплатно
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
                        от {formatMoney(method.estimatedFeeKzt)} ₸
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
                        по договорённости
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
                Для выбранной страны доступные методы доставки не найдены.
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
                {selectedMethod?.labelRu ?? "Самовывоз"}
              </p>
              <p className="mt-1 text-sm text-[--color-muted]">
                После оформления заказа мы свяжемся с вами для уточнения
                времени и места получения. Доставка бесплатная.
              </p>
            </div>
          );
        }

        if (selectedMethod?.requiresCitySearch) {
          return (
            <div className="flex flex-col gap-5">
              <p className="text-sm text-[--color-muted]">
                Введите название города и выберите пункт выдачи СДЭК.
                Сроки доставки рассчитываются автоматически.
              </p>

              <div className="flex flex-col gap-2">
                <label className="flex flex-col gap-1.5">
                  <FieldLabel>Поиск города *</FieldLabel>
                  <input
                    value={cdekCitySearch}
                    onChange={(e) => {
                      setCdekCitySearch(e.target.value);
                      if (selectedCity) setSelectedCity(null);
                    }}
                    placeholder="Например, Алматы"
                    autoComplete="off"
                    className={inputClass}
                  />
                </label>
                {citiesQuery.isFetching ? (
                  <p className="text-xs text-[--color-muted]">
                    Поиск городов…
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
                      Загружаем пункты выдачи…
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
                            Пункт выдачи СДЭК * ({filtered.length} из {all.length})
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
                                  : "Выберите пункт выдачи…"}
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
                                    placeholder="Код, улица или адрес"
                                    autoComplete="off"
                                    autoFocus
                                    className="w-full border border-[--color-border] px-3 py-2 text-sm outline-none focus:border-black focus:ring-1 focus:ring-black"
                                  />
                                </div>
                                <ul className="m-0 max-h-52 overflow-y-auto list-none p-0">
                                  {filtered.length === 0 ? (
                                    <li className="px-3 py-2.5 text-xs text-amber-600">Ничего не найдено</li>
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
                      Для этого города список ПВЗ пуст. Попробуйте другой
                      город.
                    </p>
                  ) : null}

                  {selectedPoint ? (
                    <div className="flex flex-col gap-2">
                      {cdekCalcPending ? (
                        <p className="text-xs text-[--color-muted]">
                          Рассчитываем сроки доставки…
                        </p>
                      ) : cdekCalcError ? (
                        <p className="text-xs text-amber-600">{cdekCalcError}</p>
                      ) : cdekTariff ? (
                        <div className="border border-[--color-border] bg-[--color-surface] px-4 py-3">
                          <div className="flex items-center justify-between">
                            <span className="text-sm text-black">
                              Стоимость доставки:{" "}
                              <span className="font-semibold">
                                {formatMoney(cdekTariff.deliveryPrice)} ₸
                              </span>
                            </span>
                            <span className="text-[0.65rem] uppercase tracking-[0.08em] text-[--color-muted]">
                              оплата при получении
                            </span>
                          </div>
                          {cdekTariff.minDays && cdekTariff.maxDays ? (
                            <p className="mt-1 text-[0.65rem] text-[--color-muted]">
                              Срок: {cdekTariff.minDays}–{cdekTariff.maxDays} дн.
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
                  Данные получателя
                </p>
                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="flex flex-col gap-1.5">
                    <FieldLabel>Имя получателя *</FieldLabel>
                    <input
                      required
                      autoComplete="name"
                      value={cdekRecipientName}
                      onChange={(e) => setCdekRecipientName(e.target.value)}
                      className={inputClass}
                    />
                  </label>
                  <label className="flex flex-col gap-1.5">
                    <FieldLabel>Телефон получателя *</FieldLabel>
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
              Укажите адрес доставки.
              {selectedMethod?.cityRestriction ? (
                <>
                  {" "}
                  Доступно только в:{" "}
                  <strong>{selectedMethod.cityRestriction}</strong>.
                </>
              ) : null}{" "}
              Стоимость согласовывается с оператором после оформления заказа.
            </p>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>Город *</FieldLabel>
              <input
                required
                value={addrCity}
                onChange={(e) => setAddrCity(e.target.value)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <FieldLabel>Улица, дом *</FieldLabel>
              <input
                required
                value={addrStreet}
                onChange={(e) => setAddrStreet(e.target.value)}
                className={inputClass}
              />
            </label>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-1.5">
                <FieldLabel>Квартира / офис *</FieldLabel>
                <input
                  required
                  placeholder="Нет — «—»"
                  value={addrApartment}
                  onChange={(e) => setAddrApartment(e.target.value)}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <FieldLabel optional>Индекс</FieldLabel>
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
                <FieldLabel>Получатель *</FieldLabel>
                <input
                  required
                  autoComplete="name"
                  value={addrRecipientName}
                  onChange={(e) => setAddrRecipientName(e.target.value)}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <FieldLabel>Телефон получателя *</FieldLabel>
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
          DELIVERY_LABELS[deliveryType] ??
          deliveryType;

        return (
          <div className="flex flex-col gap-5">
            <p className="text-sm text-[--color-muted]">
              Проверьте данные перед оформлением заказа.
            </p>

            <section className="border border-[--color-border] bg-white">
              <div className="flex items-center justify-between border-b border-[--color-border] px-5 py-3">
                <p className="text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                  Контакты
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setStep(1);
                    setFormError(null);
                  }}
                  className="text-[0.55rem] uppercase tracking-[0.1em] text-[--color-muted] hover:text-black transition"
                >
                  Изменить
                </button>
              </div>
              <dl className="flex flex-col gap-2 px-5 py-4 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">Имя</dt>
                  <dd className="m-0 font-medium text-black">{customerName}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">Телефон</dt>
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
                  Доставка
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setStep(3);
                    setFormError(null);
                  }}
                  className="text-[0.55rem] uppercase tracking-[0.1em] text-[--color-muted] hover:text-black transition"
                >
                  Изменить
                </button>
              </div>
              <dl className="flex flex-col gap-2 px-5 py-4 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">Регион</dt>
                  <dd className="m-0 font-medium text-black">
                    {regionObj?.label ?? countryIso2}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-[--color-muted]">Способ</dt>
                  <dd className="m-0 font-medium text-black">
                    {deliveryMethodLabel}
                  </dd>
                </div>
                {requiresAddress &&
                !selectedMethod?.requiresCitySearch &&
                addrCity ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">Адрес</dt>
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
                      <dt className="text-[--color-muted]">Город СДЭК</dt>
                      <dd className="m-0 font-medium text-black">
                        {selectedCity.region?.trim()
                          ? `${selectedCity.city}, ${selectedCity.region}`
                          : selectedCity.city}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-4">
                      <dt className="text-[--color-muted]">Пункт выдачи</dt>
                      <dd className="m-0 text-right font-medium text-black">
                        {selectedPoint.name}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-4">
                      <dt className="text-[--color-muted]">Получатель</dt>
                      <dd className="m-0 font-medium text-black">
                        {cdekRecipientName}, {cdekRecipientPhone}
                      </dd>
                    </div>
                  </>
                ) : null}
                {selectedMethod?.requiresCitySearch ? (
                  <div className="flex justify-between gap-4 border-t border-[--color-border] pt-2">
                    <dt className="text-[--color-muted]">Доставка СДЭК</dt>
                    <dd className="m-0 text-right text-[--color-muted]">
                      {cdekTariff
                        ? `${formatMoney(cdekTariff.deliveryPrice)} ₸ · оплата при получении`
                        : "оплата при получении"}
                    </dd>
                  </div>
                ) : selectedMethod?.estimatedFeeKzt === 0 ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">Стоимость</dt>
                    <dd className="m-0 font-medium text-emerald-600">
                      Бесплатно
                    </dd>
                  </div>
                ) : selectedMethod?.estimatedFeeKzt != null ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">Стоимость</dt>
                    <dd className="m-0 font-medium text-black">
                      {formatMoney(selectedMethod.estimatedFeeKzt)} ₸
                    </dd>
                  </div>
                ) : (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[--color-muted]">Стоимость</dt>
                    <dd className="m-0 text-[--color-muted]">
                      по договорённости
                    </dd>
                  </div>
                )}
              </dl>
            </section>

            <section className="border border-[--color-border] bg-white">
              <div className="border-b border-[--color-border] px-5 py-3">
                <p className="text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                  Товары
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
                      {formatMoney(l.price * l.qty)} ₸
                    </span>
                  </li>
                ))}
              </ul>
              <div className="flex flex-col gap-2 border-t border-[--color-border] px-5 py-4 text-sm">
                <div className="flex justify-between">
                  <span className="text-[--color-muted]">Товары</span>
                  <span className="font-medium text-black">
                    {formatMoney(subtotal)} ₸
                  </span>
                </div>
                {deliveryType === "CDEK" ? (
                  <div className="flex justify-between">
                    <span className="text-[--color-muted]">Доставка СДЭК</span>
                    <span className="text-[--color-muted]">при получении</span>
                  </div>
                ) : null}
                <div className="flex justify-between border-t border-[--color-border] pt-2">
                  <span className="font-semibold text-black">Итого</span>
                  <span className="text-base font-semibold text-black">
                    {formatMoney(grandTotal)} ₸
                  </span>
                </div>
              </div>
            </section>

            <label className="flex flex-col gap-1.5">
              <FieldLabel optional>Комментарий к заказу</FieldLabel>
              <textarea
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Размер, цвет, пожелания по доставке…"
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
            onContinue={() => setCompletedOrder(null)}
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

  // ── Render: Cart review ───────────────────────────────────────────────────────

  if (phase === "cart") {
    return (
      <div className="py-14">
        <Container>
          <motion.h1
            className="mb-6 text-4xl font-extrabold uppercase tracking-[-0.02em] text-black md:text-5xl"
            initial={{ opacity: 0, y: reduceMotion ? 0 : 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.3 }}
          >
            Корзина
          </motion.h1>

          {justAddedTitle && lines.length > 0 ? (
            <div className="mb-6 flex flex-wrap items-center justify-between gap-3 border border-[--color-border] bg-[--color-surface] px-4 py-3 text-sm">
              <span className="text-black">
                «{justAddedTitle}» добавлен в корзину.
              </span>
              <button
                type="button"
                onClick={() => setJustAddedTitle(null)}
                className="text-[0.65rem] font-semibold uppercase tracking-[0.12em] text-[--color-muted] hover:text-black"
              >
                Скрыть
              </button>
            </div>
          ) : null}

          {lines.length === 0 ? (
            <div className="max-w-sm border border-[--color-border] bg-white px-6 py-10 text-center">
              <p className="m-0 text-sm text-[--color-muted]">
                Пока пусто — загляните в каталог.
              </p>
              <button
                type="button"
                onClick={() => navigate("/catalog")}
                className="mt-6 bg-black px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
              >
                В каталог
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
                        {formatMoney(line.price)} ₸ × {line.qty}{" "}
                        <span className="text-[--color-border]">·</span>{" "}
                        <strong className="text-black">
                          {formatMoney(line.price * line.qty)} ₸
                        </strong>
                      </p>
                    </div>
                    <div className="flex items-center gap-px border border-[--color-border]">
                      <button
                        type="button"
                        onClick={() => decrement(line.lineKey)}
                        aria-label="Уменьшить"
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
                        aria-label="Увеличить"
                        className="flex h-8 w-8 items-center justify-center text-sm transition hover:bg-[--color-surface]"
                      >
                        +
                      </button>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeLine(line.lineKey)}
                      className="text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-[--color-danger]"
                    >
                      Удалить
                    </button>
                  </motion.li>
                ))}
              </ul>

              <div className="mt-4 flex flex-wrap items-center justify-between gap-4 border border-[--color-border] bg-[--color-surface] px-5 py-5">
                <div>
                  <p className="m-0 text-[0.6rem] uppercase tracking-[0.16em] text-[--color-muted]">
                    {totalQty}&thinsp;
                    {totalQty === 1
                      ? "позиция"
                      : totalQty < 5
                        ? "позиции"
                        : "позиций"}
                  </p>
                  <p className="mt-1 text-2xl font-semibold text-black">
                    {formatMoney(subtotal)} ₸
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                  <button
                    type="button"
                    onClick={clear}
                    className="text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-[--color-danger]"
                  >
                    Очистить
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
                    className="bg-black px-8 py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
                  >
                    Оформить заказ
                  </button>
                </div>
              </div>

              <Link
                to="/catalog"
                className="mt-8 inline-block text-[0.65rem] uppercase tracking-[0.12em] text-[--color-muted] transition hover:text-black"
              >
                ← В каталог
              </Link>
            </div>
          )}
        </Container>
      </div>
    );
  }

  // ── Render: Checkout ──────────────────────────────────────────────────────────

  return (
    <div className="py-10">
      <Container>
        <div className="mb-2">
          <h1 className="text-3xl font-extrabold uppercase tracking-[-0.02em] text-black md:text-4xl">
            Оформление заказа
          </h1>
          <p className="mt-1 text-sm text-[--color-muted]">
            {totalQty}&thinsp;
            {totalQty === 1 ? "товар" : totalQty < 5 ? "товара" : "товаров"}{" "}
            · {formatMoney(subtotal)} ₸
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
                    Шаг {step} из 5
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
                    ← {step === 1 ? "Корзина" : "Назад"}
                  </button>

                  {step < 5 ? (
                    <button
                      type="button"
                      onClick={handleNext}
                      className="bg-black px-8 py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
                    >
                      Продолжить
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={orderMutation.isPending}
                      onClick={handleSubmitOrder}
                      className="bg-black px-8 py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800 disabled:opacity-50"
                    >
                      {orderMutation.isPending
                        ? "Отправляем…"
                        : "Подтвердить заказ"}
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
            />
          </div>
        </div>
      </Container>
    </div>
  );
}
