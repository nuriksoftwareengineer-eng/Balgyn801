import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

const inputClass =
  "w-full rounded-none border border-[--color-border] bg-white px-3 py-2.5 text-sm text-black outline-none transition focus:border-black focus:ring-1 focus:ring-black";

export function CustomDesignPage() {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [description, setDescription] = useState("");
  const [referenceUrl, setReferenceUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [hint, setHint] = useState(false);

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!name.trim() || !phone.trim() || !description.trim()) {
      setError("Заполните имя, телефон и описание идеи");
      return;
    }
    const subject = encodeURIComponent("Заявка: свой дизайн BALGYN");
    const body = encodeURIComponent(
      `Имя: ${name.trim()}\nТелефон: ${phone.trim()}\n\nИдея / ТЗ:\n${description.trim()}\n\nРеференс (ссылка): ${referenceUrl.trim() || "—"}\n`,
    );
    window.location.href = `mailto:${CONTACT_EMAIL}?subject=${subject}&body=${body}`;
    setHint(true);
  }

  return (
    <div className="py-12 md:py-16">
      <Container className="max-w-2xl">
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            Главная
          </Link>
          <span aria-hidden>›</span>
          <span className="text-black">Свой дизайн</span>
        </nav>

        <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
          Свой дизайн
        </h1>
        <p className="mt-4 text-sm leading-relaxed text-[--color-muted]">
          Опишите задачу — откроется черновик письма на нашу почту. Так мы
          соберём заявки до запуска полноценной формы в личном кабинете.
        </p>

        <ol className="mt-8 flex flex-col divide-y divide-[--color-border] border border-[--color-border] bg-white text-sm">
          <li className="flex gap-3 px-4 py-3">
            <span className="font-semibold text-black">1.</span>
            <span className="text-zinc-700">
              Текст, логотип или ссылка на референс
            </span>
          </li>
          <li className="flex gap-3 px-4 py-3">
            <span className="font-semibold text-black">2.</span>
            <span className="text-zinc-700">
              Желаемая вещь (худи, футболка, размер)
            </span>
          </li>
          <li className="flex gap-3 px-4 py-3">
            <span className="font-semibold text-black">3.</span>
            <span className="text-zinc-700">
              Мы ответим с ориентиром по цене и сроку
            </span>
          </li>
        </ol>

        <form
          onSubmit={submit}
          className="mt-10 space-y-5 border border-[--color-border] bg-[--color-surface] p-6 md:p-8"
        >
          <label className="flex flex-col gap-1.5">
            <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
              Имя *
            </span>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
              Телефон или Telegram *
            </span>
            <input
              required
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+7 … или @username"
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
              Описание идеи *
            </span>
            <textarea
              required
              rows={5}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Что вышить, цвета, размер, пожелания…"
              className={`${inputClass} resize-y`}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
              Ссылка на референс{" "}
              <span className="normal-case font-normal">(необязательно)</span>
            </span>
            <input
              type="url"
              value={referenceUrl}
              onChange={(e) => setReferenceUrl(e.target.value)}
              placeholder="https://…"
              className={inputClass}
            />
          </label>

          {error ? (
            <p className="text-sm font-medium text-[--color-danger]" role="alert">
              {error}
            </p>
          ) : null}
          {hint ? (
            <p className="text-sm text-zinc-700">
              Если почта не открылась — дублируйте заявку в{" "}
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600"
              >
                Telegram
              </a>
              .
            </p>
          ) : null}

          <div className="flex flex-wrap gap-3 pt-2">
            <button
              type="submit"
              className="inline-flex h-11 items-center justify-center bg-black px-7 text-sm font-medium tracking-wide text-white transition hover:bg-zinc-800"
            >
              Отправить заявку
            </button>
            <button
              type="button"
              className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-7 text-sm font-medium tracking-wide text-black transition hover:border-black"
              onClick={() =>
                window.open(STORE_TELEGRAM_URL, "_blank", "noopener,noreferrer")
              }
            >
              Telegram
            </button>
          </div>
        </form>
      </Container>
    </div>
  );
}
