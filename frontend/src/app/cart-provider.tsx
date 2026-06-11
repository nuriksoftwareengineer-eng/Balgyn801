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
  makeDesignLineKey,
  type CartAddOptions,
  type CartLine,
  type CartLineLegacy,
  type CartLineDesign,
  type CartProductInput,
  type CartDesignInput,
} from "@/app/cart-context";

const STORAGE_KEY = "balgyn_cart_v3";
const V2_STORAGE_KEY = "balgyn_cart_v2";
const V1_STORAGE_KEY = "balgyn_cart_v1";

// ── Normalizers ───────────────────────────────────────────────────────────────

function normalizeLegacyLine(l: Record<string, unknown>): CartLineLegacy | null {
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
    kind: "legacy",
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

function normalizeDesignLine(l: Record<string, unknown>): CartLineDesign | null {
  if (
    typeof l.designGarmentId !== "number" ||
    typeof l.designId !== "number" ||
    typeof l.designSlug !== "string" ||
    typeof l.groupSlug !== "string" ||
    typeof l.collectionSlug !== "string" ||
    typeof l.title !== "string" ||
    typeof l.garmentType !== "string" ||
    typeof l.garmentLabel !== "string" ||
    typeof l.price !== "number" ||
    typeof l.qty !== "number" ||
    l.qty <= 0 ||
    typeof l.colorId !== "number" ||
    typeof l.colorName !== "string" ||
    typeof l.colorHex !== "string" ||
    typeof l.sizeId !== "number" ||
    typeof l.sizeLabel !== "string"
  ) {
    return null;
  }
  const lineKey =
    typeof l.lineKey === "string"
      ? l.lineKey
      : makeDesignLineKey(l.designGarmentId, l.colorId, l.sizeId);
  return {
    kind: "design",
    lineKey,
    designGarmentId: l.designGarmentId,
    designId: l.designId,
    designSlug: l.designSlug,
    groupSlug: l.groupSlug,
    collectionSlug: l.collectionSlug,
    title: l.title,
    garmentType: l.garmentType,
    garmentLabel: l.garmentLabel,
    price: l.price,
    imageUrl: typeof l.imageUrl === "string" ? l.imageUrl : null,
    qty: l.qty,
    colorId: l.colorId,
    colorName: l.colorName,
    colorHex: l.colorHex,
    sizeId: l.sizeId,
    sizeLabel: l.sizeLabel,
  };
}

function normalizeCartLine(x: unknown): CartLine | null {
  if (!x || typeof x !== "object") return null;
  const l = x as Record<string, unknown>;
  if (l.kind === "design") return normalizeDesignLine(l);
  // "legacy" or no kind (v2 migration)
  return normalizeLegacyLine(l);
}

// ── Storage ───────────────────────────────────────────────────────────────────

function loadLines(): CartLine[] {
  try {
    let raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      // v2 → v3 migration: read old lines, tag them as legacy
      raw = sessionStorage.getItem(V2_STORAGE_KEY);
      if (raw) {
        sessionStorage.removeItem(V2_STORAGE_KEY);
      }
    }
    if (!raw) {
      // v1 → v3 (two-step migration path)
      raw = sessionStorage.getItem(V1_STORAGE_KEY);
      if (raw) sessionStorage.removeItem(V1_STORAGE_KEY);
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

// ── Merge helpers ─────────────────────────────────────────────────────────────

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
        kind: "legacy",
        lineKey,
        productId: product.id,
        title: product.title,
        price: Number.isFinite(price) ? price : 0,
        imageUrl: product.imageUrl,
        inStock: product.inStock,
        qty: 1,
        size,
        color,
      } satisfies CartLineLegacy,
    ];
  }
  const next = [...lines];
  next[idx] = { ...next[idx], qty: next[idx].qty + 1 };
  return next;
}

function mergeAddDesign(lines: CartLine[], input: CartDesignInput): CartLine[] {
  const lineKey = makeDesignLineKey(input.designGarmentId, input.colorId, input.sizeId);
  const idx = lines.findIndex((l) => l.lineKey === lineKey);
  if (idx === -1) {
    return [
      ...lines,
      {
        kind: "design",
        lineKey,
        designGarmentId: input.designGarmentId,
        designId: input.designId,
        designSlug: input.designSlug,
        groupSlug: input.groupSlug,
        collectionSlug: input.collectionSlug,
        title: input.title,
        garmentType: input.garmentType,
        garmentLabel: input.garmentLabel,
        price: input.price,
        imageUrl: input.imageUrl ?? null,
        qty: 1,
        colorId: input.colorId,
        colorName: input.colorName,
        colorHex: input.colorHex,
        sizeId: input.sizeId,
        sizeLabel: input.sizeLabel,
      } satisfies CartLineDesign,
    ];
  }
  const next = [...lines];
  next[idx] = { ...next[idx], qty: next[idx].qty + 1 };
  return next;
}

// ── Provider ──────────────────────────────────────────────────────────────────

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

  const addDesignItem = useCallback((input: CartDesignInput) => {
    setLines((prev) => mergeAddDesign(prev, input));
  }, []);

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
      addDesignItem,
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
      addDesignItem,
      increment,
      decrement,
      removeLine,
      clear,
    ],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
