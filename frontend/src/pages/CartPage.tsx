import { useMutation } from "@tanstack/react-query";
import { motion, useReducedMotion } from "framer-motion";
import { useState } from "react";
import { Link } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import { createOrder } from "@/shared/api/backend-api";
import type { CreateOrderRequest, DeliveryType, OrderResponse } from "@/shared/api/types";
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

function OrderSuccess({
  order,
  onContinue,
}: {
  order: OrderResponse;
  onContinue: () => void;
}) {
  const dt = DELIVERY_OPTIONS.find((o) => o.value === order.deliveryType)?.label;
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

export function CartPage() {
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

  const [addrCity, setAddrCity] = useState("");
  const [addrStreet, setAddrStreet] = useState("");
  const [addrApartment, setAddrApartment] = useState("");
  const [addrPostal, setAddrPostal] = useState("");
  const [addrRecipientName, setAddrRecipientName] = useState("");
  const [addrRecipientPhone, setAddrRecipientPhone] = useState("");

  const needsAddress =
    deliveryType === "TAXI" || deliveryType === "CDEK";

  const orderMutation = useMutation({
    mutationFn: (body: CreateOrderRequest) => createOrder(body),
    onSuccess: (order) => {
      setFormError(null);
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
    },
    onError: (err: unknown) => {
      setFormError(
        err instanceof ApiError ? err.message : "Не удалось оформить заказ",
      );
    },
  });

  function handleCheckout(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);
    const tg = telegramUsername.trim().replace(/^@/, "");
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
      address:
        deliveryType === "TAXI" || deliveryType === "CDEK"
          ? {
              city: addrCity.trim(),
              street: addrStreet.trim(),
              apartment: addrApartment.trim(),
              postalCode: addrPostal.trim(),
              recipientName: addrRecipientName.trim(),
              recipientPhone: addrRecipientPhone.trim(),
            }
          : null,
    };
    orderMutation.mutate(payload);
  }

  if (completedOrder) {
    return (
      <div className="py-14">
        <Container>
          <OrderSuccess
            order={completedOrder}
            onContinue={() => setCompletedOrder(null)}
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
                      Адрес доставки
                    </h3>
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
