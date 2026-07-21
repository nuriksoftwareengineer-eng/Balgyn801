import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import { Scissors, Truck, ShieldCheck } from "lucide-react";

/** Production/Delivery cards are backend-driven (site_settings) — see DesignPage.
 *  Secure checkout is a static, non-admin-editable card per spec, still routed
 *  through i18n rather than inlined, matching the rest of the app's convention. */
export function DesignInfoStrip({
  productionText,
  deliveryText,
}: {
  productionText: string;
  deliveryText: string;
}) {
  const { t } = useTranslation();

  const cards = [
    { icon: Scissors, title: t("design.info.production"), desc: productionText },
    { icon: Truck, title: t("design.info.delivery"), desc: deliveryText },
    { icon: ShieldCheck, title: t("design.info.secureCheckout"), desc: t("design.info.secureCheckoutDesc") },
  ];

  return (
    <section className="border-y border-[--color-border] bg-white">
      <div className="container mx-auto px-4 md:px-8">
        <div className="grid grid-cols-1 divide-y divide-[--color-border] sm:grid-cols-3 sm:divide-y-0 sm:divide-x">
          {cards.map((c, i) => (
            <motion.div
              key={c.title}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: i * 0.08, ease: [0.16, 1, 0.3, 1] }}
              className="group flex flex-col items-start gap-4 py-10 sm:px-8 md:py-14"
            >
              <c.icon className="h-5 w-5 text-black transition-transform duration-500 group-hover:-translate-y-1" strokeWidth={1.5} />
              <h4 className="text-[13px] font-semibold uppercase leading-snug tracking-[0.06em]">
                {c.title}
              </h4>
              <p className="whitespace-pre-line text-[13px] leading-relaxed text-[--color-muted]">
                {c.desc}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
