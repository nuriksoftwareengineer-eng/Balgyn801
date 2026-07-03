import { useTranslation } from "react-i18next";
import { CONTACT_EMAIL } from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function PrivacyPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t("privacyPage.title")} lead={t("privacyPage.lead")}>
      <InfoSection heading={t("privacyPage.s1.heading")}>
        <ul className="list-disc space-y-1.5 pl-5">
          <li>{t("privacyPage.s1.li1")}</li>
          <li>{t("privacyPage.s1.li2")}</li>
          <li>{t("privacyPage.s1.li3")}</li>
          <li>{t("privacyPage.s1.li4")}</li>
          <li>{t("privacyPage.s1.li5")}</li>
        </ul>
      </InfoSection>

      <InfoSection heading={t("privacyPage.s2.heading")}>
        <ul className="list-disc space-y-1.5 pl-5">
          <li>{t("privacyPage.s2.li1")}</li>
          <li>{t("privacyPage.s2.li2")}</li>
          <li>{t("privacyPage.s2.li3")}</li>
          <li>{t("privacyPage.s2.li4")}</li>
        </ul>
        <p>{t("privacyPage.s2.noSpam")}</p>
      </InfoSection>

      <InfoSection heading={t("privacyPage.s3.heading")}>
        <p>{t("privacyPage.s3.p1")}</p>
      </InfoSection>

      <InfoSection heading={t("privacyPage.s4.heading")}>
        <p>{t("privacyPage.s4.p1")}</p>
        <p>{t("privacyPage.s4.p2")}</p>
      </InfoSection>

      <InfoSection heading={t("privacyPage.s5.heading")}>
        <p>
          {t("privacyPage.s5.before")}
          <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
            {CONTACT_EMAIL}
          </a>
          {t("privacyPage.s5.after")}
        </p>
      </InfoSection>
    </InfoPage>
  );
}
