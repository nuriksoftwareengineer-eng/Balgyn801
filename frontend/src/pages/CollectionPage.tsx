import { Link, useParams } from "react-router-dom";
import { useCatalogCollection } from "@/shared/api/catalog-api";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { DesignGrid } from "@/widgets/DesignGrid";
import { Container } from "@/shared/ui/container";

export function CollectionPage() {
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
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-muted]">Загружаем…</p>
        </Container>
      </div>
    );
  }

  if (error || !collection) {
    return (
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-danger]">Коллекция не найдена.</p>
          <Link
            to={groupSlug ? `/catalog/${groupSlug}` : "/catalog"}
            className="mt-4 inline-block text-sm text-[--color-muted] hover:text-black"
          >
            ← Назад
          </Link>
        </Container>
      </div>
    );
  }

  return (
    <div className="py-14">
      <Container>
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.1em] text-[--color-muted]">
          <Link to="/" className="hover:text-black transition">Главная</Link>
          <span>›</span>
          <Link to="/catalog" className="hover:text-black transition">Каталог</Link>
          <span>›</span>
          <Link to={`/catalog/${groupSlug}`} className="hover:text-black transition">
            {collection.groupName}
          </Link>
          <span>›</span>
          <span className="text-black">{collection.name}</span>
        </nav>

        <h1 className="mb-8 text-4xl font-semibold uppercase tracking-[0.04em] text-black">
          {collection.name}
        </h1>

        <DesignGrid
          designs={collection.designs}
          groupSlug={groupSlug ?? ""}
          collectionSlug={collectionSlug ?? ""}
        />
      </Container>
    </div>
  );
}
