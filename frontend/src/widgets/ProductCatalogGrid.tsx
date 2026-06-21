import { motion, useReducedMotion } from "framer-motion";
import { useProducts } from "@/shared/api/queries";
import { ProductCard } from "@/widgets/ProductCard";

type GridVariant = "default" | "catalog";

const GRID_CLASSES: Record<GridVariant, string> = {
  default: "gap-[22px] sm:grid-cols-2 lg:grid-cols-[repeat(auto-fill,minmax(260px,1fr))]",
  catalog: "gap-5 sm:grid-cols-2 lg:grid-cols-3",
};

function SkeletonGrid({ variant }: { variant: GridVariant }) {
  return (
    <div className={`grid ${GRID_CLASSES[variant]}`}>
      {Array.from({ length: variant === "catalog" ? 6 : 6 }).map((_, i) => (
        <div key={i} className="overflow-hidden border border-[--color-border] bg-white">
          <div className="skeleton-shimmer aspect-[4/5] bg-[--color-surface]" />
          <div className="space-y-3 p-4">
            <div className="skeleton-shimmer h-4 w-3/4 bg-[--color-surface]" />
            <div className="skeleton-shimmer h-4 w-1/2 bg-[--color-surface]" />
            <div className="skeleton-shimmer mt-2 h-9 w-full bg-[--color-surface]" />
          </div>
        </div>
      ))}
    </div>
  );
}

const listVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.055, delayChildren: 0.04 },
  },
};

const cardVariants = {
  hidden: { opacity: 0, y: 14 },
  show: {
    opacity: 1,
    y: 0,
    transition: { type: "spring", stiffness: 380, damping: 26 },
  },
};

export function ProductCatalogGrid({
  categoryFilter = null,
  variant = "default",
}: {
  categoryFilter?: string | null;
  variant?: GridVariant;
}) {
  const { data: products, isPending, isError, error } =
    useProducts(categoryFilter);
  const reduceMotion = useReducedMotion();
  const gridClass = GRID_CLASSES[variant];

  if (isPending) {
    return (
      <div className="py-6">
        <SkeletonGrid variant={variant} />
        <p className="mt-8 text-center text-sm text-[--color-muted]">
          Загружаем каталог…
        </p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="py-12 text-center text-sm font-semibold text-[--color-danger]">
        {error instanceof Error ? error.message : "Ошибка загрузки"}
      </div>
    );
  }

  if (!products?.length) {
    return (
      <p className="py-16 text-center text-sm text-[--color-muted]">
        В этой категории пока нет товаров.
      </p>
    );
  }

  if (reduceMotion) {
    return (
      <div className={`grid ${gridClass}`}>
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    );
  }

  return (
    <motion.div
      className={`grid ${gridClass}`}
      variants={listVariants}
      initial="hidden"
      animate="show"
    >
      {products.map((product) => (
        <motion.div key={product.id} variants={cardVariants}>
          <ProductCard product={product} />
        </motion.div>
      ))}
    </motion.div>
  );
}
