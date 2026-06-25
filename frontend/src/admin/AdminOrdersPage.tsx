import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { getOrders } from "@/shared/api/backend-api";
import { deliveryTypeLabel } from "@/shared/lib/delivery-labels";
import { formatMoney } from "@/shared/lib/format-money";
import { orderStatusLabel } from "@/shared/lib/order-status-labels";
import { cn } from "@/shared/lib/cn";

const ORDER_STATUS_STYLES: Record<string, string> = {
  PENDING_PAYMENT:  "bg-yellow-900/40 text-yellow-400",
  PAID:             "bg-blue-900/40 text-blue-400",
  PROCESSING:       "bg-sky-900/40 text-sky-400",
  SHIPPED:          "bg-purple-900/40 text-purple-400",
  DELIVERED:        "bg-emerald-900/40 text-emerald-400",
  CANCELLED:        "bg-red-900/40 text-red-400",
  EXPIRED:          "bg-zinc-700/60 text-zinc-400",
  REFUNDED:         "bg-orange-900/40 text-orange-400",
};

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

function resolveItemsTotal(totalPrice: number, deliveryFee?: number | null): number {
  const fee = deliveryFee ?? 0;
  return Math.max(0, totalPrice - fee);
}

export function AdminOrdersPage() {
  const { token } = useAuth();

  const q = useQuery({
    queryKey: ["admin-orders"],
    queryFn: async () => {
      if (!token) throw new Error("Нет токена");
      const list = await getOrders(token);
      return [...list].sort((a, b) => {
        const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return tb - ta;
      });
    },
    enabled: !!token,
  });

  return (
    <div>
      <h1 className="mb-8 text-2xl font-bold text-zinc-100">
        Заказы
      </h1>

      {q.isPending ? (
        <p className="text-zinc-500">Загрузка…</p>
      ) : q.isError ? (
        <p className="text-red-400">
          {q.error instanceof Error ? q.error.message : "Ошибка загрузки"}
        </p>
      ) : !q.data?.length ? (
        <p className="text-zinc-500">Пока нет заказов.</p>
      ) : (
        <div className="overflow-x-auto rounded-[14px] border border-white/10">
          <table className="w-full min-w-[760px] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-white/10 bg-zinc-900/80 text-xs uppercase tracking-wide text-zinc-500">
                <th className="px-4 py-3 font-semibold">№</th>
                <th className="px-4 py-3 font-semibold">Дата</th>
                <th className="px-4 py-3 font-semibold">Клиент</th>
                <th className="px-4 py-3 font-semibold">Телефон</th>
                <th className="px-4 py-3 font-semibold">Товар</th>
                <th className="px-4 py-3 font-semibold">Доставка</th>
                <th className="px-4 py-3 font-semibold">Статус</th>
                <th className="px-4 py-3 font-semibold">Сумма</th>
                <th className="px-4 py-3 font-semibold"> </th>
              </tr>
            </thead>
            <tbody>
              {q.data.map((o) => (
                <tr
                  key={o.id}
                  className="border-b border-white/5 hover:bg-white/[0.03]"
                >
                  <td className="px-4 py-3 tabular-nums text-zinc-500">
                    {o.id}
                  </td>
                  <td className="px-4 py-3 text-zinc-400">
                    {formatDate(o.createdAt)}
                  </td>
                  <td className="max-w-[160px] truncate px-4 py-3 font-medium text-zinc-100">
                    {o.customerName}
                  </td>
                  <td className="px-4 py-3 tabular-nums text-zinc-400">
                    {o.customerPhone}
                  </td>
                  <td className="max-w-[160px] truncate px-4 py-3 text-zinc-300">
                    {(() => {
                      const first = o.items?.[0];
                      const name = first?.productTitle ?? first?.designName ?? "—";
                      const count = (o.items?.length ?? 0) > 1 ? ` +${(o.items?.length ?? 1) - 1}` : "";
                      return name + count;
                    })()}
                  </td>
                  <td className="px-4 py-3 text-zinc-400">
                    {deliveryTypeLabel(o.deliveryType)}
                  </td>
                  <td className="px-4 py-3">
                    <span className={cn(
                      "rounded px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-[0.08em]",
                      ORDER_STATUS_STYLES[o.status ?? ""] ?? "bg-zinc-700/60 text-zinc-400",
                    )}>
                      {orderStatusLabel(o.status)}
                    </span>
                  </td>
                  <td className="px-4 py-3 tabular-nums text-zinc-300">
                    <p className="m-0 text-xs text-zinc-500">
                      Товары: {formatMoney(resolveItemsTotal(o.totalPrice, o.deliveryFee))} ₸
                    </p>
                    <p className="m-0 text-xs text-zinc-500">
                      Доставка: {formatMoney(o.deliveryFee ?? 0)} ₸
                    </p>
                    <p className="m-0 font-semibold text-zinc-200">
                      Итого: {formatMoney(o.totalPrice)} ₸
                    </p>
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/admin/orders/${o.id}`}
                      className="font-semibold text-zinc-300 underline-offset-2 hover:text-white hover:underline"
                    >
                      Открыть
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
