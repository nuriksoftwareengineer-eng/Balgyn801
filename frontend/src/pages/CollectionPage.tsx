import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogCollection } from "@/shared/api/catalog-api";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { DesignGrid } from "@/widgets/DesignGrid";
import { Container } from "@/shared/ui/container";

export function CollectionPage() {
  const { t } = useTranslation();
  const { groupSlug, collectionSlug } = useParams<{
    groupSlug: string;
    collectionSlug: string;
  }>();
  const { data: collection, isLoading, error } = useCatalogCollection(collectionSlug);

  useSeoMeta({
    title: collection ? `${collection.name} — Balgyn` : "Коллекция — Balgyn",
    canonical:
      collection
        ? `${window.location.origin}/catalog/${groupSlug}/${collectionSlug}`
        : undefined,
  });

  if (isLoading) {
    return (
      <>
        <div className="border-b border-[--color-border] bg-black">
          <Container className="py-14 md:py-20">
            <div className="mb-5 h-2 w-40 animate-pulse bg-white/10" />
            <div className="h-10 w-56 animate-pulse bg-white/10 md:h-14" />
          </Container>
        </div>
        <div className="py-10 md:py-14">
          <Container>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="overflow-hidden">
                  <div className="aspect-square animate-pulse bg-[--color-surface]" />
                  <div className="mt-2 h-2 w-20 animate-pulse bg-[--color-surface]" />
                </div>
              ))}
            </div>
          </Container>
        </div>
      </>
    );
  }

  if (error || !collection) {
    return (
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-danger]">{t("catalog.collectionNotFound")}</p>
          <Link
            to={groupSlug ? `/catalog/${groupSlug}` : "/catalog"}
            className="mt-4 inline-block text-sm text-[--color-muted] hover:text-black"
          >
            {t("catalog.back")}
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
            <Link to={`/catalog/${groupSlug}`} className="transition hover:text-white/70">
              {collection.groupName}
            </Link>
            <span>/</span>
            <span className="text-white/70">{collection.name}</span>
          </nav>
          <h1 className="text-5xl font-semibold uppercase tracking-[0.04em] text-white md:text-7xl">
            {collection.name}
          </h1>
        </Container>
      </div>

      {/* ── Designs grid ────────────────────────────────────── */}
      <div className="py-10 md:py-14">
        <Container>
          <DesignGrid
            designs={collection.designs}
            groupSlug={groupSlug ?? ""}
            collectionSlug={collectionSlug ?? ""}
          />
        </Container>
      </div>
    </>
  );
}
