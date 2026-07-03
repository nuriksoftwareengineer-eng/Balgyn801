import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { searchAdminPayments } from "@/shared/api/backend-api";
import type { PaymentProvider, PaymentStatus } from "@/shared/api/types";
import { cn } from "@/shared/lib/cn";
import { AdminPagination, AdminSearchBar } from "@/shared/ui/admin-pagination";
import { useDebounce } from "@/shared/hooks/useDebounce";

const STATUS_COLORS: Record<string, string> = {
  PENDING: "text-amber-400",
  SUCCEEDED: "text-emerald-400",
  FAILED: "text-red-400",
  CANCELLED: "text-zinc-400",
  REFUNDED: "text-sky-400",
};

const PROVIDERS: Array<{ value: PaymentProvider | ""; label: string }> = [
  { value: "", label: "Все провайдеры" },
  { value: "FREEDOM_PAY", label: "Freedom Pay" },
  { value: "PAYPAL", label: "PayPal" },
];

const STATUSES: Array<{ value: PaymentStatus | ""; label: string }> = [
  { value: "", label: "Все статусы" },
  { value: "PENDING", label: "PENDING" },
  { value: "SUCCEEDED", label: "SUCCEEDED" },
  { value: "FAILED", label: "FAILED" },
  { value: "CANCELLED", label: "CANCELLED" },
  { value: "REFUNDED", label: "REFUNDED" },
];

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function AdminPaymentsPage() {
  const { token } = useAuth();
  const [provider, setProvider] = useState<PaymentProvider | "">("");
  const [status, setStatus] = useState<PaymentStatus | "">("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["admin-payments-search", provider, status, debouncedSearch, page],
    queryFn: () =>
      searchAdminPayments(token!, {
        provider: provider || undefined,
        status: status || undefined,
        q: debouncedSearch || undefined,
        page,
        size: 50,
      }),
    enabled: !!token,
  });

  const payments = data?.content ?? [];

  const selectClass =
    "rounded-[8px] border border-white/15 bg-zinc-800 px-3 py-2 text-sm text-zinc-200 outline-none focus:border-white/40";

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="font-display text-2xl text-zinc-100">Платежи</h1>
        <button
          type="button"
          onClick={() => void refetch()}
          className="rounded-[8px] border border-white/15 bg-zinc-800 px-4 py-2 text-sm font-semibold text-zinc-200 hover:bg-zinc-700"
        >
          Обновить
        </button>
      </div>

      <div className="mb-5 flex flex-wrap gap-3 items-center">
        <AdminSearchBar
          value={search}
          onChange={(v) => { setSearch(v); setPage(0); }}
          placeholder="Поиск по ID заказа, провайдеру…"
        />
        <select
          value={provider}
          onChange={(e) => { setProvider(e.target.value as PaymentProvider | ""); setPage(0); }}
          className={selectClass}
        >
          {PROVIDERS.map((p) => (
            <option key={p.value} value={p.value}>
              {p.label}
            </option>
          ))}
        </select>
        <select
          value={status}
          onChange={(e) => { setStatus(e.target.value as PaymentStatus | ""); setPage(0); }}
          className={selectClass}
        >
          {STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        {data && (
          <span className="text-xs text-zinc-500">{data.totalElements} записей</span>
        )}
      </div>

      {isLoading && (
        <p className="text-sm text-zinc-400">Загрузка…</p>
      )}
      {error && (
        <p className="text-sm text-red-400">
          Ошибка: {error instanceof Error ? error.message : "Неизвестная ошибка"}
        </p>
      )}

      {data && (
        <>
          <div className="overflow-x-auto rounded-[12px] border border-white/10">
            <table className="w-full min-w-[720px] text-sm">
              <thead>
                <tr className="border-b border-white/10 text-left text-xs text-zinc-500">
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Заказ</th>
                  <th className="px-4 py-3">Провайдер</th>
                  <th className="px-4 py-3">Статус</th>
                  <th className="px-4 py-3">Сумма</th>
                  <th className="px-4 py-3">PayPal / FP ID</th>
                  <th className="px-4 py-3">Дата</th>
                  <th className="px-4 py-3">Ошибка</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
                  <tr
                    key={payment.id}
                    className="border-b border-white/5 hover:bg-white/3"
                  >
                    <td className="px-4 py-3 font-mono text-zinc-300">
                      #{payment.id}
                    </td>
                    <td className="px-4 py-3">
                      {payment.orderId ? (
                        <Link
                          to={`/admin/orders/${payment.orderId}`}
                          className="font-medium text-zinc-100 underline-offset-2 hover:underline"
                        >
                          #{payment.orderId}
                        </Link>
                      ) : (
                        <span className="text-zinc-600">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-zinc-300">
                      {payment.provider}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={cn(
                          "font-semibold",
                          STATUS_COLORS[payment.status] ?? "text-zinc-300",
                        )}
                      >
                        {payment.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-zinc-300">
                      {Number(payment.amount).toFixed(2)}{" "}
                      <span className="text-zinc-500">{payment.currency}</span>
                    </td>
                    <td className="max-w-[180px] truncate px-4 py-3 font-mono text-xs text-zinc-400">
                      {payment.providerPaymentId ?? "—"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-xs text-zinc-400">
                      {fmt(payment.createdAt)}
                    </td>
                    <td className="max-w-[180px] truncate px-4 py-3 text-xs text-red-400">
                      {payment.errorMessage ?? ""}
                    </td>
                  </tr>
                ))}
                {payments.length === 0 && (
                  <tr>
                    <td
                      colSpan={8}
                      className="px-4 py-8 text-center text-sm text-zinc-500"
                    >
                      Платежей не найдено
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <AdminPagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            onPage={setPage}
          />
        </>
      )}
    </div>
  );
}
