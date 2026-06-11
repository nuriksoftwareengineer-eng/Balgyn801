import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { getMyOrders } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import type { OrderResponse, OrderStatus, DeliveryType } from "@/shared/api/types";

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

const STATUS_COLOR: Record<OrderStatus, string> = {
  NEW: "bg-blue-50 text-blue-700",
  CONFIRMED: "bg-amber-50 text-amber-700",
  IN_PRODUCTION: "bg-zinc-100 text-zinc-700",
  READY: "bg-emerald-50 text-emerald-700",
  SHIPPED: "bg-sky-50 text-sky-700",
  DELIVERED: "bg-emerald-100 text-emerald-800",
  CANCELLED: "bg-red-50 text-red-700",
  PENDING_PAYMENT: "bg-zinc-100 text-zinc-600",
  EXPIRED: "bg-gray-100 text-gray-500",
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
    <div className="min-h-screen bg-white">
      <div className="mx-auto max-w-[900px] px-5 py-12">
        <h1 className="mb-8 text-2xl font-semibold uppercase tracking-[0.06em] text-black">
          Мои заказы
        </h1>

        {error ? (
          <p className="text-sm text-[--color-danger]">{error}</p>
        ) : orders === null ? (
          <ul className="flex flex-col gap-4">
            {[0, 1, 2].map((i) => (
              <li key={i} className="h-28 animate-pulse rounded-none border border-[--color-border] bg-[--color-surface]" />
            ))}
          </ul>
        ) : orders.length === 0 ? (
          <div className="flex flex-col items-start gap-4">
            <p className="text-sm text-[--color-muted]">Заказов пока нет.</p>
            <Link
              to="/catalog"
              className="text-[0.7rem] font-medium uppercase tracking-[0.14em] text-black underline underline-offset-2 hover:text-[--color-muted]"
            >
              Перейти в каталог
            </Link>
          </div>
        ) : (
          <ul className="flex flex-col gap-4">
            {orders.map((order) => (
              <li
                key={order.id}
                className="border border-[--color-border] bg-white px-6 py-5"
              >
                {/* Header row */}
                <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-semibold text-black">
                      Заказ #{order.id}
                    </span>
                    {order.status ? (
                      <span
                        className={`px-2 py-0.5 text-[0.6rem] font-medium uppercase tracking-[0.1em] ${STATUS_COLOR[order.status]}`}
                      >
                        {STATUS_LABEL[order.status]}
                      </span>
                    ) : null}
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-[0.65rem] text-[--color-muted]">
                      {fmtDate(order.createdAt)}
                    </span>
                    <span className="text-sm font-semibold text-black">
                      {fmt(order.totalPrice)}
                    </span>
                  </div>
                </div>

                {/* Items */}
                {order.items && order.items.length > 0 ? (
                  <ul className="mb-3 flex flex-col gap-1.5 border-t border-[--color-border] pt-3">
                    {order.items.map((item) => (
                      <li key={item.id} className="flex items-center justify-between gap-4 text-sm">
                        <span className="text-black">
                          {item.productTitle}
                          {item.sizeLabel ? ` · ${item.sizeLabel}` : ""}
                          {item.colorName ? ` · ${item.colorName}` : ""}
                        </span>
                        <span className="shrink-0 text-[--color-muted]">
                          {item.quantity} × {fmt(item.unitPrice)}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : null}

                {/* Footer row */}
                <div className="flex flex-wrap gap-4 border-t border-[--color-border] pt-3 text-[0.65rem] text-[--color-muted]">
                  <span>{DELIVERY_LABEL[order.deliveryType]}</span>
                  {order.deliveryFee != null && order.deliveryFee > 0 ? (
                    <span>Доставка: {fmt(order.deliveryFee)}</span>
                  ) : null}
                  {order.comment ? (
                    <span className="italic">"{order.comment}"</span>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
