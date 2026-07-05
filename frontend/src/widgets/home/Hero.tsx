import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import heroVideo from "@/assets/figma/homepage.mp4";
import heroImg from "@/assets/figma/photo_5289740941222682479_y.jpg";

export function Hero() {
  const { t } = useTranslation();

  return (
    <section className="relative min-h-[88vh] w-full bg-black text-white overflow-hidden flex flex-col justify-center items-center">
      <div className="absolute inset-0 h-full w-full overflow-hidden pointer-events-none">
        <video
          autoPlay
          muted
          loop
          playsInline
          preload="auto"
          poster={heroImg}
          aria-hidden="true"
          className="absolute left-0 top-0 h-full w-full object-cover"
        >
          <source src={heroVideo} type="video/mp4" />
        </video>
      </div>
      <div className="absolute inset-0 bg-black mix-blend-multiply opacity-[0.68]" />

      <div className="relative z-10 flex w-full flex-col items-center justify-center px-4 md:px-8">
        <motion.p
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="mb-10 text-center text-[11px] font-semibold uppercase tracking-[0.24em] text-white"
        >
          {t("home.hero.badge")}
        </motion.p>

        <h1 className="text-center text-[72px] font-extrabold uppercase leading-[0.9] tracking-[-0.04em] sm:text-[120px] md:text-[180px]">
          BALGYN
        </h1>

        <motion.p
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.4, ease: "easeOut" }}
          className="mb-10 mt-6 max-w-[600px] text-center text-[16px] leading-[26px] text-white/85 md:text-[20px]"
        >
          {t("home.hero.tagline")}
        </motion.p>

        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.6, ease: "easeOut" }}
        >
          <Link
            to="/catalog"
            className="group inline-flex h-[64px] w-full items-center justify-center gap-4 bg-white px-10 text-[16px] font-semibold uppercase tracking-[0.08em] text-black transition hover:bg-[#f0f0f0] sm:w-[354px] md:h-[72px] md:text-[20px]"
          >
            {t("home.hero.cta")}
            <svg
              className="h-[18px] w-[18px] transition-transform duration-300 group-hover:translate-x-1 group-hover:-translate-y-1"
              viewBox="0 0 21 21"
              fill="none"
            >
              <path
                d="M1 20H20V1M20 20L1 1"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
              />
            </svg>
          </Link>
        </motion.div>
      </div>
    </section>
  );
}
