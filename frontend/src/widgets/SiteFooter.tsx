import { Link } from "react-router-dom";
import { Container } from "@/shared/ui/container";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-[--color-border] bg-white">
      <Container className="py-14">
        <div className="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr_1fr]">

          {/* Brand */}
          <div>
            <p className="text-base font-semibold uppercase tracking-[0.18em] text-black">
              BALGYN
            </p>
            <p className="mt-3 max-w-xs text-xs leading-relaxed text-[--color-muted]">
              Вышивка и уличная культура. Дизайн ваш — качество наше.
            </p>
          </div>

          {/* Buyers */}
          <div>
            <p className="mb-4 text-[0.6rem] font-semibold uppercase tracking-[0.18em] text-black">
              Покупателям
            </p>
            <ul className="space-y-3">
              <li>
                <Link
                  to="/terms"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Пользовательское соглашение
                </Link>
              </li>
              <li>
                <Link
                  to="/privacy"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Политика конфиденциальности
                </Link>
              </li>
              <li>
                <Link
                  to="/returns"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Возврат и обмен
                </Link>
              </li>
              <li>
                <Link
                  to="/delivery"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Доставка и оплата
                </Link>
              </li>
              <li>
                <Link
                  to="/contacts"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Контакты
                </Link>
              </li>
              <li>
                <Link
                  to="/track-order"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Отследить заказ
                </Link>
              </li>
            </ul>
          </div>

          {/* Company */}
          <div>
            <p className="mb-4 text-[0.6rem] font-semibold uppercase tracking-[0.18em] text-black">
              Компания
            </p>
            <ul className="space-y-3">
              <li>
                <Link
                  to="/about"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  О нас
                </Link>
              </li>
              <li>
                <Link
                  to="/contacts"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Контакты
                </Link>
              </li>
              <li>
                <Link
                  to="/catalog"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Каталог
                </Link>
              </li>
              <li>
                <Link
                  to="/custom-design"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Свой дизайн
                </Link>
              </li>
            </ul>
          </div>

          {/* Social */}
          <div>
            <p className="mb-4 text-[0.6rem] font-semibold uppercase tracking-[0.18em] text-black">
              Соцсети
            </p>
            <ul className="space-y-3">
              <li>
                <a
                  href="https://instagram.com/balgyn_shop"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Instagram
                </a>
              </li>
              <li>
                <a
                  href="https://tiktok.com/@balgyn_shop"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  TikTok
                </a>
              </li>
              <li>
                <a
                  href="https://instagram.com/balgyn_shop/reels/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs text-[--color-muted] transition-colors hover:text-black"
                >
                  Reels
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom strip */}
        <div className="mt-12 border-t border-[--color-border] pt-6">
          <p className="text-[0.6rem] uppercase tracking-[0.14em] text-[--color-muted]">
            © {year} Balgyn
          </p>
        </div>
      </Container>
    </footer>
  );
}
