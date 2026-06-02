import { useMutation, useQuery } from "@tanstack/react-query";
import { motion, useReducedMotion } from "framer-motion";
import { type FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import {
  calculateCdekTariffByOrder,
  createOrder,
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
  DeliveryType,
  OrderResponse,
  PaymentProvider,
} from "@/shared/api/types";
import { ApiError } from "@/shared/api/http";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";

const DELIVERY_OPTIONS: { value: DeliveryType; label: string; hint: string }[] = [
  { value: "PICKUP", label: "Самовывоз", hint: "Заберёте сами по договорённости" },
  { value: "TAXI", label: "Такси / курьер", hint: "Доставка по городу" },
  { value: "CDEK", label: "СДЭК", hint: "Отправка транспортной компанией" },
];

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), delayMs);
    return () => window.clearTimeout(id);
  }, [value, delayMs]);
  return debounced;
}

function sumOrderItemsGoods(order: OrderResponse): number | null {
  const items = order.items;
  if (!items?.length) return null;
  return items.reduce((s, i) => s + i.unitPrice * i.quantity, 0);
}

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
  onPaymentProviderChange: (next: PaymentProvider) => void;
  paymentBusy: boolean;
  paymentError: string | null;
}) {
  const dt = DELIVERY_OPTIONS.find((o) => o.value === order.deliveryType)?.label;
  const fee =
    order.deliveryFee != null && order.deliveryFee > 0 ? order.deliveryFee : null;
  const goodsFromItems = sumOrderItemsGoods(order);
  const goods =
    fee != null
      ? goodsFromItems ?? Math.max(0, order.totalPrice - fee)
      : goodsFromItems ?? order.totalPrice;

  return (
    <motion.div
      className="mx-auto max-w-lg rounded-[14px] border border-green-500/25 bg-green-500/5 px-8 py-10 text-center"
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: "spring", stiffness: 320, damping: 26 }}
    >
      <p className="m-0 text-sm font-semibold uppercase tracking-wider text-green-400">
        Заказ принят
      </p>
      <p className="font-display mt-3 text-4xl tracking-wide text-zinc-100">
        № {order.id}
      </p>
      {fee != null ? (
        <div className="mt-4 space-y-1.5 text-left text-sm text-zinc-400">
          <p className="m-0 flex justify-between gap-4">
            <span>Товары</span>
            <strong className="text-zinc-200">{formatMoney(goods)} ₸</strong>
          </p>
          <p className="m-0 flex justify-between gap-4">
            <span>Доставка</span>
            <strong className="text-zinc-200">{formatMoney(fee)} ₸</strong>
          </p>
          <p className="m-0 flex justify-between gap-4 border-t border-white/10 pt-2 text-zinc-300">
            <span className="font-semibold">Итого</span>
            <strong className="text-lg text-zinc-100">
              {formatMoney(order.totalPrice)} ₸
            </strong>
          </p>
        </div>
      ) : (
        <p className="mt-4 text-zinc-400">
          Сумма:{" "}
          <strong className="text-zinc-100">{formatMoney(order.totalPrice)} ₸</strong>
          {dt ? (
            <>
              {" "}
              · <span className="text-zinc-300">{dt}</span>
            </>
          ) : null}
        </p>
      )}
      {fee != null && dt ? (
        <p className="mt-2 text-sm text-zinc-500">
          Способ: <span className="text-zinc-400">{dt}</span>
        </p>
      ) : null}
      {order.address ? (
        <p className="mt-4 rounded-[10px] border border-white/10 bg-zinc-900/40 px-4 py-3 text-left text-sm text-zinc-400">
          <span className="font-semibold text-zinc-300">Адрес доставки: </span>
          {order.address.city}, {order.address.street}, кв./оф. {order.address.apartment}
          , {order.address.postalCode}. Получатель: {order.address.recipientName},{" "}
          {order.address.recipientPhone}
        </p>
      ) : null}
      <p className="mt-4 text-sm text-zinc-500">
        Мы свяжемся с вами по указанному телефону.
      </p>
      <div className="mt-5 rounded-[10px] border border-violet-500/20 bg-violet-500/[0.07] px-4 py-3 text-left">
        <p className="m-0 text-xs font-semibold uppercase tracking-wide text-zinc-400">
          Шаг оплаты
        </p>
        <p className="mt-1 text-sm text-zinc-400">
          Выберите провайдера и перейдите к оплате заказа.
        </p>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <select
            value={paymentProvider}
            onChange={(e) =>
              onPaymentProviderChange(e.target.value as PaymentProvider)
            }
            className="rounded-[10px] border border-white/15 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
          >
            <option value="KASPI">Kaspi</option>
            <option value="YOOKASSA">YooKassa</option>
            <option value="PAYPAL">PayPal</option>
          </select>
          <Button
            type="button"
            variant="primary"
            className="rounded-full"
            disabled={paymentBusy}
            onClick={onPay}
          >
            {paymentBusy ? "Переходим к оплате…" : "Оплатить"}
          </Button>
        </div>
        {paymentError ? (
          <p className="mt-2 text-sm text-red-400">{paymentError}</p>
        ) : null}
      </div>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link
          to="/catalog"
          className={cn(
            "inline-flex items-center justify-center rounded-full bg-gradient-to-br from-violet-500 to-purple-600 px-7 py-3.5 font-semibold text-white shadow-[0_0_40px_rgba(168,85,247,0.35)] transition hover:-translate-y-0.5 hover:shadow-[0_8px_48px_rgba(168,85,247,0.35)]",
          )}
        >
          В каталог
        </Link>
        <Button type="button" variant="outline" onClick={onContinue}>
          Закрыть
        </Button>
      </div>
    </motion.div>
  );
}

