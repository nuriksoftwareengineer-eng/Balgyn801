import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { DesignSummary } from "@/shared/types/catalog";
import { DesignCard } from "@/widgets/DesignCard";

interface DesignGridProps {
  designs: DesignSummary[];
  groupSlug: string;
  collectionSlug: string;
}

function DesignsEmptyState() {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-start gap-5 py-10">
      <div className="flex h-16 w-16 items-center justify-center bg-[--color-surface] text-3xl select-none">
        🧵
      </div>
      <div>
        <p className="mb-1 text-[15px] font-semibold text-black">{t("catalog.emptyTitle")}</p>
        <p className="text-[13px] text-[--color-muted]">
          {t("catalog.emptyDesc")}
        </p>
      </div>
      <Link
        to="/custom-design"
        className="inline-block border border-black px-5 py-2.5 text-[11px] font-bold uppercase tracking-[0.14em] text-black transition hover:bg-black hover:text-white"
      >
        {t("catalog.customCta")}
      </Link>
    </div>
  );
}

export function DesignGrid({ designs, groupSlug, collectionSlug }: DesignGridProps) {
  if (designs.length === 0) {
    return <DesignsEmptyState />;
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
