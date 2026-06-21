import { Link } from "react-router-dom";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function AboutPage() {
  return (
    <div className="py-12 md:py-16">
      <Container className="max-w-4xl">
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">
            Главная
          </Link>
          <span aria-hidden>›</span>
          <span className="text-black">О нас</span>
        </nav>

        <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black md:text-4xl">
          О нас
        </h1>
        <p className="mt-4 mb-12 max-w-2xl text-sm leading-relaxed text-[--color-muted]">
          BALGYN — одежда с акцентом на вышивку и характер: худи, футболки и
          вещи под кастом, когда важны детали и сроки.
        </p>

        <section className="mb-14 grid gap-10 md:grid-cols-[minmax(0,280px)_1fr] md:items-center md:gap-12">
          <div className="mx-auto w-full max-w-[280px] md:mx-0">
            <div className="relative overflow-hidden border border-[--color-border] bg-[--color-surface]">
              <div className="relative flex aspect-[4/5] items-center justify-center">
                <div className="border border-[--color-border] bg-white px-8 py-8 text-center">
                  <p className="m-0 text-5xl font-semibold tracking-[0.08em] text-black">
                    DA
                  </p>
                  <p className="m-0 mt-1 text-[0.6rem] uppercase tracking-[0.2em] text-[--color-muted]">
                    Balgyn Studio
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div>
            <p className="text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-[--color-muted]">
              CEO
            </p>
            <h2 className="mt-2 text-2xl font-semibold uppercase tracking-[0.04em] text-black md:text-3xl">
              Dias Abris
            </h2>
            <p className="mt-4 text-sm leading-relaxed text-zinc-700">
              Руковожу развитием бренда и производством: от идеи и макета до
              финальной вышивки и отгрузки. Задача BALGYN — сочетать уличную
              эстетику и ремесленное качество, чтобы вещь носилась годами, а не
              сезон.
            </p>
            <p className="mt-4 text-sm leading-relaxed text-[--color-muted]">
              По вопросам сотрудничества и кастома пишите в Telegram или на
              почту — отвечаем по возможности в течение рабочего дня.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link
                to="/custom-design"
                className="inline-flex h-11 items-center justify-center bg-black px-6 text-sm font-medium tracking-wide text-white transition hover:bg-zinc-800"
              >
                Свой дизайн
              </Link>
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex h-11 items-center justify-center border border-[--color-border] bg-white px-6 text-sm font-medium tracking-wide text-black transition hover:border-black"
              >
                Telegram
              </a>
            </div>
          </div>
        </section>

        <section className="mb-10">
          <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            Как мы работаем
          </h2>
          <ul className="list-disc space-y-2 pl-5 text-sm leading-relaxed text-zinc-700">
            <li>
              Заказ оформляется на сайте: вы выбираете модели в{" "}
              <Link to="/catalog" className={linkClass}>
                каталоге
              </Link>
              , указываете способ получения и контакты.
            </li>
            <li>
              Индивидуальные макеты — через раздел{" "}
              <Link to="/custom-design" className={linkClass}>
                Свой дизайн
              </Link>{" "}
              или напрямую в Telegram.
            </li>
            <li>
              Производство и отгрузка — после подтверждения заказа; сроки
              зависят от загрузки и сложности вышивки.
            </li>
          </ul>
        </section>

        <section className="mb-10">
          <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            Качество и упаковка
          </h2>
          <p className="text-sm leading-relaxed text-zinc-700">
            Проверяем посадку изделий, крепление вышивки и упаковку перед
            отправкой. Если что-то пошло не так по дороге — напишите нам,
            разберём ситуацию.
          </p>
        </section>

        <section className="border border-[--color-border] bg-[--color-surface] p-6">
          <h2 className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-black">
            Контакты
          </h2>
          <ul className="m-0 flex list-none flex-col gap-2 p-0 text-sm text-zinc-700">
            <li>
              Email:{" "}
              <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
                {CONTACT_EMAIL}
              </a>
            </li>
            <li>
              Telegram:{" "}
              <a
                href={STORE_TELEGRAM_URL}
                target="_blank"
                rel="noopener noreferrer"
                className={linkClass}
              >
                написать в магазин
              </a>
            </li>
            <li>
              Все способы связи — на странице{" "}
              <Link to="/contacts" className={linkClass}>
                «Контакты»
              </Link>
              .
            </li>
          </ul>
        </section>
      </Container>
    </div>
  );
}
