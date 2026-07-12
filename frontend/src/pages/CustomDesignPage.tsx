import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { SUPPORT_TELEGRAM_URL} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

const inputClass =
  "w-full border-b border-[--color-border] bg-transparent py-3 text-[15px] text-black outline-none transition placeholder:text-[--color-muted] focus:border-black";

const labelClass =
  "text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-[--color-muted]";

function tgUsername(url: string): string {
  return url.replace(/^https?:\/\/t\.me\//, "").split("?")[0];
}

export function CustomDesignPage() {
  const { t } = useTranslation();
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [description, setDescription] = useState("");
  const [wishes, setWishes] = useState("");
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);

  function buildTelegramLink(): string {
    // Message to store owner is always in Russian (business language)
    const message = [
      "Новая заявка BALGYN",
      "",
      `Имя: ${name.trim()}`,
      `Телефон: ${phone.trim()}`,
      `Описание: ${description.trim()}`,
      `Пожелания: ${wishes.trim() || "—"}`,
      `Комментарий: ${comment.trim() || "—"}`,
    ].join("\n");

    const username = tgUsername(SUPPORT_TELEGRAM_URL);
    return `https://t.me/${username}?text=${encodeURIComponent(message)}`;
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!name.trim() || !phone.trim() || !description.trim()) {
      setError(t("customDesign.errorRequired"));
      return;
    }
    window.open(buildTelegramLink(), "_blank", "noopener,noreferrer");
  }

  return (
    <>
      {/* ── Header — light editorial ─────────────────────────── */}
      <Container className="pb-4 pt-12 md:pt-16">
        <nav className="mb-6 flex items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">{t("nav.home")}</Link>
          <span aria-hidden>/</span>
          <span className="text-black">{t("customDesign.title")}</span>
        </nav>
        <h1 className="display text-[40px] uppercase text-black md:text-[60px]">
          {t("customDesign.title")}
        </h1>
      </Container>

      {/* ── Content ──────────────────────────────────────────── */}
      <Container className="py-8 md:py-10">
        <div className="max-w-2xl">
          <p className="mb-10 text-[15px] leading-relaxed text-[--color-muted]">
            {t("customDesign.lead")}
          </p>

          <form onSubmit={submit} className="flex flex-col gap-8">
            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>{t("customDesign.nameLabel")}</span>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t("customDesign.namePlaceholder")}
                className={inputClass}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>{t("customDesign.phoneLabel")}</span>
              <input
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder={t("customDesign.phonePlaceholder")}
                className={inputClass}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>{t("customDesign.descLabel")}</span>
              <textarea
                required
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t("customDesign.descPlaceholder")}
                className={`${inputClass} resize-y`}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>
                {t("customDesign.wishesLabel")}{" "}
                <span className="normal-case font-normal tracking-normal">
                  {t("customDesign.optional")}
                </span>
              </span>
              <textarea
                rows={3}
                value={wishes}
                onChange={(e) => setWishes(e.target.value)}
                placeholder={t("customDesign.wishesPlaceholder")}
                className={`${inputClass} resize-y`}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>
                {t("customDesign.commentLabel")}{" "}
                <span className="normal-case font-normal tracking-normal">
                  {t("customDesign.optional")}
                </span>
              </span>
              <input
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder={t("customDesign.commentPlaceholder")}
                className={inputClass}
              />
            </label>

            {error ? (
              <p className="text-[13px] font-medium text-[--color-danger]" role="alert">
                {error}
              </p>
            ) : null}

            <button
              type="submit"
              className="mt-2 flex w-full items-center justify-center gap-3 bg-black py-4 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800 sm:w-auto sm:px-10"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current" aria-hidden="true">
                <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
              </svg>
              {t("customDesign.submitBtn")}
            </button>
          </form>
        </div>
      </Container>
    </>
  );
}
