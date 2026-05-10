import { createContext } from "react";
import type { Product } from "@/shared/api/client";

const LINE_SEP = "\u001f";

export function makeCartLineKey(
  productId: number,
  size: string | null | undefined,
  color: string | null | undefined,
): string {
  return `${productId}${LINE_SEP}${size ?? ""}${LINE_SEP}${color ?? ""}`;
}

export type CartLine = {
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

export type CartAddOptions = {
  size?: string | null;
  color?: string | null;
};

export type CartProductInput = Pick<
  Product,
  "id" | "title" | "price" | "imageUrl" | "inStock"
>;

export type CartContextValue = {
  lines: CartLine[];
  totalQty: number;
  subtotal: number;
  addItem: (product: CartProductInput, options?: CartAddOptions) => void;
  increment: (lineKey: string) => void;
  decrement: (lineKey: string) => void;
  removeLine: (lineKey: string) => void;
  clear: () => void;
};

export const CartContext = createContext<CartContextValue | null>(null);
