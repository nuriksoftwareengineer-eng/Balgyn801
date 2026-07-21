import { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";
import { AccordionItem } from "@/shared/ui/AccordionItem";
import {
  CONTACT_EMAIL,
  INSTAGRAM_URL,
  STORE_TELEGRAM_URL,
  WHATSAPP_URL,
} from "@/shared/constants/store-content";

const FAQ_KEYS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13] as const;

export function FAQPage() {
  const { t } = useTranslation();
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  return (
    <div className="py-12 md:py-20">
      <Container className="max-w-3xl">
        <nav className="mb-6 flex items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            {t("aboutPage.breadcrumbHome")}
          </Link>
          <span aria-hidden>/</span>
          <span className="text-black">{t("faq.breadcrumb")}</span>
        </nav>

        <h1 className="display text-[40px] uppercase text-black md:text-[60px]">
          {t("faq.title")}
        </h1>
        <p className="mt-4 mb-12 text-[15px] text-[--color-muted]">
          {t("faq.subtitle")}
        </p>

        <div className="border-t border-[--color-border]">
          {FAQ_KEYS.map((n) => (
            <AccordionItem
              key={n}
              title={t(`faq.q${n}`)}
              content={t(`faq.a${n}`)}
              open={openIndex === n}
              onToggle={() => setOpenIndex(openIndex === n ? null : n)}
            />
          ))}
        </div>

        {/* Contact CTA */}
        <div className="mt-14 border-t border-[--color-border] pt-10 text-center">
          <h2 className="text-xl font-semibold uppercase tracking-[-0.01em] text-black md:text-2xl">
            {t("faq.cta.heading")}
          </h2>
          <p className="mt-2 text-[14px] text-[--color-muted]">
            {t("faq.cta.desc")}
          </p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <a
              href={WHATSAPP_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-6 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black"
            >
              WhatsApp
            </a>
            <a
              href={STORE_TELEGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-6 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black"
            >
              Telegram
            </a>
            <a
              href={INSTAGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-6 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black"
            >
              Instagram
            </a>
          </div>
        </div>

        <div className="mt-12 border-t border-[--color-border] pt-8">
          <p className="mb-4 text-xs text-[--color-muted]">
            {t("contactsPage.contactsHeading")}
          </p>
          <div className="grid grid-cols-2 gap-x-6 gap-y-4 text-[14px] sm:grid-cols-4">
            <div>
              <p className="mb-1 text-xs text-[--color-muted]">Instagram</p>
              <a href={INSTAGRAM_URL} target="_blank" rel="noopener noreferrer" className="text-zinc-800 hover:text-black">
                @balgyn.bol
              </a>
            </div>
            <div>
              <p className="mb-1 text-xs text-[--color-muted]">Telegram</p>
              <a href={STORE_TELEGRAM_URL} target="_blank" rel="noopener noreferrer" className="text-zinc-800 hover:text-black">
                @balgyn.bol
              </a>
            </div>
            <div>
              <p className="mb-1 text-xs text-[--color-muted]">WhatsApp</p>
              <a href={WHATSAPP_URL} target="_blank" rel="noopener noreferrer" className="text-zinc-800 hover:text-black">
                +7 708 193 75 10
              </a>
            </div>
            <div>
              <p className="mb-1 text-xs text-[--color-muted]">Email</p>
              <a href={`mailto:${CONTACT_EMAIL}`} className="text-zinc-800 hover:text-black">
                {CONTACT_EMAIL}
              </a>
            </div>
          </div>
          <p className="mt-5 text-[13px] text-[--color-muted]">Пн–Пт, 10:00–18:00 (GMT+5)</p>
        </div>
      </Container>
    </div>
  );
}
