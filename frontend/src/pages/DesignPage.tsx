import { useState, useMemo } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useCatalogDesign } from "@/shared/api/catalog-api";
import {
  dedupeColors,
  dedupeSizes,
  garmentLabel,
  kztPrice,
} from "@/shared/types/catalog";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { useCart } from "@/app/use-cart";
import { formatMoney } from "@/shared/lib/format-money";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/components/ui/button";
import { Container } from "@/shared/ui/container";

export function DesignPage() {
  const { groupSlug, collectionSlug, designSlug } = useParams<{
    groupSlug: string;
    collectionSlug: string;
    designSlug: string;
  }>();
  const navigate = useNavigate();
  const { addDesignItem } = useCart();

  const { data: design, isLoading, error } = useCatalogDesign(designSlug);

  // ── Selections ────────────────────────────────────────────────────────────
  const [selectedGarmentId, setSelectedGarmentId] = useState<number | null>(null);
  const [selectedColorId, setSelectedColorId] = useState<number | null>(null);
  const [selectedSizeId, setSelectedSizeId] = useState<number | null>(null);
  const [added, setAdded] = useState(false);

  useSeoMeta({
    title: design ? `${design.name} — Balgyn` : "Дизайн — Balgyn",
    description: design?.description ?? undefined,
    canonical:
      design
        ? `${window.location.origin}/catalog/${groupSlug}/${collectionSlug}/${designSlug}`
        : undefined,
  });

  // ── Derived ───────────────────────────────────────────────────────────────
  const activeGarments = useMemo(
    () => (design?.garments ?? []).filter((g) => g.active),
    [design],
  );

  const selectedGarment = useMemo(
    () => activeGarments.find((g) => g.id === selectedGarmentId) ?? null,
    [activeGarments, selectedGarmentId],
  );

  const availableColors = useMemo(
    () => dedupeColors(selectedGarment?.colors ?? []),
    [selectedGarment],
  );

  const availableSizes = useMemo(
    () => dedupeSizes(selectedGarment?.sizes ?? []),
    [selectedGarment],
  );

  const selectedColor = availableColors.find((c) => c.id === selectedColorId) ?? null;
  const selectedSize = availableSizes.find((s) => s.id === selectedSizeId) ?? null;
  const displayPrice = selectedGarment ? kztPrice(selectedGarment) : null;

  const canAdd =
    selectedGarment !== null &&
    selectedColor !== null &&
    selectedSize !== null &&
    displayPrice !== null;

  // Reset color/size when garment changes
  function handleSelectGarment(id: number) {
    if (id !== selectedGarmentId) {
      setSelectedGarmentId(id);
      setSelectedColorId(null);
      setSelectedSizeId(null);
      setAdded(false);
    }
  }

  function handleAddToCart() {
    if (!canAdd || !design || !selectedGarment || !selectedColor || !selectedSize || displayPrice === null) return;

    addDesignItem({
      designGarmentId: selectedGarment.id,
      designId: design.id,
      designSlug: design.slug,
      groupSlug: groupSlug ?? design.groupSlug,
      collectionSlug: collectionSlug ?? design.collectionSlug,
      title: design.name,
      garmentType: selectedGarment.garmentType,
      garmentLabel: garmentLabel(selectedGarment.garmentType),
      price: displayPrice,
      imageUrl: design.mainImageUrl,
      colorId: selectedColor.id,
      colorName: selectedColor.name,
      colorHex: selectedColor.hexCode,
      sizeId: selectedSize.id,
      sizeLabel: selectedSize.label,
    });

    setAdded(true);
    navigate("/cart", { state: { justAdded: design.name } });
  }

  // ── Loading / error states ────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-muted]">Загружаем…</p>
        </Container>
      </div>
    );
  }

  if (error || !design) {
    return (
      <div className="py-14">
        <Container>
          <p className="text-sm text-[--color-danger]">Дизайн не найден.</p>
          <Link
            to={collectionSlug ? `/catalog/${groupSlug}/${collectionSlug}` : "/catalog"}
            className="mt-4 inline-block text-sm text-[--color-muted] hover:text-black"
          >
            ← Назад
          </Link>
        </Container>
      </div>
    );
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="py-14">
      <Container>
        {/* Breadcrumb */}
        <nav className="mb-6 flex items-center gap-2 text-[0.6rem] uppercase tracking-[0.1em] text-[--color-muted]">
          <Link to="/" className="hover:text-black transition">Главная</Link>
          <span>›</span>
          <Link to="/catalog" className="hover:text-black transition">Каталог</Link>
          <span>›</span>
          <Link to={`/catalog/${groupSlug}`} className="hover:text-black transition">
            {design.groupName}
          </Link>
          <span>›</span>
          <Link to={`/catalog/${groupSlug}/${collectionSlug}`} className="hover:text-black transition">
            {design.collectionName}
          </Link>
          <span>›</span>
          <span className="text-black">{design.name}</span>
        </nav>

        <div className="flex flex-col gap-10 lg:flex-row lg:items-start lg:gap-14">
          {/* Image */}
          <div className="w-full lg:w-[420px] lg:shrink-0">
            <div className="aspect-square w-full overflow-hidden bg-black">
              {design.mainImageUrl ? (
                <img
                  src={design.mainImageUrl}
                  alt={design.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <span className="text-7xl font-semibold text-white">
                    {design.name.charAt(0).toUpperCase()}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Selectors */}
          <div className="flex-1 min-w-0">
            <h1 className="text-3xl font-semibold uppercase tracking-[0.04em] text-black">
              {design.name}
            </h1>

            {design.description ? (
              <p className="mt-3 text-sm text-[--color-muted] leading-relaxed">
                {design.description}
              </p>
            ) : null}

            {activeGarments.length === 0 ? (
              <p className="mt-6 text-sm text-amber-600">
                Нет доступных вариантов для этого дизайна.
              </p>
            ) : (
              <div className="mt-8 flex flex-col gap-6">
                {/* Garment selector */}
                <div>
                  <p className="mb-2 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                    Изделие
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {activeGarments.map((g) => {
                      const price = kztPrice(g);
                      return (
                        <button
                          key={g.id}
                          type="button"
                          onClick={() => handleSelectGarment(g.id)}
                          className={cn(
                            "border px-4 py-2.5 text-sm transition",
                            selectedGarmentId === g.id
                              ? "border-black bg-black text-white"
                              : "border-[--color-border] bg-white text-black hover:border-zinc-400",
                          )}
                        >
                          {garmentLabel(g.garmentType)}
                          {price != null ? (
                            <span className={cn(
                              "ml-2 text-xs",
                              selectedGarmentId === g.id ? "text-white/70" : "text-[--color-muted]",
                            )}>
                              {formatMoney(price)} ₸
                            </span>
                          ) : null}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Color selector */}
                {selectedGarment && availableColors.length > 0 && (
                  <div>
                    <p className="mb-2 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                      Цвет{selectedColor ? `: ${selectedColor.name}` : ""}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {availableColors.map((c) => (
                        <button
                          key={`${c.name}::${c.hexCode}`}
                          type="button"
                          title={c.name}
                          onClick={() => setSelectedColorId(c.id)}
                          className={cn(
                            "h-9 w-9 rounded-none border-2 transition",
                            selectedColorId === c.id
                              ? "border-black scale-110"
                              : "border-transparent hover:border-zinc-300",
                          )}
                          style={{ backgroundColor: c.hexCode }}
                          aria-label={c.name}
                          aria-pressed={selectedColorId === c.id}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Size selector */}
                {selectedGarment && availableSizes.length > 0 && (
                  <div>
                    <p className="mb-2 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                      Размер
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {availableSizes.map((s) => (
                        <button
                          key={s.label}
                          type="button"
                          onClick={() => setSelectedSizeId(s.id)}
                          className={cn(
                            "min-w-[2.5rem] border px-3 py-2 text-sm font-medium transition",
                            selectedSizeId === s.id
                              ? "border-black bg-black text-white"
                              : "border-[--color-border] bg-white text-black hover:border-zinc-400",
                          )}
                        >
                          {s.label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Price */}
                {displayPrice != null && (
                  <div>
                    <p className="text-2xl font-semibold text-black">
                      {formatMoney(displayPrice)} ₸
                    </p>
                  </div>
                )}

                {/* Add to cart */}
                <div>
                  <Button
                    size="lg"
                    disabled={!canAdd || added}
                    onClick={handleAddToCart}
                    className="w-full sm:w-auto"
                  >
                    {added ? "Добавлено" : "В корзину"}
                  </Button>
                  {!selectedGarment && (
                    <p className="mt-2 text-xs text-[--color-muted]">
                      Выберите изделие, цвет и размер
                    </p>
                  )}
                  {selectedGarment && !selectedColor && availableColors.length > 0 && (
                    <p className="mt-2 text-xs text-[--color-muted]">Выберите цвет</p>
                  )}
                  {selectedGarment && selectedColor && !selectedSize && availableSizes.length > 0 && (
                    <p className="mt-2 text-xs text-[--color-muted]">Выберите размер</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </Container>
    </div>
  );
}
