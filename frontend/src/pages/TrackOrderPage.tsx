import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { STORE_TELEGRAM_URL } from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const inputClass =
  "w-full rounded-none border border-[--color-border] bg-white px-3 py-2.5 text-sm text-black outline-none transition focus:border-black focus:ring-1 focus:ring-black";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

/**
 * Отслеживание заказа. Интерфейс подготовлен под будущую интеграцию трекинга
 * СДЭК: форма уже собирает номер заказа и телефон; когда на бэкенде появится
 * публичный эндпоинт статуса, сюда добавится запрос вместо текущей подсказки.
 */
export function TrackOrderPage() {
  const { token } = useAuth();
  const [orderNumber, setOrderNumber] = useState("");
  const [phone, setPhone] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!orderNumber.trim() || !phone.trim()) {
      setError("Укажите номер заказа и телефон, на который он оформлен");
      return;
    }
    setSubmitted(true);
  }

  return (
    <InfoPage
      title="Отследить заказ"
      lead="Укажите номер заказа и телефон, на который он был оформлен."
    >
      <form
        onSubmit={submit}
        className="flex max-w-md flex-col gap-4 border border-[--color-border] bg-[--color-surface] p-5"
      >
        <label className="flex flex-col gap-1.5">
          <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
            Номер заказа *
          </span>
          <input
            required
            inputMode="numeric"
            placeholder="Например, 24"
            value={orderNumber}
            onChange={(e) => setOrderNumber(e.target.value)}
            className={inputClass}
          />
        </label>
        <label className="flex flex-col gap-1.5">
          <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
            Телефон *
          </span>
          <input
            required
            type="tel"
            placeholder="+7 …"
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
          Проверить статус
        </button>
      </form>

      {submitted ? (
        <div
          className="max-w-md border border-[--color-border] bg-white px-5 py-4"
          role="status"
        >
          <p className="m-0 text-sm font-semibold text-black">
            Заказ №{orderNumber.trim()}
          </p>
          <p className="m-0 mt-1.5 text-sm leading-relaxed text-[--color-muted]">
            Онлайн-трекинг СДЭК скоро появится на этой странице. Пока статус
            этого заказа можно узнать за минуту в{" "}
            <a
              href={STORE_TELEGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              Telegram
            </a>{" "}
            — назовите номер заказа и телефон.
          </p>
        </div>
      ) : null}

      <InfoSection heading="Личный кабинет">
        <p>
          {token ? (
            <>
              Статусы всех ваших заказов доступны в{" "}
              <Link to="/orders" className={linkClass}>
                истории заказов
              </Link>
              .
            </>
          ) : (
            <>
              Если вы оформляли заказ под аккаунтом —{" "}
              <Link to="/login" className={linkClass}>
                войдите
              </Link>
              , и статусы всех заказов будут в{" "}
              <Link to="/orders" className={linkClass}>
                истории заказов
              </Link>
              .
            </>
          )}
        </p>
      </InfoSection>

      <InfoSection heading="Статусы заказа">
        <ul className="list-disc space-y-1.5 pl-5">
          <li>
            <strong className="font-semibold text-black">Ожидает оплаты</strong>{" "}
            — заказ создан, ждём подтверждения платежа;
          </li>
          <li>
            <strong className="font-semibold text-black">Подтверждён</strong> —
            оплата получена, заказ в очереди на производство;
          </li>
          <li>
            <strong className="font-semibold text-black">В производстве</strong>{" "}
            — изделие вышивается;
          </li>
          <li>
            <strong className="font-semibold text-black">Готов</strong> — ждёт
            передачи в доставку, присылаем фото;
          </li>
          <li>
            <strong className="font-semibold text-black">Отправлен</strong> —
            передан в службу доставки;
          </li>
          <li>
            <strong className="font-semibold text-black">Доставлен</strong> —
            заказ у вас.
          </li>
        </ul>
      </InfoSection>
    </InfoPage>
  );
}
