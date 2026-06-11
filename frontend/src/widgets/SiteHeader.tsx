import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { useCartDrawer } from "@/app/cart-drawer-context";
import { useCart } from "@/app/use-cart";
import { useCatalogGroups } from "@/shared/api/catalog-api";
import { CartDrawer } from "@/widgets/CartDrawer";
import { cn } from "@/shared/lib/cn";
import { Container } from "@/shared/ui/container";

// ─── Nav link helper ──────────────────────────────────────────────────────

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    "text-[0.7rem] font-medium uppercase tracking-[0.14em] transition-colors duration-150",
    isActive ? "text-black" : "text-[--color-muted] hover:text-black",
  );

// ─── SVG icons ────────────────────────────────────────────────────────────

function MenuIcon({ open }: { open: boolean }) {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      {open ? (
        <path d="M18 6L6 18M6 6l12 12" />
      ) : (
        <path d="M4 6h16M4 12h16M4 18h16" />
      )}
    </svg>
  );
}

function CartIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      <path d="M6 6h15l-1.5 9h-12z" />
      <circle cx="9" cy="20" r="1" />
      <circle cx="18" cy="20" r="1" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  );
}

function ChevronIcon({ open }: { open: boolean }) {
  return (
    <svg
      width="12"
      height="12"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      aria-hidden="true"
      className={cn("transition-transform duration-200", open ? "rotate-180" : "")}
    >
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

// ─── Icon button ──────────────────────────────────────────────────────────

function HeaderIconBtn({
  onClick,
  label,
  children,
  className,
}: {
  onClick?: () => void;
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className={cn(
        "flex h-9 w-9 items-center justify-center text-black transition-colors hover:text-zinc-500",
        className,
      )}
    >
      {children}
    </button>
  );
}

// ─── Main component ───────────────────────────────────────────────────────

export function SiteHeader() {
  const { user, loading, logout, isAdmin } = useAuth();
  const { totalQty } = useCart();
  const navigate = useNavigate();
  const { open: cartOpen, openDrawer, closeDrawer } = useCartDrawer();

  const [menuOpen, setMenuOpen] = useState(false);
  const [megaOpen, setMegaOpen] = useState(false);
  const [catalogAccordionOpen, setCatalogAccordionOpen] = useState(false);

  const { data: groups } = useCatalogGroups();

  // Hover timers for desktop mega-menu
  const openTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cancelOpen = () => {
    if (openTimerRef.current) clearTimeout(openTimerRef.current);
  };
  const cancelClose = () => {
    if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
  };

  const scheduleMegaOpen = () => {
    cancelClose();
    openTimerRef.current = setTimeout(() => setMegaOpen(true), 150);
  };
  const scheduleMegaClose = () => {
    cancelOpen();
    closeTimerRef.current = setTimeout(() => setMegaOpen(false), 150);
  };

  // Escape key closes mega-menu
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setMegaOpen(false);
      }
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, []);

  // Clean up timers on unmount
  useEffect(() => {
    return () => {
      cancelOpen();
      cancelClose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const closeMenu = () => {
    setMenuOpen(false);
    setCatalogAccordionOpen(false);
  };

  const closeMega = () => {
    cancelOpen();
    cancelClose();
    setMegaOpen(false);
  };

  return (
    <>
      <header className="sticky top-9 z-[100] border-b border-[--color-border] bg-white">
        <Container className="flex h-14 items-center justify-between gap-6 md:h-[68px]">

          {/* ── Logo ── */}
          <Link
            to="/"
            onClick={closeMenu}
            className="text-base font-semibold uppercase tracking-[0.18em] text-black"
          >
            BALGYN
          </Link>

          {/* ── Desktop nav ── */}
          <nav className="hidden items-center gap-8 md:flex" aria-label="Основная навигация">
            {/* Catalog — hover opens mega-menu; click navigates */}
            <div
              onMouseEnter={scheduleMegaOpen}
              onMouseLeave={scheduleMegaClose}
              className="relative"
            >
              <NavLink
                to="/catalog"
                className={navLinkClass}
                onClick={closeMega}
              >
                Каталог
              </NavLink>
            </div>

            <NavLink to="/custom-design" className={navLinkClass}>Свой дизайн</NavLink>
            <NavLink to="/about" className={navLinkClass}>О нас</NavLink>
            {isAdmin && (
              <NavLink to="/admin" className={navLinkClass}>Админ</NavLink>
            )}
          </nav>

          {/* ── Right cluster ── */}
          <div className="flex items-center gap-1">

            {/* Desktop: user area */}
            <div className="hidden items-center gap-3 md:flex">
              {loading ? (
                <span className="h-2 w-16 animate-pulse rounded bg-[--color-surface]" />
              ) : user ? (
                <>
                  <Link
                    to="/profile"
                    className="max-w-[120px] truncate text-[0.65rem] text-[--color-muted] transition hover:text-black"
                  >
                    {user.email}
                  </Link>
                  <Link
                    to="/orders"
                    className="text-[0.65rem] uppercase tracking-widest text-[--color-muted] transition hover:text-black"
                  >
                    Заказы
                  </Link>
                  <button
                    type="button"
                    onClick={logout}
                    className="text-[0.65rem] uppercase tracking-widest text-[--color-muted] transition hover:text-black"
                  >
                    Выйти
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="text-[0.65rem] uppercase tracking-widest text-[--color-muted] transition hover:text-black"
                  >
                    Вход
                  </Link>
                  <Link
                    to="/register"
                    className="text-[0.65rem] uppercase tracking-widest text-[--color-muted] transition hover:text-black"
                  >
                    Регистрация
                  </Link>
                </>
              )}
            </div>

            {/* Mobile: user icon → login or profile */}
            <HeaderIconBtn
              label={user ? "Профиль" : "Войти"}
              onClick={() => navigate(user ? "/profile" : "/login")}
              className="md:hidden"
            >
              <UserIcon />
            </HeaderIconBtn>

            {/* Cart */}
            <HeaderIconBtn
              label={`Корзина${totalQty > 0 ? `, ${totalQty} поз.` : ""}`}
              onClick={openDrawer}
              className="relative"
            >
              <CartIcon />
              {totalQty > 0 && (
                <span
                  key={totalQty}
                  className="absolute -right-0.5 -top-0.5 flex h-[17px] min-w-[17px] animate-[cart-badge-pop_0.35s_ease-out] items-center justify-center bg-black px-0.5 text-[0.55rem] font-bold text-white"
                >
                  {totalQty > 99 ? "99+" : totalQty}
                </span>
              )}
            </HeaderIconBtn>

            {/* Mobile: hamburger */}
            <HeaderIconBtn
              label={menuOpen ? "Закрыть меню" : "Меню"}
              onClick={() => setMenuOpen((v) => !v)}
              className="md:hidden"
            >
              <MenuIcon open={menuOpen} />
            </HeaderIconBtn>
          </div>
        </Container>

        {/* ── Desktop mega-menu ── */}
        {megaOpen && (
          <div
            onMouseEnter={cancelClose}
            onMouseLeave={scheduleMegaClose}
            className="hidden border-t border-[--color-border] bg-white md:block"
          >
            <Container className="py-6">
              {groups && groups.length > 0 ? (
                <div className="flex flex-wrap gap-x-10 gap-y-2">
                  <Link
                    to="/catalog"
                    onClick={closeMega}
                    className="text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-[--color-muted] transition hover:text-black"
                  >
                    Все коллекции
                  </Link>
                  {groups.map((g) => (
                    <Link
                      key={g.id}
                      to={`/catalog/${g.slug}`}
                      onClick={closeMega}
                      className="text-[0.65rem] font-medium uppercase tracking-[0.14em] text-[--color-muted] transition hover:text-black"
                    >
                      {g.name}
                    </Link>
                  ))}
                </div>
              ) : (
                <p className="text-[0.65rem] text-[--color-muted]">Загружаем каталог…</p>
              )}
            </Container>
          </div>
        )}

        {/* ── Mobile nav drawer ── */}
        {menuOpen && (
          <div className="border-t border-[--color-border] bg-white md:hidden">
            <Container className="py-6">
              <nav className="flex flex-col gap-5" aria-label="Мобильное меню">

                {/* Catalog accordion */}
                <div className="flex flex-col">
                  <div className="flex items-center justify-between">
                    <NavLink
                      to="/catalog"
                      className={navLinkClass}
                      onClick={closeMenu}
                    >
                      Каталог
                    </NavLink>
                    {groups && groups.length > 0 && (
                      <button
                        type="button"
                        onClick={() => setCatalogAccordionOpen((v) => !v)}
                        aria-label={catalogAccordionOpen ? "Свернуть" : "Развернуть"}
                        className="flex h-7 w-7 items-center justify-center text-[--color-muted] transition hover:text-black"
                      >
                        <ChevronIcon open={catalogAccordionOpen} />
                      </button>
                    )}
                  </div>

                  {catalogAccordionOpen && groups && groups.length > 0 && (
                    <div className="mt-3 flex flex-col gap-3 pl-3 border-l border-[--color-border]">
                      {groups.map((g) => (
                        <Link
                          key={g.id}
                          to={`/catalog/${g.slug}`}
                          onClick={closeMenu}
                          className="text-[0.65rem] uppercase tracking-[0.12em] text-[--color-muted] transition hover:text-black"
                        >
                          {g.name}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>

                <NavLink
                  to="/custom-design"
                  className={navLinkClass}
                  onClick={closeMenu}
                >
                  Свой дизайн
                </NavLink>
                <NavLink
                  to="/about"
                  className={navLinkClass}
                  onClick={closeMenu}
                >
                  О нас
                </NavLink>
                {isAdmin && (
                  <NavLink to="/admin" className={navLinkClass} onClick={closeMenu}>
                    Админ
                  </NavLink>
                )}

                <div className="my-1 border-t border-[--color-border]" />

                {user ? (
                  <>
                    <NavLink to="/profile" className={navLinkClass} onClick={closeMenu}>
                      {user.email}
                    </NavLink>
                    <NavLink to="/orders" className={navLinkClass} onClick={closeMenu}>
                      Мои заказы
                    </NavLink>
                    <button
                      type="button"
                      onClick={() => { logout(); closeMenu(); }}
                      className="text-left text-[0.7rem] uppercase tracking-widest text-[--color-muted] transition hover:text-black"
                    >
                      Выйти
                    </button>
                  </>
                ) : (
                  <>
                    <NavLink to="/login" className={navLinkClass} onClick={closeMenu}>
                      Вход
                    </NavLink>
                    <NavLink to="/register" className={navLinkClass} onClick={closeMenu}>
                      Регистрация
                    </NavLink>
                  </>
                )}
              </nav>
            </Container>
          </div>
        )}
      </header>

      <CartDrawer open={cartOpen} onClose={closeDrawer} />
    </>
  );
}
