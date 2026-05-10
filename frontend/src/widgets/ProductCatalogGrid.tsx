import { useProducts } from "@/shared/api/queries";
import { ProductCard } from "@/widgets/ProductCard";

export function ProductCatalogGrid() {
  const { data: products, isPending, isError, error } = useProducts();

  if (isPending) {
    return (
      <div className="py-12 text-center">
        <div
          className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-[3px] border-white/10 border-t-violet-500"
          aria-hidden
        />
        <p className="m-0 text-zinc-400">Загружаем каталог...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="py-12 text-center font-semibold text-red-400">
        {error instanceof Error ? error.message : "Ошибка загрузки"}
      </div>
    );
  }

  if (!products?.length) {
    return (
      <p className="text-center text-zinc-400">
        Пока нет товаров. Добавьте позиции через API.
      </p>
    );
  }

  return (
    <div className="grid gap-[22px] sm:grid-cols-2 lg:grid-cols-[repeat(auto-fill,minmax(260px,1fr))]">
      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}
