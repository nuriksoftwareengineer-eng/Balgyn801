import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Button } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";

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
        <Link
          to="/"
          className="mb-6 inline-block text-sm font-semibold text-zinc-300 hover:text-white hover:underline"
        >
          ← На главную
        </Link>
        <h1 className="font-display text-4xl tracking-wide text-zinc-100 md:text-5xl">
          Свой дизайн
        </h1>
        <p className="mt-4 text-lg leading-relaxed text-zinc-400">
          Опишите задачу — откроется черновик письма на нашу почту. Так мы
          соберём заявки до запуска полноценной формы в личном кабинете.
        </p>

        <ol className="mt-8 space-y-3 text-sm text-zinc-500">
          <li>
            <span className="font-semibold text-zinc-100">1.</span> Текст,
            логотип или ссылка на референс
          </li>
          <li>
            <span className="font-semibold text-zinc-100">2.</span> Желаемая
            вещь (худи, футболка, размер)
          </li>
          <li>
            <span className="font-semibold text-zinc-100">3.</span> Мы ответим
            с ориентиром по цене и сроку
          </li>
        </ol>

        <form
          onSubmit={submit}
          className="mt-10 space-y-5 rounded-[14px] border border-white/10 bg-zinc-900/40 p-6 md:p-8"
        >
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="text-zinc-400">Имя</span>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="text-zinc-400">Телефон или Telegram</span>
            <input
              required
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+7 … или @username"
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="text-zinc-400">Описание идеи</span>
            <textarea
              required
              rows={5}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Что вышить, цвета, размер, пожелания…"
              className="resize-y rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="text-zinc-400">Ссылка на референс (необязательно)</span>
            <input
              type="url"
              value={referenceUrl}
              onChange={(e) => setReferenceUrl(e.target.value)}
              placeholder="https://…"
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            />
          </label>

          {error ? (
            <p className="text-sm font-medium text-red-400">{error}</p>
          ) : null}
          {hint ? (
            <p className="text-sm text-zinc-300">
              Если почта не открылась — дублируйте заявку в{" "}
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="font-semibold text-zinc-100 underline underline-offset-2 hover:text-white"
              >
                Telegram
              </a>
              .
            </p>
          ) : null}

          <div className="flex flex-wrap gap-3 pt-2">
            <Button type="submit" variant="primary" className="rounded-full px-8">
              Отправить заявку
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full"
              onClick={() =>
                window.open(STORE_TELEGRAM_URL, "_blank", "noopener,noreferrer")
              }
            >
              Telegram
            </Button>
          </div>
        </form>
      </Container>
    </div>
  );
}
