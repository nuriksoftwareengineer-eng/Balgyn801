import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useAuth } from "@/app/auth-context";
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

// Корзина хранится по идентификатору пользователя: у каждого аккаунта своя
// корзина, плюс отдельная гостевая. Logout не удаляет пользовательскую корзину —
// она восстанавливается при повторном входе под тем же аккаунтом. Чужая корзина
// при этом не отображается, т.к. ключ хранилища привязан к email.
const KEY_PREFIX = "balgyn_cart_v3";
const GUEST_KEY = `${KEY_PREFIX}:guest`;
// Старые ключи из прежней (единой, в sessionStorage) модели — мигрируем в гостевую.
const LEGACY_SESSION_KEYS = [
  "balgyn_cart_v3",
  "balgyn_cart_v2",
  "balgyn_cart_v1",
];

function userKey(email: string): string {
  return `${KEY_PREFIX}:u:${email.trim().toLowerCase()}`;
}

function identityKey(identity: string | null): string {
  return identity ? userKey(identity) : GUEST_KEY;
}

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
  // "legacy" или без kind (миграция со старой схемы)
  return normalizeLegacyLine(l);
}

// ── Storage ───────────────────────────────────────────────────────────────────

function readCart(key: string): CartLine[] {
  try {
    const raw = localStorage.getItem(key);
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

function writeCart(key: string, lines: CartLine[]) {
  try {
    localStorage.setItem(key, JSON.stringify(lines));
  } catch {
    /* ignore quota */
  }
}

function removeCart(key: string) {
  try {
    localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}

let legacyMigrated = false;

/** Однократно переносит корзину из старой единой схемы (sessionStorage) в гостевую. */
function migrateLegacyGuestCart() {
  if (legacyMigrated) return;
  legacyMigrated = true;
  try {
    if (localStorage.getItem(GUEST_KEY)) return; // уже есть гостевая корзина
    let legacyRaw: string | null = null;
    for (const k of LEGACY_SESSION_KEYS) {
      const v = sessionStorage.getItem(k);
      if (v && legacyRaw == null) legacyRaw = v;
      sessionStorage.removeItem(k);
    }
    if (!legacyRaw) return;
    const parsed = JSON.parse(legacyRaw) as unknown;
    if (!Array.isArray(parsed)) return;
    const lines = parsed
      .map((row) => normalizeCartLine(row))
      .filter((row): row is CartLine => row != null);
    if (lines.length > 0) writeCart(GUEST_KEY, lines);
  } catch {
    /* ignore */
  }
}

// ── Merge helpers ─────────────────────────────────────────────────────────────

/** Объединяет две корзины по lineKey, суммируя количество одинаковых позиций. */
function mergeCarts(base: CartLine[], incoming: CartLine[]): CartLine[] {
  const map = new Map<string, CartLine>();
  for (const l of base) map.set(l.lineKey, { ...l });
  for (const l of incoming) {
    const existing = map.get(l.lineKey);
    if (existing) {
      map.set(l.lineKey, { ...existing, qty: existing.qty + l.qty });
    } else {
      map.set(l.lineKey, { ...l });
    }
  }
  return Array.from(map.values());
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
  const { user } = useAuth();
  const identity = user?.email ?? null; // null = гость

  const [lines, setLines] = useState<CartLine[]>(() => {
    if (typeof localStorage === "undefined") return [];
    migrateLegacyGuestCart();
    // На монтировании пользователь ещё не загружен (getMe асинхронный) — стартуем
    // с гостевой корзины; когда identity появится, сработает мёрдж/переключение.
    return readCart(GUEST_KEY);
  });

  // Ключ хранилища активной корзины. Ref (а не state), чтобы persist-эффект
  // всегда писал в актуальный ключ без гонки с эффектом смены идентичности.
  const activeKeyRef = useRef<string>(GUEST_KEY);

  // Сохраняем активную корзину при любом изменении строк.
  useEffect(() => {
    writeCart(activeKeyRef.current, lines);
  }, [lines]);

  // Вход/выход: переключаем активную корзину по идентификатору пользователя.
  useEffect(() => {
    const nextKey = identityKey(identity);
    const prevKey = activeKeyRef.current;
    if (nextKey === prevKey) return;

    if (prevKey === GUEST_KEY && nextKey !== GUEST_KEY) {
      // Вход под аккаунтом: переносим гостевую корзину в пользовательскую
      // (мёрдж по позициям) и очищаем гостевую.
      const guestLines = readCart(GUEST_KEY);
      const userLines = readCart(nextKey);
      const merged = mergeCarts(userLines, guestLines);
      activeKeyRef.current = nextKey;
      writeCart(nextKey, merged);
      removeCart(GUEST_KEY);
      setLines(merged);
    } else {
      // Выход (-> гость) или смена аккаунта: показываем корзину новой
      // идентичности. Прежняя пользовательская корзина остаётся в хранилище
      // и восстановится при повторном входе.
      activeKeyRef.current = nextKey;
      setLines(readCart(nextKey));
    }
  }, [identity]);

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
