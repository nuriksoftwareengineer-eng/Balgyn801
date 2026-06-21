import { motion, useReducedMotion } from "framer-motion";
import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import { useCartDrawer } from "@/app/cart-drawer-context";
import { useProduct } from "@/shared/api/queries";
import type { Product } from "@/shared/api/types";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Container } from "@/shared/ui/container";
import { ProductImage } from "@/widgets/ProductImage";

function ProductBuyColumn({ product }: { product: Product }) {
  const { addItem } = useCart();
  const { openDrawer } = useCartDrawer();
  const navigate = useNavigate();

  const sizes = useMemo(
    () =>
      product.sizes
        ?.filter((s) => s && String(s).trim())
        .map((s) => String(s).trim()) ?? [],
    [product],
  );
  const colors = useMemo(
    () =>
      product.colors?.filter((c) => c?.name && String(c.name).trim()) ?? [],
    [product],
  );

  const [pickSize, setPickSize] = useState<string | null>(
    () => sizes[0] ?? null,
  );
  const [pickColor, setPickColor] = useState<string | null>(
    () => (colors[0]?.name ? String(colors[0].name).trim() : null),
  );

  const needSize = sizes.length > 0;
  const needColor = colors.length > 0;
  const canAdd =
    product.inStock &&
    (!needSize || !!pickSize) &&
    (!needColor || !!pickColor);

  function handleAddToCart() {
    if (!canAdd) return;
    addItem(product, {
      size: needSize ? pickSize : null,
      color: needColor ? pickColor : null,
    });
    openDrawer();
  }

  return (
    <div className="flex flex-col py-8 lg:px-12 lg:py-0">
      {product.category ? (
        <p className="text-[0.6rem] font-medium uppercase tracking-[0.22em] text-[--color-muted]">
          {product.category}
        </p>
      ) : null}

      <h1
        className="mt-3 font-extrabold uppercase text-black"
        style={{
          fontSize: "clamp(1.75rem, 4vw, 3rem)",
          lineHeight: 1.05,
          letterSpacing: "-0.02em",
        }}
      >
        {product.title}
      </h1>

      {product.description ? (
        <p className="mt-5 whitespace-pre-line text-sm leading-relaxed text-[--color-muted]">
          {product.description}
        </p>
      ) : null}

      {/* Price + stock */}
      <div className="mt-8 flex flex-wrap items-baseline gap-4">
        <span className="text-2xl font-semibold text-black">
          {formatMoney(product.price)} ₸
        </span>
        <span
          className={cn(
            "text-[0.65rem] uppercase tracking-[0.12em]",
            product.inStock ? "text-emerald-600" : "text-[--color-danger]",
          )}
        >
          {product.inStock ? "В наличии" : "Нет в наличии"}
        </span>
      </div>

      {/* Size selector */}
      {needSize ? (
        <div className="mt-8">
          <p className="mb-3 text-[0.6rem] uppercase tracking-[0.16em] text-[--color-muted]">
            Размер
          </p>
          <div className="flex flex-wrap gap-2">
            {sizes.map((s) => {
              const active = pickSize === s;
              return (
                <button
                  key={s}
                  type="button"
                  onClick={() => setPickSize(s)}
                  className={cn(
                    "min-w-[3rem] border px-4 py-2.5 text-[0.7rem] font-semibold uppercase tracking-[0.08em] transition-colors duration-150",
                    active
                      ? "border-black bg-black text-white"
                      : "border-[--color-border] text-black hover:border-black",
                  )}
                >
                  {s}
                </button>
              );
            })}
          </div>
        </div>
      ) : null}

      {/* Color selector */}
      {needColor ? (
        <div className="mt-7">
          <p className="mb-3 text-[0.6rem] uppercase tracking-[0.16em] text-[--color-muted]">
            Цвет{pickColor ? ` — ${pickColor}` : ""}
          </p>
          <div className="flex flex-wrap gap-2">
            {colors.map((c) => {
              const name = String(c.name).trim();
              const active = pickColor === name;
              const hex =
                typeof c.hex === "string" && /^#[0-9a-fA-F]{6}$/.test(c.hex)
                  ? c.hex
                  : null;
              return (
                <button
                  key={name}
                  type="button"
                  onClick={() => setPickColor(name)}
                  className={cn(
                    "flex items-center gap-2.5 border px-3 py-2.5 text-[0.7rem] font-medium transition-colors duration-150",
                    active
                      ? "border-black text-black"
                      : "border-[--color-border] text-[--color-muted] hover:border-black hover:text-black",
                  )}
                >
                  {hex ? (
                    <span
                      className="h-3.5 w-3.5 shrink-0 border border-black/10"
                      style={{ backgroundColor: hex }}
                      aria-hidden
                    />
                  ) : null}
                  {name}
                </button>
              );
            })}
          </div>
        </div>
      ) : null}

      {/* CTA buttons */}
      <div className="mt-10 flex flex-wrap gap-3">
        <button
          type="button"
          disabled={!canAdd}
          onClick={handleAddToCart}
          className="flex-1 bg-black py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800 disabled:opacity-40 sm:min-w-[200px] sm:flex-none sm:px-8"
        >
          {product.inStock ? "В корзину" : "Нет в наличии"}
        </button>
        <button
          type="button"
          onClick={() => navigate("/catalog")}
          className="border border-[--color-border] px-6 py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-black transition hover:border-black"
        >
          К каталогу
        </button>
      </div>
    </div>
  );
}

