import { motion, useReducedMotion } from "framer-motion";
import { Container } from "@/shared/ui/container";

const items = [
  { title: "Доставка", desc: "По Казахстану и за рубеж при необходимости" },
  { title: "Вышивка", desc: "Плотные стежки, стойкие нити" },
  { title: "Качество", desc: "Проверка перед отправкой" },
];

export function TrustStrip() {
  const reduceMotion = useReducedMotion();

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
