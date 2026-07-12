import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import { getCatalogDesigns } from "@/shared/api/backend-api";

export function BestSellers() {
  const { t } = useTranslation();
  const { data, isPending } = useQuery({
    queryKey: ["catalog", "designs", "all"],
    queryFn: () => getCatalogDesigns(),
    staleTime: 5 * 60 * 1000,
  });
  const designs = (data ?? []).slice(0, 4);

  return (
    <section className="relative overflow-hidden py-24 md:py-32">

      <div className="container mx-auto px-4 md:px-8">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.7 }}
          className="mb-14 flex flex-col justify-between gap-5 md:flex-row md:items-end"
        >
          <h2 className="display text-[38px] uppercase sm:text-[52px] md:text-[76px]">
            {t("home.bestSellers.title")}
          </h2>
          <p className="max-w-[320px] text-[15px] leading-relaxed text-[--color-muted]">
            {t("home.bestSellers.desc")}
          </p>
        </motion.div>

        {isPending ? (
          <div className="grid grid-cols-2 gap-4 md:gap-6 lg:grid-cols-4">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="aspect-[3/4] skeleton-shimmer" />
            ))}
          </div>
        ) : designs.length === 0 ? (
          <p className="text-[#7A7A7A]">{t("home.bestSellers.empty")}</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 md:gap-6 lg:grid-cols-4">
            {designs.map((design, i) => (
              <motion.div
                key={design.id}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-60px" }}
                transition={{ duration: 0.6, delay: i * 0.08 }}
              >
                <Link
                  to={`/catalog/${design.groupSlug}/${design.collectionSlug}/${design.slug}`}
                  className="group block"
                >
                  <div className="relative aspect-[4/5] overflow-hidden bg-[--color-surface]">
                    {design.mainImageUrl ? (
                      <img
                        src={design.mainImageUrl}
                        alt={design.name}
                        className="gallery-img absolute inset-0 h-full w-full object-cover"
                      />
                    ) : (
                      <div className="absolute inset-0 flex items-center justify-center text-[96px] font-semibold text-black/[0.08]">
                        {design.name.charAt(0).toUpperCase()}
                      </div>
                    )}
                    <span className="absolute right-3 top-3 text-[11px] tabular-nums text-black/40">
                      {String(i + 1).padStart(2, "0")}
                    </span>
                  </div>
                  <div className="mt-3.5 flex flex-col gap-1">
                    <span className="text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
                      {design.collectionName}
                    </span>
                    <h4 className="text-[13px] font-medium">
                      <span className="link-underline">{design.name}</span>
                    </h4>
                  </div>
                </Link>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
