import { Link } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "@/app/auth-context";
import { grantAdminRole, revokeAdminRole } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

export function AdminDashboardPage() {
  const { token, isAdmin } = useAuth();
  const [adminEmail, setAdminEmail] = useState("");
  const [adminBusy, setAdminBusy] = useState(false);
  const [adminMsg, setAdminMsg] = useState<string | null>(null);
  const [adminErr, setAdminErr] = useState<string | null>(null);

  async function runAdminAction(
    action: "grant" | "revoke",
  ): Promise<void> {
    if (!token || !isAdmin) return;
    const email = adminEmail.trim().toLowerCase();
    if (!email) {
      setAdminErr("Укажите email пользователя");
      return;
    }
    setAdminErr(null);
    setAdminMsg(null);
    setAdminBusy(true);
    try {
      const res =
        action === "grant"
          ? await grantAdminRole(email, token)
          : await revokeAdminRole(email, token);
      setAdminMsg(
        action === "grant"
          ? `Роль ADMIN выдана: ${res.email} (${res.roles.join(", ")})`
          : `Роли обновлены: ${res.email} (${res.roles.join(", ")})`,
      );
      setAdminEmail("");
    } catch (e) {
      setAdminErr(
        e instanceof ApiError ? e.message : e instanceof Error ? e.message : "Ошибка",
      );
    } finally {
      setAdminBusy(false);
    }
  }

  return (
    <div>
      <h1 className="font-display mb-3 text-4xl tracking-wide text-zinc-100">
        Обзор
      </h1>
      <p className="max-w-xl text-zinc-400">
        Просматривайте заказы и управляйте каталогом. Счётчики и аналитику можно
        добавить позже.
      </p>
      <div className="mt-8 flex flex-wrap gap-3">
        <Link
          to="/admin/orders"
          className="inline-flex rounded-full border border-white/35 bg-white/10 px-6 py-3 text-sm font-semibold text-white transition hover:bg-white/20"
        >
          Заказы
        </Link>
        <Link
          to="/admin/products"
          className="inline-flex rounded-full border border-white/15 bg-white/5 px-6 py-3 text-sm font-semibold text-zinc-200 transition hover:bg-white/10"
        >
          Товары
        </Link>
        <Link
          to="/admin/customers"
          className="inline-flex rounded-full border border-white/15 bg-white/5 px-6 py-3 text-sm font-semibold text-zinc-200 transition hover:bg-white/10"
        >
          Клиенты
        </Link>
      </div>

      {isAdmin && token ? (
        <section className="mt-12 max-w-lg rounded-[14px] border border-white/10 bg-zinc-900/40 p-6">
          <h2 className="mb-2 text-lg font-semibold text-zinc-200">
            Администраторы
          </h2>
          <p className="mb-4 text-sm text-zinc-500">
            Выдать или снять роль ADMIN у зарегистрированного пользователя по
            email (снятие у последнего админа запрещено).
          </p>
          <label className="mb-3 flex flex-col gap-1 text-sm">
            <span className="text-zinc-400">Email пользователя</span>
            <input
              type="email"
              value={adminEmail}
              onChange={(e) => setAdminEmail(e.target.value)}
              placeholder="user@example.com"
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            />
          </label>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="primary"
              className="rounded-[10px]"
              disabled={adminBusy}
              onClick={() => void runAdminAction("grant")}
            >
              Выдать ADMIN
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-[10px]"
              disabled={adminBusy}
              onClick={() => void runAdminAction("revoke")}
            >
              Снять ADMIN
            </Button>
          </div>
          {adminErr ? (
            <p className="mt-3 text-sm text-red-400">{adminErr}</p>
          ) : null}
          {adminMsg ? (
            <p className="mt-3 text-sm text-emerald-400">{adminMsg}</p>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
