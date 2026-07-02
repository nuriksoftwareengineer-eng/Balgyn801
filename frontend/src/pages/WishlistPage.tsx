import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/app/auth-context";
import { useWishlist } from "@/app/wishlist-context";
import { Container } from "@/shared/ui/container";
import type { WishlistItemResponse } from "@/shared/api/types";

function WishlistCard({ item }: { item: WishlistItemResponse }) {
  const { toggle } = useWishlist();
  const href = `/catalog/${item.groupSlug}/${item.collectionName.toLowerCase().replace(/\s+/g, "-")}/${item.designSlug}`;

  return (
    <div className="group relative border border-[--color-border] bg-white">
      <Link to={href} className="block">
        <div className="aspect-square w-full overflow-hidden bg-[--color-surface]">
          {item.mainImageUrl ? (
            <img
              src={item.mainImageUrl}
              alt={item.designName}
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-zinc-900">
              <span className="text-4xl font-bold text-white/15">{item.designName.charAt(0)}</span>
            </div>
          )}
        </div>
        <div className="p-3">
          <p className="truncate text-sm font-semibold uppercase tracking-wide text-black">{item.designName}</p>
          <p className="text-xs text-zinc-500">{item.collectionName}</p>
        </div>
      </Link>
      <button
        onClick={() => void toggle(item.designId)}
        className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 shadow text-red-500 transition hover:scale-110"
        aria-label="Убрать из избранного"
      >
        <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
        </svg>
      </button>
    </div>
  );
}

export function WishlistPage() {
  const { t } = useTranslation();
  const { token } = useAuth();
  const { items, guestIds } = useWishlist() as any;

  // Guest mode — show count only
  if (!token) {
    const count = guestIds?.length ?? 0;
    return (
      <Container className="py-12">
        <h1 className="mb-8 text-2xl font-semibold uppercase tracking-widest">{t("wishlist.title", "Избранное")}</h1>
        {count === 0 ? (
          <div className="py-16 text-center text-zinc-500">
            <p className="text-lg">{t("wishlist.empty", "В избранном пусто")}</p>
            <Link to="/catalog" className="mt-4 inline-block text-sm underline text-zinc-700">{t("wishlist.browse", "Перейти в каталог")}</Link>
          </div>
        ) : (
          <p className="text-zinc-600">{t("wishlist.guestNote", "Войдите в аккаунт, чтобы сохранить избранное. {{count}} дизайнов сохранено в этой сессии.", { count })}</p>
        )}
        <div className="mt-6">
          <Link to="/auth/login" className="inline-block border border-black px-6 py-2.5 text-sm font-semibold uppercase tracking-widest hover:bg-black hover:text-white transition">
            {t("nav.login", "Войти")}
          </Link>
        </div>
      </Container>
    );
  }

  return (
    <Container className="py-12">
      <h1 className="mb-8 text-2xl font-semibold uppercase tracking-widest">{t("wishlist.title", "Избранное")}</h1>
      {items.length === 0 ? (
        <div className="py-16 text-center text-zinc-500">
          <p className="text-lg">{t("wishlist.empty", "В избранном пусто")}</p>
          <Link to="/catalog" className="mt-4 inline-block text-sm underline text-zinc-700">{t("wishlist.browse", "Перейти в каталог")}</Link>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {items.map((item: WishlistItemResponse) => (
            <WishlistCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </Container>
  );
}
