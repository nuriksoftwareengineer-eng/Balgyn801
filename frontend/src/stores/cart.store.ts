import { create } from "zustand";
import { persist } from "zustand/middleware";

// ─── Types ────────────────────────────────────────────────────────────────

/** One line in the cart, keyed by the combination of garment + color + size. */
export interface CartItem {
  /** Composite key: `${designGarmentId}:${colorId}:${sizeId}` */
  key: string;
  designGarmentId: number;
  colorId: number;
  sizeId: number;
  quantity: number;
  /** Price snapshot at the moment the item was added. */
  unitPrice: number;
  // Display snapshots — so the drawer renders without API calls
  designName: string;
  garmentTypeName: string; // "Hoodie" | "T-Shirt" | "Sweatshirt" etc.
  colorName: string;
  sizeLabel: string;
  imageUrl: string | null;
}

export type AddCartItemInput = Omit<CartItem, "key" | "quantity"> & {
  quantity?: number;
};

export interface CartState {
  items: CartItem[];

  // Actions
  addItem: (input: AddCartItemInput) => void;
  updateQty: (key: string, qty: number) => void;
  removeLine: (key: string) => void;
  clear: () => void;
}

// ─── Helpers ─────────────────────────────────────────────────────────────

function makeKey(
  designGarmentId: number,
  colorId: number,
  sizeId: number,
): string {
  return `${designGarmentId}:${colorId}:${sizeId}`;
}

// ─── Store ────────────────────────────────────────────────────────────────

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],

      addItem: (input) => {
        const key = makeKey(
          input.designGarmentId,
          input.colorId,
          input.sizeId,
        );
        const qty = input.quantity ?? 1;

        set((state) => {
          const existing = state.items.find((i) => i.key === key);
          if (existing) {
            return {
              items: state.items.map((i) =>
                i.key === key ? { ...i, quantity: i.quantity + qty } : i,
              ),
            };
          }
          const newItem: CartItem = { ...input, key, quantity: qty };
          return { items: [...state.items, newItem] };
        });
      },

      updateQty: (key, qty) => {
        if (qty <= 0) {
          get().removeLine(key);
          return;
        }
        set((state) => ({
          items: state.items.map((i) =>
            i.key === key ? { ...i, quantity: qty } : i,
          ),
        }));
      },

      removeLine: (key) => {
        set((state) => ({
          items: state.items.filter((i) => i.key !== key),
        }));
      },

      clear: () => set({ items: [] }),
    }),
    {
      name: "balgyn-cart-v1",
      version: 1,
    },
  ),
);

// ─── Selectors ────────────────────────────────────────────────────────────

export const selectCartItems    = (s: CartState) => s.items;
export const selectCartTotalQty = (s: CartState) =>
  s.items.reduce((sum, i) => sum + i.quantity, 0);
export const selectCartSubtotal = (s: CartState) =>
  s.items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
