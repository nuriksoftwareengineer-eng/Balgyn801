import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { getOrderByIdAdmin } from "@/shared/api/backend-api";
import { deliveryTypeLabel } from "@/shared/lib/delivery-labels";
import { formatMoney } from "@/shared/lib/format-money";
import { orderStatusLabel } from "@/shared/lib/order-status-labels";

function formatDate(iso: string | undefined) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("ru-RU", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export function AdminOrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const id = Number.parseInt(orderId ?? "", 10);
  const { token } = useAuth();

  const q = useQuery({
    queryKey: ["admin-order", id],
    queryFn: async () => {
      if (!token) throw new Error("Нет токена");
      if (!Number.isFinite(id) || id <= 0) throw new Error("Некорректный ID");
      return getOrderByIdAdmin(id, token);
    },
    enabled: !!token && Number.isFinite(id) && id > 0,
  });

  if (!Number.isFinite(id) || id <= 0) {
    return (
      <div>
        <p className="text-red-400">Некорректная ссылка на заказ.</p>
        <Link
          to="/admin/orders"
          className="mt-4 inline-block text-violet-400 hover:underline"
        >
          ← К списку заказов
        </Link>
      </div>
    );
  }

  if (q.isPending) {
    return <p className="text-zinc-500">Загрузка…</p>;
  }

  if (q.isError) {
    return (
      <div>
        <p className="text-red-400">
          {q.error instanceof Error ? q.error.message : "Ошибка загрузки"}
        </p>
        <Link
          to="/admin/orders"
          className="mt-4 inline-block text-violet-400 hover:underline"
        >
          ← К списку заказов
        </Link>
      </div>
    );
  }

  const o = q.data!;

  return (
    <div>
      <Link
        to="/admin/orders"
        className="mb-6 inline-block text-sm font-semibold text-violet-400 hover:underline"
      >
        ← Все заказы
      </Link>
      <h1 className="font-display mb-2 text-4xl tracking-wide text-zinc-100">
        Заказ №{o.id}
      </h1>
      <p className="mb-8 text-sm text-zinc-500">{formatDate(o.createdAt)}</p>

      <div className="mb-8 grid gap-6 md:grid-cols-2">
        <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Клиент
          </h2>
          <p className="text-zinc-100">{o.customerName}</p>
          <p className="mt-1 text-zinc-400">{o.customerPhone}</p>
          {o.comment ? (
            <p className="mt-3 text-sm text-zinc-500">
              <span className="font-medium text-zinc-400">Комментарий: </span>
              {o.comment}
            </p>
          ) : null}
        </section>
        <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Доставка и статус
          </h2>
          <p className="text-zinc-100">{deliveryTypeLabel(o.deliveryType)}</p>
          <p className="mt-2 text-sm text-zinc-400">
            Статус:{" "}
            <span className="font-medium text-zinc-200">
              {orderStatusLabel(o.status)}
            </span>
          </p>
          <p className="mt-3 text-lg font-bold text-zinc-100">
            Итого: {formatMoney(o.totalPrice)} ₸
          </p>
        </section>
      </div>

      {o.address ? (
        <section className="mb-8 rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Адрес
          </h2>
          <p className="text-zinc-200">
            {o.address.postalCode}, {o.address.city}, {o.address.street},{" "}
            {o.address.apartment}
          </p>
          <p className="mt-2 text-sm text-zinc-400">
            Получатель: {o.address.recipientName}, {o.address.recipientPhone}
          </p>
        </section>
      ) : null}

      <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-zinc-500">
          Позиции
        </h2>
        {!o.items?.length ? (
          <p className="text-zinc-500">Позиции не загружены или заказ пустой.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[480px] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-white/10 text-xs uppercase text-zinc-500">
                  <th className="py-2 pr-4 font-semibold">Товар</th>
                  <th className="py-2 pr-4 font-semibold">Вариант</th>
                  <th className="py-2 pr-4 font-semibold">Кол-во</th>
                  <th className="py-2 pr-4 font-semibold">Цена</th>
                  <th className="py-2 font-semibold">Сумма</th>
                </tr>
              </thead>
              <tbody>
                {o.items.map((line) => (
                  <tr key={line.id} className="border-b border-white/5">
                    <td className="py-3 pr-4 text-zinc-100">
                      {line.productTitle}
                    </td>
                    <td className="py-3 pr-4 text-sm text-zinc-500">
                      {[line.sizeLabel, line.colorName].filter(Boolean).join(" · ") ||
                        "—"}
                    </td>
                    <td className="py-3 pr-4 tabular-nums text-zinc-400">
                      {line.quantity}
                    </td>
                    <td className="py-3 pr-4 tabular-nums text-zinc-400">
                      {formatMoney(line.unitPrice)} ₸
                    </td>
                    <td className="py-3 tabular-nums text-zinc-200">
                      {formatMoney(line.unitPrice * line.quantity)} ₸
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
