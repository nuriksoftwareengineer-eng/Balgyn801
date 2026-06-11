import { create } from "zustand";
import {
  clearStoredToken,
  readStoredRefresh,
  readStoredToken,
  writeStoredTokens,
} from "@/shared/lib/auth-storage";
import {
  getMe,
  login as loginApi,
  refreshAuth,
  register as registerApi,
} from "@/shared/api/backend-api";
import { ApiError, setAccessTokenRefresher } from "@/shared/api/http";
import type { AuthMeResponse } from "@/shared/api/types";

// ─── Types ────────────────────────────────────────────────────────────────

export interface AuthState {
  token: string | null;
  user: AuthMeResponse | null;
  loading: boolean;
  isAdmin: boolean;

  // Actions
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
  /** Reads persisted token from localStorage and fetches /auth/me. Call once on app boot. */
  hydrate: () => Promise<void>;
  /** Wires the JWT refresh handler into the HTTP client. Returns a cleanup function. */
  initRefresher: () => () => void;
}

// ─── Store ────────────────────────────────────────────────────────────────

export const useAuthStore = create<AuthState>()((set, get) => ({
  token: null,
  user: null,
  loading: false,
  isAdmin: false,

  login: async (email, password) => {
    const res = await loginApi({ email, password });
    writeStoredTokens(res.accessToken, res.refreshToken);
    set({ token: res.accessToken });
    try {
      const me = await getMe(res.accessToken);
      set({ user: me, isAdmin: !!me?.roles?.includes("ADMIN") });
    } catch {
      // token is set; user will hydrate on next boot
    }
  },

  register: async (email, password) => {
    const res = await registerApi({ email, password });
    writeStoredTokens(res.accessToken, res.refreshToken);
    set({ token: res.accessToken });
    try {
      const me = await getMe(res.accessToken);
      set({ user: me, isAdmin: !!me?.roles?.includes("ADMIN") });
    } catch {
      // acceptable
    }
  },

  logout: () => {
    clearStoredToken();
    set({ token: null, user: null, isAdmin: false });
  },

  hydrate: async () => {
    const token = readStoredToken();
    if (!token) {
      set({ loading: false });
      return;
    }
    set({ token, loading: true });
    try {
      const me = await getMe(token);
      set({ user: me, isAdmin: !!me?.roles?.includes("ADMIN"), loading: false });
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        clearStoredToken();
        set({ token: null, user: null, isAdmin: false });
      }
      set({ loading: false });
    }
  },

  initRefresher: () => {
    setAccessTokenRefresher(async () => {
      const refresh = readStoredRefresh();
      if (!refresh) return null;
      try {
        const res = await refreshAuth({ refreshToken: refresh });
        writeStoredTokens(res.accessToken, res.refreshToken);
        set({ token: res.accessToken });
        return res.accessToken;
      } catch {
        clearStoredToken();
        get().logout();
        return null;
      }
    });
    return () => setAccessTokenRefresher(null);
  },
}));

// ─── Selectors ────────────────────────────────────────────────────────────

export const selectToken    = (s: AuthState) => s.token;
export const selectUser     = (s: AuthState) => s.user;
export const selectIsAdmin  = (s: AuthState) => s.isAdmin;
export const selectLoading  = (s: AuthState) => s.loading;
