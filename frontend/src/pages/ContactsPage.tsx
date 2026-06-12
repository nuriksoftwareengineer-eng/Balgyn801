import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function ContactsPage() {
  return (
    <InfoPage
      title="Контакты"
      lead="Отвечаем по возможности в течение рабочего дня. Быстрее всего — Telegram."
    >
      <InfoSection heading="Связь">
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

      <InfoSection heading="Самовывоз">
        <p>
          Город Алматы, по договорённости: после оформления заказа мы согласуем
          удобные место и время передачи по телефону или в Telegram.
        </p>
      </InfoSection>

      <InfoSection heading="По вопросам заказов">
        <p>
          Назовите номер заказа — он указан на экране подтверждения и в истории
          заказов личного кабинета. Так мы ответим быстрее.
        </p>
      </InfoSection>
    </InfoPage>
  );
}
