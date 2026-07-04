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
            <Link to="/faq" className={linkClass}>{t("footer.help.faq")}</Link>
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

        {/* Payment logos */}
        <div className="mb-10 flex flex-wrap items-center gap-3 border-t border-[#2A2A2A] pt-8">
          {/* Visa — official wordmark SVG (Simple Icons) */}
          <div className="flex h-9 w-[60px] items-center justify-center rounded border border-white/10 bg-white/5">
            <svg viewBox="0 0 24 24" className="h-4 w-auto fill-white" aria-label="Visa" xmlns="http://www.w3.org/2000/svg">
              <path d="M9.112 8.262L5.97 15.758H3.92L2.374 9.775c-.094-.368-.175-.503-.461-.658C1.447 8.864.677 8.627 0 8.479l.046-.217h3.3a.904.904 0 01.894.764l.816 4.335 2.016-5.1zm8.033 5.049c.008-1.979-2.736-2.088-2.717-2.972.006-.269.262-.555.822-.628a3.66 3.66 0 011.913.336l.34-1.59a5.207 5.207 0 00-1.814-.333c-1.917 0-3.266 1.02-3.278 2.479-.012 1.079.963 1.68 1.698 2.04.756.366 1.01.602 1.006.929-.005.502-.603.724-1.16.732-.975.015-1.54-.263-1.99-.473l-.351 1.642c.453.208 1.289.39 2.156.398 2.037 0 3.37-1.006 3.375-2.56zm5.061 2.447H24l-1.565-7.496h-1.656a.883.883 0 00-.826.55l-2.909 6.946h2.036l.405-1.12h2.488zm-2.163-2.656l1.02-2.815.588 2.815zm-8.16-4.84l-1.603 7.496H8.34l1.605-7.496z"/>
            </svg>
          </div>
          {/* Mastercard — two overlapping circles with brand colors */}
          <div className="flex h-9 w-[60px] items-center justify-center rounded border border-white/10 bg-white/5">
            <svg viewBox="0 0 38 26" className="h-6 w-auto" aria-label="Mastercard" xmlns="http://www.w3.org/2000/svg">
              <circle cx="14" cy="13" r="12" fill="#EB001B"/>
              <circle cx="24" cy="13" r="12" fill="#F79E1B" fillOpacity="0.9"/>
              <path d="M19 3.49A12 12 0 0 1 19 22.51A12 12 0 0 1 19 3.49Z" fill="#FF5F00"/>
            </svg>
          </div>
          {/* PayPal — official monogram SVG (Simple Icons) */}
          <div className="flex h-9 w-[60px] items-center justify-center rounded border border-white/10 bg-white/5">
            <svg viewBox="0 0 24 24" className="h-5 w-auto fill-white" aria-label="PayPal" xmlns="http://www.w3.org/2000/svg">
              <path d="M7.076 21.337H2.47a.641.641 0 01-.633-.74L4.944.901C5.026.382 5.474 0 5.998 0h7.46c2.57 0 4.578.543 5.69 1.81 1.01 1.15 1.304 2.42 1.012 4.287-.023.143-.047.288-.077.437-.983 5.05-4.349 6.797-8.647 6.797h-2.19c-.524 0-.968.382-1.05.9l-1.12 7.106zm14.146-14.42a3.35 3.35 0 00-.607-.541c-.013.076-.026.175-.041.254-.93 4.778-4.005 7.201-9.138 7.201h-2.19a.563.563 0 00-.556.479l-1.187 7.527h-.506l-.24 1.516a.56.56 0 00.554.647h3.882c.46 0 .85-.334.922-.788.06-.26.76-4.852.816-5.09a.932.932 0 01.923-.788h.58c3.76 0 6.705-1.528 7.565-5.946.36-1.847.174-3.388-.777-4.471z"/>
            </svg>
          </div>
          {/* FreedomPay */}
          <div className="flex h-9 min-w-[72px] items-center justify-center rounded border border-white/10 bg-white/5 px-3">
            <span className="text-[9px] font-extrabold uppercase tracking-widest text-white/70">
              Freedom<span className="text-[#00C853]">Pay</span>
            </span>
          </div>
          {/* VTB */}
          <div className="flex h-9 w-[60px] items-center justify-center rounded border border-[#0066B2]/60 bg-[#0066B2]/20">
            <span className="text-[13px] font-extrabold tracking-[2px] text-white">ВТБ</span>
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
