import { useTranslation } from "react-i18next";
import type { DeliveryType, OrderStatus, OrderTrackingResponse } from "@/shared/api/types";

const STATUS_PILL: Record<OrderStatus, string> = {
  NEW:             "bg-blue-50  text-blue-700  border-blue-200",
  CONFIRMED:       "bg-amber-50 text-amber-700 border-amber-200",
  IN_PRODUCTION:   "bg-zinc-100 text-zinc-600  border-zinc-200",
  READY:           "bg-emerald-50 text-emerald-700 border-emerald-200",
  SHIPPED:         "bg-sky-50   text-sky-700   border-sky-200",
  DELIVERED:       "bg-green-50 text-green-700 border-green-200",
  CANCELLED:       "bg-red-50   text-red-600   border-red-200",
  PENDING_PAYMENT: "bg-yellow-50 text-yellow-700 border-yellow-200",
  EXPIRED:         "bg-gray-100 text-gray-500  border-gray-200",
};

const STATUS_DOT: Record<OrderStatus, string> = {
  NEW:             "bg-blue-500",
  CONFIRMED:       "bg-amber-500",
  IN_PRODUCTION:   "bg-zinc-400",
  READY:           "bg-emerald-500",
  SHIPPED:         "bg-sky-500",
  DELIVERED:       "bg-green-600",
  CANCELLED:       "bg-red-500",
  PENDING_PAYMENT: "bg-yellow-400",
  EXPIRED:         "bg-gray-300",
};

const LANG_LOCALE: Record<string, string> = {
  ru: "ru-RU",
  kk: "kk-KZ",
  en: "en-US",
};

