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
      fetch("/api/v1/site-settings").then((r) => r.json() as Promise<Record<string, string>>),
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
    <div className="py-12 md:py-16">
      <Container className="max-w-4xl">
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            {t("aboutPage.breadcrumbHome")}
          </Link>
          <span aria-hidden>›</span>
          <span className="text-black">{t("aboutPage.breadcrumb")}</span>
        </nav>

        <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
          {t("aboutPage.title")}
        </h1>
        <p className="mt-4 mb-12 max-w-2xl text-sm leading-relaxed text-[--color-muted]">
          {t("aboutPage.subtitle")}
        </p>

        <section className="mb-14 grid gap-10 md:grid-cols-[minmax(0,280px)_1fr] md:items-center md:gap-12">
          <div className="mx-auto w-full max-w-[280px] md:mx-0">
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
            <p className="text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-[--color-muted]">
              {t("aboutPage.ceo.role")}
            </p>
            <h2 className="mt-2 text-2xl font-semibold uppercase tracking-[0.04em] text-black md:text-3xl">
              Dias Abris
            </h2>
            <p className="mt-4 text-sm leading-relaxed text-zinc-700">
              {t("aboutPage.ceo.bio")}
            </p>
            <p className="mt-4 text-sm leading-relaxed text-[--color-muted]">
              {t("aboutPage.ceo.contact")}
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link
                to="/custom-design"
                className="inline-flex h-11 items-center justify-center bg-black px-6 text-sm font-medium tracking-wide text-white transition hover:bg-zinc-800"
              >
                {t("aboutPage.ceo.customBtn")}
              </Link>
              <a
                href={SUPPORT_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-6 text-sm font-medium tracking-wide text-black transition hover:border-black"
              >
                {t("aboutPage.ceo.telegramBtn")}
              </a>
            </div>
          </div>
        </section>

        <section className="mb-10">
          <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            {t("aboutPage.howWeWork.heading")}
          </h2>
          <ul className="list-disc space-y-2 pl-5 text-sm leading-relaxed text-zinc-700">
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

        <section className="mb-10">
          <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            {t("aboutPage.quality.heading")}
          </h2>
          <p className="text-sm leading-relaxed text-zinc-700">
            {t("aboutPage.quality.p1")}
          </p>
        </section>

        <section className="border border-[--color-border] bg-[--color-surface] p-6">
          <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            {t("aboutPage.contacts.heading")}
          </h2>
          <ul className="m-0 flex list-none flex-col gap-2 p-0 text-sm text-zinc-700">
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
