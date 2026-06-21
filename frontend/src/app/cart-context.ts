import { createContext } from "react";
import type { Product } from "@/shared/api/client";

const LINE_SEP = "";

// ── Key factories ─────────────────────────────────────────────────────────────

export function makeCartLineKey(
  productId: number,
  size: string | null | undefined,
  color: string | null | undefined,
): string {
  return `${productId}${LINE_SEP}${size ?? ""}${LINE_SEP}${color ?? ""}`;
}

export function makeDesignLineKey(
  designGarmentId: number,
  colorId: number,
  sizeId: number,
): string {
  return `design${LINE_SEP}${designGarmentId}${LINE_SEP}${colorId}${LINE_SEP}${sizeId}`;
}

// ── Discriminated union line types ────────────────────────────────────────────

export type CartLineLegacy = {
  kind: "legacy";
  lineKey: string;
  productId: number;
  title: string;
  price: number;
  imageUrl?: string | null;
  inStock: boolean;
  qty: number;
  size: string | null;
  color: string | null;
};

export type CartLineDesign = {
  kind: "design";
  lineKey: string;
  designGarmentId: number;
  designId: number;
  designSlug: string;
  groupSlug: string;
  collectionSlug: string;
  title: string;
  garmentType: string;
  garmentLabel: string;
  price: number;
  imageUrl?: string | null;
  qty: number;
  colorId: number;
  colorName: string;
  colorHex: string;
  sizeId: number;
  sizeLabel: string;
};

export type CartLine = CartLineLegacy | CartLineDesign;

// ── Type guards ───────────────────────────────────────────────────────────────

export function isLegacyLine(line: CartLine): line is CartLineLegacy {
  return line.kind === "legacy";
}

export function isDesignLine(line: CartLine): line is CartLineDesign {
  return line.kind === "design";
}

// ── Input types ───────────────────────────────────────────────────────────────

export type CartAddOptions = {
  size?: string | null;
  color?: string | null;
};

export type CartProductInput = Pick<
  Product,
  "id" | "title" | "price" | "imageUrl" | "inStock"
>;

export type CartDesignInput = {
  designGarmentId: number;
  designId: number;
  designSlug: string;
  groupSlug: string;
  collectionSlug: string;
  title: string;
  garmentType: string;
  garmentLabel: string;
  price: number;
  imageUrl?: string | null;
  colorId: number;
  colorName: string;
  colorHex: string;
  sizeId: number;
  sizeLabel: string;
};

// ── Context value ─────────────────────────────────────────────────────────────

export type CartContextValue = {
  lines: CartLine[];
  totalQty: number;
  subtotal: number;
  addItem: (product: CartProductInput, options?: CartAddOptions) => void;
  addDesignItem: (input: CartDesignInput) => void;
  increment: (lineKey: string) => void;
  decrement: (lineKey: string) => void;
  removeLine: (lineKey: string) => void;
  clear: () => void;
};

export const CartContext = createContext<CartContextValue | null>(null);
