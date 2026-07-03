import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogGroup } from "@/shared/api/catalog-api";
import { localizeName } from "@/shared/types/catalog";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { Container } from "@/shared/ui/container";

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
        <div className="border-b border-[--color-border] bg-black">
          <Container className="py-14 md:py-20">
            <div className="mb-5 h-2 w-32 animate-pulse bg-white/10" />
            <div className="h-10 w-48 animate-pulse bg-white/10 md:h-14" />
          </Container>
        </div>
        <div className="py-10 md:py-14">
          <Container>
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
      {/* ── Hero ────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-14 md:py-20">
          <nav className="mb-5 flex items-center gap-2 text-[0.55rem] uppercase tracking-[0.16em] text-white/40">
            <Link to="/" className="transition hover:text-white/70">{t("nav.home")}</Link>
            <span>/</span>
            <Link to="/catalog" className="transition hover:text-white/70">{t("nav.catalog")}</Link>
            <span>/</span>
            <span className="text-white/70">{localizeName(group, i18n.language)}</span>
          </nav>
          <h1 className="text-5xl font-semibold uppercase tracking-[0.04em] text-white md:text-7xl">
            {localizeName(group, i18n.language)}
          </h1>
        </Container>
      </div>

      {/* ── Collections grid ────────────────────────────────── */}
      <div className="py-10 md:py-14">
        <Container>
          {group.collections.length > 0 ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
              {group.collections.map((col) => (
                <Link
                  key={col.id}
                  to={`/catalog/${groupSlug}/${col.slug}`}
                  className="group block overflow-hidden border border-[--color-border] bg-white transition-shadow hover:shadow-md"
                >
                  <div className="aspect-[3/2] flex items-center justify-center bg-zinc-800">
                    <span className="text-7xl font-bold uppercase text-white/10 select-none">
                      {col.name.charAt(0)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between px-4 py-3.5">
                    <p className="text-sm font-semibold uppercase tracking-[0.06em] text-black">
                      {localizeName(col, i18n.language)}
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
                <p className="mb-1 text-[15px] font-semibold text-black">{t("catalog.collectionsEmptyTitle")}</p>
                <p className="text-[13px] text-[--color-muted]">
                  {t("catalog.collectionsEmptySubtitle")}
                </p>
              </div>
              <Link
                to="/catalog"
                className="inline-block border border-black px-5 py-2.5 text-[11px] font-bold uppercase tracking-[0.14em] text-black transition hover:bg-black hover:text-white"
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
