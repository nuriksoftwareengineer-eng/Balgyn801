import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { getCatalogDesigns } from "@/shared/api/backend-api";

export function BestSellers() {
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
          className="mb-12 flex flex-col justify-between gap-4 md:flex-row md:items-end"
        >
          <h2 className="text-[40px] font-extrabold uppercase leading-[1.1] tracking-[-0.04em] sm:text-[48px] md:text-[88px]">
            Хиты
          </h2>
          <p className="max-w-[300px] text-[16px] text-black">
            Подобрано редакцией бренда на основе откликов и продаж за последний месяц.
          </p>
        </motion.div>

        {isPending ? (
          <div className="grid grid-cols-2 gap-4 md:gap-6 lg:grid-cols-4">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="aspect-[3/4] skeleton-shimmer" />
            ))}
          </div>
        ) : designs.length === 0 ? (
          <p className="text-[#7A7A7A]">Скоро здесь появятся хиты продаж.</p>
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
                  <div className="relative aspect-[3/4] overflow-hidden border border-[#E6E6E6] bg-[#F5F5F5] transition-colors group-hover:border-black">
                    {design.mainImageUrl ? (
                      <img
                        src={design.mainImageUrl}
                        alt={design.name}
                        className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
                      />
                    ) : (
                      <div className="absolute inset-0 flex items-center justify-center text-[96px] font-extrabold text-black/10">
                        {design.name.charAt(0).toUpperCase()}
                      </div>
                    )}
                    <div className="absolute right-4 top-4 bg-white px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.2em] text-black">
                      № 0{i + 1}
                    </div>
                  </div>
                  <div className="mt-4 flex flex-col gap-1">
                    <span className="text-[10px] uppercase tracking-[0.2em] text-[#7A7A7A]">
                      {design.collectionName}
                    </span>
                    <h4 className="text-[14px] font-semibold group-hover:underline">
                      {design.name}
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
