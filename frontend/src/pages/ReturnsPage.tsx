import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function ReturnsPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t("returnsPage.title")} lead={t("returnsPage.lead")}>
      <InfoSection heading={t("returnsPage.s1.heading")}>
        <p>{t("returnsPage.s1.p1")}</p>
        <p>{t("returnsPage.s1.p2")}</p>
      </InfoSection>

      <InfoSection heading={t("returnsPage.s2.heading")}>
        <p>
          {t("returnsPage.s2.before")}
          <Link to="/custom-design" className={linkClass}>
            {t("returnsPage.s2.customLink")}
          </Link>
          {t("returnsPage.s2.after")}
        </p>
      </InfoSection>

      <InfoSection heading={t("returnsPage.s3.heading")}>
        <p>{t("returnsPage.s3.p1")}</p>
      </InfoSection>

      <InfoSection heading={t("returnsPage.s4.heading")}>
        <ol className="list-decimal space-y-1.5 pl-5">
          <li>
            {t("returnsPage.s4.step1before")}
            <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
              {CONTACT_EMAIL}
            </a>{" "}
            {t("returnsPage.s4.step1telegram") && (
              <>
                {"или "}
                <a
                  href={STORE_TELEGRAM_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={linkClass}
                >
                  {t("returnsPage.s4.step1telegram")}
                </a>
              </>
            )}
            {t("returnsPage.s4.step1after")}
          </li>
          <li>{t("returnsPage.s4.step2")}</li>
          <li>{t("returnsPage.s4.step3")}</li>
        </ol>
      </InfoSection>
    </InfoPage>
  );
}
