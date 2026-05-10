import { Link } from "react-router-dom";

export function AdminDashboardPage() {
  return (
    <div>
      <h1 className="font-display mb-3 text-4xl tracking-wide text-zinc-100">
        Обзор
      </h1>
      <p className="max-w-xl text-zinc-400">
        Дальше здесь можно вывести счётчики заказов и быстрые действия. Сейчас доступно
        управление каталогом.
      </p>
      <Link
        to="/admin/products"
        className="mt-8 inline-flex rounded-full border border-violet-500/35 bg-violet-500/10 px-6 py-3 text-sm font-semibold text-violet-200 transition hover:bg-violet-500/20"
      >
        Перейти к товарам
      </Link>
    </div>
  );
}
