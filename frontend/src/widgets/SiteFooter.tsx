import { Link } from "react-router-dom";
import { Marquee } from "@/widgets/home/Marquee";

const linkClass =
  "text-[14px] text-[#D9D9D9] transition-colors hover:text-white";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="bg-black text-white">
      <div className="border-y border-white/20 py-4 md:py-6">
        <Marquee
          items={["balgyn", "✸", "оставайся свежей", "✸", "вышивка · алматы", "✸"]}
          speed={70}
        />
      </div>

      <div className="container mx-auto px-4 pb-12 pt-20 md:px-8 md:pt-24">
        <div className="mb-20 grid grid-cols-2 gap-12 md:grid-cols-4 md:mb-24">
          <div className="flex flex-col gap-4">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">Магазин</h4>
            <Link to="/catalog" className={linkClass}>Каталог</Link>
            <Link to="/custom-design" className={linkClass}>Свой дизайн</Link>
            <Link to="/about" className={linkClass}>О нас</Link>
          </div>

          <div className="flex flex-col gap-4">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">Помощь</h4>
            <Link to="/delivery" className={linkClass}>Доставка и оплата</Link>
            <Link to="/returns" className={linkClass}>Возврат и обмен</Link>
            <Link to="/track-order" className={linkClass}>Отследить заказ</Link>
            <Link to="/contacts" className={linkClass}>Контакты</Link>
          </div>

          <div className="flex flex-col gap-4">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">Соцсети</h4>
            <a href="https://instagram.com/balgyn_shop" target="_blank" rel="noopener noreferrer" className={linkClass}>Instagram</a>
            <a href="https://tiktok.com/@balgyn_shop" target="_blank" rel="noopener noreferrer" className={linkClass}>TikTok</a>
            <a href="https://instagram.com/balgyn_shop/reels/" target="_blank" rel="noopener noreferrer" className={linkClass}>Reels</a>
          </div>

          <div className="col-span-2 flex flex-col gap-4 md:col-span-1">
            <h4 className="mb-2 text-[12px] font-semibold uppercase tracking-wider">Рассылка</h4>
            <p className="mb-2 text-[14px] text-[#D9D9D9]">Только обновления и дроп-релизы.</p>
            <a
              href="https://instagram.com/balgyn_shop"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex w-fit items-center gap-2 border-b border-[#7A7A7A] pb-2 text-[14px] text-[#D9D9D9] transition-colors hover:border-white hover:text-white"
            >
              Подписаться в Instagram →
            </a>
          </div>
        </div>

        <div className="flex flex-col gap-8 border-t border-[#2A2A2A] pt-8">
          <p className="text-[44px] font-extrabold uppercase leading-none tracking-[-0.04em] md:text-[72px]">
            BALGYN
          </p>
          <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
            <p className="text-center text-[12px] uppercase tracking-wider text-[#7A7A7A] md:text-left">
              © {year} BALGYN · Все права защищены · Алматы, KZ
            </p>
            <div className="flex flex-wrap justify-center gap-6 text-[10px] uppercase tracking-[0.2em] text-[#7A7A7A]">
              <Link to="/privacy" className="transition-colors hover:text-white">Политика</Link>
              <Link to="/terms" className="transition-colors hover:text-white">Оферта</Link>
              <Link to="/returns" className="transition-colors hover:text-white">Возврат</Link>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
