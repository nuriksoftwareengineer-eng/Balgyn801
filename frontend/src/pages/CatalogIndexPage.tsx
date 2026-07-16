import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogGroups } from "@/shared/api/catalog-api";
import { localizeName } from "@/shared/types/catalog";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { Container } from "@/shared/ui/container";
import { CatalogCard } from "@/widgets/catalog/CatalogCard";

export function CatalogIndexPage() {
  const { t, i18n } = useTranslation();
  const { data: groups, isLoading, error } = useCatalogGroups();

  useSeoMeta({
    title: t("nav.catalog"),
    description: t("catalog.metaDesc", "Коллекции вышивки Balgyn"),
  });

  return (
    <>
      {/* ── Header — light editorial ─────────────────────────── */}
      <Container className="pb-8 pt-12 md:pt-16">
        <nav className="mb-6 flex items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">{t("nav.home")}</Link>
          <span aria-hidden>/</span>
          <span className="text-black">{t("nav.catalog")}</span>
        </nav>
        <h1 className="display text-[44px] uppercase text-black md:text-[64px]">
          {t("nav.catalog")}
        </h1>
      </Container>

      {/* ── Groups grid ─────────────────────────────────────── */}
      <div className="pb-16 md:pb-24">
        <Container>
          {isLoading ? (
            <div className="grid grid-cols-1 gap-x-4 gap-y-10 sm:grid-cols-2 md:grid-cols-3">
              {[1, 2, 3].map((i) => (
                <div key={i}>
                  <div className="aspect-[4/5] animate-pulse bg-[--color-surface]" />
                  <div className="mt-4 h-3 w-28 animate-pulse bg-[--color-surface]" />
                </div>
              ))}
            </div>
          ) : error ? (
            <p className="text-sm text-[--color-danger]">{t("catalog.loadError", "Не удалось загрузить каталог.")}</p>
          ) : groups && groups.length > 0 ? (
            <div className="grid grid-cols-1 gap-x-4 gap-y-10 sm:grid-cols-2 md:grid-cols-3">
              {groups.map((group) => (
                <CatalogCard
                  key={group.id}
                  to={`/catalog/${group.slug}`}
                  title={localizeName(group, i18n.language)}
                  cover={group.coverImageUrl}
                  hint={t("home.categories.viewAll", "Смотреть →")}
                />
              ))}
            </div>
          ) : (
            <div className="mx-auto flex max-w-md flex-col items-center gap-6 py-20 text-center">
              <div className="flex h-16 w-16 items-center justify-center bg-[--color-surface] text-3xl select-none">
                🧵
              </div>
              <div>
                <p className="mb-1.5 text-[16px] font-medium text-black">
                  {t("catalog.emptyTitle", "Каталог пока пуст")}
                </p>
                <p className="text-[14px] leading-relaxed text-[--color-muted]">
                  {t("catalog.emptySubtitle", "Мы добавляем коллекции. Скоро они появятся здесь.")}
                </p>
              </div>
              <Link
                to="/custom-design"
                className="inline-flex items-center justify-center bg-black px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
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
