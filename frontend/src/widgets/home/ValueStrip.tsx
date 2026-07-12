import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import { Truck, ShieldCheck, Sparkles, Paintbrush } from "lucide-react";

export function ValueStrip() {
  const { t } = useTranslation();

  const values = [
    { icon: Truck, title: t("home.values.embroidery.title"), desc: t("home.values.embroidery.desc") },
    { icon: ShieldCheck, title: t("home.values.unique.title"), desc: t("home.values.unique.desc") },
    { icon: Sparkles, title: t("home.values.quality.title"), desc: t("home.values.quality.desc") },
    { icon: Paintbrush, title: t("home.values.limited.title"), desc: t("home.values.limited.desc") },
  ];

  return (
    <section className="border-y border-[--color-border] bg-white">
      <div className="container mx-auto px-4 md:px-8">
        <div className="grid grid-cols-1 divide-y divide-[--color-border] sm:grid-cols-2 sm:divide-y-0 sm:divide-x lg:grid-cols-4">
          {values.map((v, i) => {
            const Icon = v.icon;
            return (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: i * 0.08, ease: [0.16, 1, 0.3, 1] }}
                className="group flex flex-col items-start gap-4 py-10 sm:px-8 md:py-14"
              >
                <Icon className="h-5 w-5 text-black transition-transform duration-500 group-hover:-translate-y-1" strokeWidth={1.5} />
                <h4 className="text-[13px] font-semibold uppercase leading-snug tracking-[0.06em]">
                  {v.title}
                </h4>
                <p className="text-[13px] leading-relaxed text-[--color-muted]">
                  {v.desc}
                </p>
              </motion.div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
