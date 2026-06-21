import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/app/auth-context";
import { STORE_TELEGRAM_URL } from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const inputClass =
  "w-full rounded-none border border-[--color-border] bg-white px-3 py-2.5 text-sm text-black outline-none transition focus:border-black focus:ring-1 focus:ring-black";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function TrackOrderPage() {
  const { t } = useTranslation();
  const { token } = useAuth();
  const [orderNumber, setOrderNumber] = useState("");
  const [phone, setPhone] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!orderNumber.trim() || !phone.trim()) {
      setError(t("trackOrder.errorRequired"));
      return;
    }
    setSubmitted(true);
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

        {error ? (
          <p className="m-0 text-sm font-medium text-[--color-danger]" role="alert">
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          className="inline-flex h-11 items-center justify-center bg-black px-6 text-sm font-medium tracking-wide text-white transition hover:bg-zinc-800"
        >
          {t("trackOrder.submitBtn")}
        </button>
      </form>

      {submitted ? (
        <div
          className="max-w-md border border-[--color-border] bg-white px-5 py-4"
          role="status"
        >
          <p className="m-0 text-sm font-semibold text-black">
            {t("trackOrder.resultTitle", { number: orderNumber.trim() })}
          </p>
          <p className="m-0 mt-1.5 text-sm leading-relaxed text-[--color-muted]">
            {t("trackOrder.resultDesc")}{" "}
            <a
              href={STORE_TELEGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              Telegram
            </a>
          </p>
        </div>
      ) : null}

      <InfoSection heading={t("footer.help.trackOrder")}>
        <p>
          {token ? (
            <>
              {/* Logged-in: link to order history */}
              <Link to="/orders" className={linkClass}>
                {t("profile.myOrders")}
              </Link>
            </>
          ) : (
            <>
              <Link to="/login" className={linkClass}>
                {t("auth.loginLinkText")}
              </Link>{" "}
              →{" "}
              <Link to="/orders" className={linkClass}>
                {t("orders.title")}
              </Link>
            </>
          )}
        </p>
      </InfoSection>
    </InfoPage>
  );
}
