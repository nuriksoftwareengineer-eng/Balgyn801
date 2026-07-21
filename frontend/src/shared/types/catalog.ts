/** TypeScript types mirroring the catalog storefront API responses. */

export interface CatalogGroupSummary {
  id: number;
  name: string;
  nameKk?: string | null;
  nameEn?: string | null;
  slug: string;
  sortOrder: number;
  active: boolean;
  coverImageUrl?: string | null;
  bannerImageUrl?: string | null;
}

export interface CollectionSummary {
  id: number;
  name: string;
  nameKk?: string | null;
  nameEn?: string | null;
  slug: string;
  groupId: number;
  groupName: string;
  groupNameKk?: string | null;
  groupNameEn?: string | null;
  sortOrder: number;
  active: boolean;
  coverImageUrl?: string | null;
  bannerImageUrl?: string | null;
}

export interface DesignSummary {
  id: number;
  name: string;
  nameKk?: string | null;
  nameEn?: string | null;
  slug: string;
  description: string | null;
  mainImageUrl: string | null;
  collectionId: number;
  collectionName: string;
  collectionNameKk?: string | null;
  collectionNameEn?: string | null;
  collectionSlug: string;
  groupName: string;
  groupNameKk?: string | null;
  groupNameEn?: string | null;
  groupSlug: string;
  active: boolean;
  /** Минимальная KZT-цена активных вариантов — «цена от» на карточках. */
  minPriceKzt?: number | null;
}

export interface GarmentPrice {
  id: number;
  designGarmentId: number;
  currency: string;
  amount: number;
}

export interface ColorInfo {
  id: number;
  name: string;
  hexCode: string;
  sortOrder: number;
}

export interface SizeInfo {
  id: number;
  label: string;
  sortOrder: number;
}

export interface GarmentDetail {
  id: number;
  designId: number;
  designName: string;
  garmentType: string;       // English profile name (fallback)
  garmentTypeRu?: string | null;
  garmentTypeKk?: string | null;
  /** Material composition/care notes from the garment profile, e.g. "95% cotton, 5% polyester". */
  materialDescription?: string | null;
  active: boolean;
  prices: GarmentPrice[];
  colors: ColorInfo[];
  sizes: SizeInfo[];
  /** colorId → sizeId → quantity. Present only on the storefront detail endpoint. */
  stockMap?: Record<number, Record<number, number>>;
}

export interface DesignDetail extends DesignSummary {
  collectionSlug: string;
  groupSlug: string;
  gallery: string[];
  garments: GarmentDetail[];
}

export interface CatalogGroupDetail extends CatalogGroupSummary {
  collections: CollectionSummary[];
}

export interface CollectionDetail extends CollectionSummary {
  designs: DesignSummary[];
}

// ── Deduplication ─────────────────────────────────────────────────────────────

/** Deduplicates by (name + hexCode) — NOT by id; duplicate ids exist in seed data. */
export function dedupeColors(colors: ColorInfo[]): ColorInfo[] {
  const seen = new Set<string>();
  return colors.filter((c) => {
    const key = `${c.name}::${c.hexCode}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

/** Deduplicates by label — NOT by id; duplicate ids exist in seed data. */
export function dedupeSizes(sizes: SizeInfo[]): SizeInfo[] {
  const seen = new Set<string>();
  return sizes.filter((s) => {
    if (seen.has(s.label)) return false;
    seen.add(s.label);
    return true;
  });
}

/** Canonical garment size order. Sizes are labels in the DB (sort_order unset),
 *  so a plain string sort puts XL before M — this fixed list is authoritative.
 *  Unknown labels sink to the end, keeping their relative order. */
const SIZE_ORDER = ["XXXS", "XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "4XL", "5XL"];

export function sortSizes(sizes: SizeInfo[]): SizeInfo[] {
  return [...sizes].sort((a, b) => {
    const ia = SIZE_ORDER.indexOf(a.label.trim().toUpperCase());
    const ib = SIZE_ORDER.indexOf(b.label.trim().toUpperCase());
    return (ia === -1 ? SIZE_ORDER.length : ia) - (ib === -1 ? SIZE_ORDER.length : ib);
  });
}

// ── Localized name helper ─────────────────────────────────────────────────────

/** Pick the right language name, falling back to `name` (the primary/Russian field for catalog
 *  entities, English for GarmentProfile). */
export function localizeName(
  item: { name: string; nameRu?: string | null; nameKk?: string | null; nameEn?: string | null },
  lang: string,
): string {
  const l = lang.split("-")[0];
  if (l === "kk") return item.nameKk || item.nameRu || item.name;
  if (l === "en") return item.nameEn || item.name;
  return item.nameRu || item.name;
}

// ── Garment display labels ────────────────────────────────────────────────────

/** Returns a localized garment type label from a GarmentDetail object.
 *  Falls back to the English profile name if no translation is available. */
export function garmentLabel(
  garment: { garmentType: string; garmentTypeRu?: string | null; garmentTypeKk?: string | null },
  lang: string,
): string {
  const l = lang.split("-")[0];
  if (l === "kk") return garment.garmentTypeKk || garment.garmentTypeRu || garment.garmentType;
  if (l === "en") return garment.garmentType;
  return garment.garmentTypeRu || garment.garmentType;
}

// ── Price helpers ─────────────────────────────────────────────────────────────

/** Returns the KZT price amount for a garment, or null if none defined. */
export function kztPrice(garment: GarmentDetail): number | null {
  const row = garment.prices.find((p) => p.currency === "KZT");
  return row?.amount ?? null;
}

/** Returns min/max KZT price across garments for display on design cards. */
export function priceRange(garments: GarmentDetail[]): { min: number; max: number } | null {
  const prices = garments
    .map(kztPrice)
    .filter((p): p is number => p !== null);
  if (prices.length === 0) return null;
  return { min: Math.min(...prices), max: Math.max(...prices) };
}
