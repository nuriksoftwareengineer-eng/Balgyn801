import { motion, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { Container } from "@/shared/ui/container";
import { Button } from "@/shared/ui/button";

export function HeroSection() {
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();
  const t = reduceMotion ? { duration: 0 } : { duration: 0.55, ease: [0.22, 1, 0.36, 1] as const };

  return (
    <section className="relative overflow-hidden py-14 text-center md:py-[72px]">
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.55]"
        aria-hidden
      >
        <div className="hero-aurora absolute -left-1/4 top-0 h-[420px] w-[70%] rounded-full blur-[100px]" />
        <div className="hero-aurora-delay absolute -right-1/4 bottom-0 h-[380px] w-[60%] rounded-full blur-[90px]" />
      </div>

      <Container className="relative">
        <motion.p
          className="mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-violet-400"
          initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...t, delay: reduceMotion ? 0 : 0.05 }}
        >
          Вышивка • Стритвир • Под заказ
        </motion.p>
        <motion.h1
          className="font-display mx-auto max-w-4xl text-[clamp(3rem,12vw,5.5rem)] leading-[0.95] tracking-[0.02em] text-zinc-100"
          initial={{ opacity: 0, y: reduceMotion ? 0 : 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...t, delay: reduceMotion ? 0 : 0.12 }}
        >
          НОВЫЙ ДРОП — ОДЕЖДА С ХАРАКТЕРОМ
        </motion.h1>
        <motion.p
          className="mx-auto mt-5 max-w-lg text-[1.0625rem] text-zinc-400"
          initial={{ opacity: 0, y: reduceMotion ? 0 : 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...t, delay: reduceMotion ? 0 : 0.2 }}
        >
          Яркие принты и плотная вышивка на качественном материале. Выберите модель из
          каталога или пришлите свой референс для индивидуального дизайна.
        </motion.p>
        <motion.div
          initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...t, delay: reduceMotion ? 0 : 0.28 }}
        >
          <Button
            type="button"
            variant="primary"
            className="group mt-8 rounded-full shadow-[0_0_0_0_rgba(139,92,246,0)] transition-shadow hover:shadow-[0_12px_40px_-12px_rgba(139,92,246,0.65)]"
            onClick={() => navigate("/catalog")}
          >
            Перейти в каталог
          </Button>
        </motion.div>
      </Container>
    </section>
  );
}
