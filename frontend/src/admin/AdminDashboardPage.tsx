import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import { getDashboardStats } from "@/shared/api/backend-api";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, PieChart, Pie, Cell,
} from "recharts";
import type { DashboardStatsResponse } from "@/shared/types/dashboard";

const COLORS = ["#18181b", "#52525b", "#a1a1aa", "#d4d4d8", "#e4e4e7", "#f4f4f5"];

function KpiCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="rounded border border-white/10 bg-zinc-800/60 p-4">
      <p className="text-xs uppercase tracking-wider text-zinc-400">{label}</p>
      <p className="mt-1 text-2xl font-bold text-white">{value}</p>
      {sub && <p className="mt-0.5 text-xs text-zinc-500">{sub}</p>}
    </div>
  );
}

function fmt(n: number) {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(0)}K`;
  return String(Math.round(n));
}

function fmtKzt(n: number) { return `${fmt(n)} ₸`; }

const STATUS_LABELS: Record<string, string> = {
  NEW: "Новый", CONFIRMED: "Подтверждён", IN_PRODUCTION: "В производстве",
  READY: "Готов", SHIPPED: "Отправлен", DELIVERED: "Доставлен", CANCELLED: "Отменён",
};

export function AdminDashboardPage() {
  const { token } = useAuth();
  const [days, setDays] = useState(30);

  const { data, isLoading } = useQuery<DashboardStatsResponse>({
    queryKey: ["dashboard-stats", days, token],
    queryFn: () => getDashboardStats(token!, days),
    enabled: !!token,
    staleTime: 2 * 60_000,
  });

  if (isLoading) return <p className="text-sm text-zinc-400">Загрузка...</p>;
  if (!data) return null;

  const statusData = data.ordersByStatus.map(s => ({
    name: STATUS_LABELS[s.status] ?? s.status,
    value: s.count,
  }));

  const revenueData = data.dailyRevenue.map(d => ({
    date: d.date.slice(5),
    revenue: Number(d.revenue),
    orders: d.orders,
  }));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-white">Дашборд</h1>
        <select
          value={days}
          onChange={e => setDays(Number(e.target.value))}
          className="rounded border border-white/20 bg-zinc-800 px-3 py-1.5 text-sm text-white"
        >
          <option value={7}>7 дней</option>
          <option value={30}>30 дней</option>
          <option value={90}>90 дней</option>
          <option value={365}>Год</option>
        </select>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        <KpiCard label="Всего заказов" value={String(data.totalOrders)} sub={`+${data.ordersToday} сегодня`} />
        <KpiCard label="Выручка" value={fmtKzt(data.revenueTotal)} sub={`+${fmtKzt(data.revenueToday)} сегодня`} />
        <KpiCard label="Средний чек" value={fmtKzt(data.avgOrderValue)} />
        <KpiCard label="Пользователей" value={String(data.totalUsers)} sub={`+${data.usersToday} сегодня`} />
      </div>

      <div className="rounded border border-white/10 bg-zinc-800/40 p-4">
        <h2 className="mb-4 text-sm font-medium text-zinc-300">Выручка за {days} дней</h2>
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={revenueData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#3f3f46" />
            <XAxis dataKey="date" tick={{ fontSize: 10, fill: "#a1a1aa" }} />
            <YAxis tick={{ fontSize: 10, fill: "#a1a1aa" }} tickFormatter={v => `${fmt(v)}₸`} />
            <Tooltip
              contentStyle={{ background: "#27272a", border: "1px solid #3f3f46", color: "#fff", fontSize: 12 }}
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              formatter={((v: number) => [`${v.toLocaleString("ru")} ₸`, "Выручка"]) as any}
            />
            <Line type="monotone" dataKey="revenue" stroke="#a1a1aa" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded border border-white/10 bg-zinc-800/40 p-4">
          <h2 className="mb-4 text-sm font-medium text-zinc-300">Топ дизайнов</h2>
          {data.topDesigns.length === 0 ? (
            <p className="text-xs text-zinc-500">Нет данных</p>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={data.topDesigns.slice(0, 8)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#3f3f46" horizontal={false} />
                <XAxis type="number" tick={{ fontSize: 10, fill: "#a1a1aa" }} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 10, fill: "#a1a1aa" }} width={100} />
                <Tooltip
                  contentStyle={{ background: "#27272a", border: "1px solid #3f3f46", color: "#fff", fontSize: 12 }}
                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  formatter={((v: number) => [v, "Заказов"]) as any}
                />
                <Bar dataKey="count" fill="#52525b" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="rounded border border-white/10 bg-zinc-800/40 p-4">
          <h2 className="mb-4 text-sm font-medium text-zinc-300">Заказы по статусу</h2>
          {statusData.length === 0 ? (
            <p className="text-xs text-zinc-500">Нет данных</p>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={statusData} cx="50%" cy="50%" outerRadius={70} dataKey="value"
                  label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                  labelLine={false} fontSize={9}>
                  {statusData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip contentStyle={{ background: "#27272a", border: "1px solid #3f3f46", color: "#fff", fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
}