import { Link } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";

/**
 * Карточка группы/коллекции каталога (обложка + hover-стрелка).
 * Показывает только реальную обложку из админки. Если её нет —
 * аккуратный монограммный плейсхолдер, без временных/тестовых изображений.
 */
export function CatalogCard({
  to,
  title,
  cover,
  hint = "Смотреть →",
}: {
  to: string;
  title: string;
  cover?: string | null;
  hint?: string;
}) {
  return (
    <Link to={to} className="group block cursor-pointer">
      <div className="relative mb-4 aspect-[4/5] overflow-hidden bg-[--color-surface]">
        {cover ? (
          <img
            src={cover}
            alt={title}
            className="gallery-img h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-[--color-surface]">
            <span className="select-none text-[96px] font-bold uppercase leading-none tracking-tight text-black/[0.11]">
              {title.charAt(0)}
            </span>
          </div>
        )}
        <div className="absolute inset-0 bg-black/0 transition-colors duration-700 group-hover:bg-black/[0.06]" />
        <div className="absolute bottom-4 right-4 flex h-10 w-10 translate-y-1 items-center justify-center bg-white opacity-0 transition-all duration-500 group-hover:translate-y-0 group-hover:opacity-100">
          <ArrowUpRight className="h-4 w-4" strokeWidth={1.5} />
        </div>
      </div>
      <div className="flex flex-col items-start gap-1.5">
        <h3 className="text-[20px] font-semibold uppercase tracking-[-0.01em] md:text-[26px]">
          {title}
        </h3>
        <span className="text-[11px] uppercase tracking-[0.2em] text-[--color-muted]">
          {hint}
        </span>
      </div>
    </Link>
  );
}
