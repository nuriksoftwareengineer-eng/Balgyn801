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
    <div className="flex h-full w-full items-center justify-center bg-[--color-surface]">
      <span className="text-6xl font-semibold uppercase text-black/[0.08] select-none">
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
      className={`absolute right-3 top-3 flex h-8 w-8 items-center justify-center opacity-0 transition-all duration-300 group-hover:opacity-100 focus-visible:opacity-100 ${active ? "text-black opacity-100" : "text-black/50 hover:text-black"}`}
    >
      <svg viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth={1.6} className="h-[18px] w-[18px]">
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
      {/* Image — borderless gallery frame */}
      <div className="relative aspect-[4/5] w-full overflow-hidden bg-[--color-surface]">
        {design.mainImageUrl ? (
          <img
            src={design.mainImageUrl}
            alt={localName}
            className="gallery-img h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <DesignPlaceholder name={localName} />
        )}
        {isNewArrival && (
          <span className="absolute left-3 top-3 text-[10px] font-medium uppercase tracking-[0.18em] text-black">
            New
          </span>
        )}
        {design.id != null && <HeartButton designId={design.id} />}
      </div>

      {/* Info */}
      <div className="mt-3.5">
        <p className="truncate text-[13px] font-medium tracking-[0.01em] text-black">
          {localName}
        </p>
        {design.description ? (
          <p className="mt-1 line-clamp-1 text-[12px] text-[--color-muted]">
            {design.description}
          </p>
        ) : null}
      </div>
    </Link>
  );
}