function fmtEpoch(epochMs: number, lang: string) {
  const locale = LANG_LOCALE[lang] ?? "ru-RU";
  return new Date(epochMs).toLocaleString(locale, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function trackingUrl(trackingNumber: string, deliveryType: DeliveryType): string {
  if (deliveryType === "CDEK") {
    return `https://www.cdek.ru/ru/tracking/?order_id=${encodeURIComponent(trackingNumber)}`;
  }
  // KazPost and others: no URL parameter support — user copies the number
  return "https://track.kazpost.kz/";
}

interface Props {
  tracking: OrderTrackingResponse;
  /** Show invoice/barcode PDF links (admin or authenticated user) */
  showDocs?: boolean;
}

export function OrderTrackingPanel({ tracking, showDocs = false }: Props) {
  const { t, i18n } = useTranslation();
  const { cdekShipment, statusHistory, trackingNumber, deliveryType } = tracking;

  // Reverse so oldest → newest (chronological order for timeline)
  const chronological = [...statusHistory].reverse();

  const isCdek = deliveryType === "CDEK";
  const isPostal = deliveryType === "POSTAL";
  const hasTracking = !!trackingNumber;

  return (
    <div className="flex flex-col gap-6">
      {/* ── Delivery method ── */}
      <div className="flex flex-wrap items-center gap-3">
        <span
          className={`inline-flex items-center gap-1.5 border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.1em] ${STATUS_PILL[tracking.orderStatus]}`}
        >
          <span className={`h-1.5 w-1.5 rounded-full ${STATUS_DOT[tracking.orderStatus]}`} />
          {t(`orders.status.${tracking.orderStatus}`)}
        </span>
        <span className="text-[12px] uppercase tracking-[0.1em] text-[--color-muted]">
          {t(`orders.delivery_type.${tracking.deliveryType}`, tracking.deliveryType)}
        </span>
        {isCdek && (
          <span className="text-[12px] font-medium text-[--color-muted]">
            · {t("tracking.carrierCdek")}
          </span>
        )}
        {isPostal && (
          <span className="text-[12px] font-medium text-[--color-muted]">
            · {t("tracking.carrierKazpost")}
          </span>
        )}
      </div>

      {/* ── Status history timeline ── */}
      {chronological.length > 0 && (
        <div>
          <p className="mb-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
            {t("tracking.historyTitle")}
          </p>
          <ol className="flex flex-col gap-0">
            {chronological.map((entry, idx) => (
              <li key={idx} className="flex gap-3">
                {/* timeline column */}
                <div className="flex flex-col items-center">
                  <span
                    className={`h-2.5 w-2.5 rounded-full border-2 border-white ring-2 ${
                      idx === chronological.length - 1
                        ? "ring-black bg-black"
                        : "ring-zinc-300 bg-zinc-300"
                    }`}
                  />
                  {idx < chronological.length - 1 && (
                    <span className="w-px flex-1 bg-zinc-200" style={{ minHeight: "24px" }} />
                  )}
                </div>
                {/* content */}
                <div className="pb-4">
                  <p className="text-[13px] font-medium text-black">
                    {t(`orders.status.${entry.status}`)}
                  </p>
                  <p className="text-[11px] text-[--color-muted]">
                    {fmtEpoch(entry.occurredAt, i18n.language)}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      )}

      {/* ── Tracking number block (CDEK + POSTAL) ── */}
      {(isCdek || isPostal) && (
        <div className="border border-[--color-border] bg-[--color-surface] p-4 flex flex-col gap-3">
          <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
            {t("tracking.trackNumber")}
          </p>

          {hasTracking ? (
            <div className="flex flex-wrap items-center gap-3">
              <span className="font-mono text-[15px] font-semibold text-black tracking-wider">
                {trackingNumber}
              </span>
              <a
                href={trackingUrl(trackingNumber!, deliveryType)}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 border border-black bg-black px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.1em] text-white transition-colors hover:bg-zinc-800"
              >
                {t("tracking.trackBtn")} →
              </a>
            </div>
          ) : (
            <p className="text-[13px] text-[--color-muted]">
              {t("tracking.noTrackingYet")}
            </p>
          )}

          <p className="text-[11px] leading-relaxed text-[--color-muted]">
            {t("tracking.hint")}
          </p>
        </div>
      )}

      {/* ── CDEK shipment details ── */}
      {cdekShipment && cdekShipment.cdekOrderUuid && (
        <div className="border border-[--color-border] bg-[--color-surface] p-4">
          <p className="mb-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
            {t("orders.cdek.title")}
          </p>

          <div className="flex flex-col gap-2">
            {cdekShipment.status && (
              <div className="flex items-center gap-2">
                <span className="text-[12px] text-[--color-muted]">{t("orders.cdek.status")}:</span>
                <span className="text-[12px] font-medium text-black">
                  {t(`orders.cdek.status_${cdekShipment.status}`, cdekShipment.status)}
                </span>
              </div>
            )}

            {cdekShipment.estimatedDeliveryDate && (
              <div className="flex items-center gap-2">
                <span className="text-[12px] text-[--color-muted]">{t("orders.cdek.eta")}:</span>
                <span className="text-[12px] font-medium text-black">
                  {cdekShipment.estimatedDeliveryDate}
                </span>
              </div>
            )}

            {cdekShipment.deliveryPointAddress && (
              <div className="flex items-start gap-2">
                <span className="shrink-0 text-[12px] text-[--color-muted]">{t("orders.cdek.point")}:</span>
                <span className="text-[12px] font-medium text-black">
                  {cdekShipment.deliveryPointAddress}
                </span>
              </div>
            )}

            {showDocs && (cdekShipment.invoiceUrl || cdekShipment.barcodeUrl) && (
              <div className="mt-1 flex gap-4">
                {cdekShipment.invoiceUrl && (
                  <a
                    href={cdekShipment.invoiceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[11px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-2 hover:text-zinc-600"
                  >
                    {t("orders.cdek.invoice")}
                  </a>
                )}
                {cdekShipment.barcodeUrl && (
                  <a
                    href={cdekShipment.barcodeUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[11px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-2 hover:text-zinc-600"
                  >
                    {t("orders.cdek.barcode")}
                  </a>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
