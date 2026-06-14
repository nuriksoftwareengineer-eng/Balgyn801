import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { STORE_TELEGRAM_URL } from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

const inputClass =
  "w-full border-b border-[--color-border] bg-transparent py-3 text-[15px] text-black outline-none transition placeholder:text-[--color-muted] focus:border-black";

const labelClass =
  "text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-[--color-muted]";

/** Extracts the Telegram username from the store URL (https://t.me/username). */
function tgUsername(url: string): string {
  return url.replace(/^https?:\/\/t\.me\//, "").split("?")[0];
}

export function CustomDesignPage() {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [description, setDescription] = useState("");
  const [wishes, setWishes] = useState("");
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);

  function buildTelegramLink(): string {
    const message = [
      "Новая заявка BALGYN",
      "",
      `Имя: ${name.trim()}`,
      `Телефон: ${phone.trim()}`,
      `Описание: ${description.trim()}`,
      `Пожелания: ${wishes.trim() || "—"}`,
      `Комментарий: ${comment.trim() || "—"}`,
    ].join("\n");

    const username = tgUsername(STORE_TELEGRAM_URL);
    return `https://t.me/${username}?text=${encodeURIComponent(message)}`;
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!name.trim() || !phone.trim() || !description.trim()) {
      setError("Заполните имя, телефон и описание");
      return;
    }
    window.open(buildTelegramLink(), "_blank", "noopener,noreferrer");
  }

  return (
    <>
      {/* ── Hero ─────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-12 md:py-16">
          <nav className="mb-5 flex items-center gap-2 text-[0.55rem] uppercase tracking-[0.16em] text-white/40">
            <Link to="/" className="transition hover:text-white/70">Главная</Link>
            <span>/</span>
            <span className="text-white/70">Свой дизайн</span>
          </nav>
          <h1 className="text-4xl font-extrabold uppercase tracking-[-0.02em] text-white md:text-6xl">
            Свой дизайн
          </h1>
        </Container>
      </div>

      {/* ── Content ──────────────────────────────────────────── */}
      <Container className="py-10 md:py-14">
        <div className="max-w-2xl">
          <p className="mb-10 text-[15px] leading-relaxed text-[--color-muted]">
            Заполните форму — нажмите кнопку, и Telegram откроется с готовым сообщением.
            Нам останется только отправить.
          </p>

          <form onSubmit={submit} className="flex flex-col gap-8">
            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>Имя *</span>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ваше имя"
                className={inputClass}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>Телефон или Telegram *</span>
              <input
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+7 … или @username"
                className={inputClass}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>Описание *</span>
              <textarea
                required
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Что вышить, на чём, референсы…"
                className={`${inputClass} resize-y`}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>Пожелания <span className="normal-case font-normal tracking-normal">(необязательно)</span></span>
              <textarea
                rows={3}
                value={wishes}
                onChange={(e) => setWishes(e.target.value)}
                placeholder="Цвет, размер, срок…"
                className={`${inputClass} resize-y`}
              />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className={labelClass}>Комментарий <span className="normal-case font-normal tracking-normal">(необязательно)</span></span>
              <input
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Любая дополнительная информация"
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
              className="mt-2 flex w-full items-center justify-center gap-3 bg-black py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800 sm:w-auto sm:px-10"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current" aria-hidden="true">
                <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
              </svg>
              Отправить в Telegram
            </button>
          </form>
        </div>
      </Container>
    </>
  );
}
