import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { cn } from "@/shared/lib/cn";

const navClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    "block rounded-[10px] px-3 py-2 text-sm font-semibold transition",
    isActive
      ? "bg-white/15 text-white"
      : "text-zinc-400 hover:bg-white/5 hover:text-zinc-200",
  );

export function AdminLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen bg-zinc-950 text-zinc-100">
      <aside className="flex w-[240px] shrink-0 flex-col border-r border-white/10 bg-zinc-900/50 px-4 py-6">
        <p className="font-display mb-6 text-lg tracking-wide text-zinc-100">
          Админка
        </p>
        <nav className="flex flex-col gap-1">
          <NavLink to="/admin" end className={navClass}>
            Обзор
          </NavLink>
          <NavLink to="/admin/orders" className={navClass}>
            Заказы
          </NavLink>
          <NavLink to="/admin/categories" className={navClass}>
            Категории
          </NavLink>
          <NavLink to="/admin/collections" className={navClass}>
            Коллекции
          </NavLink>
          <NavLink to="/admin/designs" className={navClass}>
            Дизайны
          </NavLink>
          <NavLink to="/admin/customers" className={navClass}>
            Клиенты
          </NavLink>
          <NavLink to="/admin/size-charts" className={navClass}>
            Размерные сетки
          </NavLink>
          <NavLink to="/admin/payments" className={navClass}>
            Платежи
          </NavLink>
          <NavLink to="/admin/exchange-rate" className={navClass}>
            Курс валют
          </NavLink>
          <NavLink to="/admin/users" className={navClass}>
            Пользователи
          </NavLink>
        </nav>
        <div className="mt-auto border-t border-white/10 pt-4">
          <p className="truncate text-xs text-zinc-500">{user?.email}</p>
          <button
            type="button"
            className="mt-2 text-sm font-semibold text-zinc-300 hover:text-white"
            onClick={logout}
          >
            Выйти
          </button>
          <NavLink
            to="/"
            className="mt-3 block text-xs font-medium text-zinc-500 hover:text-zinc-300"
          >
            ← На сайт
          </NavLink>
        </div>
      </aside>
      <main className="min-h-screen flex-1 overflow-auto px-6 py-8 md:px-10">
        <Outlet />
      </main>
    </div>
  );
}
