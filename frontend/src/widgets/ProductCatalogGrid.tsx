import { motion, useReducedMotion } from "framer-motion";
import { useProducts } from "@/shared/api/queries";
import { ProductCard } from "@/widgets/ProductCard";

function SkeletonGrid() {
  return (
    <div className="grid gap-[22px] sm:grid-cols-2 lg:grid-cols-[repeat(auto-fill,minmax(260px,1fr))]">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="overflow-hidden rounded-[14px] border border-white/10 bg-zinc-900"
        >
          <div className="skeleton-shimmer aspect-[4/5] bg-zinc-800/90" />
          <div className="space-y-3 p-[18px]">
            <div className="skeleton-shimmer h-4 w-3/4 rounded bg-zinc-800" />
            <div className="skeleton-shimmer h-4 w-1/2 rounded bg-zinc-800/80" />
            <div className="skeleton-shimmer mt-2 h-10 w-full rounded-[10px] bg-zinc-800/70" />
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

export function ProductCatalogGrid() {
  const { data: products, isPending, isError, error } = useProducts();
  const reduceMotion = useReducedMotion();

  if (isPending) {
    return (
      <div className="py-6">
        <SkeletonGrid />
        <p className="mt-8 text-center text-sm text-zinc-500">
          Загружаем каталог из API…
        </p>
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
        Пока нет товаров. Добавьте позиции через админку или Swagger (POST /product).
      </p>
    );
  }

  if (reduceMotion) {
    return (
      <div className="grid gap-[22px] sm:grid-cols-2 lg:grid-cols-[repeat(auto-fill,minmax(260px,1fr))]">
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    );
  }

  return (
    <motion.div
      className="grid gap-[22px] sm:grid-cols-2 lg:grid-cols-[repeat(auto-fill,minmax(260px,1fr))]"
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
