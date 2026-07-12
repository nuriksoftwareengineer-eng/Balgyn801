import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { getNewArrivals } from "@/shared/api/backend-api";
import type { DesignResponse } from "@/shared/api/types";
import { useWishlist } from "@/app/wishlist-context";

function HeartBtn({ designId }: { designId: number }) {
  const { isInWishlist, toggle } = useWishlist();
  const active = isInWishlist(designId);
  return (
    <button
      type="button"
      onClick={(e) => { e.preventDefault(); e.stopPropagation(); void toggle(designId); }}
      className={`absolute right-3 top-3 flex h-8 w-8 items-center justify-center opacity-0 transition-all duration-300 group-hover:opacity-100 ${active ? "text-black opacity-100" : "text-black/50 hover:text-black"}`}
    >
      <svg viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth={1.6} className="h-[18px] w-[18px]">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
      </svg>
    </button>
  );
}

function DesignCard({ d }: { d: DesignResponse }) {
  const href = `/catalog/${d.groupSlug}/${d.collectionSlug}/${d.slug}`;
  return (
    <Link to={href} className="group block">
      <div className="relative aspect-[4/5] w-full overflow-hidden bg-[--color-surface]">
        {d.mainImageUrl ? (
          <img src={d.mainImageUrl} alt={d.name} className="gallery-img h-full w-full object-cover" loading="lazy" />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <span className="text-5xl font-semibold uppercase text-black/[0.08]">{d.name.charAt(0)}</span>
          </div>
        )}
        <span className="absolute left-3 top-3 text-[10px] font-medium uppercase tracking-[0.18em] text-black">New</span>
        <HeartBtn designId={d.id} />
      </div>
      <p className="mt-3.5 truncate text-[13px] font-medium text-black">
        {d.name}
      </p>
    </Link>
  );
}

export function NewArrivalsSection() {
  const { t } = useTranslation();
  const { data } = useQuery({ queryKey: ["new-arrivals"], queryFn: () => getNewArrivals(8), staleTime: 5 * 60_000 });

  if (!data || data.length === 0) return null;

  return (
    <section className="py-14 md:py-20">
      <div className="mx-auto max-w-[1440px] px-4 md:px-6">
        <div className="mb-8 flex items-center justify-between">
          <h2 className="text-xl font-semibold uppercase tracking-[0.12em]">{t("home.newArrivals", "Новинки")}</h2>
          <Link to="/catalog" className="text-xs font-semibold uppercase tracking-widest text-zinc-500 hover:text-black transition">
            {t("home.viewAll", "Смотреть все")} →
          </Link>
        </div>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4">
          {data.slice(0, 8).map(d => <DesignCard key={d.id} d={d} />)}
        </div>
      </div>
    </section>
  );
}
