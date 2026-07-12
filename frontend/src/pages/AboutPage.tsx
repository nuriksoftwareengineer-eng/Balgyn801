import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
  SUPPORT_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

function useSiteSettings() {
  return useQuery({
    queryKey: ["site-settings"],
    queryFn: () =>
      fetch("/api/v1/site-settings").then(
        (r) => r.json() as Promise<Record<string, string>>,
      ),
    staleTime: 5 * 60 * 1000,
  });
}

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function AboutPage() {
  const { t } = useTranslation();
  const { data: settings } = useSiteSettings();
  const ceoPhotoUrl = settings?.["ceo_photo_url"];

  return (
    <div className="pb-20">
      {/* Hero */}
      <div className="border-b border-[--color-border] bg-black py-20 md:py-28">
        <Container className="max-w-4xl">
          <nav className="mb-8 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-white/40">
            <Link to="/" className="transition-colors hover:text-white/70">
              {t("aboutPage.breadcrumbHome")}
            </Link>
            <span aria-hidden>/</span>
            <span className="text-white/60">{t("aboutPage.breadcrumb")}</span>
          </nav>
          <p className="mb-4 text-[0.6rem] font-semibold uppercase tracking-[0.24em] text-white/40">
            {t("aboutPage.hero.eyebrow")}
          </p>
          <h1 className="text-3xl font-bold uppercase leading-none tracking-[-0.02em] text-white md:text-5xl lg:text-6xl">
            {t("aboutPage.title")}
          </h1>
          <p className="mt-6 max-w-xl text-[15px] leading-relaxed text-white/60">
            {t("aboutPage.hero.tagline")}
          </p>
        </Container>
      </div>

      <Container className="max-w-4xl pt-16 md:pt-20">
        {/* Story */}
        <section className="mb-16 grid gap-8 md:grid-cols-2 md:gap-16">
          <div>
            <p className="mb-4 text-xs font-medium tracking-wide text-[--color-muted]">
              {t("aboutPage.story.heading")}
            </p>
            <p className="text-[15px] leading-relaxed text-zinc-700">
              {t("aboutPage.story.p1")}
            </p>
          </div>
          <div className="flex flex-col justify-center">
            <p className="text-[15px] leading-relaxed text-zinc-700">
              {t("aboutPage.story.p2")}
            </p>
          </div>
        </section>

        {/* Values */}
        <section className="mb-16 border-t border-[--color-border] pt-12">
          <p className="mb-8 text-xs font-medium tracking-wide text-[--color-muted]">
            {t("aboutPage.values.heading")}
          </p>
          <div className="grid gap-6 sm:grid-cols-3">
            {(["v1", "v2", "v3"] as const).map((k) => (
              <div
                key={k}
                className="border border-[--color-border] bg-[--color-surface] p-6"
              >
                <p className="mb-2 text-[13px] font-semibold uppercase tracking-[0.1em] text-black">
                  {t(`aboutPage.values.${k}.title`)}
                </p>
                <p className="text-[13px] leading-relaxed text-zinc-600">
                  {t(`aboutPage.values.${k}.desc`)}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* CEO / Founder */}
        <section className="mb-16 border-t border-[--color-border] pt-12">
          <p className="mb-8 text-xs font-medium tracking-wide text-[--color-muted]">
            {t("aboutPage.ceo.role")}
          </p>
          <div className="grid gap-10 md:grid-cols-[minmax(0,260px)_1fr] md:items-center md:gap-14">
            <div className="mx-auto w-full max-w-[260px] md:mx-0">
              <div className="relative overflow-hidden border border-[--color-border] bg-[--color-surface]">
                <div className="relative aspect-[4/5]">
                  {ceoPhotoUrl ? (
                    <img
                      src={ceoPhotoUrl}
                      alt="Dias Abris — CEO"
                      className="h-full w-full object-cover object-center"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <div className="border border-[--color-border] bg-white px-8 py-8 text-center">
                        <p className="m-0 text-5xl font-semibold tracking-[0.08em] text-black">
                          DA
                        </p>
                        <p className="m-0 mt-1 text-[0.6rem] uppercase tracking-[0.2em] text-[--color-muted]">
                          Balgyn Studio
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div>
              <h2 className="mb-1 text-2xl font-bold uppercase tracking-[-0.01em] text-black">
                Dias Abris
              </h2>
              <p className="mb-4 text-xs font-medium tracking-wide text-[--color-muted]">
                Founder & CEO, Balgyn
              </p>
              <p className="mb-4 text-[14px] leading-relaxed text-zinc-700">
                {t("aboutPage.ceo.bio")}
              </p>
              <p className="mb-6 text-[13px] leading-relaxed text-[--color-muted]">
                {t("aboutPage.ceo.contact")}
              </p>
              <div className="flex flex-wrap gap-3">
                <Link
                  to="/custom-design"
                  className="inline-flex h-11 items-center justify-center bg-black px-6 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
                >
                  {t("aboutPage.ceo.customBtn")}
                </Link>
                <a
                  href={SUPPORT_TELEGRAM_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-6 text-[12px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black"
                >
                  {t("aboutPage.ceo.telegramBtn")}
                </a>
              </div>
            </div>
          </div>
        </section>

        {/* How we work */}
        <section className="mb-16 border-t border-[--color-border] pt-12">
          <p className="mb-4 text-xs font-medium tracking-wide text-[--color-muted]">
            {t("aboutPage.howWeWork.heading")}
          </p>
          <ul className="list-disc space-y-3 pl-5 text-[14px] leading-relaxed text-zinc-700">
            <li>
              {t("aboutPage.howWeWork.li1before")}
              <Link to="/catalog" className={linkClass}>
                {t("aboutPage.howWeWork.li1link")}
              </Link>
              {t("aboutPage.howWeWork.li1after")}
            </li>
            <li>
              {t("aboutPage.howWeWork.li2before")}
              <Link to="/custom-design" className={linkClass}>
                {t("aboutPage.howWeWork.li2link")}
              </Link>
              {t("aboutPage.howWeWork.li2after")}
            </li>
            <li>{t("aboutPage.howWeWork.li3")}</li>
          </ul>
        </section>

        {/* Custom design CTA */}
        <section className="mb-16 border-t border-[--color-border] pt-12">
          <div className="grid gap-6 md:grid-cols-2 md:items-center">
            <div>
              <p className="mb-2 text-xs font-medium tracking-wide text-[--color-muted]">
                {t("aboutPage.custom.heading")}
              </p>
              <p className="text-[14px] leading-relaxed text-zinc-700">
                {t("aboutPage.custom.desc")}
              </p>
            </div>
            <div className="md:text-right">
              <Link
                to="/custom-design"
                className="inline-flex h-12 items-center justify-center bg-black px-8 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
              >
                {t("aboutPage.custom.btn")}
              </Link>
            </div>
          </div>
        </section>

        {/* Quality */}
        <section className="mb-16 border-t border-[--color-border] pt-12">
          <p className="mb-3 text-xs font-medium tracking-wide text-[--color-muted]">
            {t("aboutPage.quality.heading")}
          </p>
          <p className="text-[14px] leading-relaxed text-zinc-700">
            {t("aboutPage.quality.p1")}
          </p>
        </section>

        {/* Contacts */}
        <section className="border-t border-[--color-border] pt-12">
          <p className="mb-3 text-xs text-[--color-muted]">
            {t("aboutPage.contacts.heading")}
          </p>
          <ul className="m-0 flex list-none flex-col gap-2 p-0 text-[14px] text-zinc-700">
            <li>
              {t("aboutPage.contacts.emailLabel")}{" "}
              <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
                {CONTACT_EMAIL}
              </a>
            </li>
            <li>
              {t("aboutPage.contacts.telegramLabel")}{" "}
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className={linkClass}
              >
                {t("aboutPage.contacts.telegramLinkText")}
              </a>
            </li>
            <li>
              {t("aboutPage.contacts.allContacts")}
              <Link to="/contacts" className={linkClass}>
                {t("aboutPage.contacts.contactsLink")}
              </Link>
              {t("aboutPage.contacts.allContactsAfter")}
            </li>
          </ul>
        </section>
      </Container>
    </div>
  );
}
