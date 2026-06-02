import { motion, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { Container } from "@/shared/ui/container";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/cn";

export function HeroSection() {
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();
  const t = reduceMotion ? { duration: 0 } : { duration: 0.55, ease: [0.22, 1, 0.36, 1] as const };

  return (
    <section className="relative min-h-[min(78vh,700px)] overflow-hidden py-11 md:py-14">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_70%_65%_at_50%_40%,rgba(139,92,246,0.28),rgba(9,9,11,0)_70%)]" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(120deg,rgba(30,27,75,0.45),rgba(9,9,11,0.4),rgba(76,29,149,0.38))]" />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_90%_75%_at_50%_110%,rgba(59,7,100,0.4),transparent_60%)]" />

      <div
        className="pointer-events-none absolute inset-0 opacity-[0.42]"
        aria-hidden
      >
        <div className="hero-aurora absolute -left-1/4 top-0 h-[420px] w-[70%] rounded-full blur-[110px]" />
        <div className="hero-aurora-delay absolute -right-1/4 bottom-0 h-[380px] w-[60%] rounded-full blur-[100px]" />
      </div>

      <Container className="relative z-[1] flex min-h-[min(60vh,540px)] items-center justify-center">
        <div className="w-full max-w-2xl px-4 text-center md:max-w-3xl">
          <motion.p
            className="mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-violet-200/95"
            initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...t, delay: reduceMotion ? 0 : 0.05 }}
          >
            Вышивка • Стритвир • Под заказ
          </motion.p>
          <motion.h1
            className={cn(
              "font-display mx-auto max-w-4xl text-[clamp(2rem,7.5vw,4.25rem)] leading-[0.98] tracking-[0.03em] md:text-[clamp(2.2rem,8vw,4.45rem)]",
              "bg-gradient-to-br from-zinc-50 via-violet-200 to-purple-300 bg-clip-text text-transparent [text-shadow:0_0_26px_rgba(168,85,247,0.25)]",
            )}
            initial={{ opacity: 0, y: reduceMotion ? 0 : 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...t, delay: reduceMotion ? 0 : 0.12 }}
          >
            НОВЫЙ ДРОП —
            <br />
            ОДЕЖДА С
            <br />
            ХАРАКТЕРОМ
          </motion.h1>
          <motion.p
            className="mx-auto mt-4 max-w-md text-[0.98rem] leading-relaxed text-zinc-100/88 sm:max-w-lg sm:text-[1.025rem]"
            initial={{ opacity: 0, y: reduceMotion ? 0 : 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...t, delay: reduceMotion ? 0 : 0.2 }}
          >
            Плотная вышивка и сочные принты на качественном трикотаже. Загляните в каталог
            или пришлите референс — воплотим ваш макет.
          </motion.p>
          <motion.div
            className="mt-8 flex flex-wrap items-center justify-center gap-3"
            initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...t, delay: reduceMotion ? 0 : 0.28 }}
          >
            <Button
              type="button"
              variant="primary"
              className="group rounded-full shadow-[0_0_0_0_rgba(139,92,246,0)] transition-shadow hover:shadow-[0_12px_40px_-12px_rgba(139,92,246,0.65)]"
              onClick={() => navigate("/catalog")}
            >
              Перейти в каталог
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full shadow-[0_4px_24px_rgba(0,0,0,0.5)]"
              onClick={() => navigate("/custom-design")}
            >
              Свой дизайн
            </Button>
          </motion.div>
        </div>
      </Container>
    </section>
  );
}
