import { motion, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

export function CustomDesignCTASection() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();

  return (
    <section className="bg-black py-16 md:py-24" id="custom-design">
      <Container>
        <motion.div
          initial={{ opacity: 0, y: reduceMotion ? 0 : 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{
            duration: reduceMotion ? 0 : 0.6,
            ease: [0.22, 1, 0.36, 1],
          }}
        >
          <div className="grid gap-12 md:grid-cols-2 md:items-center md:gap-20">
            {/* Left: text */}
            <div>
              <p className="text-[0.6rem] font-medium uppercase tracking-[0.3em] text-white/40">
                Индивидуальный заказ
              </p>
              <h2 className="mt-4 font-sans text-3xl font-semibold uppercase tracking-[0.04em] text-white md:text-[2.5rem]">
                Свой дизайн
              </h2>
              <p className="mt-5 text-sm leading-relaxed text-white/55">
                Расскажите идею: логотип, надпись, референс или эскиз.
                Подберём нитки, технику вышивки и модель.
              </p>
              <ul className="mt-6 space-y-3 text-sm text-white/40">
                <li className="flex gap-3">
                  <span className="text-white/70">✓</span>
                  Смета и сроки — после согласования макета
                </li>
                <li className="flex gap-3">
                  <span className="text-white/70">✓</span>
                  Фото перед отправкой, как в каталоге
                </li>
              </ul>
            </div>

            {/* Right: CTAs */}
            <div className="flex flex-col gap-4">
              <button
                type="button"
                onClick={() => navigate("/custom-design")}
                className="w-full border border-white py-4 text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-white transition-colors duration-200 hover:bg-white hover:text-black focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
              >
                Оставить заявку
              </button>
              <button
                type="button"
                onClick={() =>
                  window.open(STORE_TELEGRAM_URL, "_blank", "noopener,noreferrer")
                }
                className="w-full border border-white/20 py-4 text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-white/50 transition-colors duration-200 hover:border-white hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white/50"
              >
                Написать в Telegram
              </button>
              <a
                href={`mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent("Свой дизайн BALGYN")}`}
                className="text-center text-[0.65rem] text-white/25 transition-colors hover:text-white/50"
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
