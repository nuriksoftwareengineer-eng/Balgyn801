/** TypeScript types mirroring the catalog storefront API responses. */

export interface CatalogGroupSummary {
  id: number;
  name: string;
  slug: string;
  sortOrder: number;
  active: boolean;
}

export interface CollectionSummary {
  id: number;
  name: string;
  slug: string;
  groupId: number;
  groupName: string;
  sortOrder: number;
  active: boolean;
}

export interface DesignSummary {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  mainImageUrl: string | null;
  collectionId: number;
  collectionName: string;
  collectionSlug: string;
  groupName: string;
  groupSlug: string;
  active: boolean;
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
  garmentType: string;
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

// ── Garment display labels ────────────────────────────────────────────────────

export const GARMENT_LABELS: Record<string, string> = {
  T_SHIRT: "Футболка",
  OVERSIZE_TSHIRT: "Оверсайз",
  LONGSLEEVE: "Лонгслив",
  SWEATSHIRT: "Свитшот",
  HOODIE: "Худи",
  ZIP_HOODIE: "Худи на молнии",
};

export function garmentLabel(garmentType: string): string {
  return GARMENT_LABELS[garmentType] ?? garmentType;
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
