import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { CartContext, type CartLine, type CartProductInput } from "@/app/cart-context";

const STORAGE_KEY = "balgyn_cart_v1";

function loadLines(): CartLine[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(isValidLine);
  } catch {
    return [];
  }
}

function isValidLine(x: unknown): x is CartLine {
  if (!x || typeof x !== "object") return false;
  const l = x as Record<string, unknown>;
  return (
    typeof l.productId === "number" &&
    typeof l.title === "string" &&
    typeof l.price === "number" &&
    typeof l.qty === "number" &&
    l.qty > 0 &&
    typeof l.inStock === "boolean"
  );
}

function persist(lines: CartLine[]) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
  } catch {
    /* ignore quota */
  }
}

function mergeAdd(lines: CartLine[], product: CartProductInput): CartLine[] {
  if (!product.inStock) return lines;
  const price = Number(product.price);
  const idx = lines.findIndex((l) => l.productId === product.id);
  if (idx === -1) {
    return [
      ...lines,
      {
        productId: product.id,
        title: product.title,
        price: Number.isFinite(price) ? price : 0,
        imageUrl: product.imageUrl,
        inStock: product.inStock,
        qty: 1,
      },
    ];
  }
  const next = [...lines];
  next[idx] = { ...next[idx], qty: next[idx].qty + 1 };
  return next;
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(() =>
    typeof sessionStorage === "undefined" ? [] : loadLines(),
  );

  useEffect(() => {
    persist(lines);
  }, [lines]);

  const totalQty = useMemo(
    () => lines.reduce((acc, l) => acc + l.qty, 0),
    [lines],
  );

  const subtotal = useMemo(
    () => lines.reduce((acc, l) => acc + l.price * l.qty, 0),
    [lines],
  );

  const addItem = useCallback((product: CartProductInput) => {
    setLines((prev) => mergeAdd(prev, product));
  }, []);

  const increment = useCallback((productId: number) => {
    setLines((prev) =>
      prev.map((l) =>
        l.productId === productId ? { ...l, qty: l.qty + 1 } : l,
      ),
    );
  }, []);

  const decrement = useCallback((productId: number) => {
    setLines((prev) =>
      prev
        .map((l) =>
          l.productId === productId ? { ...l, qty: l.qty - 1 } : l,
        )
        .filter((l) => l.qty > 0),
    );
  }, []);

  const removeLine = useCallback((productId: number) => {
    setLines((prev) => prev.filter((l) => l.productId !== productId));
  }, []);

  const clear = useCallback(() => setLines([]), []);

  const value = useMemo(
    () => ({
      lines,
      totalQty,
      subtotal,
      addItem,
      increment,
      decrement,
      removeLine,
      clear,
    }),
    [
      lines,
      totalQty,
      subtotal,
      addItem,
      increment,
      decrement,
      removeLine,
      clear,
    ],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
