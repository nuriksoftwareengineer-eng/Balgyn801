import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/app/auth-context";
import { STORE_TELEGRAM_URL } from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";
import { OrderTrackingPanel } from "@/shared/ui/OrderTrackingPanel";
import {
  getMyOrderTrackingInfo,
  getOrderTrackingInfo,
} from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import type { OrderTrackingResponse } from "@/shared/api/types";

const inputClass =
  "w-full rounded-none border border-[--color-border] bg-white px-3 py-2.5 text-sm text-black outline-none transition focus:border-black focus:ring-1 focus:ring-black";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function TrackOrderPage() {
  const { t } = useTranslation();
  const { token } = useAuth();
  const isLoggedIn = !!token;

  const [orderNumber, setOrderNumber] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tracking, setTracking] = useState<OrderTrackingResponse | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setTracking(null);

    const orderId = parseInt(orderNumber.trim(), 10);
    if (!orderNumber.trim() || isNaN(orderId) || orderId <= 0) {
      setError(t("trackOrder.errorRequired"));
      return;
    }
    if (!isLoggedIn && !phone.trim()) {
      setError(t("trackOrder.errorRequired"));
      return;
    }

    setLoading(true);
    try {
      const result = isLoggedIn
        ? await getMyOrderTrackingInfo(orderId, token!)
        : await getOrderTrackingInfo(orderId, phone.trim());
      setTracking(result);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 404) {
        setError(t("trackOrder.notFound"));
      } else {
        setError(t("trackOrder.errorGeneric"));
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <InfoPage
      title={t("trackOrder.title")}
      lead={t("trackOrder.lead")}
    >
      <form
        onSubmit={submit}
        className="flex max-w-md flex-col gap-4 border border-[--color-border] bg-[--color-surface] p-5"
      >
        <label className="flex flex-col gap-1.5">
          <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
            {t("trackOrder.orderNumberLabel")}
          </span>
          <input
            required
            inputMode="numeric"
            placeholder={t("trackOrder.orderNumberPlaceholder")}
            value={orderNumber}
            onChange={(e) => setOrderNumber(e.target.value)}
            className={inputClass}
          />
        </label>

        {/* Phone is only needed for guests */}
        {!isLoggedIn && (
          <label className="flex flex-col gap-1.5">
            <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
              {t("trackOrder.phoneLabel")}
            </span>
            <input
              required
              type="tel"
              placeholder={t("trackOrder.phonePlaceholder")}
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className={inputClass}
            />
          </label>
        )}

        {error ? (
          <p className="m-0 text-sm font-medium text-[--color-danger]" role="alert">
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={loading}
          className="inline-flex h-11 items-center justify-center bg-black px-6 text-sm font-medium tracking-wide text-white transition hover:bg-zinc-800 disabled:opacity-50"
        >
          {loading ? t("trackOrder.searching") : t("trackOrder.submitBtn")}
        </button>
      </form>

      {/* ── Tracking result ── */}
      {tracking && (
        <div className="max-w-md border border-[--color-border] bg-white px-5 py-5" role="status">
          <p className="mb-4 text-[10px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
            {t("trackOrder.resultTitle", { number: tracking.orderId })}
          </p>
          <OrderTrackingPanel tracking={tracking} showDocs={isLoggedIn} />
        </div>
      )}

      {/* ── CDEK app block — shown when result has CDEK shipment ── */}
      {tracking?.cdekShipment?.cdekOrderUuid && (
        <div className="max-w-md border border-[--color-border] bg-[--color-surface] p-5">
          <p className="mb-1 text-[0.6rem] font-semibold uppercase tracking-[0.16em] text-[--color-muted]">
            {t("trackOrder.cdekApp.cdekNote")}
          </p>
          <p className="mb-1 text-[14px] font-semibold text-black">{t("trackOrder.cdekApp.title")}</p>
          <p className="mb-3 text-[13px] leading-relaxed text-zinc-600">{t("trackOrder.cdekApp.desc")}</p>
          <ul className="mb-4 flex flex-col gap-1.5">
            {(t("trackOrder.cdekApp.features", { returnObjects: true }) as string[]).map((f) => (
              <li key={f} className="flex items-center gap-2 text-[13px] text-zinc-600">
                <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-black" />
                {f}
              </li>
            ))}
          </ul>
          <div className="flex flex-wrap gap-3">
            {/* App Store */}
            <a
              href="https://apps.apple.com/ru/app/%D1%81%D0%B4%D1%8D%D0%BA/id906286734"
              target="_blank"
              rel="noopener noreferrer"
              aria-label={t("trackOrder.cdekApp.appStore")}
              className="inline-flex h-11 items-center gap-2.5 rounded-lg bg-black px-4 transition hover:bg-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-black focus-visible:ring-offset-2"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-white" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
              </svg>
              <div className="flex flex-col leading-tight">
                <span className="text-[9px] font-medium text-white/70">Download on the</span>
                <span className="text-[13px] font-semibold text-white">App Store</span>
              </div>
            </a>
            {/* Google Play */}
            <a
              href="https://play.google.com/store/apps/details?id=ru.cdek.app"
              target="_blank"
              rel="noopener noreferrer"
              aria-label={t("trackOrder.cdekApp.googlePlay")}
              className="inline-flex h-11 items-center gap-2.5 rounded-lg bg-black px-4 transition hover:bg-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-black focus-visible:ring-offset-2"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                <path d="M3.18 23.76c.3.17.64.24.99.2l13.24-12.15L13.9 8.4 3.18 23.76z" fill="#EA4335"/>
                <path d="M22.47 10.2L19.5 8.54l-3.69 3.37 3.69 3.38 2.99-1.69a1.76 1.76 0 0 0 0-3.4z" fill="#FBBC04"/>
                <path d="M3.18.24a1.75 1.75 0 0 0-.98 1.6v20.32c0 .67.37 1.27.98 1.6L16.42 11.9 3.18.24z" fill="#4285F4"/>
                <path d="M17.41 11.91L4.17.25C3.82.07 3.41.03 3.04.2L13.9 11.91l3.51-2.21v2.21z" fill="#34A853"/>
              </svg>
              <div className="flex flex-col leading-tight">
                <span className="text-[9px] font-medium text-white/70">Get it on</span>
                <span className="text-[13px] font-semibold text-white">Google Play</span>
              </div>
            </a>
          </div>
        </div>
      )}

      {/* ── Fallback: Telegram help link ── */}
      {!tracking && (
        <InfoSection heading={t("footer.help.trackOrder")}>
          <p className="text-[14px] leading-relaxed text-[--color-muted]">
            {t("trackOrder.telegramHelp")}{" "}
            <a
              href={STORE_TELEGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              Telegram
            </a>
          </p>
          {isLoggedIn ? (
            <p className="mt-2 text-[14px] leading-relaxed text-[--color-muted]">
              <Link to="/orders" className={linkClass}>
                {t("profile.myOrders")}
              </Link>
            </p>
          ) : (
            <p className="mt-2 text-[14px] leading-relaxed text-[--color-muted]">
              <Link to="/login" className={linkClass}>{t("auth.loginLinkText")}</Link>{" "}
              →{" "}
              <Link to="/orders" className={linkClass}>{t("orders.title")}</Link>
            </p>
          )}
        </InfoSection>
      )}
    </InfoPage>
  );
}
