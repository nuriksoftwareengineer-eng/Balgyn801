import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ShoppingBag, Menu, X, User, Globe, Heart } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useCart } from "@/app/use-cart";
import { useCartDrawer } from "@/app/cart-drawer-context";
import { useAuth } from "@/app/auth-context";
import { useCurrency, type Currency } from "@/app/currency-context";
import { useWishlist } from "@/app/wishlist-context";
import { CompactDropdown } from "@/shared/ui/CompactDropdown";

const LANGS = ["ru", "kk", "en"] as const;
const CURRENCIES: Currency[] = ["KZT", "USD", "EUR", "RUB"];

/** Шапка витрины: прозрачная над hero на главной, белая при скролле / на др. страницах. */
export function SiteNavbar() {
  const { t, i18n } = useTranslation();
  const { currency, setCurrency } = useCurrency();
  const location = useLocation();
  const { totalQty } = useCart();
  const { openDrawer } = useCartDrawer();
  const { user } = useAuth();
  const { count: wishlistCount } = useWishlist();
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

  useEffect(() => setMobileOpen(false), [location.pathname]);

  const navLinks = [
    { labelKey: "nav.catalog", to: "/catalog" },
    { labelKey: "nav.customDesign", to: "/custom-design" },
    { labelKey: "nav.reviews", to: "/reviews" },
    { labelKey: "nav.faq", to: "/faq" },
    { labelKey: "nav.about", to: "/about" },
    { labelKey: "nav.contacts", to: "/contacts" },
  ];

  const mutedCls = transparent ? "text-white/60 hover:text-white" : "text-[--color-muted] hover:text-black";
  const dividerCls = transparent ? "bg-white/20" : "bg-[#E6E6E6]";

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

          {/* Desktop nav */}
          <nav className="hidden items-center gap-[40px] text-[15px] font-semibold uppercase tracking-[0.1em] xl:flex">
            {navLinks.map((l) => (
              <Link key={l.to} to={l.to} className={`transition-colors ${mutedCls}`}>
                {t(l.labelKey)}
              </Link>
            ))}
          </nav>

          {/* Right cluster */}
          <div className="flex items-center gap-[16px] md:gap-[20px]">

            {/* Language dropdown — desktop */}
            <CompactDropdown
              className="hidden xl:block"
              trigger={<span className="flex items-center gap-1.5"><Globe className="h-3.5 w-3.5" />{t(`language.${i18n.resolvedLanguage ?? "ru"}`)}</span>}
              options={LANGS.map((lng) => ({ value: lng, label: t(`language.${lng}`) }))}
              value={i18n.resolvedLanguage ?? "ru"}
              onChange={(lng) => i18n.changeLanguage(lng)}
              triggerClassName={transparent ? "text-white/70 hover:text-white" : undefined}
            />

            <span className={`hidden h-4 w-px xl:block ${dividerCls}`} />

            {/* Currency dropdown — desktop */}
            <CompactDropdown
              className="hidden xl:block"
              trigger={t(`currency.${currency}`)}
              options={CURRENCIES.map((c) => ({ value: c, label: t(`currency.${c}`) }))}
              value={currency}
              onChange={(c) => setCurrency(c as Currency)}
              triggerClassName={transparent ? "text-white/70 hover:text-white" : undefined}
            />

            <span className={`hidden h-4 w-px xl:block ${dividerCls}`} />

            {/* User icon */}
            <Link
              to={user ? "/profile" : "/login"}
              aria-label={user ? t("nav.profile") : t("nav.login")}
              className={`hidden transition-colors md:flex ${mutedCls}`}
            >
              <User className="h-[26px] w-[26px]" />
            </Link>

            {/* Wishlist */}
            <Link
              to="/wishlist"
              aria-label={t("nav.wishlist", "Избранное")}
              className={`relative hidden transition-colors md:flex ${mutedCls}`}
            >
              <Heart className="h-[26px] w-[26px]" />
              {wishlistCount > 0 && (
                <span className="absolute -right-2 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[9px] font-bold text-white">
                  {wishlistCount > 9 ? "9+" : wishlistCount}
                </span>
              )}
            </Link>

            {/* Cart */}
            <button
              type="button"
              onClick={openDrawer}
              aria-label={t("header.cartItems", { count: totalQty })}
              className={`flex items-center gap-2 transition-colors ${mutedCls}`}
            >
              <ShoppingBag className="h-[26px] w-[26px]" />
              <span className="text-[16px] font-semibold leading-none tracking-[0.08em] md:text-[18px]">
                ({totalQty})
              </span>
            </button>

            {/* Hamburger */}
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className={`-mr-2 p-2 xl:hidden transition-colors ${mutedCls}`}
              aria-label={t("header.openMenu")}
            >
              <Menu className="h-7 w-7" />
            </button>
          </div>
        </div>
      </header>

      {/* Mobile full-screen menu */}
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
              aria-label={t("header.closeMenu")}
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          <div className="flex flex-1 flex-col justify-center gap-6 px-8">
            {navLinks.map((l) => (
              <Link
                key={l.to}
                to={l.to}
                className="text-[28px] font-bold uppercase tracking-[-0.02em]"
              >
                {t(l.labelKey)}
              </Link>
            ))}
            <Link to="/wishlist" className="text-[28px] font-bold uppercase tracking-[-0.02em]">
              {t("nav.wishlist", "Избранное")}
            </Link>
            <Link
              to={user ? "/profile" : "/login"}
              className="text-[28px] font-bold uppercase tracking-[-0.02em]"
            >
              {user ? t("nav.profile") : t("nav.login")}
            </Link>
          </div>

          <div className="flex flex-col gap-4 border-t border-white/20 px-8 pb-8 pt-6">
            {/* Language */}
            <div className="flex items-center gap-4">
              {LANGS.map((lng) => (
                <button
                  key={lng}
                  type="button"
                  onClick={() => i18n.changeLanguage(lng)}
                  className={`text-[13px] font-semibold uppercase tracking-[0.12em] transition-colors ${
                    i18n.resolvedLanguage === lng ? "text-white" : "text-white/60"
                  }`}
                >
                  {t(`language.${lng}`)}
                </button>
              ))}
            </div>
            {/* Currency */}
            <div className="flex items-center gap-4">
              {CURRENCIES.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setCurrency(c)}
                  className={`text-[13px] font-semibold uppercase tracking-[0.08em] transition-colors ${
                    currency === c ? "text-white" : "text-white/60"
                  }`}
                >
                  {c}
                </button>
              ))}
            </div>
            {/* Cart shortcut */}
            <button
              type="button"
              onClick={() => {
                setMobileOpen(false);
                openDrawer();
              }}
              className="inline-flex items-center gap-2 text-[12px] font-semibold uppercase tracking-[0.16em] text-white/60"
            >
              <ShoppingBag className="h-4 w-4" />
              {t("nav.cart")} ({totalQty})
            </button>
          </div>
        </div>
      ) : null}
    </>
  );
}