export function ProductPage() {
  // param is the catch-all name used by CatalogParamPage dispatcher; productId is the legacy route name
  const { productId, param } = useParams<{ productId?: string; param?: string }>();
  const id = Number.parseInt((productId ?? param) ?? "", 10);
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();

  const { data: product, isPending, isError, error } = useProduct(
    Number.isFinite(id) && id > 0 ? id : undefined,
  );

  if (!Number.isFinite(id) || id <= 0) {
    return (
      <Container className="py-16">
        <p className="text-[--color-muted]">Некорректная ссылка на товар.</p>
        <button
          type="button"
          onClick={() => navigate("/catalog")}
          className="mt-6 bg-black px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
        >
          В каталог
        </button>
      </Container>
    );
  }

  if (isPending) {
    return (
      <Container className="py-8 md:py-14">
        <div className="mb-8 h-2.5 w-52 animate-pulse bg-[--color-surface]" />
        <div className="grid gap-10 lg:grid-cols-2">
          <div className="aspect-[4/5] animate-pulse bg-[--color-surface]" />
          <div className="space-y-5 py-4">
            <div className="h-2.5 w-24 animate-pulse bg-[--color-surface]" />
            <div className="h-10 w-3/4 animate-pulse bg-[--color-surface]" />
            <div className="space-y-2.5">
              <div className="h-2.5 w-full animate-pulse bg-[--color-surface]" />
              <div className="h-2.5 w-5/6 animate-pulse bg-[--color-surface]" />
              <div className="h-2.5 w-2/3 animate-pulse bg-[--color-surface]" />
            </div>
            <div className="h-8 w-32 animate-pulse bg-[--color-surface]" />
            <div className="flex gap-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-11 w-16 animate-pulse bg-[--color-surface]" />
              ))}
            </div>
            <div className="h-12 w-48 animate-pulse bg-[--color-surface]" />
          </div>
        </div>
      </Container>
    );
  }

  if (isError || !product) {
    return (
      <Container className="py-16">
        <p className="font-medium text-[--color-danger]">
          {error instanceof Error ? error.message : "Не удалось загрузить товар"}
        </p>
        <Link
          to="/catalog"
          className="mt-6 inline-block text-sm font-medium text-black hover:underline"
        >
          ← Назад в каталог
        </Link>
      </Container>
    );
  }

  return (
    <div className="pb-20 md:pb-28">
      <Container>
        {/* Breadcrumb */}
        <motion.nav
          className="flex items-center gap-1.5 py-5 text-[0.6rem] uppercase tracking-[0.14em]"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: reduceMotion ? 0 : 0.3 }}
          aria-label="Навигация"
        >
          <Link to="/" className="text-[--color-muted] transition hover:text-black">
            Главная
          </Link>
          <span className="text-[--color-border]">/</span>
          <Link to="/catalog" className="text-[--color-muted] transition hover:text-black">
            Каталог
          </Link>
          <span className="text-[--color-border]">/</span>
          <span className="text-black">{product.title}</span>
        </motion.nav>

        {/* Content grid */}
        <motion.div
          className="grid items-start gap-10 border-t border-[--color-border] pt-8 lg:grid-cols-2 lg:gap-0 lg:pt-0"
          initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.45, ease: [0.22, 1, 0.36, 1] }}
        >
          {/* Image — sticky on desktop */}
          <div className="lg:sticky lg:top-[96px] lg:py-12">
            <div className="aspect-[4/5] overflow-hidden bg-[--color-surface]">
              <ProductImage product={product} />
            </div>
          </div>

          {/* Info */}
          <ProductBuyColumn key={product.id} product={product} />
        </motion.div>
      </Container>
    </div>
  );
}
