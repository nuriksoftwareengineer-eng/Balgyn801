import { createContext } from "react";
import type { Product } from "@/shared/api/client";

export type CartLine = {
  productId: number;
  title: string;
  price: number;
  imageUrl?: string | null;
  inStock: boolean;
  qty: number;
};

export type CartProductInput = Pick<
  Product,
  "id" | "title" | "price" | "imageUrl" | "inStock"
>;

export type CartContextValue = {
  lines: CartLine[];
  totalQty: number;
  subtotal: number;
  addItem: (product: CartProductInput) => void;
  increment: (productId: number) => void;
  decrement: (productId: number) => void;
  removeLine: (productId: number) => void;
  clear: () => void;
};

export const CartContext = createContext<CartContextValue | null>(null);
