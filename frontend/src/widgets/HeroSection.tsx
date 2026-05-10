import { motion, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import heroShowcaseLeft from "@/assets/hero-showcase-left.png";
import heroShowcaseRight from "@/assets/hero-showcase-right.png";
import heroTitleTexture from "@/assets/hero-title-texture.png";
import { Container } from "@/shared/ui/container";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/cn";

/** Тени под текст на фото — чуть мягче, без «грязи» */
const shadowEyebrow =
  "[text-shadow:0_0_18px_rgba(0,0,0,0.88),0_1px_4px_rgba(0,0,0,0.85)]";
const shadowBody =
  "[text-shadow:0_1px_2px_rgba(0,0,0,0.92),0_0_18px_rgba(0,0,0,0.65),0_3px_14px_rgba(0,0,0,0.55)]";

export function HeroSection() {
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();
  const t = reduceMotion ? { duration: 0 } : { duration: 0.55, ease: [0.22, 1, 0.36, 1] as const };

  return (
    <section className="relative min-h-[min(78vh,700px)] overflow-hidden py-11 md:py-14">
      {/* Фото 1 | Фото 2 — чуть компактнее кадр */}
      <div
        className="pointer-events-none absolute inset-0 flex min-h-full"
        aria-hidden
      >
        <div
          className="h-full min-h-[240px] w-1/2 bg-cover bg-[center_top] sm:bg-center"
          style={{ backgroundImage: `url(${heroShowcaseLeft})` }}
        />
        <div
          className="h-full min-h-[240px] w-1/2 bg-cover bg-[center_30%] sm:bg-center"
          style={{ backgroundImage: `url(${heroShowcaseRight})` }}
        />
      </div>

      {/* Вуаль чуть светлее — фото чуть ярче, центр остаётся читаемым */}
      <div
        className="pointer-events-none absolute inset-0 bg-zinc-950/[0.18]"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_92%_78%_at_50%_46%,transparent_22%,rgba(9,9,11,0.28)_58%,rgba(9,9,11,0.46)_100%)]"
        aria-hidden
      />

      <div
        className="pointer-events-none absolute inset-0 opacity-[0.16]"
        aria-hidden
      >
        <div className="hero-aurora absolute -left-1/4 top-0 h-[420px] w-[70%] rounded-full blur-[100px]" />
        <div className="hero-aurora-delay absolute -right-1/4 bottom-0 h-[380px] w-[60%] rounded-full blur-[90px]" />
      </div>

      <Container className="relative z-[1] flex min-h-[min(60vh,540px)] items-center justify-center">
        <div className="w-full max-w-2xl px-4 text-center md:max-w-3xl">
          <motion.p
            className={cn(
              "mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-violet-200",
              shadowEyebrow,
            )}
            initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...t, delay: reduceMotion ? 0 : 0.05 }}
          >
            Вышивка • Стритвир • Под заказ
          </motion.p>
          <motion.h1
            className={cn(
              "font-display mx-auto max-w-4xl text-[clamp(2rem,7.5vw,4.25rem)] leading-[0.98] tracking-[0.03em] md:text-[clamp(2.2rem,8vw,4.45rem)]",
              reduceMotion
                ? "text-zinc-100 [text-shadow:0_2px_16px_rgba(0,0,0,0.95)]"
                : "hero-title-clip",
            )}
            style={
              reduceMotion
                ? undefined
                : {
                    backgroundImage: `url(${heroTitleTexture})`,
                    /* Крупнее кроп яркого принта — меньше «разнотона» по буквам */
                    backgroundSize: "300% auto",
                    backgroundPosition: "54% 40%",
                  }
            }
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
            className={cn(
              "mx-auto mt-4 max-w-md text-[0.98rem] leading-relaxed text-zinc-100/92 sm:max-w-lg sm:text-[1.025rem]",
              shadowBody,
            )}
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
