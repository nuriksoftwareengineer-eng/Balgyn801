import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogCollection } from "@/shared/api/catalog-api";
import { localizeName } from "@/shared/types/catalog";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { DesignGrid } from "@/widgets/DesignGrid";
import { Container } from "@/shared/ui/container";

export function CollectionPage() {
  const { t, i18n } = useTranslation();
  const { groupSlug, collectionSlug } = useParams<{
    groupSlug: string;
    collectionSlug: string;
  }>();
  const { data: collection, isLoading, error } = useCatalogCollection(collectionSlug);

  useSeoMeta({
    title: collection ? localizeName(collection, i18n.language) : t("nav.catalog"),
    canonical:
      collection
        ? `${window.location.origin}/catalog/${groupSlug}/${collectionSlug}`
        : undefined,
  });

  if (isLoading) {
    return (
      <>
        <Container className="pb-8 pt-12 md:pt-16">
          <div className="mb-6 h-2.5 w-48 animate-pulse bg-[--color-surface]" />
          <div className="h-12 w-64 animate-pulse bg-[--color-surface] md:h-16" />
        </Container>
        <div className="pb-16 md:pb-24">
          <Container>
            <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 md:grid-cols-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i}>
                  <div className="aspect-[4/5] animate-pulse bg-[--color-surface]" />
                  <div className="mt-3.5 h-3 w-24 animate-pulse bg-[--color-surface]" />
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
      {/* ── Header — light editorial ─────────────────────────── */}
      <Container className="pb-8 pt-12 md:pt-16">
        <nav className="mb-6 flex flex-wrap items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">{t("nav.home")}</Link>
          <span aria-hidden>/</span>
          <Link to="/catalog" className="transition-colors hover:text-black">{t("nav.catalog")}</Link>
          <span aria-hidden>/</span>
          <Link to={`/catalog/${groupSlug}`} className="transition-colors hover:text-black">
            {localizeName({ name: collection.groupName, nameKk: collection.groupNameKk, nameEn: collection.groupNameEn }, i18n.language)}
          </Link>
          <span aria-hidden>/</span>
          <span className="text-black">{localizeName(collection, i18n.language)}</span>
        </nav>
        <h1 className="display text-[44px] uppercase text-black md:text-[64px]">
          {localizeName(collection, i18n.language)}
        </h1>
      </Container>

      {/* ── Designs grid ────────────────────────────────────── */}
      <div className="pb-16 md:pb-24">
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
