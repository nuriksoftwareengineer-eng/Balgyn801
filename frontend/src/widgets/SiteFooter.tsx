import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  STORE_TELEGRAM_URL,
  TELEGRAM_CHANNEL_URL,
  WHATSAPP_URL,
  CONTACT_EMAIL,
  MERCHANT,
} from "@/shared/constants/store-content";

const linkClass =
  "text-[13px] text-[#9A9A9A] transition-colors hover:text-white";

const headingClass =
  "mb-4 text-[10px] font-medium uppercase tracking-[0.2em] text-[#6E6E6E]";

export function SiteFooter() {
  const { t } = useTranslation();
  const year = new Date().getFullYear();

  return (
    <footer className="bg-black text-white">
      <div className="container mx-auto px-4 pb-8 pt-16 md:px-8 md:pt-24">
        <div className="mb-16 grid grid-cols-2 gap-10 md:mb-20 md:grid-cols-4 md:gap-10">
          <div className="flex flex-col gap-2.5">
            <h4 className={headingClass}>{t("footer.shop.title")}</h4>
            <Link to="/catalog" className={linkClass}>{t("footer.shop.catalog")}</Link>
            <Link to="/custom-design" className={linkClass}>{t("footer.shop.customDesign")}</Link>
            <Link to="/reviews" className={linkClass}>{t("footer.shop.reviews")}</Link>
            <Link to="/about" className={linkClass}>{t("footer.shop.about")}</Link>
          </div>

          <div className="flex flex-col gap-2.5">
            <h4 className={headingClass}>{t("footer.help.title")}</h4>
            <Link to="/delivery" className={linkClass}>{t("footer.help.delivery")}</Link>
            <Link to="/returns" className={linkClass}>{t("footer.help.returns")}</Link>
            <Link to="/faq" className={linkClass}>{t("footer.help.faq")}</Link>
            <Link to="/track-order" className={linkClass}>{t("footer.help.trackOrder")}</Link>
            <Link to="/contacts" className={linkClass}>{t("footer.help.contacts")}</Link>
          </div>

          <div className="flex flex-col gap-2.5">
            <h4 className={headingClass}>{t("footer.social.title")}</h4>
            <a href="https://instagram.com/balgyn.bol" target="_blank" rel="noopener noreferrer" className={linkClass}>Instagram</a>
            <a href="https://tiktok.com/@balgyn.bol" target="_blank" rel="noopener noreferrer" className={linkClass}>TikTok</a>
            <a href={STORE_TELEGRAM_URL} target="_blank" rel="noopener noreferrer" className={linkClass}>Telegram</a>
            <a href={TELEGRAM_CHANNEL_URL} target="_blank" rel="noopener noreferrer" className={linkClass}>{t("footer.social.telegramChannel")}</a>
            <a href={WHATSAPP_URL} target="_blank" rel="noopener noreferrer" className={linkClass}>WhatsApp</a>
            <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>Email</a>
          </div>

          <div className="col-span-2 flex flex-col gap-2.5 md:col-span-1">
            <h4 className={headingClass}>{t("footer.newsletter.title")}</h4>
            <p className="mb-1 text-[13px] text-[#B0B0B0]">{t("footer.newsletter.desc")}</p>
            <a
              href="https://instagram.com/balgyn.bol"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex w-fit items-center gap-2 border-b border-[#7A7A7A] pb-1.5 text-[13px] text-[#B0B0B0] transition-colors hover:border-white hover:text-white"
            >
              {t("footer.newsletter.follow")}
            </a>
          </div>
        </div>

        {/* Payment logos */}
        <div className="mb-12 flex flex-wrap items-center gap-2.5 md:mb-16">
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
          {/* Apple Pay */}
          <div className="flex h-9 w-[60px] items-center justify-center gap-1 rounded border border-white/10 bg-white/5" aria-label="Apple Pay">
            <svg viewBox="0 0 24 24" className="h-[15px] shrink-0 fill-white" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
              <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
            </svg>
            <span className="text-[10px] font-semibold text-white" aria-hidden="true">Pay</span>
          </div>
          {/* Google Pay */}
          <div className="flex h-9 w-[68px] items-center justify-center gap-1 rounded border border-white/10 bg-white px-2" aria-label="Google Pay">
            <svg viewBox="0 0 24 24" className="h-[14px] shrink-0" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            <span className="text-[10px] font-semibold text-[#3c4043]" aria-hidden="true">Pay</span>
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

        {/* Giant wordmark — the footer anchor */}
        <div className="border-t border-[#242424] pt-12 md:pt-16">
          <p className="select-none text-[19vw] font-bold uppercase leading-[0.8] tracking-[-0.05em] text-white md:text-[15vw]">
            BALGYN
          </p>

          {/* Bottom bar */}
          <div className="mt-10 flex flex-col gap-4 border-t border-[#1C1C1C] pt-6 md:flex-row md:items-center md:justify-between">
            <p className="text-[11px] tracking-wide text-[#6E6E6E]">
              {t("footer.rights", { year })}
            </p>
            <div className="flex flex-wrap gap-x-7 gap-y-2 text-[11px] tracking-wide text-[#6E6E6E]">
              <Link to="/privacy" className="transition-colors hover:text-white">{t("footer.legal.privacy")}</Link>
              <Link to="/terms" className="transition-colors hover:text-white">{t("footer.legal.terms")}</Link>
              <Link to="/returns" className="transition-colors hover:text-white">{t("footer.legal.returns")}</Link>
            </div>
          </div>

          {/* Requisites — secondary line (full details on Contacts & Public Offer; kept for Freedom Pay compliance) */}
          <p className="mt-4 text-[10px] leading-relaxed text-[#565656]">
            {t("merchant.ieShort")} {MERCHANT.ieName} · {t("merchant.iinShort")} {MERCHANT.iin}
            {" · "}
            <Link to="/contacts" className="underline-offset-2 transition-colors hover:text-[#9A9A9A] hover:underline">
              {t("merchant.title")}
            </Link>
          </p>
        </div>
      </div>
    </footer>
  );
}
