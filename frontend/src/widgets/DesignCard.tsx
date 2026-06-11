import { Link } from "react-router-dom";
import type { DesignSummary } from "@/shared/types/catalog";

interface DesignCardProps {
  design: DesignSummary;
  groupSlug: string;
  collectionSlug: string;
}

function DesignPlaceholder({ name }: { name: string }) {
  const initial = name.charAt(0).toUpperCase();
  return (
    <div className="flex h-full w-full items-center justify-center bg-black">
      <span className="text-4xl font-semibold text-white">{initial}</span>
    </div>
  );
}

export function DesignCard({ design, groupSlug, collectionSlug }: DesignCardProps) {
  const href = `/catalog/${groupSlug}/${collectionSlug}/${design.slug}`;

  return (
    <Link
      to={href}
      className="group block border border-[--color-border] bg-white transition-shadow hover:shadow-sm"
    >
      <div className="aspect-square w-full overflow-hidden bg-[--color-surface]">
        {design.mainImageUrl ? (
          <img
            src={design.mainImageUrl}
            alt={design.name}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <DesignPlaceholder name={design.name} />
        )}
      </div>
      <div className="px-3 py-3">
        <p className="truncate text-sm font-medium text-black">{design.name}</p>
        {design.description ? (
          <p className="mt-0.5 line-clamp-2 text-[0.65rem] text-[--color-muted]">
            {design.description}
          </p>
        ) : null}
      </div>
    </Link>
  );
}
