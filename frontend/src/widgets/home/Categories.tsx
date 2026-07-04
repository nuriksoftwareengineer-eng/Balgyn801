import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowUpRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useCatalogGroups } from "@/shared/api/catalog-api";

/** «Каталог» — реальные группы каталога с обложками. */
export function Categories() {
  const { t } = useTranslation();
  const { data, isPending } = useCatalogGroups();
  const groups = data ?? [];

  return (
    <section className="relative bg-[#F5F5F5] py-24 md:py-32">
      <div className="container mx-auto px-4 md:px-8">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.7 }}
          className="mb-12 max-w-3xl"
        >
          <h2 className="text-[40px] font-extrabold uppercase leading-[1.1] tracking-[-0.04em] sm:text-[48px] md:text-[88px]">
            {t("nav.catalog")}
          </h2>
        </motion.div>

        {isPending ? (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-3 md:gap-8">
            {[0, 1, 2].map((i) => (
              <div key={i} className="aspect-[4/5] skeleton-shimmer" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-3 md:gap-8">
            {groups.map((g, i) => (
              <motion.div
                key={g.id}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-60px" }}
                transition={{ duration: 0.6, delay: i * 0.1 }}
              >
                <Link to={`/catalog/${g.slug}`} className="group block cursor-pointer">
                  <div className="relative mb-4 aspect-[4/5] overflow-hidden border border-[#E6E6E6] bg-zinc-900">
                    {g.coverImageUrl ? (
                      <img
                        src={g.coverImageUrl}
                        alt={g.name}
                        className="h-full w-full object-cover transition-transform duration-[1200ms] ease-out group-hover:scale-110"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center">
                        <span className="text-7xl font-bold uppercase text-white/10 select-none">
                          {g.name.charAt(0)}
                        </span>
                      </div>
                    )}
                    <div className="absolute inset-0 bg-black/0 transition-colors duration-500 group-hover:bg-black/15" />
                    <div className="absolute bottom-4 right-4 flex h-10 w-10 translate-y-2 items-center justify-center rounded-full bg-white opacity-0 transition-all duration-500 group-hover:translate-y-0 group-hover:opacity-100">
                      <ArrowUpRight className="h-5 w-5" />
                    </div>
                  </div>
                  <div className="flex flex-col items-start gap-1">
                    <h3 className="text-[22px] font-bold uppercase tracking-[-0.02em] md:text-[28px]">
                      {g.name}
                    </h3>
                    <span className="text-[12px] uppercase tracking-[0.2em] text-[#7A7A7A]">
                      {t("home.categories.viewAll")}
                    </span>
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
