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
    <div className="group relative">
      <Link to={href} className="block">
        <div className="aspect-[4/5] w-full overflow-hidden bg-[--color-surface]">
          {item.mainImageUrl ? (
            <img
              src={item.mainImageUrl}
              alt={item.designName}
              className="gallery-img h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <span className="text-5xl font-semibold uppercase text-black/[0.08]">{item.designName.charAt(0)}</span>
            </div>
          )}
        </div>
        <div className="mt-3.5">
          <p className="truncate text-[13px] font-medium text-black">{item.designName}</p>
          <p className="mt-0.5 text-[11px] uppercase tracking-[0.16em] text-[--color-muted]">{item.collectionName}</p>
        </div>
      </Link>
      <button
        onClick={() => void toggle(item.designId)}
        className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center text-black transition-transform duration-300 hover:scale-110"
        aria-label="Убрать из избранного"
      >
        <svg viewBox="0 0 24 24" fill="currentColor" className="h-[18px] w-[18px]">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
        </svg>
      </button>
    </div>
  );
}

function WishlistHeader({ subtitle }: { subtitle?: string }) {
  const { t } = useTranslation();
  return (
    <div className="mb-10">
      <p className="mb-2 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
        {t("nav.profile")}
      </p>
      <h1 className="display text-[40px] uppercase text-black md:text-[56px]">
        {t("wishlist.title", "Избранное")}
      </h1>
      {subtitle ? (
        <p className="mt-4 text-[14px] leading-relaxed text-[--color-muted]">{subtitle}</p>
      ) : null}
    </div>
  );
}

function EmptyWishlist() {
  const { t } = useTranslation();
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-6 py-20 text-center">
      <div className="flex h-16 w-16 items-center justify-center bg-[--color-surface] text-3xl select-none">🧵</div>
      <p className="text-[15px] leading-relaxed text-[--color-muted]">
        {t("wishlist.empty", "В избранном пусто")}
      </p>
      <Link
        to="/catalog"
        className="inline-flex items-center justify-center bg-black px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
      >
        {t("wishlist.browse", "Перейти в каталог")}
      </Link>
    </div>
  );
}

export function WishlistPage() {
  const { t } = useTranslation();
  const { token } = useAuth();
  const { items, guestIds } = useWishlist() as any;

  // Guest mode — prompt sign-in
  if (!token) {
    const count = guestIds?.length ?? 0;
    return (
      <Container className="py-12 md:py-16">
        <WishlistHeader />
        {count === 0 ? (
          <EmptyWishlist />
        ) : (
          <div className="mx-auto flex max-w-md flex-col items-center gap-6 py-16 text-center">
            <p className="text-[15px] leading-relaxed text-[--color-muted]">
              {t("wishlist.guestNote", "Войдите в аккаунт, чтобы сохранить избранное. {{count}} дизайнов сохранено в этой сессии.", { count })}
            </p>
            <Link
              to="/login"
              className="inline-flex items-center justify-center bg-black px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
            >
              {t("nav.login", "Войти")}
            </Link>
          </div>
        )}
      </Container>
    );
  }

  return (
    <Container className="py-12 md:py-16">
      <WishlistHeader />
      {items.length === 0 ? (
        <EmptyWishlist />
      ) : (
        <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {items.map((item: WishlistItemResponse) => (
            <WishlistCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </Container>
  );
}
