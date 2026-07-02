import { useTranslation } from "react-i18next";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
  TELEGRAM_CHANNEL_URL,
  WHATSAPP_URL,
} from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function ContactsPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t("contactsPage.title")} lead={t("contactsPage.lead")}>
      <InfoSection heading={t("contactsPage.contactsHeading")}>
        <div className="flex flex-col divide-y divide-[--color-border] border border-[--color-border] bg-white">
          <div className="flex flex-wrap items-baseline justify-between gap-2 px-4 py-3">
            <span className="text-xs uppercase tracking-[0.12em] text-[--color-muted]">
              Telegram
            </span>
            <a
              href={STORE_TELEGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              @balgyn_shop
            </a>
          </div>
          <div className="flex flex-wrap items-baseline justify-between gap-2 px-4 py-3">
            <span className="text-xs uppercase tracking-[0.12em] text-[--color-muted]">
              Telegram Channel
            </span>
            <a
              href={TELEGRAM_CHANNEL_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              @balgyn_channel
            </a>
          </div>
          <div className="flex flex-wrap items-baseline justify-between gap-2 px-4 py-3">
            <span className="text-xs uppercase tracking-[0.12em] text-[--color-muted]">
              WhatsApp
            </span>
            <a
              href={WHATSAPP_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              +7 700 123-45-67
            </a>
          </div>
          <div className="flex flex-wrap items-baseline justify-between gap-2 px-4 py-3">
            <span className="text-xs uppercase tracking-[0.12em] text-[--color-muted]">
              Email
            </span>
            <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
              {CONTACT_EMAIL}
            </a>
          </div>
          <div className="flex flex-wrap items-baseline justify-between gap-2 px-4 py-3">
            <span className="text-xs uppercase tracking-[0.12em] text-[--color-muted]">
              Instagram
            </span>
            <a
              href="https://instagram.com/balgyn_shop"
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              @balgyn_shop
            </a>
          </div>
        </div>
      </InfoSection>

      <InfoSection heading={t("contactsPage.pickupHeading")}>
        <p>{t("contactsPage.pickupDesc")}</p>
      </InfoSection>

      <InfoSection heading={t("contactsPage.ordersHeading")}>
        <p>{t("contactsPage.ordersDesc")}</p>
      </InfoSection>
    </InfoPage>
  );
}
