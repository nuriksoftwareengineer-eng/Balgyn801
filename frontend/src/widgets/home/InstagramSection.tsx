import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
import p1 from "@/assets/instagram/ig1.jpg";
import p2 from "@/assets/instagram/ig2.jpg";
import p3 from "@/assets/instagram/ig3.jpg";
import p4 from "@/assets/instagram/ig4.jpg";
import p5 from "@/assets/instagram/ig5.jpg";
import p6 from "@/assets/instagram/ig6.jpg";

const PHOTOS = [p1, p2, p3, p4, p5, p6];
const IG_URL = "https://instagram.com/balgyn.bol";

function IgIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="2" y="2" width="20" height="20" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function InstagramSection() {
  const { t } = useTranslation();

  return (
    <section className="py-24 md:py-32">
      <div className="container mx-auto px-4 md:px-8">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.7 }}
          className="mb-10 flex flex-col items-start justify-between gap-4 md:flex-row md:items-end"
        >
          <h2 className="display text-[38px] uppercase sm:text-[48px] md:text-[64px]">
            BALGYN BOL
          </h2>
          <a
            href={IG_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="group inline-flex items-center gap-2 text-[13px] font-medium uppercase tracking-[0.14em] text-black transition-colors hover:text-[--color-muted]"
          >
            <IgIcon className="h-5 w-5" />
            {t("home.instagram.follow")}
          </a>
        </motion.div>

        <div className="grid grid-cols-2 gap-2 md:grid-cols-3 md:gap-4">
          {PHOTOS.map((src, i) => (
            <a
              key={i}
              href={IG_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="group relative aspect-square overflow-hidden bg-[#F5F5F5]"
            >
              <img
                src={src}
                alt={t("home.instagram.imageAlt")}
                className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
              />
              <div className="absolute inset-0 flex items-center justify-center bg-black/0 transition-colors duration-300 group-hover:bg-black/30">
                <IgIcon className="h-7 w-7 text-white opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
              </div>
            </a>
          ))}
        </div>
      </div>
    </section>
  );
}
