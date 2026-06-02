import { useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { useCart } from "@/app/use-cart";
import { cn } from "@/shared/lib/cn";
import { Container } from "@/shared/ui/container";
import { IconButton } from "@/shared/ui/icon-button";

const iconBtnLink =
  "inline-flex h-[42px] w-[42px] items-center justify-center rounded-[10px] border border-white/10 bg-zinc-900 text-zinc-100 transition hover:border-violet-500 hover:bg-violet-500/15";

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    "text-sm font-medium transition hover:text-zinc-100",
    isActive ? "text-zinc-100" : "text-zinc-400",
  );

export function SiteHeader() {
  const { totalQty } = useCart();
  const { user, loading, logout, isAdmin } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="sticky top-10 z-[100] border-b border-white/10 bg-zinc-950/85 backdrop-blur-md">
      <Container className="flex h-16 items-center justify-between gap-6">
        <Link
          to="/"
          className="font-display text-[1.75rem] tracking-[0.06em] text-zinc-100"
          onClick={closeMenu}
        >
          BALG<span className="text-violet-400">YN</span>
        </Link>

        <nav
          className="hidden items-center gap-7 md:flex"
          aria-label="Основное меню"
        >
          <NavLink to="/catalog" className={navLinkClass}>
            Каталог
          </NavLink>
          <Link
            to="/custom-design"
            className="text-sm font-medium text-zinc-400 transition hover:text-zinc-100"
          >
            Свой дизайн
          </Link>
          <NavLink to="/about" className={navLinkClass}>
            О нас
          </NavLink>
          {isAdmin ? (
            <NavLink to="/admin" className={navLinkClass}>
              Админка
            </NavLink>
          ) : null}
          {user ? (
            <>
              <span
                className="max-w-[140px] truncate text-xs text-zinc-500"
                title={user.email}
              >
                {user.email}
              </span>
              <button
                type="button"
                className="text-sm font-medium text-zinc-400 transition hover:text-zinc-100"
                onClick={logout}
              >
                Выйти
              </button>
            </>
          ) : loading ? (
            <span className="text-xs text-zinc-600">…</span>
          ) : (
            <>
              <NavLink to="/login" className={navLinkClass}>
                Вход
              </NavLink>
              <NavLink to="/register" className={navLinkClass}>
                Регистрация
              </NavLink>
            </>
          )}
        </nav>

        <div className="flex items-center gap-3">
          <IconButton
            type="button"
            className="md:hidden"
            aria-label={menuOpen ? "Закрыть меню" : "Открыть меню"}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((v) => !v)}
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              aria-hidden
            >
              {menuOpen ? (
                <path d="M18 6L6 18M6 6l12 12" />
              ) : (
                <path d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </IconButton>

          <IconButton
            type="button"
            aria-label="Поиск"
            className="hidden md:inline-flex"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              aria-hidden
            >
              <circle cx="11" cy="11" r="7" />
              <path d="M21 21l-4.35-4.35" />
            </svg>
          </IconButton>

          <Link
            to="/cart"
            className={cn(iconBtnLink, "relative")}
            aria-label={`Корзина, ${totalQty} поз.`}
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              aria-hidden
            >
              <path d="M6 6h15l-1.5 9h-12z" />
              <circle cx="9" cy="20" r="1" />
              <circle cx="18" cy="20" r="1" />
            </svg>
            {totalQty > 0 ? (
              <span className="absolute -right-1 -top-1 flex h-[18px] min-w-[18px] animate-[cart-badge-pop_0.35s_ease-out] items-center justify-center rounded-full bg-violet-500 px-1 text-[0.65rem] font-bold text-white">
                {totalQty > 99 ? "99+" : totalQty}
              </span>
            ) : null}
          </Link>
        </div>
      </Container>

      {menuOpen ? (
        <div className="border-b border-white/10 bg-zinc-900 px-5 py-3 md:hidden">
          <nav className="flex flex-col gap-3.5 font-semibold">
            <NavLink to="/catalog" onClick={closeMenu}>
              Каталог
            </NavLink>
            <Link to="/custom-design" onClick={closeMenu}>
              Свой дизайн
            </Link>
            <NavLink to="/about" onClick={closeMenu}>
              О нас
            </NavLink>
            <Link
              to="/cart"
              onClick={closeMenu}
              className="flex items-center justify-between gap-3"
            >
              <span>Корзина</span>
              {totalQty > 0 ? (
                <span className="rounded-full bg-violet-500 px-2 py-0.5 text-xs font-bold text-white">
                  {totalQty > 99 ? "99+" : totalQty}
                </span>
              ) : null}
            </Link>
            {isAdmin ? (
              <NavLink to="/admin" onClick={closeMenu}>
                Админка
              </NavLink>
            ) : null}
            {user ? (
              <>
                <span className="truncate text-xs text-zinc-500">{user.email}</span>
                <button
                  type="button"
                  className="text-left font-semibold text-violet-400"
                  onClick={() => {
                    logout();
                    closeMenu();
                  }}
                >
                  Выйти
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" onClick={closeMenu}>
                  Вход
                </NavLink>
                <NavLink to="/register" onClick={closeMenu}>
                  Регистрация
                </NavLink>
              </>
            )}
          </nav>
        </div>
      ) : null}
    </header>
  );
}
