import { useCart } from "@/app/use-cart";
import type { Product } from "@/shared/api/client";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/shared/ui/button";
import { ProductImage } from "@/widgets/ProductImage";

export function ProductCard({ product }: { product: Product }) {
  const { addOne } = useCart();

  return (
    <article className="group overflow-hidden rounded-[14px] border border-white/10 bg-zinc-900 transition hover:-translate-y-1 hover:border-violet-500/35">
      <div className="relative aspect-[4/5] bg-gradient-to-br from-zinc-800 to-zinc-950">
        <ProductImage product={product} />
      </div>
      <div className="p-[18px]">
        <h3 className="mb-1.5 text-base font-semibold leading-snug text-zinc-100">
          {product.title}
        </h3>
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
          className="mt-3.5 w-full rounded-[10px]"
          disabled={!product.inStock}
          onClick={() => product.inStock && addOne()}
        >
          {product.inStock ? "В корзину" : "Ожидаем поступление"}
        </Button>
      </div>
    </article>
  );
}
