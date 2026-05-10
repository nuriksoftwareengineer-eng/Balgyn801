import { Link } from "react-router-dom";
import { Container } from "@/shared/ui/container";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-white/10 bg-[#080809] py-12 pb-8 pt-12">
      <Container>
        <div className="grid gap-8 md:grid-cols-[1.2fr_1fr_1fr]">
          <div>
            <div className="font-display mb-2.5 text-2xl tracking-[0.06em] text-zinc-100">
              BALG<span className="text-violet-400">YN</span>
            </div>
            <p className="m-0 max-w-sm text-sm leading-relaxed text-zinc-400">
              Стритвир с вышивкой по мотивам игр, аниме и уличной культуры. Дизайн ваш — качество наше.
            </p>
          </div>
          <div>
            <h3 className="mb-3.5 text-[0.6875rem] font-bold uppercase tracking-[0.14em] text-zinc-400">
              Покупателям
            </h3>
            <ul className="m-0 list-none space-y-2.5 p-0">
              <li>
                <Link to="/catalog" className="text-sm text-zinc-400 hover:text-zinc-100">
                  Каталог
                </Link>
              </li>
              <li>
                <a href="#" className="text-sm text-zinc-400 hover:text-zinc-100">
                  Доставка
                </a>
              </li>
              <li>
                <a href="#" className="text-sm text-zinc-400 hover:text-zinc-100">
                  Возврат
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h3 className="mb-3.5 text-[0.6875rem] font-bold uppercase tracking-[0.14em] text-zinc-400">
              Компания
            </h3>
            <ul className="m-0 list-none space-y-2.5 p-0">
              <li>
                <Link to="/about" className="text-sm text-zinc-400 hover:text-zinc-100">
                  О нас
                </Link>
              </li>
              <li>
                <a href="#" className="text-sm text-zinc-400 hover:text-zinc-100">
                  Контакты
                </a>
              </li>
              <li>
                <Link
                  to="/custom-design"
                  className="text-sm text-zinc-400 hover:text-zinc-100"
                >
                  Свой дизайн
                </Link>
              </li>
            </ul>
          </div>
        </div>
        <p className="mt-10 border-t border-white/10 pt-6 text-center text-xs text-zinc-400">
          © {year} Balgyn. Каркас витрины на React Router и Tailwind.
        </p>
      </Container>
    </footer>
  );
}