function buildCdekAddress(
  city: CdekCity,
  point: CdekDeliveryPoint,
  recipientName: string,
  recipientPhone: string,
): DeliveryAddressRequest {
  const cityLabel =
    city.region != null && city.region.trim().length > 0
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

export function CartPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    lines,
    totalQty,
    subtotal,
    increment,
    decrement,
    removeLine,
    clear,
  } = useCart();
  const reduceMotion = useReducedMotion();

  const [completedOrder, setCompletedOrder] = useState<OrderResponse | null>(
    null,
  );
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [telegramUsername, setTelegramUsername] = useState("");
  const [deliveryType, setDeliveryType] = useState<DeliveryType>("PICKUP");
  const [comment, setComment] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [justAddedTitle, setJustAddedTitle] = useState<string | null>(null);
  const [paymentProvider, setPaymentProvider] =
    useState<PaymentProvider>("KASPI");
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  const [addrCity, setAddrCity] = useState("");
  const [addrStreet, setAddrStreet] = useState("");
  const [addrApartment, setAddrApartment] = useState("");
  const [addrPostal, setAddrPostal] = useState("");
  const [addrRecipientName, setAddrRecipientName] = useState("");
  const [addrRecipientPhone, setAddrRecipientPhone] = useState("");

  const [cdekCitySearch, setCdekCitySearch] = useState("");
  const [selectedCity, setSelectedCity] = useState<CdekCity | null>(null);
  const [selectedPoint, setSelectedPoint] = useState<CdekDeliveryPoint | null>(
    null,
  );
  const [cdekTariff, setCdekTariff] = useState<CdekOrderTariffResponse | null>(null);
  const [cdekCalcPending, setCdekCalcPending] = useState(false);

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

  const linesSig = useMemo(
    () => lines.map((l) => `${l.lineKey}:${l.qty}`).join("|"),
    [lines],
  );

  useEffect(() => {
    const st = location.state as { justAdded?: string } | null;
    if (st?.justAdded) {
      setJustAddedTitle(st.justAdded);
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.pathname, location.state, navigate]);

  useEffect(() => {
    if (deliveryType !== "CDEK") {
      setCdekCitySearch("");
      setSelectedCity(null);
      setSelectedPoint(null);
      setCdekTariff(null);
    }
  }, [deliveryType]);

  useEffect(() => {
    if (deliveryType === "CDEK") {
      setCdekTariff(null);
    }
  }, [linesSig, deliveryType]);

  useEffect(() => {
    if (deliveryType === "CDEK") {
      setSelectedPoint(null);
      setCdekTariff(null);
    }
  }, [selectedCity?.code, deliveryType]);

  useEffect(() => {
    if (deliveryType === "CDEK") {
      setCdekTariff(null);
    }
  }, [selectedPoint?.code, deliveryType]);

  const needsAddress = deliveryType === "TAXI" || deliveryType === "CDEK";

  const orderMutation = useMutation({
    mutationFn: (body: CreateOrderRequest) => createOrder(body),
    onSuccess: (order) => {
      setFormError(null);
      setPaymentError(null);
      setPaymentBusy(false);
      clear();
      setCompletedOrder(order);
      setCustomerName("");
      setCustomerPhone("");
      setTelegramUsername("");
      setComment("");
      setDeliveryType("PICKUP");
      setAddrCity("");
      setAddrStreet("");
      setAddrApartment("");
      setAddrPostal("");
      setAddrRecipientName("");
      setAddrRecipientPhone("");
      setCdekCitySearch("");
      setSelectedCity(null);
      setSelectedPoint(null);
      setCdekTariff(null);
    },
    onError: (err: unknown) => {
      setFormError(
        err instanceof ApiError ? err.message : "Не удалось оформить заказ",
      );
    },
  });

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
      if (!payment.paymentUrl) {
        throw new Error("Платёжный URL не получен. Попробуйте ещё раз.");
      }
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

  async function handleCalculateCdek() {
    if (!selectedCity || !selectedPoint) return;
    setFormError(null);
    setCdekCalcPending(true);
    try {
      const t = await calculateCdekTariffByOrder({
        toCityCode: selectedCity.code,
        items: lines.map((l) => ({
          productId: l.productId,
          quantity: l.qty,
        })),
      });
      setCdekTariff(t);
    } catch (err: unknown) {
      setCdekTariff(null);
      setFormError(
        err instanceof ApiError
          ? err.message
          : "Не удалось рассчитать доставку СДЭК",
      );
    } finally {
      setCdekCalcPending(false);
    }
  }

  function handleCheckout(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    const tg = telegramUsername.trim().replace(/^@/, "");

    let address: DeliveryAddressRequest | null = null;
    let deliveryFee: number | undefined;

    if (deliveryType === "TAXI") {
      address = {
        city: addrCity.trim(),
        street: addrStreet.trim(),
        apartment: addrApartment.trim(),
        postalCode: addrPostal.trim(),
        recipientName: addrRecipientName.trim(),
        recipientPhone: addrRecipientPhone.trim(),
      };
    } else if (deliveryType === "CDEK") {
      if (!selectedCity || !selectedPoint) {
        setFormError("Выберите город и пункт выдачи СДЭК");
        return;
      }
      if (!cdekTariff || cdekTariff.deliveryPrice <= 0) {
        setFormError("Нажмите «Рассчитать доставку» и дождитесь суммы");
        return;
      }
      address = buildCdekAddress(
        selectedCity,
        selectedPoint,
        addrRecipientName,
        addrRecipientPhone,
      );
      deliveryFee = cdekTariff.deliveryPrice;
    }

    const payload: CreateOrderRequest = {
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      telegramUsername: tg.length ? tg : null,
      deliveryType,
      comment: comment.trim() || null,
      items: lines.map((l) => ({
        productId: l.productId,
        quantity: l.qty,
        size: l.size,
        color: l.color,
      })),
      address: needsAddress ? address : null,
      deliveryFee: deliveryFee ?? null,
    };
    orderMutation.mutate(payload);
  }

  const grandWithCdek =
    deliveryType === "CDEK" && cdekTariff
      ? cdekTariff.orderTotal
      : subtotal;

  if (completedOrder) {
    return (
      <div className="py-14">
        <Container>
          <OrderSuccess
            order={completedOrder}
            onContinue={() => setCompletedOrder(null)}
            onPay={() => void handleInitPayment()}
            paymentProvider={paymentProvider}
            onPaymentProviderChange={(next) => {
              setPaymentProvider(next);
              setPaymentError(null);
            }}
            paymentBusy={paymentBusy}
            paymentError={paymentError}
          />
        </Container>
      </div>
    );
  }

  return (
    <div className="py-14">
      <Container>
        <motion.h1
          className="font-display mb-4 text-4xl tracking-wide text-zinc-100"
          initial={{ opacity: 0, y: reduceMotion ? 0 : 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.4 }}
        >
          Корзина
        </motion.h1>

        {justAddedTitle && lines.length > 0 ? (
          <div
            className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-[12px] border border-violet-500/30 bg-violet-500/10 px-4 py-3 text-sm text-zinc-200"
            role="status"
          >
            <span>
              «{justAddedTitle}» добавлен
              {lines.length > 1 ? " в корзину" : ""}.
            </span>
            <button
              type="button"
              className="font-semibold text-violet-300 hover:text-violet-200"
              onClick={() => setJustAddedTitle(null)}
            >
              Скрыть
            </button>
          </div>
        ) : null}

        {lines.length === 0 ? (
          <div className="max-w-lg rounded-[14px] border border-white/10 bg-zinc-900/60 px-6 py-10 text-center">
            <p className="m-0 text-zinc-400">
              Пока пусто. Загляните в каталог и добавьте что-нибудь с вышивкой.
            </p>
            <Link
              to="/catalog"
              className={cn(
                "mt-6 inline-flex items-center justify-center rounded-full bg-gradient-to-br from-violet-500 to-purple-600 px-7 py-3.5 font-semibold text-white shadow-[0_0_40px_rgba(168,85,247,0.35)] transition hover:-translate-y-0.5 hover:shadow-[0_8px_48px_rgba(168,85,247,0.35)]",
              )}
            >
              В каталог
            </Link>
          </div>
        ) : (
          <div className="mx-auto flex max-w-3xl flex-col gap-8">
            <ul className="m-0 list-none space-y-3 p-0">
              {lines.map((line) => (
                <motion.li
                  key={line.lineKey}
                  layout
                  initial={{ opacity: 0, x: reduceMotion ? 0 : -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="flex flex-wrap items-center gap-4 rounded-[14px] border border-white/10 bg-zinc-900 px-4 py-4 sm:flex-nowrap"
                >
                  <div className="min-w-0 flex-1">
                    <Link
                      to={`/catalog/${line.productId}`}
                      className="font-semibold text-zinc-100 hover:text-violet-300"
                    >
                      {line.title}
                    </Link>
                    {line.size || line.color ? (
                      <p className="mt-0.5 text-xs text-zinc-500">
                        {[line.size, line.color].filter(Boolean).join(" · ")}
                      </p>
                    ) : null}
                    <p className="mt-1 text-sm text-zinc-500">
                      {formatMoney(line.price)} ₸ × {line.qty}{" "}
                      <span className="text-zinc-600">·</span>{" "}
                      <span className="font-medium text-zinc-400">
                        {formatMoney(line.price * line.qty)} ₸
                      </span>
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      className="h-9 min-w-9 px-0"
                      aria-label="Уменьшить количество"
                      onClick={() => decrement(line.lineKey)}
                    >
                      −
                    </Button>
                    <span className="min-w-[2rem] text-center font-semibold tabular-nums text-zinc-200">
                      {line.qty}
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      className="h-9 min-w-9 px-0"
                      aria-label="Увеличить количество"
                      onClick={() => increment(line.lineKey)}
                    >
                      +
                    </Button>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    className="text-red-400 hover:border-red-500/40 hover:bg-red-500/10 hover:text-red-300"
                    onClick={() => removeLine(line.lineKey)}
                  >
                    Удалить
                  </Button>
                </motion.li>
              ))}
            </ul>

            <div className="flex flex-wrap items-center justify-between gap-4 rounded-[14px] border border-violet-500/20 bg-violet-500/5 px-5 py-5">
              <div>
                <p className="m-0 text-sm text-zinc-500">
                  Позиций: <strong className="text-zinc-300">{totalQty}</strong>
                </p>
                <p className="mt-1 font-display text-2xl tracking-wide text-zinc-100">
                  {formatMoney(subtotal)} ₸
                </p>
                {deliveryType === "CDEK" && cdekTariff ? (
                  <>
                    <p className="mt-1 text-sm text-zinc-500">
                      + доставка{" "}
                      <strong className="text-zinc-300">
                        {formatMoney(cdekTariff.deliveryPrice)} ₸
                      </strong>
                    </p>
                    <p className="mt-1 font-display text-xl tracking-wide text-violet-200">
                      Итого {formatMoney(grandWithCdek)} ₸
                    </p>
                  </>
                ) : null}
              </div>
              <Button type="button" variant="outline" onClick={clear}>
                Очистить
              </Button>
            </div>

            <section className="rounded-[14px] border border-white/10 bg-zinc-900/50 p-6">
              <h2 className="mb-4 font-display text-2xl tracking-wide text-zinc-100">
                Оформление
              </h2>
              <form onSubmit={handleCheckout} className="flex flex-col gap-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="flex flex-col gap-1.5 text-sm">
                    <span className="font-medium text-zinc-400">Имя</span>
                    <input
                      required
                      autoComplete="name"
                      value={customerName}
                      onChange={(e) => setCustomerName(e.target.value)}
                      className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                    />
                  </label>
                  <label className="flex flex-col gap-1.5 text-sm">
                    <span className="font-medium text-zinc-400">Телефон</span>
                    <input
                      required
                      type="tel"
                      autoComplete="tel"
                      placeholder="+7 …"
                      value={customerPhone}
                      onChange={(e) => setCustomerPhone(e.target.value)}
                      className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                    />
                  </label>
                </div>

                <label className="flex flex-col gap-1.5 text-sm">
                  <span className="font-medium text-zinc-400">
                    Telegram <span className="font-normal text-zinc-600">(необязательно)</span>
                  </span>
                  <input
                    autoComplete="off"
                    placeholder="@username"
                    value={telegramUsername}
                    onChange={(e) => setTelegramUsername(e.target.value)}
                    className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                  />
                </label>

                <fieldset className="m-0 border-0 p-0">
                  <legend className="mb-2 text-sm font-medium text-zinc-400">
                    Способ получения
                  </legend>
                  <div className="flex flex-col gap-2">
                    {DELIVERY_OPTIONS.map((opt) => (
                      <label
                        key={opt.value}
                        className={cn(
                          "flex cursor-pointer items-start gap-3 rounded-[10px] border px-3 py-3 transition",
                          deliveryType === opt.value
                            ? "border-violet-500/50 bg-violet-500/10"
                            : "border-white/10 bg-zinc-950/50 hover:border-white/20",
                        )}
                      >
                        <input
                          type="radio"
                          name="delivery"
                          value={opt.value}
                          checked={deliveryType === opt.value}
                          onChange={() => {
                            setDeliveryType(opt.value);
                            if (opt.value !== "PICKUP") {
                              setAddrRecipientName((n) =>
                                n.trim() ? n : customerName.trim(),
                              );
                              setAddrRecipientPhone((p) =>
                                p.trim() ? p : customerPhone.trim(),
                              );
                            }
                          }}
                          className="mt-1"
                        />
                        <span>
                          <span className="font-semibold text-zinc-100">
                            {opt.label}
                          </span>
                          <span className="mt-0.5 block text-xs text-zinc-500">
                            {opt.hint}
                          </span>
                        </span>
                      </label>
                    ))}
                  </div>
                </fieldset>

                {needsAddress ? (
                  <div className="rounded-[12px] border border-violet-500/20 bg-violet-500/[0.06] p-4">
                    <h3 className="mb-3 text-sm font-semibold text-zinc-200">
                      {deliveryType === "CDEK"
                        ? "СДЭК: город и пункт выдачи"
                        : "Адрес доставки"}
                    </h3>

                    {deliveryType === "CDEK" ? (
                      <div className="flex flex-col gap-4">
                        <p className="m-0 text-xs text-zinc-500">
                          Введите минимум 2 буквы названия города, выберите пункт и
                          рассчитайте доставку.
                        </p>
                        <label className="flex flex-col gap-1.5 text-sm">
                          <span className="font-medium text-zinc-400">Поиск города</span>
                          <input
                            value={cdekCitySearch}
                            onChange={(e) => {
                              setCdekCitySearch(e.target.value);
                              if (selectedCity) setSelectedCity(null);
                            }}
                            placeholder="Например, Алматы"
                            autoComplete="off"
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>

                        {citiesQuery.isFetching ? (
                          <p className="text-xs text-zinc-500">Ищем города…</p>
                        ) : null}
                        {citiesQuery.data && citiesQuery.data.length > 0 ? (
                          <ul className="m-0 max-h-44 list-none space-y-1 overflow-y-auto rounded-[10px] border border-white/10 bg-zinc-950/80 p-2">
                            {citiesQuery.data.map((c) => {
                              const active = selectedCity?.code === c.code;
                              const label =
                                c.region?.trim() && c.region.trim().length > 0
                                  ? `${c.city}, ${c.region}`
                                  : c.city;
                              return (
                                <li key={c.code}>
                                  <button
                                    type="button"
                                    onClick={() => {
                                      setSelectedCity(c);
                                      setCdekCitySearch(label);
                                    }}
                                    className={cn(
                                      "w-full rounded-md px-2 py-2 text-left text-sm transition",
                                      active
                                        ? "bg-violet-500/25 text-zinc-100"
                                        : "text-zinc-400 hover:bg-white/5 hover:text-zinc-200",
                                    )}
                                  >
                                    {label}
                                  </button>
                                </li>
                              );
                            })}
                          </ul>
                        ) : null}

                        {selectedCity ? (
                          <>
                            {pointsQuery.isFetching ? (
                              <p className="text-xs text-zinc-500">Загружаем ПВЗ…</p>
                            ) : null}
                            {pointsQuery.data && pointsQuery.data.length > 0 ? (
                              <label className="flex flex-col gap-1.5 text-sm">
                                <span className="font-medium text-zinc-400">
                                  Пункт выдачи
                                </span>
                                <select
                                  required
                                  value={selectedPoint?.code ?? ""}
                                  onChange={(e) => {
                                    const code = e.target.value;
                                    const p =
                                      pointsQuery.data?.find((x) => x.code === code) ??
                                      null;
                                    setSelectedPoint(p);
                                  }}
                                  className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                                >
                                  <option value="">Выберите ПВЗ</option>
                                  {pointsQuery.data.map((p) => (
                                    <option key={p.code} value={p.code}>
                                      {p.name} — {p.address}
                                    </option>
                                  ))}
                                </select>
                              </label>
                            ) : pointsQuery.isSuccess ? (
                              <p className="text-sm text-amber-400/90">
                                Для этого города список ПВЗ пуст. Попробуйте другой
                                населённый пункт.
                              </p>
                            ) : null}

                            <div className="flex flex-wrap items-center gap-3">
                              <Button
                                type="button"
                                variant="outline"
                                disabled={
                                  !selectedPoint || cdekCalcPending || totalQty < 1
                                }
                                onClick={() => void handleCalculateCdek()}
                              >
                                {cdekCalcPending ? "Считаем…" : "Рассчитать доставку"}
                              </Button>
                              {cdekTariff ? (
                                <span className="text-sm text-zinc-300">
                                  Стоимость:{" "}
                                  <strong className="text-zinc-100">
                                    {formatMoney(cdekTariff.deliveryPrice)} ₸
                                  </strong>
                                  {cdekTariff.sourcedFromStub ? (
                                    <span className="ml-2 text-xs text-amber-400">
                                      (оценка без ключей API)
                                    </span>
                                  ) : null}
                                </span>
                              ) : null}
                            </div>
                            {cdekTariff ? (
                              <p className="m-0 text-xs text-zinc-500">
                                Товары: {formatMoney(cdekTariff.itemsTotal)} ₸ · Итого с доставкой:{" "}
                                {formatMoney(cdekTariff.orderTotal)} ₸ · Вес:{" "}
                                {cdekTariff.estimatedWeightGrams} г
                              </p>
                            ) : null}
                          </>
                        ) : null}

                        <div className="grid gap-4 border-t border-white/10 pt-4 sm:grid-cols-2">
                          <label className="flex flex-col gap-1.5 text-sm sm:col-span-2">
                            <span className="font-medium text-zinc-400">
                              Получатель
                            </span>
                            <input
                              required
                              autoComplete="name"
                              value={addrRecipientName}
                              onChange={(e) => setAddrRecipientName(e.target.value)}
                              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                            />
                          </label>
                          <label className="flex flex-col gap-1.5 text-sm sm:col-span-2">
                            <span className="font-medium text-zinc-400">
                              Телефон получателя
                            </span>
                            <input
                              required
                              type="tel"
                              autoComplete="tel"
                              value={addrRecipientPhone}
                              onChange={(e) => setAddrRecipientPhone(e.target.value)}
                              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                            />
                          </label>
                        </div>
                      </div>
                    ) : (
                      <div className="grid gap-4 sm:grid-cols-2">
                        <label className="flex flex-col gap-1.5 text-sm sm:col-span-2">
                          <span className="font-medium text-zinc-400">Город</span>
                          <input
                            required={needsAddress}
                            value={addrCity}
                            onChange={(e) => setAddrCity(e.target.value)}
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>
                        <label className="flex flex-col gap-1.5 text-sm sm:col-span-2">
                          <span className="font-medium text-zinc-400">
                            Улица, дом
                          </span>
                          <input
                            required={needsAddress}
                            value={addrStreet}
                            onChange={(e) => setAddrStreet(e.target.value)}
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>
                        <label className="flex flex-col gap-1.5 text-sm">
                          <span className="font-medium text-zinc-400">
                            Квартира / офис
                          </span>
                          <input
                            required={needsAddress}
                            placeholder="Нет — укажите «—»"
                            value={addrApartment}
                            onChange={(e) => setAddrApartment(e.target.value)}
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>
                        <label className="flex flex-col gap-1.5 text-sm">
                          <span className="font-medium text-zinc-400">Индекс</span>
                          <input
                            required={needsAddress}
                            inputMode="numeric"
                            value={addrPostal}
                            onChange={(e) => setAddrPostal(e.target.value)}
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>
                        <label className="flex flex-col gap-1.5 text-sm">
                          <span className="font-medium text-zinc-400">
                            Получатель
                          </span>
                          <input
                            required={needsAddress}
                            autoComplete="name"
                            value={addrRecipientName}
                            onChange={(e) => setAddrRecipientName(e.target.value)}
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>
                        <label className="flex flex-col gap-1.5 text-sm">
                          <span className="font-medium text-zinc-400">
                            Телефон получателя
                          </span>
                          <input
                            required={needsAddress}
                            type="tel"
                            autoComplete="tel"
                            value={addrRecipientPhone}
                            onChange={(e) => setAddrRecipientPhone(e.target.value)}
                            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                          />
                        </label>
                      </div>
                    )}
                  </div>
                ) : null}

                <label className="flex flex-col gap-1.5 text-sm">
                  <span className="font-medium text-zinc-400">
                    Комментарий к заказу{" "}
                    <span className="font-normal text-zinc-600">(необязательно)</span>
                  </span>
                  <textarea
                    rows={3}
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder="Размер, цвет, пожелания по доставке…"
                    className="resize-y rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
                  />
                </label>

                {formError ? (
                  <p className="m-0 text-sm font-medium text-red-400" role="alert">
                    {formError}
                  </p>
                ) : null}

                <Button
                  type="submit"
                  variant="primary"
                  className="w-full rounded-full sm:w-auto"
                  disabled={orderMutation.isPending}
                >
                  {orderMutation.isPending ? "Отправляем…" : "Подтвердить заказ"}
                </Button>
              </form>
            </section>
          </div>
        )}

        <Link
          to="/catalog"
          className="mt-10 inline-block font-semibold text-violet-400 hover:text-violet-300 hover:underline"
        >
          ← В каталог
        </Link>
      </Container>
    </div>
  );
}
