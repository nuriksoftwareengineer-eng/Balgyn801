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
    <section className="border-y border-black bg-white">
      <div className="container mx-auto px-4 md:px-8">
        <div className="grid grid-cols-1 divide-y divide-black sm:grid-cols-2 sm:divide-y-0 sm:divide-x lg:grid-cols-4">
          {values.map((v, i) => {
            const Icon = v.icon;
            return (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: i * 0.08 }}
                className="group flex flex-col items-start gap-3 p-6 md:p-8"
              >
                <Icon className="h-5 w-5 transition-transform duration-300 group-hover:-translate-y-1 md:h-6 md:w-6" />
                <h4 className="text-[15px] font-bold uppercase leading-snug tracking-wide md:text-[18px]">
                  {v.title}
                </h4>
                <p className="text-[14px] leading-relaxed text-[#7A7A7A] md:text-[15px]">
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
