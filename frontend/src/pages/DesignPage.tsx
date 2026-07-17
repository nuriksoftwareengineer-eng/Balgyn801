import { useState, useMemo, useEffect, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useCatalogDesign } from "@/shared/api/catalog-api";
import { getSizeCharts } from "@/shared/api/backend-api";
import {
  dedupeColors,
  dedupeSizes,
  sortSizes,
  garmentLabel,
  localizeName,
  kztPrice,
} from "@/shared/types/catalog";
import { useSeoMeta } from "@/shared/hooks/useSeoMeta";
import { useCart } from "@/app/use-cart";
import { useCurrency } from "@/app/currency-context";
import { cn } from "@/shared/lib/cn";
import { haptic } from "@/shared/lib/telegram";
import { Container } from "@/shared/ui/container";
import { Toast } from "@/shared/ui/toast";
import { RecommendedSection } from "@/widgets/catalog/RecommendedSection";

// ── Lightbox ──────────────────────────────────────────────────────────────────
interface LightboxProps {
  images: string[];
  index: number;
  onClose: () => void;
  onNav: (i: number) => void;
}

function Lightbox({ images, index, onClose, onNav }: LightboxProps) {
  const [zoomed, setZoomed] = useState(false);

  const prev = useCallback(() => onNav((index - 1 + images.length) % images.length), [index, images.length, onNav]);
  const next = useCallback(() => onNav((index + 1) % images.length), [index, images.length, onNav]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
      if (e.key === "ArrowLeft") prev();
      if (e.key === "ArrowRight") next();
    }
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [onClose, prev, next]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/95"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      {/* Close */}
      <button
        type="button"
        onClick={onClose}
        aria-label="Закрыть"
        className="absolute right-4 top-4 z-10 rounded p-2 text-white/60 transition hover:text-white"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
          <path d="M18 6L6 18M6 6l12 12" />
        </svg>
      </button>

      {/* Prev */}
      {images.length > 1 && (
        <button
          type="button"
          onClick={prev}
          aria-label="Предыдущее"
          className="absolute left-4 top-1/2 -translate-y-1/2 z-10 rounded p-2 text-white/60 transition hover:text-white"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
            <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      )}

      {/* Image */}
      <div
        className="relative flex h-full max-h-[90dvh] w-full max-w-4xl items-center justify-center px-16"
        onClick={() => setZoomed((z) => !z)}
      >
        <img
          src={images[index]}
          alt={`Фото ${index + 1}`}
          draggable={false}
          className={cn(
            "max-h-full max-w-full object-contain transition-transform duration-300",
            zoomed ? "scale-[1.8] cursor-zoom-out" : "cursor-zoom-in",
          )}
        />
      </div>

      {/* Next */}
      {images.length > 1 && (
        <button
          type="button"
          onClick={next}
          aria-label="Следующее"
          className="absolute right-4 top-1/2 -translate-y-1/2 z-10 rounded p-2 text-white/60 transition hover:text-white"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
            <path d="M9 18l6-6-6-6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      )}

      {/* Counter */}
      {images.length > 1 && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2">
          {images.map((_, i) => (
            <button
              key={i}
              type="button"
              onClick={(e) => { e.stopPropagation(); onNav(i); }}
              className={cn(
                "h-1.5 rounded-full transition-all",
                i === index ? "w-6 bg-white" : "w-1.5 bg-white/40",
              )}
              aria-label={`Фото ${i + 1}`}
            />
          ))}
        </div>
      )}

      {/* Zoom hint */}
      <p className="absolute bottom-10 right-4 text-[10px] text-white/30">
        {zoomed ? "Нажмите для уменьшения" : "Нажмите для увеличения"}
      </p>
    </div>
  );
}

