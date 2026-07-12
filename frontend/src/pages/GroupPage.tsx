import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogGroup } from "@/shared/api/catalog-api";
import { localizeName } from "@/shared/types/catalog";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { Container } from "@/shared/ui/container";
import { CatalogCard } from "@/widgets/catalog/CatalogCard";

export function GroupPage() {
  const { t, i18n } = useTranslation();
  // groupSlug is set on deep routes (/catalog/:groupSlug/...); param on /catalog/:param
  const { groupSlug: routeGroupSlug, param } = useParams<{ groupSlug?: string; param?: string }>();
  const groupSlug = routeGroupSlug ?? param;
  const { data: group, isLoading, error } = useCatalogGroup(groupSlug);

  useSeoMeta({
    title: group ? `${group.name} — Balgyn` : "Каталог — Balgyn",
    canonical: group ? `${window.location.origin}/catalog/${groupSlug}` : undefined,
  });

  if (isLoading) {
    return (
      <>
        <Container className="pb-8 pt-12 md:pt-16">
          <div className="mb-6 h-2.5 w-40 animate-pulse bg-[--color-surface]" />
          <div className="h-12 w-56 animate-pulse bg-[--color-surface] md:h-16" />
        </Container>
        <div className="pb-16 md:pb-24">
          <Container>
            <div className="grid grid-cols-1 gap-x-4 gap-y-10 sm:grid-cols-2 md:grid-cols-3">
              {[1, 2, 3].map((i) => (
                <div key={i}>
                  <div className="aspect-[4/5] animate-pulse bg-[--color-surface]" />
                  <div className="mt-4 h-3 w-28 animate-pulse bg-[--color-surface]" />
                </div>
              ))}
            </div>
          </Container>
        </div>
      </>
    );
  }

  if (error || !group) {
    return (
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-danger]">{t("catalog.groupNotFound")}</p>
          <Link to="/catalog" className="mt-4 inline-block text-sm text-[--color-muted] hover:text-black">
            {t("catalog.backToCatalog")}
          </Link>
        </Container>
      </div>
    );
  }

  return (
    <>
      {/* ── Header — light editorial ─────────────────────────── */}
      <Container className="pb-8 pt-12 md:pt-16">
        <nav className="mb-6 flex flex-wrap items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">{t("nav.home")}</Link>
          <span aria-hidden>/</span>
          <Link to="/catalog" className="transition-colors hover:text-black">{t("nav.catalog")}</Link>
          <span aria-hidden>/</span>
          <span className="text-black">{localizeName(group, i18n.language)}</span>
        </nav>
        <h1 className="display text-[44px] uppercase text-black md:text-[64px]">
          {localizeName(group, i18n.language)}
        </h1>
      </Container>

      {/* ── Collections grid ────────────────────────────────── */}
      <div className="pb-16 md:pb-24">
        <Container>
          {group.collections.length > 0 ? (
            <div className="grid grid-cols-1 gap-x-4 gap-y-10 sm:grid-cols-2 md:grid-cols-3">
              {group.collections.map((col) => (
                <CatalogCard
                  key={col.id}
                  to={`/catalog/${groupSlug}/${col.slug}`}
                  title={localizeName(col, i18n.language)}
                  cover={col.coverImageUrl}
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
                <p className="mb-1.5 text-[16px] font-medium text-black">{t("catalog.collectionsEmptyTitle")}</p>
                <p className="text-[14px] leading-relaxed text-[--color-muted]">
                  {t("catalog.collectionsEmptySubtitle")}
                </p>
              </div>
              <Link
                to="/catalog"
                className="inline-flex items-center justify-center bg-black px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
              >
                {t("catalog.backToCatalog")}
              </Link>
            </div>
          )}
        </Container>
      </div>
    </>
  );
}
