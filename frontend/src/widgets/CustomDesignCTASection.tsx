import { motion, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Button } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";

export function CustomDesignCTASection() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();

  return (
    <section className="py-12 md:py-20" id="custom-design">
      <Container>
        <motion.div
          initial={{ opacity: 0, y: reduceMotion ? 0 : 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-60px" }}
          transition={{ duration: reduceMotion ? 0 : 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="relative overflow-hidden rounded-[20px] border border-violet-500/20 bg-gradient-to-br from-zinc-900 via-zinc-950 to-zinc-900 px-8 py-12 md:px-14 md:py-14"
        >
          <div
            className="pointer-events-none absolute -right-20 top-1/2 h-64 w-64 -translate-y-1/2 rounded-full bg-violet-600/20 blur-[80px]"
            aria-hidden
          />
          <div className="relative max-w-2xl">
            <p className="mb-3 text-xs font-semibold uppercase tracking-[0.24em] text-violet-400">
              Индивидуальный заказ
            </p>
            <h2 className="font-display text-3xl tracking-wide text-zinc-100 md:text-4xl">
              Сделай свой дизайн
            </h2>
            <p className="mt-4 text-lg leading-relaxed text-zinc-400">
              Расскажите идею: логотип, надпись, референс с Pinterest или эскиз.
              Подберём нитки, технику вышивки и модель (худи, футболка, кепка).
            </p>
            <ul className="mt-6 space-y-2 text-sm text-zinc-500">
              <li className="flex gap-2">
                <span className="text-violet-400">✓</span>
                Смета и сроки — после согласования макета
              </li>
              <li className="flex gap-2">
                <span className="text-violet-400">✓</span>
                Фото перед отправкой, как в каталоге
              </li>
            </ul>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button
                type="button"
                variant="primary"
                className="rounded-full px-8"
                onClick={() => navigate("/custom-design")}
              >
                Оставить заявку
              </Button>
              <Button
                type="button"
                variant="outline"
                className="rounded-full"
                onClick={() =>
                  window.open(STORE_TELEGRAM_URL, "_blank", "noopener,noreferrer")
                }
              >
                Написать в Telegram
              </Button>
              <a
                href={`mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent("Свой дизайн BALGYN")}`}
                className="inline-flex items-center rounded-full px-4 py-3 text-sm font-semibold text-zinc-400 underline-offset-4 hover:text-zinc-200 hover:underline"
              >
                {CONTACT_EMAIL}
              </a>
            </div>
          </div>
        </motion.div>
      </Container>
    </section>
  );
}
