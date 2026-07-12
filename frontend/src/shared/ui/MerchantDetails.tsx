import { useTranslation } from "react-i18next";
import { MERCHANT } from "@/shared/constants/store-content";

interface Props {
  /** `light` — Контакты/Оферта (полные реквизиты). `dark` — тёмный футер. */
  tone?: "light" | "dark";
  /** Краткая версия для футера: без имени владельца, мельче текст. Полные данные — на Контактах/Оферте. */
  compact?: boolean;
}

/**
 * Реквизиты ИП — обязательный блок для комплаенса Freedom Pay.
 * Единый источник данных — {@link MERCHANT}. Визуально всегда вторичен
 * по отношению к бренду: мелкий текст, без таблиц и рамок.
 */
export function MerchantDetails({ tone = "light", compact = false }: Props) {
  const { t } = useTranslation();

  const textCls = tone === "dark" ? "text-[#8A8A8A]" : "text-zinc-500";
  const strongCls = tone === "dark" ? "text-[#B0B0B0]" : "text-zinc-800";
  const linkCls = tone === "dark" ? "hover:text-white" : "hover:text-black";
  const size = compact ? "text-[11px]" : "text-[12px]";

  return (
    <dl className={`m-0 flex flex-col gap-0.5 ${size} leading-[1.6] transition-colors ${textCls}`}>
      <div>
        <dt className="sr-only">{t("merchant.ie")}</dt>
        <dd className={`m-0 ${strongCls}`}>
          {t("merchant.ieShort")} {MERCHANT.ieName}
        </dd>
      </div>
      {!compact && (
        <div>
          <dt className="sr-only">{t("merchant.owner")}</dt>
          <dd className="m-0">{MERCHANT.owner}</dd>
        </div>
      )}
      <div>
        <dt className="sr-only">{t("merchant.iin")}</dt>
        <dd className="m-0">
          {t("merchant.iinShort")} {MERCHANT.iin}
        </dd>
      </div>
      <div>
        <dt className="sr-only">{t("merchant.legalAddress")}</dt>
        <dd className="m-0">
          {MERCHANT.addressLines.map((line, i) => (
            <span key={i} className="block">
              {line}
            </span>
          ))}
        </dd>
      </div>
      <div>
        <dt className="sr-only">{t("merchant.phone")}</dt>
        <dd className="m-0">
          <a href={`tel:${MERCHANT.phoneHref}`} className={`${linkCls} transition-colors`}>
            {MERCHANT.phone}
          </a>
        </dd>
      </div>
      <div>
        <dt className="sr-only">{t("merchant.email")}</dt>
        <dd className="m-0">
          <a href={`mailto:${MERCHANT.email}`} className={`${linkCls} transition-colors`}>
            {MERCHANT.email}
          </a>
        </dd>
      </div>
    </dl>
  );
}
