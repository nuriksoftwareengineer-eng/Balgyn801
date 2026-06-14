import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import type { Product } from "@/shared/api/types";
import { formatMoney } from "@/shared/lib/format-money";

/** Карточка товара в стиле дизайна BALGYN. Ведёт на страницу товара (выбор размера/цвета там). */
export function DesignProductCard({
  product,
  index,
}: {
  product: Product;
  index?: number;
}) {
  const initial = product.title?.trim().charAt(0).toUpperCase() || "B";
  return (
    <Link to={`/catalog/${product.id}`} className="group relative block">
      <div className="relative aspect-[3/4] overflow-hidden border border-[#E6E6E6] bg-[#F5F5F5] transition-colors group-hover:border-black">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.title}
            className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
          />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center text-[96px] font-extrabold text-black/10">
            {initial}
          </div>
        )}

        {index ? (
          <div className="absolute right-4 top-4 bg-white px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.2em] text-black">
            № 0{index}
          </div>
        ) : null}

        <div className="absolute left-4 top-4 flex flex-col gap-2">
          {!product.inStock ? (
            <span className="border border-black bg-white px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-black">
              Распродано
            </span>
          ) : null}
        </div>

        <div className="absolute bottom-0 left-0 hidden w-full translate-y-full p-4 transition-transform duration-500 group-hover:translate-y-0 md:block">
          <span className="flex w-full items-center justify-center gap-2 bg-white/95 px-4 py-3 text-[14px] font-semibold uppercase tracking-[0.08em] text-black backdrop-blur-sm transition-colors group-hover:bg-black group-hover:text-white">
            <Plus className="h-4 w-4" />
            В корзину
          </span>
        </div>
      </div>

      <div className="mt-4 flex items-start justify-between gap-2">
        <div className="flex flex-col">
          {product.category ? (
            <span className="mb-1 text-[10px] uppercase tracking-[0.2em] text-[#7A7A7A]">
              {product.category}
            </span>
          ) : null}
          <h4 className="text-[14px] font-semibold">{product.title}</h4>
          {product.description ? (
            <p className="mt-1 line-clamp-1 text-[12px] text-[#7A7A7A]">
              {product.description}
            </p>
          ) : null}
        </div>
        <p className="whitespace-nowrap text-[14px] font-semibold">
          {formatMoney(product.price)} ₸
        </p>
      </div>
    </Link>
  );
}
