import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useCatalogDesign } from "@/shared/api/catalog-api";
import { getSizeCharts } from "@/shared/api/backend-api";
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
  const [chartOpen, setChartOpen] = useState(false);
  const [activeChartType, setActiveChartType] = useState<string | null>(null);

  const { data: sizeCharts = [] } = useQuery({
    queryKey: ["size-charts"],
    queryFn: getSizeCharts,
    staleTime: 10 * 60 * 1000,
  });

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

  const designGarmentTypes = useMemo(
    () => new Set(activeGarments.map((g) => g.garmentType)),
    [activeGarments],
  );

  const relevantSizeCharts = useMemo(
    () => sizeCharts.filter((c) => designGarmentTypes.has(c.garmentType)),
    [sizeCharts, designGarmentTypes],
  );

  const displayedChart = useMemo(() => {
    if (!relevantSizeCharts.length) return null;
    return (
      relevantSizeCharts.find((c) => c.garmentType === activeChartType) ??
      relevantSizeCharts[0]
    );
  }, [relevantSizeCharts, activeChartType]);

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
      <>
        <div className="border-b border-[--color-border] bg-black">
          <Container className="py-12 md:py-16">
            <div className="mb-3 h-2 w-32 animate-pulse rounded-none bg-white/10" />
            <div className="h-10 w-64 animate-pulse rounded-none bg-white/10 md:h-12" />
          </Container>
        </div>
        <Container className="py-10 md:py-14">
          <div className="flex flex-col gap-10 lg:flex-row lg:gap-14">
            <div className="aspect-square w-full animate-pulse bg-[--color-surface] lg:w-[420px] lg:shrink-0" />
            <div className="flex flex-1 flex-col gap-4">
              <div className="h-4 w-24 animate-pulse bg-[--color-surface]" />
              <div className="h-4 w-48 animate-pulse bg-[--color-surface]" />
            </div>
          </div>
        </Container>
      </>
    );
  }

  if (error || !design) {
    return (
      <>
        <div className="border-b border-[--color-border] bg-black">
          <Container className="py-12 md:py-16">
            <h1 className="text-4xl font-extrabold uppercase tracking-[-0.02em] text-white md:text-5xl">
              Дизайн не найден
            </h1>
          </Container>
        </div>
        <Container className="py-10">
          <Link
            to={collectionSlug ? `/catalog/${groupSlug}/${collectionSlug}` : "/catalog"}
            className="text-[0.65rem] uppercase tracking-[0.12em] text-[--color-muted] transition hover:text-black"
          >
            ← Назад
          </Link>
        </Container>
      </>
    );
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <>
      {/* ── Hero ─────────────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-12 md:py-16">
          <nav className="mb-5 flex flex-wrap items-center gap-2 text-[0.55rem] uppercase tracking-[0.16em] text-white/40">
            <Link to="/" className="transition hover:text-white/70">Главная</Link>
            <span>/</span>
            <Link to="/catalog" className="transition hover:text-white/70">Каталог</Link>
            <span>/</span>
            <Link to={`/catalog/${groupSlug}`} className="transition hover:text-white/70">
              {design.groupName}
            </Link>
            <span>/</span>
            <Link to={`/catalog/${groupSlug}/${collectionSlug}`} className="transition hover:text-white/70">
              {design.collectionName}
            </Link>
            <span>/</span>
            <span className="text-white/70">{design.name}</span>
          </nav>
          <h1 className="text-4xl font-extrabold uppercase tracking-[-0.02em] text-white md:text-6xl">
            {design.name}
          </h1>
        </Container>
      </div>

      {/* ── Content ──────────────────────────────────────────────────── */}
      <Container className="py-8 md:py-10">
        <div className="flex flex-col gap-8 lg:flex-row lg:items-start lg:gap-10">
          {/* Image */}
          <div className="w-full lg:w-[380px] lg:shrink-0">
            <div className="aspect-square w-full overflow-hidden bg-zinc-900">
              {design.mainImageUrl ? (
                <img
                  src={design.mainImageUrl}
                  alt={design.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <span className="text-7xl font-extrabold text-white/10">
                    {design.name.charAt(0).toUpperCase()}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Selectors */}
          <div className="flex-1 min-w-0">
            {design.description ? (
              <p className="mb-6 text-sm leading-relaxed text-[--color-muted]">
                {design.description}
              </p>
            ) : null}

            {/* Size Charts — toggle on click, available before garment selection */}
            {relevantSizeCharts.length > 0 && (
              <div className="mb-6">
                <button
                  type="button"
                  onClick={() => setChartOpen((o) => !o)}
                  className="flex items-center gap-1.5 text-[0.65rem] uppercase tracking-[0.1em] text-[--color-muted] transition hover:text-black"
                >
                  📏 Размерная сетка
                  <svg
                    width="8"
                    height="5"
                    viewBox="0 0 8 5"
                    fill="none"
                    aria-hidden="true"
                    className={cn("transition-transform duration-150", chartOpen && "rotate-180")}
                  >
                    <path d="M1 1l3 3 3-3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                </button>
                {chartOpen && (
                  <div className="mt-3 border border-[--color-border]">
                    {relevantSizeCharts.length > 1 && (
                      <div className="flex border-b border-[--color-border]">
                        {relevantSizeCharts.map((c) => {
                          const isActive =
                            (activeChartType ?? relevantSizeCharts[0]?.garmentType) ===
                            c.garmentType;
                          return (
                            <button
                              key={c.garmentType}
                              type="button"
                              onClick={() => setActiveChartType(c.garmentType)}
                              className={cn(
                                "-mb-px border-b-2 px-3 py-2 text-[0.65rem] font-medium uppercase tracking-[0.1em] transition",
                                isActive
                                  ? "border-black text-black"
                                  : "border-transparent text-[--color-muted] hover:text-black",
                              )}
                            >
                              {garmentLabel(c.garmentType)}
                            </button>
                          );
                        })}
                      </div>
                    )}
                    {displayedChart && (
                      <div className="bg-[--color-surface] p-4">
                        <img
                          src={displayedChart.imageUrl}
                          alt={displayedChart.title ?? garmentLabel(displayedChart.garmentType)}
                          className="max-h-96 w-full object-contain"
                        />
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {activeGarments.length === 0 ? (
              <p className="text-sm text-amber-600">
                Нет доступных вариантов для этого дизайна.
              </p>
            ) : (
              <div className="flex flex-col gap-6">
                {/* Garment selector */}
                <div>
                  <p className="mb-3 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
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
                            "border px-4 py-2.5 text-[13px] transition",
                            selectedGarmentId === g.id
                              ? "border-black bg-black text-white"
                              : "border-[--color-border] bg-white text-black hover:border-zinc-400",
                          )}
                        >
                          {garmentLabel(g.garmentType)}
                          {price != null ? (
                            <span className={cn(
                              "ml-2 text-xs",
                              selectedGarmentId === g.id ? "text-white/60" : "text-[--color-muted]",
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
                    <p className="mb-3 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
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
                    <p className="mb-3 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                      Размер
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {availableSizes.map((s) => (
                        <button
                          key={s.label}
                          type="button"
                          onClick={() => setSelectedSizeId(s.id)}
                          className={cn(
                            "min-w-[2.75rem] border px-3 py-2 text-[13px] font-medium transition",
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

                {/* Price + CTA */}
                <div className="border-t border-[--color-border] pt-5">
                  {displayPrice != null && (
                    <p className="mb-5 text-3xl font-extrabold tracking-[-0.02em] text-black">
                      {formatMoney(displayPrice)} ₸
                    </p>
                  )}
                  <button
                    type="button"
                    disabled={!canAdd || added}
                    onClick={handleAddToCart}
                    className="w-full bg-black py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800 disabled:opacity-40 sm:w-auto sm:px-10"
                  >
                    {added ? "Добавлено ✓" : "В корзину"}
                  </button>
                  {!selectedGarment && (
                    <p className="mt-3 text-[0.7rem] text-[--color-muted]">
                      Выберите изделие, цвет и размер
                    </p>
                  )}
                  {selectedGarment && !selectedColor && availableColors.length > 0 && (
                    <p className="mt-3 text-[0.7rem] text-[--color-muted]">Выберите цвет</p>
                  )}
                  {selectedGarment && selectedColor && !selectedSize && availableSizes.length > 0 && (
                    <p className="mt-3 text-[0.7rem] text-[--color-muted]">Выберите размер</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </Container>

    </>
  );
}
