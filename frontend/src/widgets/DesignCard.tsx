import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { DesignSummary } from "@/shared/types/catalog";
import { localizeName } from "@/shared/types/catalog";
import { useWishlist } from "@/app/wishlist-context";

interface DesignCardProps {
  design: DesignSummary & { id?: number };
  groupSlug: string;
  collectionSlug: string;
  isNewArrival?: boolean;
}

function DesignPlaceholder({ name }: { name: string }) {
  return (
    <div className="flex h-full w-full items-center justify-center bg-zinc-900">
      <span className="text-5xl font-bold uppercase text-white/15 select-none">
        {name.charAt(0)}
      </span>
    </div>
  );
}

function HeartButton({ designId }: { designId: number }) {
  const { isInWishlist, toggle } = useWishlist();
  const active = isInWishlist(designId);
  return (
    <button
      type="button"
      onClick={(e) => { e.preventDefault(); e.stopPropagation(); void toggle(designId); }}
      aria-label={active ? "Убрать из избранного" : "Добавить в избранное"}
      className={`absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 shadow transition hover:scale-110 ${active ? "text-red-500" : "text-zinc-400 hover:text-red-400"}`}
    >
      <svg viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth={2} className="h-4 w-4">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
      </svg>
    </button>
  );
}

export function DesignCard({ design, groupSlug, collectionSlug, isNewArrival }: DesignCardProps) {
  const { i18n } = useTranslation();
  const href = `/catalog/${groupSlug}/${collectionSlug}/${design.slug}`;
  const localName = localizeName(design, i18n.language);

  return (
    <Link to={href} className="group block">
      {/* Image */}
      <div className="relative aspect-square w-full overflow-hidden bg-[--color-surface]">
        {design.mainImageUrl ? (
          <img
            src={design.mainImageUrl}
            alt={localName}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <DesignPlaceholder name={localName} />
        )}
        {isNewArrival && (
          <span className="absolute left-2 top-2 rounded bg-black px-2 py-0.5 text-[0.6rem] font-bold uppercase tracking-wider text-white">
            Новинка
          </span>
        )}
        {design.id != null && <HeartButton designId={design.id} />}
      </div>

      {/* Info */}
      <div className="mt-2.5 px-0.5">
        <p className="truncate text-[0.8rem] font-semibold uppercase tracking-[0.04em] text-black group-hover:text-zinc-600 transition-colors">
          {localName}
        </p>
        {design.description ? (
          <p className="mt-0.5 line-clamp-1 text-[0.65rem] text-[--color-muted]">
            {design.description}
          </p>
        ) : null}
      </div>
    </Link>
  );
}
