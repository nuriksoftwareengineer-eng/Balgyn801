import { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";

const FAQ_KEYS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12] as const;

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
        className="flex w-full items-center justify-between gap-4 py-5 text-left"
      >
        <span className="text-[14px] font-semibold text-black md:text-[15px]">
          {question}
        </span>
        <span
          className={`shrink-0 text-[20px] font-light leading-none text-[--color-muted] transition-transform duration-200 ${
            open ? "rotate-45" : ""
          }`}
        >
          +
        </span>
      </button>
      {open && (
        <p className="pb-5 text-[14px] leading-relaxed text-zinc-600">
          {answer}
        </p>
      )}
    </div>
  );
}

export function FAQPage() {
  const { t } = useTranslation();
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  return (
    <div className="py-12 md:py-16">
      <Container className="max-w-3xl">
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            {t("aboutPage.breadcrumbHome")}
          </Link>
          <span aria-hidden>›</span>
          <span className="text-black">{t("faq.breadcrumb")}</span>
        </nav>

        <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
          {t("faq.title")}
        </h1>
        <p className="mt-3 mb-10 text-sm text-[--color-muted]">
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

        <div className="mt-12 border border-[--color-border] bg-[--color-surface] p-6">
          <p className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-[--color-muted]">
            {t("contactsPage.contactsHeading")}
          </p>
          <p className="text-[14px] text-zinc-700">
            Telegram: @balgyn.bol &nbsp;·&nbsp; Email:{" "}
            <a
              href="mailto:balgyn.studio@gmail.com"
              className="underline underline-offset-2 hover:text-black"
            >
              balgyn.studio@gmail.com
            </a>
          </p>
        </div>
      </Container>
    </div>
  );
}
