import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogGroups } from "@/shared/api/catalog-api";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { Container } from "@/shared/ui/container";

export function CatalogIndexPage() {
  const { t } = useTranslation();
  const { data: groups, isLoading, error } = useCatalogGroups();

  useSeoMeta({
    title: `${t("nav.catalog")} — Balgyn`,
    description: t("catalog.metaDesc", "Коллекции вышивки Balgyn"),
  });

  return (
    <>
      {/* ── Hero ────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-14 md:py-20">
          <nav className="mb-5 flex items-center gap-2 text-[0.55rem] uppercase tracking-[0.16em] text-white/40">
            <Link to="/" className="transition hover:text-white/70">{t("nav.home")}</Link>
            <span>/</span>
            <span className="text-white/70">{t("nav.catalog")}</span>
          </nav>
          <h1 className="text-5xl font-semibold uppercase tracking-[0.04em] text-white md:text-7xl">
            {t("nav.catalog")}
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
            <p className="text-sm text-[--color-danger]">{t("catalog.loadError", "Не удалось загрузить каталог.")}</p>
          ) : groups && groups.length > 0 ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
              {groups.map((group) => (
                <Link
                  key={group.id}
                  to={`/catalog/${group.slug}`}
                  className="group block overflow-hidden border border-[--color-border] bg-white transition-shadow hover:shadow-md"
                >
                  <div className="aspect-[3/2] overflow-hidden bg-zinc-900">
                    {group.coverImageUrl ? (
                      <img
                        src={group.coverImageUrl}
                        alt={group.name}
                        className="h-full w-full object-cover object-center transition-transform duration-300 group-hover:scale-105"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center">
                        <span className="text-7xl font-bold uppercase text-white/10 select-none">
                          {group.name.charAt(0)}
                        </span>
                      </div>
                    )}
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
            <div className="flex flex-col items-start gap-5 py-10">
              <div className="flex h-16 w-16 items-center justify-center bg-[--color-surface] text-3xl select-none">
                🧵
              </div>
              <div>
                <p className="mb-1 text-[15px] font-semibold text-black">
                  {t("catalog.emptyTitle", "Каталог пока пуст")}
                </p>
                <p className="text-[13px] text-[--color-muted]">
                  {t("catalog.emptySubtitle", "Мы добавляем коллекции — скоро они появятся здесь.")}
                </p>
              </div>
              <Link
                to="/custom-design"
                className="inline-block border border-black px-5 py-2.5 text-[11px] font-bold uppercase tracking-[0.14em] text-black transition hover:bg-black hover:text-white"
              >
                {t("catalog.customDesignCta", "Оформить свой дизайн →")}
              </Link>
            </div>
          )}
        </Container>
      </div>
    </>
  );
}
