/**
 * Admin catalog API client — CatalogGroup (категория) → Collection → Design.
 * Все эндпоинты под /api/v1/admin/** требуют JWT администратора (передаётся token).
 * Витрина (/api/v1/catalog/**) читает те же данные публично — созданное здесь появляется
 * на /catalog/{groupSlug}/{collectionSlug} без ручных правок БД.
 */
import { apiFetch } from "@/shared/api/http";

// ─── Types ──────────────────────────────────────────────────────────────────

export interface AdminGroup {
  id: number;
  name: string;
  slug: string;
  sortOrder: number | null;
  active: boolean;
}

export interface AdminGroupRequest {
  name: string;
  slug: string;
  sortOrder?: number;
}

export interface AdminCollection {
  id: number;
  groupId: number;
  groupName: string;
  name: string;
  slug: string;
  description: string | null;
  coverImageUrl: string | null;
  bannerImageUrl: string | null;
  sortOrder: number | null;
  active: boolean;
}

export interface AdminCollectionRequest {
  groupId: number;
  name: string;
  slug: string;
  description?: string | null;
  coverImageUrl?: string | null;
  bannerImageUrl?: string | null;
  sortOrder?: number;
}

export type DesignStatus = "DRAFT" | "READY" | "PUBLISHED" | "ARCHIVED";

export interface AdminDesign {
  id: number;
  collectionId: number;
  collectionName: string;
  collectionSlug: string;
  groupName: string;
  groupSlug: string;
  name: string;
  slug: string;
  description: string | null;
  mainImageUrl: string | null;
  gallery: string[];
  status: DesignStatus;
  activeGarmentCount: number;
}

export interface AdminDesignRequest {
  collectionId: number;
  name: string;
  slug: string;
  description?: string | null;
  mainImageUrl?: string | null;
  gallery?: string[];
}

// ─── Categories (CatalogGroup) ────────────────────────────────────────────────

export function listGroups(token: string): Promise<AdminGroup[]> {
  return apiFetch<AdminGroup[]>("/admin/catalog/groups", { token });
}

