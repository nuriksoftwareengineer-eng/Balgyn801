import type { DesignSummary } from "@/shared/types/catalog";
import { DesignCard } from "@/widgets/DesignCard";

interface DesignGridProps {
  designs: DesignSummary[];
  groupSlug: string;
  collectionSlug: string;
}

export function DesignGrid({ designs, groupSlug, collectionSlug }: DesignGridProps) {
  if (designs.length === 0) {
    return (
      <p className="text-sm text-[--color-muted]">В коллекции пока нет дизайнов.</p>
    );
  }
  return (
    <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4">
      {designs.map((d) => (
        <DesignCard
          key={d.id}
          design={d}
          groupSlug={groupSlug}
          collectionSlug={collectionSlug}
        />
      ))}
    </div>
  );
}
