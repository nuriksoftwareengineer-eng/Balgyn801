import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCart } from "@/app/use-cart";
import type { Product } from "@/shared/api/client";
import { Price } from "@/shared/ui/price";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/components/ui/button";
import { ProductImage } from "@/widgets/ProductImage";

export function ProductCard({ product }: { product: Product }) {
  const { t } = useTranslation();
  const { addItem } = useCart();
  const navigate = useNavigate();
  const hasVariants =
    (product.sizes?.length ?? 0) > 0 || (product.colors?.length ?? 0) > 0;

  return (
    <article className="group overflow-hidden border border-[--color-border] bg-white transition-[border-color,transform] duration-300 hover:-translate-y-px hover:border-black">
      <Link
        to={`/catalog/${product.id}`}
        className="block focus-visible:outline focus-visible:outline-2 focus-visible:outline-black focus-visible:outline-offset-[-2px]"
      >
        <div className="relative aspect-[4/5] bg-[--color-surface]">
          <ProductImage product={product} />
        </div>
      </Link>

      <div className="p-4">
        <Link to={`/catalog/${product.id}`} className="block focus-visible:outline-none">
          <h3 className="text-sm font-semibold leading-snug text-black">
            {product.title}
          </h3>
        </Link>

        <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
          <span className="text-base font-bold text-black">
            <Price kzt={product.price} />
          </span>
          <span
            className={cn(
              "text-[0.6rem] uppercase tracking-[0.1em]",
              product.inStock ? "text-[--color-muted]" : "text-[--color-danger]",
            )}
          >
            {product.inStock ? t("product.inStock") : t("product.outOfStock")}
          </span>
        </div>

        <Button
          variant="outline"
          size="sm"
          className="mt-3 w-full"
          disabled={!product.inStock}
          onClick={() => {
            if (!product.inStock) return;
            if (hasVariants) {
              navigate(`/catalog/${product.id}`);
              return;
            }
            addItem(product);
          }}
        >
          {product.inStock
            ? hasVariants
              ? t("product.selectSize")
              : t("product.addToCart")
            : t("product.comingSoon")}
        </Button>
      </div>
    </article>
  );
}
