import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useAuth } from "@/app/auth-context";
import {
  getWishlist,
  addToWishlist,
  removeFromWishlist,
  checkWishlist,
} from "@/shared/api/backend-api";
import type { WishlistItemResponse } from "@/shared/api/types";

const LS_KEY = "balgyn_wishlist";

type WishlistCtx = {
  items: WishlistItemResponse[];
  count: number;
  isInWishlist: (designId: number) => boolean;
  toggle: (designId: number) => Promise<void>;
  refresh: () => Promise<void>;
};

const Ctx = createContext<WishlistCtx>({
  items: [],
  count: 0,
  isInWishlist: () => false,
  toggle: async () => {},
  refresh: async () => {},
});

export function WishlistProvider({ children }: { children: React.ReactNode }) {
  const { token } = useAuth();
  const [items, setItems] = useState<WishlistItemResponse[]>([]);
  const [guestIds, setGuestIds] = useState<number[]>(() => {
    try { return JSON.parse(localStorage.getItem(LS_KEY) ?? "[]"); } catch { return []; }
  });
  const prevToken = useRef<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    try {
      const data = await getWishlist(token);
      setItems(data);
    } catch {}
  }, [token]);

  // When user logs in, sync guest wishlist to server and reload
  useEffect(() => {
    if (token && prevToken.current !== token) {
      prevToken.current = token;
      const idsToSync = [...guestIds];
      setGuestIds([]);
      localStorage.setItem(LS_KEY, "[]");
      const syncAndLoad = async () => {
        for (const id of idsToSync) {
          try { await addToWishlist(token, id); } catch {}
        }
        await refresh();
      };
      syncAndLoad();
    } else if (!token) {
      prevToken.current = null;
      setItems([]);
    }
  }, [token, guestIds, refresh]);

  useEffect(() => { if (token) refresh(); }, [token, refresh]);

  const isInWishlist = useCallback((designId: number) => {
    if (token) return items.some(i => i.designId === designId);
    return guestIds.includes(designId);
  }, [token, items, guestIds]);

  const toggle = useCallback(async (designId: number) => {
    if (!token) {
      setGuestIds(prev => {
        const next = prev.includes(designId) ? prev.filter(i => i !== designId) : [...prev, designId];
        localStorage.setItem(LS_KEY, JSON.stringify(next));
        return next;
      });
      return;
    }
    try {
      const { inWishlist } = await checkWishlist(token, designId);
      if (inWishlist) {
        await removeFromWishlist(token, designId);
        setItems(prev => prev.filter(i => i.designId !== designId));
      } else {
        const item = await addToWishlist(token, designId);
        setItems(prev => [item, ...prev]);
      }
    } catch {}
  }, [token]);

  const count = token ? items.length : guestIds.length;

  const value = useMemo(() => ({ items, count, isInWishlist, toggle, refresh }), [items, count, isInWishlist, toggle, refresh]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useWishlist() { return useContext(Ctx); }
