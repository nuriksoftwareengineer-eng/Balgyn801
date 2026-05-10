import { Link } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import type { Product } from "@/shared/api/client";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/shared/ui/button";
import { ProductImage } from "@/widgets/ProductImage";

export function ProductCard({ product }: { product: Product }) {
  const { addItem } = useCart();

  return (
    <article className="group overflow-hidden rounded-[14px] border border-white/10 bg-zinc-900 shadow-[0_0_0_0_rgba(0,0,0,0)] transition-[transform,box-shadow,border-color] duration-300 hover:-translate-y-1 hover:border-violet-500/40 hover:shadow-[0_20px_40px_-24px_rgba(139,92,246,0.45)]">
      <Link
        to={`/catalog/${product.id}`}
        className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-violet-500/80 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950"
      >
        <div className="relative aspect-[4/5] bg-gradient-to-br from-zinc-800 to-zinc-950">
          <ProductImage product={product} />
          <span className="pointer-events-none absolute inset-0 bg-gradient-to-t from-zinc-950/60 via-transparent to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
        </div>
      </Link>
      <div className="p-[18px]">
        <Link
          to={`/catalog/${product.id}`}
          className="block text-left focus:outline-none focus-visible:text-violet-300"
        >
          <h3 className="mb-1.5 text-base font-semibold leading-snug text-zinc-100 transition-colors group-hover:text-violet-100">
            {product.title}
          </h3>
        </Link>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span className="text-lg font-bold text-zinc-100">
            {formatMoney(product.price)} ₸
          </span>
          <span
            className={cn(
              "rounded-full px-2.5 py-1 text-[0.6875rem] font-bold uppercase tracking-[0.06em]",
              product.inStock
                ? "bg-green-500/15 text-green-400"
                : "bg-red-500/12 text-red-400",
            )}
          >
            {product.inStock ? "В наличии" : "Нет в наличии"}
          </span>
        </div>
        <Button
          variant="outline"
          className="mt-3.5 w-full rounded-[10px] transition-all hover:border-violet-400/50 hover:bg-violet-500/10"
          disabled={!product.inStock}
          onClick={() => product.inStock && addItem(product)}
        >
          {product.inStock ? "В корзину" : "Ожидаем поступление"}
        </Button>
      </div>
    </article>
  );
}
