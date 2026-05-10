import { motion, useReducedMotion } from "framer-motion";
import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import { useProduct } from "@/shared/api/queries";
import type { Product } from "@/shared/api/types";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import { ProductImage } from "@/widgets/ProductImage";

function ProductBuyColumn({ product }: { product: Product }) {
  const { addItem } = useCart();
  const navigate = useNavigate();

  const sizes = useMemo(
    () => product.sizes?.filter((s) => s && String(s).trim()) ?? [],
    [product],
  );
  const colors = useMemo(
    () =>
      product.colors?.filter((c) => c?.name && String(c.name).trim()) ?? [],
    [product],
  );

  const [pickSize, setPickSize] = useState<string | null>(() =>
    sizes[0] ? String(sizes[0]).trim() : null,
  );
  const [pickColor, setPickColor] = useState<string | null>(() =>
    colors[0]?.name ? String(colors[0].name).trim() : null,
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
  }

  return (
    <div className="flex flex-col pt-1">
      {product.category ? (
        <p className="mb-1 text-xs font-bold uppercase tracking-[0.12em] text-violet-400/90">
          {product.category}
        </p>
      ) : null}
      <h1 className="font-display text-[clamp(2rem,5vw,3rem)] leading-[1.05] tracking-wide text-zinc-100">
        {product.title}
      </h1>
      {product.description ? (
        <p className="mt-5 whitespace-pre-line text-lg leading-relaxed text-zinc-400">
          {product.description}
        </p>
      ) : null}

      <div className="mt-8 flex flex-wrap items-center gap-4">
        <span className="text-3xl font-bold text-zinc-100">
          {formatMoney(product.price)} ₸
        </span>
        <span
          className={cn(
            "rounded-full px-3 py-1 text-xs font-bold uppercase tracking-[0.06em]",
            product.inStock
              ? "bg-green-500/15 text-green-400"
              : "bg-red-500/12 text-red-400",
          )}
        >
          {product.inStock ? "В наличии" : "Нет в наличии"}
        </span>
      </div>

      {needSize ? (
        <div className="mt-8">
          <p className="mb-2 text-sm font-semibold text-zinc-400">Размер</p>
          <div className="flex flex-wrap gap-2">
            {sizes.map((s) => {
              const label = String(s).trim();
              const active = pickSize === label;
              return (
                <button
                  key={label}
                  type="button"
                  onClick={() => setPickSize(label)}
                  className={cn(
                    "min-w-[2.75rem] rounded-full border px-4 py-2 text-sm font-semibold transition",
                    active
                      ? "border-violet-500 bg-violet-500/20 text-zinc-100"
                      : "border-white/15 bg-zinc-900 text-zinc-400 hover:border-violet-500/40 hover:text-zinc-200",
                  )}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>
      ) : null}

      {needColor ? (
        <div className="mt-6">
          <p className="mb-2 text-sm font-semibold text-zinc-400">Цвет</p>
          <div className="flex flex-wrap gap-3">
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
                    "flex items-center gap-2 rounded-full border px-3 py-2 text-sm font-semibold transition",
                    active
                      ? "border-violet-500 bg-violet-500/15 text-zinc-100"
                      : "border-white/15 bg-zinc-900 text-zinc-400 hover:border-violet-500/40 hover:text-zinc-200",
                  )}
                >
                  {hex ? (
                    <span
                      className="h-5 w-5 rounded-full border border-white/20 shadow-inner"
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

      <div className="mt-10 flex flex-wrap gap-3">
        <Button
          type="button"
          variant="primary"
          className="rounded-full px-8"
          disabled={!canAdd}
          onClick={handleAddToCart}
        >
          {product.inStock ? "В корзину" : "Ожидаем поступление"}
        </Button>
        <Button
          type="button"
          variant="outline"
          className="rounded-full"
          onClick={() => navigate("/catalog")}
        >
          Другие модели
        </Button>
      </div>
    </div>
  );
}

export function ProductPage() {
  const { productId } = useParams<{ productId: string }>();
  const id = Number.parseInt(productId ?? "", 10);
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();

  const { data: product, isPending, isError, error } = useProduct(
    Number.isFinite(id) && id > 0 ? id : undefined,
  );

  const dur = reduceMotion ? 0 : 0.45;

  if (!Number.isFinite(id) || id <= 0) {
    return (
      <Container className="py-16">
        <p className="text-zinc-400">Некорректная ссылка на товар.</p>
        <Button type="button" variant="outline" className="mt-6" onClick={() => navigate("/catalog")}>
          В каталог
        </Button>
      </Container>
    );
  }

  if (isPending) {
    return (
      <Container className="py-14">
        <div className="grid gap-10 lg:grid-cols-2 lg:gap-14">
          <div className="skeleton-shimmer aspect-[4/5] rounded-[14px] border border-white/10 bg-zinc-900" />
          <div className="space-y-4 pt-2">
            <div className="skeleton-shimmer h-8 w-4/5 rounded-md bg-zinc-800" />
            <div className="skeleton-shimmer h-4 w-full rounded-md bg-zinc-800/80" />
            <div className="skeleton-shimmer h-4 w-5/6 rounded-md bg-zinc-800/80" />
            <div className="skeleton-shimmer mt-8 h-12 w-40 rounded-full bg-zinc-800" />
          </div>
        </div>
      </Container>
    );
  }

  if (isError || !product) {
    return (
      <Container className="py-16">
        <p className="font-semibold text-red-400">
          {error instanceof Error ? error.message : "Не удалось загрузить товар"}
        </p>
        <Link
          to="/catalog"
          className="mt-6 inline-block font-semibold text-violet-400 hover:underline"
        >
          ← Назад в каталог
        </Link>
      </Container>
    );
  }

  return (
    <div className="py-10 md:py-14">
      <Container>
        <motion.div
          initial={{ opacity: 0, y: reduceMotion ? 0 : 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: dur }}
        >
          <nav className="mb-8 text-sm text-zinc-500">
            <Link to="/" className="hover:text-zinc-300">
              Главная
            </Link>
            <span className="mx-2 text-zinc-600">/</span>
            <Link to="/catalog" className="hover:text-zinc-300">
              Каталог
            </Link>
            <span className="mx-2 text-zinc-600">/</span>
            <span className="text-zinc-400">{product.title}</span>
          </nav>

          <div className="grid gap-10 lg:grid-cols-2 lg:gap-14">
            <motion.div
              className="overflow-hidden rounded-[14px] border border-white/10 bg-zinc-900"
              initial={{ opacity: 0, scale: reduceMotion ? 1 : 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: dur, delay: reduceMotion ? 0 : 0.06 }}
            >
              <div className="relative aspect-[4/5] bg-gradient-to-br from-zinc-800 to-zinc-950">
                <ProductImage product={product} />
              </div>
            </motion.div>

            <ProductBuyColumn key={product.id} product={product} />
          </div>
        </motion.div>
      </Container>
    </div>
  );
}
