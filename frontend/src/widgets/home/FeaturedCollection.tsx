import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getProducts } from "@/shared/api/backend-api";
import { DesignProductCard } from "./DesignProductCard";

export function FeaturedCollection() {
  const { t } = useTranslation();
  const { data, isPending } = useQuery({
    queryKey: ["products", "all"],
    queryFn: () => getProducts(),
    staleTime: 5 * 60 * 1000,
  });
  const products = (data ?? []).slice(0, 3);

  return (
    <section className="py-24 md:py-32">
      <div className="container mx-auto px-4 md:px-8">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.7 }}
          className="mb-12 flex flex-col justify-between gap-6 md:flex-row md:items-end"
        >
          <h2 className="text-[40px] font-extrabold uppercase leading-[1.1] tracking-[-0.04em] sm:text-[48px] md:text-[88px]">
            {t("home.featured.tag")}
          </h2>
          <Link
            to="/catalog"
            className="group hidden items-center self-end text-[20px] font-semibold uppercase tracking-[0.08em] md:flex"
          >
            {t("home.featured.viewAll")}
            <ArrowRight className="ml-2 h-5 w-5 transition-transform group-hover:translate-x-1" />
          </Link>
        </motion.div>

        {isPending ? (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 md:gap-8 lg:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="aspect-[3/4] skeleton-shimmer" />
            ))}
          </div>
        ) : products.length > 0 ? (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 md:gap-8 lg:grid-cols-3">
            {products.map((p, i) => (
              <motion.div
                key={p.id}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-60px" }}
                transition={{ duration: 0.6, delay: i * 0.1 }}
              >
                <DesignProductCard product={p} index={i + 1} />
              </motion.div>
            ))}
          </div>
        ) : (
          <p className="text-[#7A7A7A]">{t("home.featured.empty")}</p>
        )}

        <div className="mt-8 md:hidden">
          <Link
            to="/catalog"
            className="flex h-[56px] w-full items-center justify-center bg-black text-[14px] font-semibold uppercase tracking-[0.08em] text-white"
          >
            {t("home.featured.viewAll")}
          </Link>
        </div>
      </div>
    </section>
  );
}
