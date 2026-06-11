import { Link } from "react-router-dom";
import { useCatalogGroups } from "@/shared/api/catalog-api";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { Container } from "@/shared/ui/container";

export function CatalogIndexPage() {
  const { data: groups, isLoading, error } = useCatalogGroups();

  useSeoMeta({
    title: "Каталог — Balgyn",
    description: "Коллекции вышивки Balgyn",
  });

  return (
    <div className="py-14">
      <Container>
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.1em] text-[--color-muted]">
          <Link to="/" className="hover:text-black transition">Главная</Link>
          <span>›</span>
          <span className="text-black">Каталог</span>
        </nav>

        <h1 className="mb-8 text-4xl font-semibold uppercase tracking-[0.04em] text-black">
          Каталог
        </h1>

        {isLoading ? (
          <p className="text-sm text-[--color-muted]">Загружаем каталог…</p>
        ) : error ? (
          <p className="text-sm text-[--color-danger]">Не удалось загрузить каталог.</p>
        ) : groups && groups.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
            {groups.map((group) => (
              <Link
                key={group.id}
                to={`/catalog/${group.slug}`}
                className="group flex flex-col justify-between border border-[--color-border] bg-white px-6 py-8 transition-shadow hover:shadow-sm"
              >
                <p className="text-lg font-semibold text-black group-hover:underline underline-offset-4">
                  {group.name}
                </p>
                <p className="mt-4 text-[0.6rem] font-medium uppercase tracking-[0.12em] text-[--color-muted] transition group-hover:text-black">
                  Смотреть →
                </p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="text-sm text-[--color-muted]">Каталог пока пуст.</p>
        )}
      </Container>
    </div>
  );
}
