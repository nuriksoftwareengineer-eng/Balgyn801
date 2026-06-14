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
    <>
      {/* ── Hero ────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-14 md:py-20">
          <nav className="mb-5 flex items-center gap-2 text-[0.55rem] uppercase tracking-[0.16em] text-white/40">
            <Link to="/" className="transition hover:text-white/70">Главная</Link>
            <span>/</span>
            <span className="text-white/70">Каталог</span>
          </nav>
          <h1 className="text-5xl font-semibold uppercase tracking-[0.04em] text-white md:text-7xl">
            Каталог
          </h1>
        </Container>
      </div>

      {/* ── Groups grid ─────────────────────────────────────── */}
      <div className="py-10 md:py-14">
        <Container>
          {isLoading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="overflow-hidden border border-[--color-border]">
                  <div className="aspect-[3/2] animate-pulse bg-[--color-surface]" />
                  <div className="px-4 py-4">
                    <div className="h-2 w-24 animate-pulse bg-[--color-surface]" />
                  </div>
                </div>
              ))}
            </div>
          ) : error ? (
            <p className="text-sm text-[--color-danger]">Не удалось загрузить каталог.</p>
          ) : groups && groups.length > 0 ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
              {groups.map((group) => (
                <Link
                  key={group.id}
                  to={`/catalog/${group.slug}`}
                  className="group block overflow-hidden border border-[--color-border] bg-white transition-shadow hover:shadow-md"
                >
                  <div className="aspect-[3/2] flex items-center justify-center bg-zinc-900">
                    <span className="text-7xl font-bold uppercase text-white/10 select-none">
                      {group.name.charAt(0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between px-4 py-3.5">
                    <p className="text-sm font-semibold uppercase tracking-[0.06em] text-black">
                      {group.name}
                    </p>
                    <span className="text-[0.6rem] font-medium uppercase tracking-[0.14em] text-[--color-muted] transition group-hover:text-black">
                      →
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <p className="text-sm text-[--color-muted]">Каталог пока пуст.</p>
          )}
        </Container>
      </div>
    </>
  );
}
