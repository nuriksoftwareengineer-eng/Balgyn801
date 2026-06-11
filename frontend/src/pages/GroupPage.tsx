import { Link, useParams } from "react-router-dom";
import { useCatalogGroup } from "@/shared/api/catalog-api";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { Container } from "@/shared/ui/container";

export function GroupPage() {
  // groupSlug is set when rendered at /catalog/:groupSlug/:collectionSlug/...
  // param is set when rendered via CatalogParamPage at /catalog/:param
  const { groupSlug: routeGroupSlug, param } = useParams<{ groupSlug?: string; param?: string }>();
  const groupSlug = routeGroupSlug ?? param;
  const { data: group, isLoading, error } = useCatalogGroup(groupSlug);

  useSeoMeta({
    title: group ? `${group.name} — Balgyn` : "Каталог — Balgyn",
    canonical: group ? `${window.location.origin}/catalog/${groupSlug}` : undefined,
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

  if (error || !group) {
    return (
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-danger]">Группа не найдена.</p>
          <Link to="/catalog" className="mt-4 inline-block text-sm text-[--color-muted] hover:text-black">
            ← В каталог
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
          <span className="text-black">{group.name}</span>
        </nav>

        <h1 className="mb-8 text-4xl font-semibold uppercase tracking-[0.04em] text-black">
          {group.name}
        </h1>

        {group.collections.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
            {group.collections.map((col) => (
              <Link
                key={col.id}
                to={`/catalog/${groupSlug}/${col.slug}`}
                className="group flex flex-col justify-between border border-[--color-border] bg-white px-6 py-8 transition-shadow hover:shadow-sm"
              >
                <p className="text-lg font-semibold text-black group-hover:underline underline-offset-4">
                  {col.name}
                </p>
                <p className="mt-4 text-[0.6rem] font-medium uppercase tracking-[0.12em] text-[--color-muted] transition group-hover:text-black">
                  Смотреть →
                </p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="text-sm text-[--color-muted]">В этой группе пока нет коллекций.</p>
        )}
      </Container>
    </div>
  );
}
