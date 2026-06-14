import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { getMyOrders } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import type { OrderResponse, OrderStatus, DeliveryType } from "@/shared/api/types";
import { Container } from "@/shared/ui/container";

const STATUS_LABEL: Record<OrderStatus, string> = {
  NEW: "Новый",
  CONFIRMED: "Подтверждён",
  IN_PRODUCTION: "В производстве",
  READY: "Готов",
  SHIPPED: "Отправлен",
  DELIVERED: "Доставлен",
  CANCELLED: "Отменён",
  PENDING_PAYMENT: "Ожидает оплаты",
  EXPIRED: "Просрочен",
};

const STATUS_DOT: Record<OrderStatus, string> = {
  NEW: "bg-blue-500",
  CONFIRMED: "bg-amber-500",
  IN_PRODUCTION: "bg-zinc-400",
  READY: "bg-emerald-500",
  SHIPPED: "bg-sky-500",
  DELIVERED: "bg-emerald-600",
  CANCELLED: "bg-red-500",
  PENDING_PAYMENT: "bg-zinc-300",
  EXPIRED: "bg-gray-300",
};

const DELIVERY_LABEL: Record<DeliveryType, string> = {
  PICKUP: "Самовывоз",
  TAXI: "Такси / курьер",
  CDEK: "СДЭК",
  POSTAL: "Казпочта",
  INTERNATIONAL: "Международная доставка",
};

function fmt(price: number) {
  return price.toLocaleString("ru-RU") + " ₸";
}

function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

export function OrderHistoryPage() {
  const { token } = useAuth();
  const [orders, setOrders] = useState<OrderResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    getMyOrders(token)
      .then(setOrders)
      .catch((e) =>
        setError(e instanceof ApiError ? e.message : "Не удалось загрузить заказы"),
      );
  }, [token]);

  return (
    <>
      {/* ── Hero ────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-12 md:py-16">
          <h1 className="text-4xl font-extrabold uppercase tracking-[-0.01em] text-white md:text-5xl">
            Мои заказы
          </h1>
        </Container>
      </div>

      {/* ── Orders list ─────────────────────────────────────── */}
      <Container className="py-10 md:py-14">
        {error ? (
          <p className="text-[14px] text-[--color-danger]">{error}</p>
        ) : orders === null ? (
          <div className="flex flex-col gap-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-28 animate-pulse border border-[--color-border] bg-[--color-surface]" />
            ))}
          </div>
        ) : orders.length === 0 ? (
          <div className="flex flex-col gap-5">
            <p className="text-[15px] text-[--color-muted]">Заказов пока нет.</p>
            <Link
              to="/catalog"
              className="inline-block bg-black px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
            >
              Перейти в каталог
            </Link>
          </div>
        ) : (
          <ul className="flex flex-col divide-y divide-[--color-border] border border-[--color-border]">
            {orders.map((order) => (
              <li key={order.id} className="bg-white px-5 py-5 md:px-6 md:py-6">
                {/* Header row */}
                <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="text-[15px] font-bold text-black">
                      #{order.id}
                    </span>
                    {order.status ? (
                      <span className="flex items-center gap-1.5">
                        <span className={`h-2 w-2 rounded-full ${STATUS_DOT[order.status]}`} />
                        <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[--color-muted]">
                          {STATUS_LABEL[order.status]}
                        </span>
                      </span>
                    ) : null}
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-[12px] text-[--color-muted]">
                      {fmtDate(order.createdAt)}
                    </span>
                    <span className="text-[15px] font-bold text-black">
                      {fmt(order.totalPrice)}
                    </span>
                  </div>
                </div>

                {/* Items */}
                {order.items && order.items.length > 0 ? (
                  <ul className="mb-4 flex flex-col gap-1.5 border-t border-[--color-border] pt-4">
                    {order.items.map((item) => (
                      <li key={item.id} className="flex items-center justify-between gap-4">
                        <span className="text-[13px] text-black">
                          {item.productTitle}
                          {item.sizeLabel ? ` · ${item.sizeLabel}` : ""}
                          {item.colorName ? ` · ${item.colorName}` : ""}
                        </span>
                        <span className="shrink-0 text-[12px] text-[--color-muted]">
                          {item.quantity} × {fmt(item.unitPrice)}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : null}

                {/* Footer row */}
                <div className="flex flex-wrap gap-4 border-t border-[--color-border] pt-3">
                  <span className="text-[11px] uppercase tracking-[0.1em] text-[--color-muted]">
                    {order.deliveryType === "CDEK"
                      ? "СДЭК · оплата при получении"
                      : DELIVERY_LABEL[order.deliveryType]}
                  </span>
                  {order.deliveryType !== "CDEK" && order.deliveryFee != null && order.deliveryFee > 0 ? (
                    <span className="text-[11px] uppercase tracking-[0.1em] text-[--color-muted]">
                      Доставка: {fmt(order.deliveryFee)}
                    </span>
                  ) : null}
                  {order.comment ? (
                    <span className="text-[11px] italic text-[--color-muted]">
                      "{order.comment}"
                    </span>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Container>
    </>
  );
}
