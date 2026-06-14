import { motion } from "framer-motion";
import { Marquee } from "./Marquee";

/** Чёрная секция «О бренде». */
export function AboutBand() {
  return (
    <section className="relative overflow-hidden bg-black text-white">
      <div className="py-24 md:py-40">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.9 }}
          className="container mx-auto px-4 md:px-8"
        >
          <div className="flex flex-col items-center justify-center text-center">
            <h2 className="mb-6 text-center text-[40px] font-extrabold uppercase leading-[1.1] tracking-[-0.04em] sm:text-[48px] md:mb-8 md:text-[88px]">
              О бренде
            </h2>
            <p className="mx-auto mb-12 max-w-[560px] text-center text-[16px] font-normal leading-relaxed text-[#D9D9D9] md:mb-16 md:text-[20px]">
              Вышивка — единственное место, где BALGYN допускает орнамент. Это шов
              между казахскими корнями бренда и его минималистичным настоящим.
              Доказательство, что сдержанность здесь — ремесленное решение, а не
              пустота.
            </p>

            <div className="mx-auto grid w-full max-w-[800px] grid-cols-1 gap-6 border-t border-white/20 pt-8 text-[12px] uppercase tracking-[0.15em] sm:grid-cols-3 md:gap-12 md:text-[14px] md:tracking-[0.2em]">
              <div className="flex flex-col items-center gap-2 text-center">
                <span className="text-white/60">Тираж</span>
                <span className="font-semibold text-white">Малые партии</span>
              </div>
              <div className="flex flex-col items-center gap-2 text-center">
                <span className="text-white/60">Материал</span>
                <span className="font-semibold text-white">100% хлопок</span>
              </div>
              <div className="flex flex-col items-center gap-2 text-center">
                <span className="text-white/60">Пошив</span>
                <span className="font-semibold text-white">Алматы</span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      <div className="border-t border-white/20 py-6">
        <Marquee
          items={["balgyn", "✶", "крой — нить — шов", "✶", "made in kz", "✶"]}
          speed={60}
          reverse
        />
      </div>
    </section>
  );
}
