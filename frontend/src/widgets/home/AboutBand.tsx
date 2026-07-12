import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import { Marquee } from "./Marquee";

export function AboutBand() {
  const { t } = useTranslation();

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
            <h2 className="display mb-6 text-center text-[38px] uppercase sm:text-[48px] md:mb-8 md:text-[80px]">
              {t("home.about.title")}
            </h2>
            <p className="mx-auto mb-12 max-w-[540px] text-center text-[16px] font-normal leading-relaxed text-white/70 md:mb-16 md:text-[19px]">
              {t("home.about.desc")}
            </p>

            <div className="mx-auto grid w-full max-w-[800px] grid-cols-1 gap-6 border-t border-white/20 pt-8 text-[12px] uppercase tracking-[0.15em] sm:grid-cols-3 md:gap-12 md:text-[14px] md:tracking-[0.2em]">
              <div className="flex flex-col items-center gap-2 text-center">
                <span className="text-white/60">{t("home.about.stat1.value")}</span>
                <span className="font-semibold text-white">{t("home.about.stat1.label")}</span>
              </div>
              <div className="flex flex-col items-center gap-2 text-center">
                <span className="text-white/60">{t("home.about.stat2.value")}</span>
                <span className="font-semibold text-white">{t("home.about.stat2.label")}</span>
              </div>
              <div className="flex flex-col items-center gap-2 text-center">
                <span className="text-white/60">{t("home.about.stat3.value")}</span>
                <span className="font-semibold text-white">{t("home.about.stat3.label")}</span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      <div className="border-t border-white/20 py-6">
        <Marquee
          items={["balgyn", "✶", "крой · нить · шов", "✶", "made in kz", "✶"]}
          speed={60}
          reverse
        />
      </div>
    </section>
  );
}
