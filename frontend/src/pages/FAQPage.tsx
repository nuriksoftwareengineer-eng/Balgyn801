import { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AnimatePresence, motion } from "framer-motion";
import { Container } from "@/shared/ui/container";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
  WHATSAPP_URL,
} from "@/shared/constants/store-content";

const FAQ_KEYS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13] as const;

function AccordionItem({
  question,
  answer,
  open,
  onToggle,
}: {
  question: string;
  answer: string;
  open: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="border-b border-[--color-border]">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        className="flex w-full items-center justify-between gap-6 py-6 text-left transition-opacity focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-black focus-visible:ring-offset-2 md:py-7"
      >
        <span className={`text-[16px] tracking-[-0.01em] transition-colors md:text-[18px] ${open ? "font-medium text-black" : "font-normal text-black"}`}>
          {question}
        </span>
        <span
          className={`relative h-4 w-4 shrink-0 text-[--color-muted] transition-transform duration-300 ${open ? "rotate-45" : ""}`}
          aria-hidden
        >
          <span className="absolute left-1/2 top-1/2 h-[1.5px] w-4 -translate-x-1/2 -translate-y-1/2 bg-current" />
          <span className="absolute left-1/2 top-1/2 h-4 w-[1.5px] -translate-x-1/2 -translate-y-1/2 bg-current" />
        </span>
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            className="overflow-hidden"
          >
            <p className="max-w-[640px] pb-7 text-[14px] leading-relaxed text-zinc-600 md:text-[15px]">
              {answer}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

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
              question={t(`faq.q${n}`)}
              answer={t(`faq.a${n}`)}
              open={openIndex === n}
              onToggle={() => setOpenIndex(openIndex === n ? null : n)}
            />
          ))}
        </div>

        <div className="mt-12 border-t border-[--color-border] pt-8">
          <p className="mb-4 text-xs text-[--color-muted]">
            {t("contactsPage.contactsHeading")}
          </p>
          <div className="grid grid-cols-2 gap-x-6 gap-y-4 text-[14px] sm:grid-cols-4">
            <div>
              <p className="mb-1 text-xs text-[--color-muted]">Instagram</p>
              <a href="https://instagram.com/balgyn.bol" target="_blank" rel="noopener noreferrer" className="text-zinc-800 hover:text-black">
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
