import { Outlet, useLocation } from "react-router-dom";
import { useCartDrawer } from "@/app/cart-drawer-context";
import { ScrollToTop } from "@/app/ScrollToTop";
import { CartDrawer } from "@/widgets/CartDrawer";
import { SiteFooter } from "@/widgets/SiteFooter";
import { SiteNavbar } from "@/widgets/SiteNavbar";

export function MainLayout() {
  const location = useLocation();
  const { open, closeDrawer } = useCartDrawer();
  // На главной hero уходит под прозрачную шапку; на остальных страницах
  // добавляем отступ под фиксированную шапку.
  const isHome = location.pathname === "/";

  return (
    <div className="flex min-h-screen flex-col bg-white text-black antialiased">
      <ScrollToTop />
      <SiteNavbar />
      <main className={`flex-1 ${isHome ? "" : "pt-[72px] md:pt-[96px]"}`}>
        <Outlet />
      </main>
      <SiteFooter />
      <CartDrawer open={open} onClose={closeDrawer} />
    </div>
  );
}
