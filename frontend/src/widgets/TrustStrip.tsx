import { motion, useReducedMotion } from "framer-motion";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";

export function TrustStrip() {
  const reduceMotion = useReducedMotion();
  const { t } = useTranslation();

  const items = [
    { title: t("home.trust.delivery.title"), desc: t("home.trust.delivery.desc") },
    { title: t("home.trust.embroidery.title"), desc: t("home.trust.embroidery.desc") },
    { title: t("home.trust.quality.title"), desc: t("home.trust.quality.desc") },
  ];

  return (
    <Container className="pb-12">
      <div className="grid gap-4 sm:grid-cols-3">
        {items.map((item, i) => (
          <motion.div
            key={item.title}
            initial={{ opacity: 0, y: reduceMotion ? 0 : 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-40px" }}
            transition={{
              duration: reduceMotion ? 0 : 0.45,
              delay: reduceMotion ? 0 : i * 0.08,
              ease: [0.22, 1, 0.36, 1],
            }}
            className="rounded-[14px] border border-white/10 bg-zinc-900 px-5 py-5 text-center transition-colors hover:border-white/25 hover:bg-zinc-900/95"
          >
            <strong className="mb-1.5 block text-xs font-bold uppercase tracking-[0.06em] text-zinc-100">
              {item.title}
            </strong>
            <span className="text-sm text-zinc-400">{item.desc}</span>
          </motion.div>
        ))}
      </div>
    </Container>
  );
}
