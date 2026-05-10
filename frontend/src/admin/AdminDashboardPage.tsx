import { Link } from "react-router-dom";

export function AdminDashboardPage() {
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
          className="inline-flex rounded-full border border-violet-500/35 bg-violet-500/10 px-6 py-3 text-sm font-semibold text-violet-200 transition hover:bg-violet-500/20"
        >
          Заказы
        </Link>
        <Link
          to="/admin/products"
          className="inline-flex rounded-full border border-white/15 bg-white/5 px-6 py-3 text-sm font-semibold text-zinc-200 transition hover:bg-white/10"
        >
          Товары
        </Link>
      </div>
    </div>
  );
}
