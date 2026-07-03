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
      className={`absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 shadow transition hover:scale-110 ${active ? "text-red-500" : "text-zinc-400 hover:text-red-400"}`}
    >
      <svg viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth={2} className="h-4 w-4">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
      </svg>
    </button>
  );
}

function DesignCard({ d }: { d: DesignResponse }) {
  const href = `/catalog/${d.groupSlug}/${d.collectionSlug}/${d.slug}`;
  return (
    <Link to={href} className="group block">
      <div className="relative aspect-square w-full overflow-hidden bg-[--color-surface]">
        {d.mainImageUrl ? (
          <img src={d.mainImageUrl} alt={d.name} className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105" loading="lazy" />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-zinc-900">
            <span className="text-4xl font-bold text-white/15 uppercase">{d.name.charAt(0)}</span>
          </div>
        )}
        <span className="absolute left-2 top-2 rounded bg-black px-2 py-0.5 text-[0.6rem] font-bold uppercase tracking-wider text-white">NEW</span>
        <HeartBtn designId={d.id} />
      </div>
      <p className="mt-2 truncate text-[0.8rem] font-semibold uppercase tracking-[0.04em] text-black group-hover:text-zinc-600 transition-colors">
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
