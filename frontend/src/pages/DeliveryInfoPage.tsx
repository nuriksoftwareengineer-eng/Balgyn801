import { useTranslation } from "react-i18next";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

export function DeliveryInfoPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t("deliveryPage.title")} lead={t("deliveryPage.lead")}>
      <InfoSection heading={t("deliveryPage.kazakhstan.heading")}>
        <div className="flex flex-col divide-y divide-[--color-border] border border-[--color-border] bg-white">
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">
              {t("deliveryPage.kazakhstan.pickup.title")}
            </p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              {t("deliveryPage.kazakhstan.pickup.desc")}
            </p>
          </div>
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">
              {t("deliveryPage.kazakhstan.courier.title")}
            </p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              {t("deliveryPage.kazakhstan.courier.desc")}
            </p>
          </div>
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">
              {t("deliveryPage.kazakhstan.kazpost.title")}
            </p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              {t("deliveryPage.kazakhstan.kazpost.desc")}
            </p>
          </div>
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">
              {t("deliveryPage.kazakhstan.cdek.title")}
            </p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              {t("deliveryPage.kazakhstan.cdek.desc")}
            </p>
          </div>
        </div>
      </InfoSection>

      <InfoSection heading={t("deliveryPage.russia.heading")}>
        <p>{t("deliveryPage.russia.p1")}</p>
      </InfoSection>

      <InfoSection heading={t("deliveryPage.international.heading")}>
        <p>{t("deliveryPage.international.p1")}</p>
      </InfoSection>

      <InfoSection heading={t("deliveryPage.payment.heading")}>
        <p>{t("deliveryPage.payment.p1")}</p>
        <p>{t("deliveryPage.payment.p2")}</p>
      </InfoSection>

      <InfoSection heading={t("deliveryPage.timing.heading")}>
        <p>{t("deliveryPage.timing.p1")}</p>
      </InfoSection>
    </InfoPage>
  );
}
