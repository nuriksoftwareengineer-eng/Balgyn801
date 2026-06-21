import { Link } from "react-router-dom";
import type { DesignSummary } from "@/shared/types/catalog";

interface DesignCardProps {
  design: DesignSummary;
  groupSlug: string;
  collectionSlug: string;
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

export function DesignCard({ design, groupSlug, collectionSlug }: DesignCardProps) {
  const href = `/catalog/${groupSlug}/${collectionSlug}/${design.slug}`;

  return (
    <Link to={href} className="group block">
      {/* Image */}
      <div className="aspect-square w-full overflow-hidden bg-[--color-surface]">
        {design.mainImageUrl ? (
          <img
            src={design.mainImageUrl}
            alt={design.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <DesignPlaceholder name={design.name} />
        )}
      </div>

      {/* Info */}
      <div className="mt-2.5 px-0.5">
        <p className="truncate text-[0.8rem] font-semibold uppercase tracking-[0.04em] text-black group-hover:text-zinc-600 transition-colors">
          {design.name}
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
