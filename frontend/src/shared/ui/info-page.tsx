import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";

/**
 * Обёртка информационных страниц (оферта, конфиденциальность, доставка и т.п.)
 * в минималистичном стиле BALGYN: белый фон, чёрный текст, тонкие границы.
 */
export function InfoPage({
  title,
  lead,
  children,
}: {
  title: string;
  lead?: string;
  children: ReactNode;
}) {
  const { t } = useTranslation();
  return (
    <div className="py-12 md:py-16">
      <Container className="max-w-3xl">
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            {t("nav.home")}
          </Link>
          <span aria-hidden>›</span>
          <span className="text-black">{title}</span>
        </nav>

        <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
          {title}
        </h1>
        {lead ? (
          <p className="mt-4 max-w-2xl text-sm leading-relaxed text-[--color-muted]">
            {lead}
          </p>
        ) : null}

        <div className="mt-10 flex flex-col gap-10">{children}</div>
      </Container>
    </div>
  );
}

/** Секция информационной страницы: малый заголовок-капс + произвольный контент. */
export function InfoSection({
  heading,
  children,
}: {
  heading: string;
  children: ReactNode;
}) {
  return (
    <section>
      <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
        {heading}
      </h2>
      <div className="flex flex-col gap-3 text-sm leading-relaxed text-zinc-700">
        {children}
      </div>
    </section>
  );
}
