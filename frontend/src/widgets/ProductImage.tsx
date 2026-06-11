import type { Product } from "@/shared/api/client";

export function ProductImage({ product }: { product: Product }) {
  const letter = product.title?.charAt(0)?.toUpperCase() ?? "?";

  if (product.imageUrl) {
    return (
      <img
        src={product.imageUrl}
        alt={product.title}
        loading="lazy"
        decoding="async"
        className="h-full w-full object-cover"
      />
    );
  }

  return (
    <div className="flex h-full w-full items-center justify-center text-7xl font-semibold text-[--color-border]">
      {letter}
    </div>
  );
}
