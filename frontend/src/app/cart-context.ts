import { createContext } from "react";

export type CartContextValue = {
  count: number;
  addOne: () => void;
};

export const CartContext = createContext<CartContextValue | null>(null);