export function DesignPage() {
  const { t, i18n } = useTranslation();
  const { groupSlug, collectionSlug, designSlug } = useParams<{
    groupSlug: string;
    collectionSlug: string;
    designSlug: string;
  }>();
  const { addDesignItem } = useCart();
  const { format } = useCurrency();

  const { data: design, isLoading, error } = useCatalogDesign(designSlug);

  // ── Selections ────────────────────────────────────────────────────────────
  const [selectedGarmentId, setSelectedGarmentId] = useState<number | null>(null);
  const [selectedColorId, setSelectedColorId] = useState<number | null>(null);
  const [selectedSizeId, setSelectedSizeId] = useState<number | null>(null);
  const [added, setAdded] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [chartOpen, setChartOpen] = useState(false);
  const [activeChartType, setActiveChartType] = useState<string | null>(null);
  const [galleryIndex, setGalleryIndex] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  const { data: sizeCharts = [] } = useQuery({
    queryKey: ["size-charts"],
    queryFn: getSizeCharts,
    staleTime: 10 * 60 * 1000,
  });

  useSeoMeta({
    title: design ? localizeName(design, i18n.language) : t("nav.catalog"),
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

  // Stock for the currently selected color: sizeId → quantity
  const stockForColor = useMemo<Record<number, number>>(
    () => (selectedColorId != null ? (selectedGarment?.stockMap?.[selectedColorId] ?? {}) : {}),
    [selectedGarment, selectedColorId],
  );

  const availableSizes = useMemo(() => {
    const all = sortSizes(dedupeSizes(selectedGarment?.sizes ?? []));
    // If no stockMap provided (API omits it), show all sizes unchanged
    if (!selectedGarment?.stockMap || selectedColorId == null) return all;
    return all.filter((s) => (stockForColor[s.id] ?? 0) > 0);
  }, [selectedGarment, selectedColorId, stockForColor]);

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

  // When color changes, clear size if it has no stock in the new color
  function handleColorChange(colorId: number) {
    setSelectedColorId(colorId);
    setAdded(false);
    if (selectedSizeId != null && selectedGarment?.stockMap) {
      const newColorStock = selectedGarment.stockMap[colorId] ?? {};
      if ((newColorStock[selectedSizeId] ?? 0) === 0) {
        setSelectedSizeId(null);
      }
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
      garmentLabel: garmentLabel(selectedGarment, i18n.language),
      price: displayPrice,
      imageUrl: design.mainImageUrl,
      colorId: selectedColor.id,
      colorName: selectedColor.name,
      colorHex: selectedColor.hexCode,
      sizeId: selectedSize.id,
      sizeLabel: selectedSize.label,
    });

    setAdded(true);
    setToastMessage(t("cart.addedToCart", { title: design.name }));
    haptic("success");
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
            <h1 className="text-4xl font-bold uppercase tracking-[-0.02em] text-white md:text-5xl">
              {t("design.notFound")}
            </h1>
          </Container>
        </div>
        <Container className="py-10">
          <Link
            to={collectionSlug ? `/catalog/${groupSlug}/${collectionSlug}` : "/catalog"}
            className="text-[0.65rem] uppercase tracking-[0.12em] text-[--color-muted] transition hover:text-black"
          >
            {t("design.back")}
          </Link>
        </Container>
      </>
    );
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <>
      {/* ── Content — photo-first two-column layout ──────────────────── */}
      <Container className="py-6 md:py-10">
        {/* Breadcrumb */}
        <nav className="mb-6 flex flex-wrap items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted] md:mb-10">
          <Link to="/" className="transition hover:text-black">{t("nav.home")}</Link>
          <span>/</span>
          <Link to="/catalog" className="transition hover:text-black">{t("nav.catalog")}</Link>
          <span>/</span>
          <Link to={`/catalog/${groupSlug}/${collectionSlug}`} className="transition hover:text-black">
            {localizeName({ name: design.collectionName, nameKk: design.collectionNameKk, nameEn: design.collectionNameEn }, i18n.language)}
          </Link>
          <span>/</span>
          <span className="text-black">{localizeName(design, i18n.language)}</span>
        </nav>

        <div className="flex flex-col gap-8 lg:flex-row lg:items-start lg:gap-16">
          {/* Image + Gallery — dominant column */}
          {(() => {
            const allImages = [
              ...(design.mainImageUrl ? [design.mainImageUrl] : []),
              ...(design.gallery ?? []),
            ];
            const activeImg = allImages[galleryIndex];
            return (
              <div className="w-full lg:flex-1">
                {/* Main image */}
                <div
                  className="group relative aspect-[4/5] w-full cursor-zoom-in overflow-hidden bg-[--color-surface]"
                  onClick={() => { if (allImages.length) setLightboxOpen(true); }}
                >
                  {activeImg ? (
                    <img
                      src={activeImg}
                      alt={design.name}
                      className="gallery-img h-full w-full object-cover"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <span className="text-8xl font-semibold text-black/[0.07]">
                        {design.name.charAt(0).toUpperCase()}
                      </span>
                    </div>
                  )}
                  {/* Expand hint */}
                  {allImages.length > 0 && (
                    <div className="absolute right-3 top-3 rounded bg-black/60 px-2 py-1 text-[10px] text-white/70 opacity-0 transition-opacity group-hover:opacity-100">
                      🔍 {allImages.length > 1 ? `1/${allImages.length}` : "Увеличить"}
                    </div>
                  )}
                </div>

                {/* Thumbnail rail */}
                {allImages.length > 1 && (
                  <div className="mt-2 flex gap-1 overflow-x-auto pb-1">
                    {allImages.map((url, i) => (
                      <button
                        key={i}
                        type="button"
                        onClick={() => setGalleryIndex(i)}
                        aria-label={`Фото ${i + 1}`}
                        className={cn(
                          "aspect-square h-16 shrink-0 overflow-hidden bg-zinc-900 transition",
                          galleryIndex === i
                            ? "ring-2 ring-black ring-offset-1"
                            : "opacity-60 hover:opacity-90",
                        )}
                      >
                        <img
                          src={url}
                          alt={`${design.name} ${i + 1}`}
                          className="h-full w-full object-cover"
                        />
                      </button>
                    ))}
                  </div>
                )}

                {/* Lightbox */}
                {lightboxOpen && allImages.length > 0 && (
                  <Lightbox
                    images={allImages}
                    index={galleryIndex}
                    onClose={() => setLightboxOpen(false)}
                    onNav={setGalleryIndex}
                  />
                )}
              </div>
            );
          })()}

          {/* Selectors — secondary column, sticky on desktop */}
          <div className="w-full min-w-0 lg:w-[380px] lg:shrink-0 lg:sticky lg:top-28">
            {/* Title */}
            <p className="mb-2 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
              {localizeName({ name: design.collectionName, nameKk: design.collectionNameKk, nameEn: design.collectionNameEn }, i18n.language)}
            </p>
            <h1 className="mb-5 text-[28px] font-semibold uppercase leading-[1.05] tracking-[-0.01em] md:text-[34px]">
              {localizeName(design, i18n.language)}
            </h1>

            {design.description ? (
              <p className="mb-7 text-[14px] leading-relaxed text-[--color-muted]">
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
                  📏 {t("design.sizeChart")}
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
                              {garmentLabel({ garmentType: c.garmentType }, i18n.language)}
                            </button>
                          );
                        })}
                      </div>
                    )}
                    {displayedChart && (
                      <div className="bg-[--color-surface] p-4">
                        <img
                          src={displayedChart.imageUrl}
                          alt={displayedChart.title ?? garmentLabel({ garmentType: displayedChart.garmentType }, i18n.language)}
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
                {t("design.noVariants")}
              </p>
            ) : (
              <div className="flex flex-col gap-6">
                {/* Garment selector */}
                <div>
                  <p className="mb-3 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-black">
                    {t("design.garment")}
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
                              : "border-[--color-border] bg-white text-black hover:border-black",
                          )}
                        >
                          {garmentLabel(g, i18n.language)}
                          {price != null ? (
                            <span className={cn(
                              "ml-2 text-xs",
                              selectedGarmentId === g.id ? "text-white/60" : "text-[--color-muted]",
                            )}>
                              {format(price)}
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
                      {selectedColor ? t("design.colorSelected", { name: selectedColor.name }) : t("design.color")}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {availableColors.map((c) => (
                        <button
                          key={`${c.name}::${c.hexCode}`}
                          type="button"
                          title={c.name}
                          onClick={() => handleColorChange(c.id)}
                          className={cn(
                            "h-9 w-9 rounded-none border-2 transition",
                            selectedColorId === c.id
                              ? "border-black scale-110"
                              : "border-zinc-200 hover:border-black",
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
                      {t("design.size")}
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
                              : "border-[--color-border] bg-white text-black hover:border-black",
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
                    <p className="mb-5 text-3xl font-bold tracking-[-0.02em] text-black">
                      {format(displayPrice)}
                    </p>
                  )}
                  <button
                    type="button"
                    disabled={!canAdd || added}
                    onClick={handleAddToCart}
                    className="w-full bg-black py-4 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800 disabled:opacity-40 sm:w-auto sm:px-10"
                  >
                    {added ? t("design.added") : t("design.addToCart")}
                  </button>
                  {!selectedGarment && (
                    <p className="mt-3 text-[0.7rem] text-[--color-muted]">
                      {t("design.selectAll")}
                    </p>
                  )}
                  {selectedGarment && !selectedColor && availableColors.length > 0 && (
                    <p className="mt-3 text-[0.7rem] text-[--color-muted]">{t("design.selectColor")}</p>
                  )}
                  {selectedGarment && selectedColor && !selectedSize && availableSizes.length > 0 && (
                    <p className="mt-3 text-[0.7rem] text-[--color-muted]">{t("design.selectSize")}</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </Container>

      {design && <RecommendedSection designId={design.id} />}

      <Toast message={toastMessage} onDismiss={() => setToastMessage(null)} />
    </>
  );
}
