import { useNavigate } from "react-router-dom";
import { motion, useReducedMotion } from "framer-motion";
import { Container } from "@/shared/ui/container";
import { Button } from "@/components/ui/button";

export function HeroSection() {
  const navigate = useNavigate();
  const rm = useReducedMotion();
  const dur = rm ? 0 : 0.65;
  const ease = [0.22, 1, 0.36, 1] as const;

  return (
    <section className="relative flex min-h-screen items-center justify-center overflow-hidden bg-white">
      {/* ── Decorative background ── */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
        {/* Dot grid */}
        <div
          className="absolute inset-0 opacity-40"
          style={{
            backgroundImage:
              "radial-gradient(circle, rgba(0,0,0,0.1) 1px, transparent 1px)",
            backgroundSize: "32px 32px",
          }}
        />
        {/* Ring 1 — large, upper right */}
        <motion.div
          className="absolute right-[-12%] top-[-18%] h-[70vmax] w-[70vmax] rounded-full border border-black/[0.05]"
          animate={rm ? {} : { rotate: 360 }}
          transition={{ duration: 90, repeat: Infinity, ease: "linear" }}
        />
        {/* Ring 2 — large, lower left */}
        <motion.div
          className="absolute bottom-[-8%] left-[-8%] h-[55vmax] w-[55vmax] rounded-full border border-black/[0.04]"
          animate={rm ? {} : { rotate: -360 }}
          transition={{ duration: 120, repeat: Infinity, ease: "linear" }}
        />
        {/* Ring 3 — small, inner right */}
        <motion.div
          className="absolute right-[6%] top-[10%] h-[28vmax] w-[28vmax] rounded-full border border-black/[0.04]"
          animate={rm ? {} : { rotate: 360 }}
          transition={{ duration: 65, repeat: Infinity, ease: "linear" }}
        />
      </div>

      {/* ── Content ── */}
      <Container className="relative z-10 pb-28 pt-20">
        <div className="flex flex-col items-center text-center">
          {/* Eyebrow */}
          <motion.p
            className="text-[0.6rem] font-medium uppercase tracking-[0.32em] text-[--color-muted]"
            initial={{ opacity: 0, y: rm ? 0 : 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: dur, ease, delay: rm ? 0 : 0.08 }}
          >
            Вышивка · Казахстан
          </motion.p>

          {/* Brand name — primary focal point */}
          <motion.h1
            className="mt-4 font-sans font-semibold uppercase text-black"
            style={{
              fontSize: "clamp(3.75rem, 18vw, 13.5rem)",
              lineHeight: 1,
              letterSpacing: "0.14em",
            }}
            initial={{ opacity: 0, y: rm ? 0 : 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: dur, ease, delay: rm ? 0 : 0.16 }}
          >
            BALGYN
          </motion.h1>

          {/* Rule */}
          <motion.div
            className="mt-8 h-px w-12 bg-black"
            initial={{ scaleX: 0 }}
            animate={{ scaleX: 1 }}
            transition={{ duration: rm ? 0 : 0.5, ease, delay: rm ? 0 : 0.32 }}
            style={{ transformOrigin: "center" }}
          />

          {/* Sub-headline */}
          <motion.p
            className="mt-5 text-[0.7rem] uppercase tracking-[0.2em] text-[--color-muted]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: dur, delay: rm ? 0 : 0.42 }}
          >
            Эксклюзивная вышивка на одежде
          </motion.p>

          {/* CTA buttons */}
          <motion.div
            className="mt-10 flex flex-wrap items-center justify-center gap-3"
            initial={{ opacity: 0, y: rm ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: dur, ease, delay: rm ? 0 : 0.52 }}
          >
            <Button size="lg" onClick={() => navigate("/catalog")}>
              Каталог
            </Button>
            <Button
              size="lg"
              variant="outline"
              onClick={() => navigate("/custom-design")}
            >
              Свой дизайн
            </Button>
          </motion.div>
        </div>
      </Container>

      {/* ── Scroll indicator ── */}
      <motion.div
        className="absolute bottom-8 left-1/2 flex -translate-x-1/2 flex-col items-center"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.8, delay: rm ? 0 : 1.4 }}
      >
        <motion.div
          className="h-12 w-px bg-gradient-to-b from-black/60 to-transparent"
          animate={rm ? {} : { scaleY: [0.3, 1, 0.3], opacity: [0.4, 1, 0.4] }}
          transition={{ duration: 2.5, repeat: Infinity, ease: "easeInOut" }}
        />
      </motion.div>
    </section>
  );
}
