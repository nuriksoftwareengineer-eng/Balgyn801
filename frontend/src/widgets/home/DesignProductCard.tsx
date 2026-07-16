import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { Product } from "@/shared/api/types";
import { Price } from "@/shared/ui/price";

/** Карточка товара в стиле дизайна BALGYN. Ведёт на страницу товара (выбор размера/цвета там). */
export function DesignProductCard({
  product,
  index,
}: {
  product: Product;
  index?: number;
}) {
  const { t } = useTranslation();
  const initial = product.title?.trim().charAt(0).toUpperCase() || "B";
  return (
    <Link to={`/catalog/${product.id}`} className="group relative block">
      <div className="relative aspect-[4/5] overflow-hidden bg-[--color-surface]">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.title}
            className="gallery-img absolute inset-0 h-full w-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center text-[96px] font-semibold text-black/[0.08]">
            {initial}
          </div>
        )}

        {index ? (
          <span className="absolute right-3 top-3 text-[11px] tabular-nums text-black/40">
            {String(index).padStart(2, "0")}
          </span>
        ) : null}

        {!product.inStock ? (
          <span className="absolute left-3 top-3 text-[10px] font-medium uppercase tracking-[0.18em] text-black">
            {t("product.soldOut")}
          </span>
        ) : null}

        <div className="absolute inset-x-0 bottom-0 hidden translate-y-2 p-3 opacity-0 transition-all duration-500 group-hover:translate-y-0 group-hover:opacity-100 md:block">
          <span className="flex w-full items-center justify-center gap-2 bg-black px-4 py-3 text-[12px] font-medium uppercase tracking-[0.14em] text-white">
            <Plus className="h-3.5 w-3.5" />
            {t("product.addToCart")}
          </span>
        </div>
      </div>

      <div className="mt-3.5 flex items-start justify-between gap-2">
        <div className="flex flex-col">
          {product.category ? (
            <span className="mb-1 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
              {product.category}
            </span>
          ) : null}
          <h4 className="text-[13px] font-medium">{product.title}</h4>
          {product.description ? (
            <p className="mt-1 line-clamp-1 text-[12px] text-[--color-muted]">
              {product.description}
            </p>
          ) : null}
        </div>
        <p className="whitespace-nowrap text-[13px] font-medium">
          <Price kzt={product.price} />
        </p>
      </div>
    </Link>
  );
}
