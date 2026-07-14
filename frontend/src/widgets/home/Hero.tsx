import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import heroVideo from "@/assets/figma/homepage.mp4";
import heroPoster from "@/assets/figma/homepage-poster.webp";

const EASE = [0.16, 1, 0.3, 1] as const;

export function Hero() {
  const { t } = useTranslation();
  // Measured: autoplay makes Chrome fetch the whole file regardless of preload="metadata",
  // competing with the page's critical JS/CSS/API requests during first paint. The <source>
  // (and therefore the network request) is withheld until after the first paint so the video
  // load can never delay it; the poster covers this gap instantly. rAF pair = "after next
  // paint" (standard pattern); the timeout is a fallback for tabs where rAF doesn't fire.
  const [videoSourceReady, setVideoSourceReady] = useState(false);

  useEffect(() => {
    let raf1 = 0;
    let raf2 = 0;
    raf1 = requestAnimationFrame(() => {
      raf2 = requestAnimationFrame(() => setVideoSourceReady(true));
    });
    const fallback = setTimeout(() => setVideoSourceReady(true), 300);
    return () => {
      cancelAnimationFrame(raf1);
      cancelAnimationFrame(raf2);
      clearTimeout(fallback);
    };
  }, []);

  return (
    <section className="relative flex min-h-[100svh] w-full flex-col overflow-hidden bg-black text-white">
      {/* Video — poster paints the first frame instantly; H.264 MP4 plays in every modern browser */}
      <div className="absolute inset-0 h-full w-full overflow-hidden pointer-events-none">
        <video
          autoPlay
          muted
          loop
          playsInline
          preload="metadata"
          poster={heroPoster}
          aria-hidden="true"
          // Setting `src` directly (not a child <source>) is required here: appending a
          // <source> to an already-mounted <video> does NOT trigger the browser's resource
          // selection algorithm per spec — only changing `src`/calling .load() does.
          src={videoSourceReady ? heroVideo : undefined}
          className="absolute left-0 top-0 h-full w-full object-cover"
        />
      </div>
      {/* Gradient scrim — heavier at the bottom where the type sits */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/25 to-black/40" />

      {/* Top eyebrow */}
      <div className="relative z-10 px-4 pt-28 md:px-8 md:pt-32">
        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, ease: EASE }}
          className="text-[11px] font-medium uppercase tracking-[0.28em] text-white/70"
        >
          {t("home.hero.badge")}
        </motion.p>
      </div>

      {/* Bottom-anchored composition */}
      <div className="relative z-10 mt-auto px-4 pb-12 md:px-8 md:pb-16">
        <div className="flex flex-col gap-8 md:flex-row md:items-end md:justify-between">
          <div className="max-w-[900px]">
            <motion.h1
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 1.1, delay: 0.1, ease: EASE }}
              className="text-[clamp(64px,18vw,220px)] font-bold uppercase leading-[0.82] tracking-[-0.04em]"
            >
              BALGYN
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.9, delay: 0.35, ease: EASE }}
              className="mt-5 max-w-[440px] text-[15px] leading-relaxed text-white/75 md:text-[17px]"
            >
              {t("home.hero.tagline")}
            </motion.p>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.9, delay: 0.5, ease: EASE }}
            className="shrink-0"
          >
            <Link
              to="/catalog"
              className="group inline-flex h-14 items-center justify-center gap-3 border border-white/80 bg-white px-9 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition-colors duration-500 hover:bg-transparent hover:text-white md:h-[60px]"
            >
              {t("home.hero.cta")}
              <svg
                className="h-3.5 w-3.5 transition-transform duration-500 group-hover:translate-x-1 group-hover:-translate-y-1"
                viewBox="0 0 21 21"
                fill="none"
              >
                <path d="M1 20H20V1M20 20L1 1" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
              </svg>
            </Link>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
