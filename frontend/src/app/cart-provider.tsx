import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  CartContext,
  makeCartLineKey,
  type CartAddOptions,
  type CartLine,
  type CartProductInput,
} from "@/app/cart-context";

const STORAGE_KEY = "balgyn_cart_v2";
const LEGACY_STORAGE_KEY = "balgyn_cart_v1";

function normalizeCartLine(x: unknown): CartLine | null {
  if (!x || typeof x !== "object") return null;
  const l = x as Record<string, unknown>;
  if (
    typeof l.productId !== "number" ||
    typeof l.title !== "string" ||
    typeof l.price !== "number" ||
    typeof l.qty !== "number" ||
    l.qty <= 0 ||
    typeof l.inStock !== "boolean"
  ) {
    return null;
  }
  const size = typeof l.size === "string" ? l.size : null;
  const color = typeof l.color === "string" ? l.color : null;
  const lineKey =
    typeof l.lineKey === "string"
      ? l.lineKey
      : makeCartLineKey(l.productId, size, color);
  return {
    lineKey,
    productId: l.productId,
    title: l.title,
    price: l.price,
    imageUrl: typeof l.imageUrl === "string" ? l.imageUrl : null,
    inStock: l.inStock,
    qty: l.qty,
    size,
    color,
  };
}

function loadLines(): CartLine[] {
  try {
    let raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      raw = sessionStorage.getItem(LEGACY_STORAGE_KEY);
      if (raw) {
        sessionStorage.removeItem(LEGACY_STORAGE_KEY);
      }
    }
    if (!raw) return [];
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((row) => normalizeCartLine(row))
      .filter((row): row is CartLine => row != null);
  } catch {
    return [];
  }
}

function persist(lines: CartLine[]) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
  } catch {
    /* ignore quota */
  }
}

function mergeAdd(
  lines: CartLine[],
  product: CartProductInput,
  options?: CartAddOptions,
): CartLine[] {
  if (!product.inStock) return lines;
  const price = Number(product.price);
  const size = options?.size ?? null;
  const color = options?.color ?? null;
  const lineKey = makeCartLineKey(product.id, size, color);
  const idx = lines.findIndex((l) => l.lineKey === lineKey);
  if (idx === -1) {
    return [
      ...lines,
      {
        lineKey,
        productId: product.id,
        title: product.title,
        price: Number.isFinite(price) ? price : 0,
        imageUrl: product.imageUrl,
        inStock: product.inStock,
        qty: 1,
        size,
        color,
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

  const addItem = useCallback(
    (product: CartProductInput, options?: CartAddOptions) => {
      setLines((prev) => mergeAdd(prev, product, options));
    },
    [],
  );

  const increment = useCallback((lineKey: string) => {
    setLines((prev) =>
      prev.map((l) =>
        l.lineKey === lineKey ? { ...l, qty: l.qty + 1 } : l,
      ),
    );
  }, []);

  const decrement = useCallback((lineKey: string) => {
    setLines((prev) =>
      prev
        .map((l) =>
          l.lineKey === lineKey ? { ...l, qty: l.qty - 1 } : l,
        )
        .filter((l) => l.qty > 0),
    );
  }, []);

  const removeLine = useCallback((lineKey: string) => {
    setLines((prev) => prev.filter((l) => l.lineKey !== lineKey));
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
