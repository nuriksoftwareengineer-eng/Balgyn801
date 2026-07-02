import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { CONTACT_EMAIL } from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function TermsPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t("termsPage.title")} lead={t("termsPage.lead")}>
      <InfoSection heading={t("termsPage.s1.heading")}>
        <p>{t("termsPage.s1.p1")}</p>
        <p>{t("termsPage.s1.p2")}</p>
      </InfoSection>

      <InfoSection heading={t("termsPage.s2.heading")}>
        <p>{t("termsPage.s2.p1")}</p>
        <p>{t("termsPage.s2.p2")}</p>
      </InfoSection>

      <InfoSection heading={t("termsPage.s3.heading")}>
        <p>{t("termsPage.s3.p1")}</p>
      </InfoSection>

      <InfoSection heading={t("termsPage.s4.heading")}>
        <p>
          {t("termsPage.s4.before")}
          <Link to="/delivery" className={linkClass}>
            {t("termsPage.s4.deliveryLink")}
          </Link>
          {t("termsPage.s4.after")}
        </p>
      </InfoSection>

      <InfoSection heading={t("termsPage.s5.heading")}>
        <p>
          {t("termsPage.s5.before")}
          <Link to="/returns" className={linkClass}>
            {t("termsPage.s5.returnsLink")}
          </Link>
          {t("termsPage.s5.after")}
        </p>
      </InfoSection>

      <InfoSection heading={t("termsPage.s6.heading")}>
        <p>
          {t("termsPage.s6.before")}
          <Link to="/privacy" className={linkClass}>
            {t("termsPage.s6.privacyLink")}
          </Link>
          {t("termsPage.s6.after")}
        </p>
      </InfoSection>

      <InfoSection heading={t("termsPage.s7.heading")}>
        <p>
          {t("termsPage.s7.before")}
          <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
            {CONTACT_EMAIL}
          </a>
          {t("termsPage.s7.middle")}
          <Link to="/contacts" className={linkClass}>
            {t("termsPage.s7.contactsLinkText")}
          </Link>
          {t("termsPage.s7.after")}
        </p>
      </InfoSection>
    </InfoPage>
  );
}