export function createGroup(body: AdminGroupRequest, token: string): Promise<AdminGroup> {
  return apiFetch<AdminGroup>("/admin/catalog/groups", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

export function updateGroup(id: number, body: AdminGroupRequest, token: string): Promise<AdminGroup> {
  return apiFetch<AdminGroup>(`/admin/catalog/groups/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
    token,
  });
}

export function deleteGroup(id: number, token: string): Promise<void> {
  return apiFetch<void>(`/admin/catalog/groups/${id}`, { method: "DELETE", token });
}

// ─── Collections ──────────────────────────────────────────────────────────────

export function listCollections(token: string, groupId?: number): Promise<AdminCollection[]> {
  const q = groupId != null ? `?groupId=${groupId}` : "";
  return apiFetch<AdminCollection[]>(`/admin/catalog/collections${q}`, { token });
}

export function createCollection(body: AdminCollectionRequest, token: string): Promise<AdminCollection> {
  return apiFetch<AdminCollection>("/admin/catalog/collections", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

export function updateCollection(id: number, body: AdminCollectionRequest, token: string): Promise<AdminCollection> {
  return apiFetch<AdminCollection>(`/admin/catalog/collections/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
    token,
  });
}

export function deleteCollection(id: number, token: string): Promise<void> {
  return apiFetch<void>(`/admin/catalog/collections/${id}`, { method: "DELETE", token });
}

// ─── Designs ──────────────────────────────────────────────────────────────────

export function listDesigns(token: string, collectionId?: number): Promise<AdminDesign[]> {
  const q = collectionId != null ? `?collectionId=${collectionId}` : "";
  return apiFetch<AdminDesign[]>(`/admin/catalog/designs${q}`, { token });
}

export function createDesign(body: AdminDesignRequest, token: string): Promise<AdminDesign> {
  return apiFetch<AdminDesign>("/admin/catalog/designs", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

export function updateDesign(id: number, body: AdminDesignRequest, token: string): Promise<AdminDesign> {
  return apiFetch<AdminDesign>(`/admin/catalog/designs/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
    token,
  });
}

export function deleteDesign(id: number, token: string): Promise<void> {
  return apiFetch<void>(`/admin/catalog/designs/${id}`, { method: "DELETE", token });
}

export function publishDesign(id: number, token: string): Promise<AdminDesign> {
  return apiFetch<AdminDesign>(`/admin/catalog/designs/${id}/publish`, {
    method: "PATCH",
    token,
  });
}

export function archiveDesign(id: number, token: string): Promise<AdminDesign> {
  return apiFetch<AdminDesign>(`/admin/catalog/designs/${id}/archive`, {
    method: "PATCH",
    token,
  });
}

// ─── Variants (DesignGarment) ─────────────────────────────────────────────────

/** Типы изделий для нового варианта (этап 3). */
export const GARMENT_TYPES = ["HOODIE", "SWEATSHIRT", "T_SHIRT"] as const;
export type GarmentType = (typeof GARMENT_TYPES)[number];

export const GARMENT_TYPE_LABELS: Record<string, string> = {
  HOODIE: "Худи",
  SWEATSHIRT: "Свитшот",
  T_SHIRT: "Футболка",
};

export interface AdminColor {
  id: number;
  name: string;
  hexCode: string;
  sortOrder: number | null;
}

export interface AdminSize {
  id: number;
  label: string;
  sortOrder: number | null;
}

export interface AdminPrice {
  id: number;
  designGarmentId: number;
  currency: string;
  amount: number;
}

export interface AdminGarment {
  id: number;
  designId: number;
  designName: string;
  garmentType: string;
  active: boolean;
  prices: AdminPrice[];
  colors: AdminColor[];
  sizes: AdminSize[];
}

export interface AdminInventory {
  id: number;
  designGarmentId: number;
  colorId: number;
  colorName: string;
  sizeId: number;
  sizeLabel: string;
  quantity: number;
}

export interface CreateGarmentRequest {
  designId: number;
  garmentType: string;
  colorIds?: number[];
  sizeIds?: number[];
}

export interface UpdateGarmentRequest {
  active?: boolean;
  colorIds: number[];
  sizeIds: number[];
}

// Garments
export function listGarments(designId: number, token: string): Promise<AdminGarment[]> {
  return apiFetch<AdminGarment[]>(`/admin/catalog/garments?designId=${designId}`, { token });
}
export function createGarment(body: CreateGarmentRequest, token: string): Promise<AdminGarment> {
  return apiFetch<AdminGarment>("/admin/catalog/garments", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}
export function updateGarment(id: number, body: UpdateGarmentRequest, token: string): Promise<AdminGarment> {
  return apiFetch<AdminGarment>(`/admin/catalog/garments/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
    token,
  });
}
export function deleteGarment(id: number, token: string): Promise<void> {
  return apiFetch<void>(`/admin/catalog/garments/${id}`, { method: "DELETE", token });
}

// Prices (upsert per currency)
export function upsertPrice(
  body: { designGarmentId: number; currency: string; amount: number },
  token: string,
): Promise<AdminPrice> {
  return apiFetch<AdminPrice>("/admin/catalog/prices", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

// Inventory (set quantity per color×size)
export function listInventory(designGarmentId: number, token: string): Promise<AdminInventory[]> {
  return apiFetch<AdminInventory[]>(`/admin/catalog/inventory?designGarmentId=${designGarmentId}`, { token });
}
export function setInventory(
  body: { designGarmentId: number; colorId: number; sizeId: number; quantity: number },
  token: string,
): Promise<AdminInventory> {
  return apiFetch<AdminInventory>("/admin/catalog/inventory", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

// Color / size dictionaries
export function listColors(token: string): Promise<AdminColor[]> {
  return apiFetch<AdminColor[]>("/admin/catalog/colors", { token });
}
export function createColor(body: { name: string; hexCode: string; sortOrder?: number }, token: string): Promise<AdminColor> {
  return apiFetch<AdminColor>("/admin/catalog/colors", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}
export function listSizes(token: string): Promise<AdminSize[]> {
  return apiFetch<AdminSize[]>("/admin/catalog/sizes", { token });
}
export function createSize(body: { label: string; sortOrder?: number }, token: string): Promise<AdminSize> {
  return apiFetch<AdminSize>("/admin/catalog/sizes", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}
