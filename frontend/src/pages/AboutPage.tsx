import { Link } from "react-router-dom";
import ceoPortrait from "@/assets/ceo-dias-abris.png";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

export function AboutPage() {
  return (
    <div className="py-14">
      <Container className="max-w-4xl">
        <h1 className="font-display mb-4 text-4xl tracking-wide text-zinc-100 md:text-5xl">
          О нас
        </h1>
        <p className="mb-12 max-w-2xl text-lg leading-relaxed text-zinc-400">
          BALGYN — одежда с акцентом на вышивку и характер: худи, футболки и вещи
          под кастом, когда важны детали и сроки.
        </p>

        <section className="mb-14 grid gap-10 md:grid-cols-[minmax(0,280px)_1fr] md:items-center md:gap-12">
          <div className="mx-auto w-full max-w-[280px] md:mx-0">
            <div className="overflow-hidden rounded-2xl border border-white/10 bg-zinc-900 shadow-[0_24px_60px_-20px_rgba(0,0,0,0.65)] ring-1 ring-white/5">
              <img
                src={ceoPortrait}
                alt="Dias Abris, CEO BALGYN"
                className="aspect-[4/5] w-full object-cover object-[center_15%]"
                loading="lazy"
                decoding="async"
              />
            </div>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-violet-400">
              CEO
            </p>
            <h2 className="font-display mt-2 text-3xl tracking-wide text-zinc-100 md:text-4xl">
              DIAS ABRIS
            </h2>
            <p className="mt-4 leading-relaxed text-zinc-400">
              Руковожу развитием бренда и производством: от идеи и макета до финальной
              вышивки и отгрузки. Задача BALGYN — сочетать уличную эстетику и
              ремесленное качество, чтобы вещь носилась годами, а не сезон.
            </p>
            <p className="mt-4 leading-relaxed text-zinc-500">
              По вопросам сотрудничества и кастома пишите в Telegram или на почту —
              отвечаем по возможности в течение рабочего дня.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link
                to="/custom-design"
                className="inline-flex rounded-full bg-violet-500 px-6 py-2.5 text-sm font-semibold text-white transition hover:brightness-110"
              >
                Свой дизайн
              </Link>
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex rounded-full border border-white/15 bg-white/5 px-6 py-2.5 text-sm font-semibold text-zinc-200 transition hover:bg-white/10"
              >
                Telegram
              </a>
            </div>
          </div>
        </section>

        <section className="mb-10">
          <h2 className="mb-3 text-xl font-semibold text-zinc-100">
            Как мы работаем
          </h2>
          <ul className="list-inside list-disc space-y-2 text-zinc-400">
            <li>
              Заказ оформляется на сайте: вы выбираете модели в{" "}
              <Link to="/catalog" className="text-violet-400 hover:underline">
                каталоге
              </Link>
              , указываете способ получения и контакты.
            </li>
            <li>
              Индивидуальные макеты — через раздел{" "}
              <Link to="/custom-design" className="text-violet-400 hover:underline">
                Свой дизайн
              </Link>{" "}
              или напрямую в Telegram.
            </li>
            <li>
              Производство и отгрузка — после подтверждения заказа; сроки зависят от
              загрузки и сложности вышивки.
            </li>
          </ul>
        </section>

        <section className="mb-10">
          <h2 className="mb-3 text-xl font-semibold text-zinc-100">
            Качество и упаковка
          </h2>
          <p className="leading-relaxed text-zinc-400">
            Проверяем посадку изделий, крепление вышивки и упаковку перед отправкой.
            Если что-то пошло не так по дороге — напишите нам, разберём ситуацию.
          </p>
        </section>

        <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-6">
          <h2 className="mb-3 text-xl font-semibold text-zinc-100">Контакты</h2>
          <ul className="space-y-2 text-zinc-400">
            <li>
              Email:{" "}
              <a
                href={`mailto:${CONTACT_EMAIL}`}
                className="font-medium text-violet-400 hover:underline"
              >
                {CONTACT_EMAIL}
              </a>
            </li>
            <li>
              Telegram:{" "}
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium text-violet-400 hover:underline"
              >
                написать в магазин
              </a>
            </li>
          </ul>
        </section>
      </Container>
    </div>
  );
}
