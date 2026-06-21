import { Link } from "react-router-dom";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import { getOrders, getCustomers, grantAdminRole, revokeAdminRole } from "@/shared/api/backend-api";
import { listDesigns } from "@/shared/api/admin-catalog";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

function StatCard({ label, value, to }: { label: string; value: number | string; to: string }) {
  return (
    <Link
      to={to}
      className="group flex flex-col gap-2 rounded-lg border border-white/10 bg-zinc-900/60 p-5 transition hover:bg-zinc-800/60"
    >
      <p className="text-xs font-semibold uppercase tracking-[0.1em] text-zinc-500">{label}</p>
      <p className="text-3xl font-bold text-zinc-100">{value}</p>
    </Link>
  );
}

export function AdminDashboardPage() {
  const { token, isAdmin } = useAuth();
  const [adminEmail, setAdminEmail] = useState("");
  const [adminBusy, setAdminBusy] = useState(false);
  const [adminMsg, setAdminMsg] = useState<string | null>(null);
  const [adminErr, setAdminErr] = useState<string | null>(null);

  const ordersQ = useQuery({
    queryKey: ["admin-orders"],
    queryFn: () => getOrders(token!),
    enabled: !!token,
  });

  const customersQ = useQuery({
    queryKey: ["admin-customers"],
    queryFn: () => getCustomers(token!),
    enabled: !!token,
  });

  const designsQ = useQuery({
    queryKey: ["admin-designs"],
    queryFn: () => listDesigns(token!),
    enabled: !!token,
  });

  const totalOrders = ordersQ.data?.length ?? "—";
  // Admin orders API excludes PENDING_PAYMENT and EXPIRED; "new" = paid but not yet processed.
  const newOrders = ordersQ.data?.filter((o) => o.status === "NEW").length ?? "—";
  const publishedDesigns = designsQ.data?.filter((d) => d.status === "PUBLISHED").length ?? "—";
  const totalCustomers = customersQ.data?.length ?? "—";

  async function runAdminAction(action: "grant" | "revoke"): Promise<void> {
    if (!token || !isAdmin) return;
    const email = adminEmail.trim().toLowerCase();
    if (!email) { setAdminErr("Укажите email пользователя"); return; }
    setAdminErr(null);
    setAdminMsg(null);
    setAdminBusy(true);
    try {
      const res = action === "grant"
        ? await grantAdminRole(email, token)
        : await revokeAdminRole(email, token);
      setAdminMsg(
        action === "grant"
          ? `Роль ADMIN выдана: ${res.email} (${res.roles.join(", ")})`
          : `Роли обновлены: ${res.email} (${res.roles.join(", ")})`,
      );
      setAdminEmail("");
    } catch (e) {
      setAdminErr(e instanceof ApiError ? e.message : e instanceof Error ? e.message : "Ошибка");
    } finally {
      setAdminBusy(false);
    }
  }

  return (
    <div>
      <h1 className="mb-8 text-2xl font-bold text-zinc-100">Обзор</h1>

      {/* KPI cards */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Всего заказов" value={totalOrders} to="/admin/orders" />
        <StatCard label="Новые заказы" value={newOrders} to="/admin/orders" />
        <StatCard label="Дизайнов активно" value={publishedDesigns} to="/admin/designs" />
        <StatCard label="Клиентов" value={totalCustomers} to="/admin/customers" />
      </div>

      {/* Quick links */}
      <div className="mt-8 flex flex-wrap gap-3">
        <Link
          to="/admin/orders"
          className="inline-flex rounded-lg border border-white/35 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/20"
        >
          Заказы →
        </Link>
        <Link
          to="/admin/designs"
          className="inline-flex rounded-lg border border-white/15 bg-white/5 px-5 py-2.5 text-sm font-semibold text-zinc-200 transition hover:bg-white/10"
        >
          Дизайны →
        </Link>
        <Link
          to="/admin/customers"
          className="inline-flex rounded-lg border border-white/15 bg-white/5 px-5 py-2.5 text-sm font-semibold text-zinc-200 transition hover:bg-white/10"
        >
          Клиенты →
        </Link>
      </div>

      {/* Admin role management */}
      {isAdmin && token ? (
        <section className="mt-12 max-w-lg rounded-lg border border-white/10 bg-zinc-900/40 p-6">
          <h2 className="mb-2 text-base font-semibold text-zinc-200">Администраторы</h2>
          <p className="mb-4 text-sm text-zinc-500">
            Выдать или снять роль ADMIN у зарегистрированного пользователя по email.
          </p>
          <label className="mb-3 flex flex-col gap-1 text-sm">
            <span className="text-zinc-400">Email пользователя</span>
            <input
              type="email"
              value={adminEmail}
              onChange={(e) => setAdminEmail(e.target.value)}
              placeholder="user@example.com"
              className="rounded-lg border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            />
          </label>
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="primary" className="rounded-lg" disabled={adminBusy} onClick={() => void runAdminAction("grant")}>
              Выдать ADMIN
            </Button>
            <Button type="button" variant="outline" className="rounded-lg" disabled={adminBusy} onClick={() => void runAdminAction("revoke")}>
              Снять ADMIN
            </Button>
          </div>
          {adminErr ? <p className="mt-3 text-sm text-red-400">{adminErr}</p> : null}
          {adminMsg ? <p className="mt-3 text-sm text-emerald-400">{adminMsg}</p> : null}
        </section>
      ) : null}
    </div>
  );
}
