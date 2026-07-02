import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Marquee } from "@/widgets/home/Marquee";
import { STORE_TELEGRAM_URL, TELEGRAM_CHANNEL_URL } from "@/shared/constants/store-content";

const linkClass =
  "text-[14px] text-[#D9D9D9] transition-colors hover:text-white";

export function SiteFooter() {
  const { t } = useTranslation();
  const year = new Date().getFullYear();

  return (
    <footer className="bg-black text-white">
      <div className="border-y border-white/20 py-4 md:py-6">
        <Marquee
          items={["balgyn", "✸", t("footer.marquee.fresh"), "✸", t("footer.marquee.embroidery"), "✸"]}
          speed={70}
        />
      </div>

      <div className="container mx-auto px-4 pb-12 pt-20 md:px-8 md:pt-24">
        <div className="mb-20 grid grid-cols-2 gap-12 md:grid-cols-4 md:mb-24">
          <div className="flex flex-col gap-4">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">{t("footer.shop.title")}</h4>
            <Link to="/catalog" className={linkClass}>{t("footer.shop.catalog")}</Link>
            <Link to="/custom-design" className={linkClass}>{t("footer.shop.customDesign")}</Link>
            <Link to="/about" className={linkClass}>{t("footer.shop.about")}</Link>
          </div>

          <div className="flex flex-col gap-4">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">{t("footer.help.title")}</h4>
            <Link to="/delivery" className={linkClass}>{t("footer.help.delivery")}</Link>
            <Link to="/returns" className={linkClass}>{t("footer.help.returns")}</Link>
            <Link to="/track-order" className={linkClass}>{t("footer.help.trackOrder")}</Link>
            <Link to="/contacts" className={linkClass}>{t("footer.help.contacts")}</Link>
          </div>

          <div className="flex flex-col gap-4">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">{t("footer.social.title")}</h4>
            <a href="https://instagram.com/balgyn.bol" target="_blank" rel="noopener noreferrer" className={linkClass}>Instagram</a>
            <a href="https://tiktok.com/@balgyn.bol" target="_blank" rel="noopener noreferrer" className={linkClass}>TikTok</a>
            <a href={STORE_TELEGRAM_URL} target="_blank" rel="noopener noreferrer" className={linkClass}>Telegram</a>
            <a href={TELEGRAM_CHANNEL_URL} target="_blank" rel="noopener noreferrer" className={linkClass}>{t("footer.social.telegramChannel")}</a>
          </div>

          <div className="col-span-2 flex flex-col gap-4 md:col-span-1">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">{t("footer.newsletter.title")}</h4>
            <p className="mb-2 text-[14px] text-[#D9D9D9]">{t("footer.newsletter.desc")}</p>
            <a
              href="https://instagram.com/balgyn.bol"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex w-fit items-center gap-2 border-b border-[#7A7A7A] pb-2 text-[14px] text-[#D9D9D9] transition-colors hover:border-white hover:text-white"
            >
              {t("footer.newsletter.follow")}
            </a>
          </div>
        </div>

        <div className="flex flex-col gap-8 border-t border-[#2A2A2A] pt-8">
          <p className="text-[44px] font-extrabold uppercase leading-none tracking-[-0.04em] md:text-[72px]">
            BALGYN
          </p>
          <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
            <p className="text-center text-[12px] uppercase tracking-wider text-[#7A7A7A] md:text-left">
              {t("footer.rights", { year })}
            </p>
            <div className="flex flex-wrap justify-center gap-6 text-[10px] uppercase tracking-[0.2em] text-[#7A7A7A]">
              <Link to="/privacy" className="transition-colors hover:text-white">{t("footer.legal.privacy")}</Link>
              <Link to="/terms" className="transition-colors hover:text-white">{t("footer.legal.terms")}</Link>
              <Link to="/returns" className="transition-colors hover:text-white">{t("footer.legal.returns")}</Link>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
