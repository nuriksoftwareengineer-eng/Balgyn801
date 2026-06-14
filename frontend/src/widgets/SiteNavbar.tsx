import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Search, ShoppingBag, Menu, X, User } from "lucide-react";
import { useCart } from "@/app/use-cart";
import { useCartDrawer } from "@/app/cart-drawer-context";
import { useAuth } from "@/app/auth-context";

const NAV_LINKS = [
  { label: "Каталог", to: "/catalog" },
  { label: "Свой дизайн", to: "/custom-design" },
  { label: "О нас", to: "/about" },
  { label: "Контакты", to: "/contacts" },
];

/** Шапка витрины в стиле дизайна: прозрачная над hero на главной, белая при скролле / на др. страницах. */
export function SiteNavbar() {
  const location = useLocation();
  const { totalQty } = useCart();
  const { openDrawer } = useCartDrawer();
  const { user } = useAuth();
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const isHome = location.pathname === "/";
  const transparent = isHome && !scrolled;

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Закрыть мобильное меню при смене маршрута.
  useEffect(() => setMobileOpen(false), [location.pathname]);

  return (
    <>
      <header
        className={`fixed left-0 top-0 z-50 w-full transition-colors duration-300 ${
          transparent
            ? "bg-transparent text-white"
            : "border-b border-[#E6E6E6] bg-white text-black"
        }`}
      >
        <div className="mx-auto flex h-[72px] max-w-[1440px] items-center justify-between px-4 md:h-[96px] md:px-8">
          <Link
            to="/"
            className="text-[24px] font-extrabold uppercase tracking-[-0.02em] md:text-[30px]"
          >
            BALGYN
          </Link>

          <nav className="hidden items-center gap-[40px] text-[15px] font-semibold uppercase tracking-[0.1em] xl:flex">
            {NAV_LINKS.map((l) => (
              <Link key={l.to} to={l.to} className="transition-colors hover:text-[#7A7A7A]">
                {l.label}
              </Link>
            ))}
          </nav>

          <div className="flex items-center gap-[20px] md:gap-[28px]">
            <Link
              to={user ? "/profile" : "/login"}
              aria-label={user ? "Профиль" : "Войти"}
              className="hidden transition-colors hover:text-[#7A7A7A] md:flex"
            >
              <User className="h-[26px] w-[26px]" />
            </Link>
            <button
              type="button"
              onClick={openDrawer}
              aria-label={`Корзина, ${totalQty} поз.`}
              className="flex items-center gap-2 transition-colors hover:text-[#7A7A7A]"
            >
              <ShoppingBag className="h-[26px] w-[26px]" />
              <span className="text-[16px] font-semibold leading-none tracking-[0.08em] md:text-[18px]">
                ({totalQty})
              </span>
            </button>
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className="-mr-2 p-2 xl:hidden"
              aria-label="Открыть меню"
            >
              <Menu className="h-7 w-7" />
            </button>
          </div>
        </div>
      </header>

      {mobileOpen ? (
        <div className="fixed inset-0 z-[60] flex flex-col bg-black text-white">
          <div className="mx-auto grid h-[72px] w-full max-w-[1440px] grid-cols-3 items-center px-4">
            <div />
            <Link
              to="/"
              className="text-center text-[18px] font-extrabold uppercase tracking-[-0.02em]"
            >
              BALGYN
            </Link>
            <button
              type="button"
              onClick={() => setMobileOpen(false)}
              className="-mr-2 justify-self-end p-2"
              aria-label="Закрыть меню"
            >
              <X className="h-6 w-6" />
            </button>
          </div>
          <div className="flex flex-1 flex-col justify-center gap-6 px-8">
            {NAV_LINKS.map((l) => (
              <Link
                key={l.to}
                to={l.to}
                className="text-[28px] font-bold uppercase tracking-[-0.02em]"
              >
                {l.label}
              </Link>
            ))}
            <Link
              to={user ? "/profile" : "/login"}
              className="text-[28px] font-bold uppercase tracking-[-0.02em]"
            >
              {user ? "Профиль" : "Войти"}
            </Link>
          </div>
          <div className="flex items-center justify-between gap-6 border-t border-white/20 p-6">
            <button type="button" className="inline-flex items-center gap-2 text-[12px] font-semibold uppercase tracking-[0.16em]">
              <Search className="h-4 w-4" />
              Поиск
            </button>
            <button
              type="button"
              onClick={() => {
                setMobileOpen(false);
                openDrawer();
              }}
              className="inline-flex items-center gap-2 text-[12px] font-semibold uppercase tracking-[0.16em]"
            >
              <ShoppingBag className="h-4 w-4" />
              Корзина ({totalQty})
            </button>
          </div>
        </div>
      ) : null}
    </>
  );
}
