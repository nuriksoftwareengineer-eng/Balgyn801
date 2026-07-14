import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  AUTH_TOKEN_STORAGE_KEY,
  clearStoredToken,
  readStoredToken,
  writeStoredToken,
} from "@/shared/lib/auth-storage";
import {
  getMe,
  login as loginApi,
  logoutAuth,
  refreshCookieAuth,
  register as registerApi,
} from "@/shared/api/backend-api";
import { ApiError, setAccessTokenRefresher } from "@/shared/api/http";
import type { AuthMeResponse } from "@/shared/api/types";

type AuthContextValue = {
  token: string | null;
  user: AuthMeResponse | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [token, setToken] = useState<string | null>(() => readStoredToken());
  const [user, setUser] = useState<AuthMeResponse | null>(null);
  // Always start loading — resolved only after we know the auth state (prevents flash of logout).
  const [loading, setLoading] = useState(true);
  // True once the startup session check is complete; prevents the token effect from
  // resolving loading prematurely while the refresh-cookie probe is still in-flight.
  const startupDone = useRef(!!readStoredToken());

  /** Вход/выход в другой вкладке — подтягиваем тот же JWT без перезагрузки. */
  useEffect(() => {
    function onStorage(e: StorageEvent) {
      if (e.storageArea !== localStorage) return;
      if (e.key !== null && e.key !== AUTH_TOKEN_STORAGE_KEY) return;
      setToken(readStoredToken());
    }
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  /**
   * Probe for an active session via HttpOnly cookie on cold start (e.g. F5 with expired
   * access token in storage). This blocks the UI in a loading state until we know whether
   * the user is authenticated — preventing the "brief logout flash" on protected routes.
   */
  useEffect(() => {
    if (readStoredToken()) return; // Stored token exists; the token effect handles it.
    refreshCookieAuth()
      .then((res) => {
        writeStoredToken(res.accessToken);
        startupDone.current = true;
        setToken(res.accessToken); // triggers token effect → /me → loading=false
      })
      .catch(() => {
        startupDone.current = true;
        setLoading(false); // No session at all — render logged-out state
      });
  // eslint-disable-next-line react-hooks/exhaustive-deps -- intentionally run once on mount
  }, []);

  useEffect(() => {
    setAccessTokenRefresher(async () => {
      try {
        const res = await refreshCookieAuth();
        writeStoredToken(res.accessToken);
        setToken(res.accessToken);
        return res.accessToken;
      } catch {
        clearStoredToken();
        setToken(null);
        setUser(null);
        return null;
      }
    });
    return () => setAccessTokenRefresher(null);
  }, []);

  useEffect(() => {
    if (!token) {
      // Only resolve loading once the startup session check is done.
      // If startupDone is still false, the refresh-cookie probe is in-flight.
      if (startupDone.current) {
        queueMicrotask(() => {
          setUser(null);
          setLoading(false);
        });
      }
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const me = await getMe(token);
        if (!cancelled) {
          setUser(me);
          setLoading(false);
        }
      } catch (e) {
        if (!cancelled && e instanceof ApiError && e.status === 401) {
          clearStoredToken();
          setToken(null);
          setUser(null);
          // loading resolved on next token-effect run (token=null, startupDone=true)
        } else if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps -- startupDone is a ref, not state
  }, [token]);

  const login = useCallback(async (email: string, password: string) => {
    const res = await loginApi({ email, password });
    writeStoredToken(res.accessToken);
    setToken(res.accessToken);
  }, []);

  const register = useCallback(async (email: string, password: string) => {
    // Регистрация НЕ выполняет автоматический вход. Бэкенд возвращает токены,
    // но мы их намеренно игнорируем: пользователь должен явно войти на /login.
    // Это убирает «тихую» авторизацию сразу после создания аккаунта.
    await registerApi({ email, password });
  }, []);

  const logout = useCallback(() => {
    clearStoredToken();
    setToken(null);
    setUser(null);
    // Сервер очищает HttpOnly refresh-cookie; ошибки игнорируем (уже вышли локально).
    logoutAuth().catch(() => undefined);
    // Очищаем кэш React Query (профиль, история заказов), чтобы следующий
    // пользователь не увидел чужих данных. Корзину НЕ трогаем: она хранится
    // по идентификатору пользователя и восстановится при повторном входе под
    // тем же аккаунтом (переключение по identity — в CartProvider).
    queryClient.clear();
  }, [queryClient]);

  const isAdmin = !!user?.roles?.includes("ADMIN");

  // Warms the admin dashboard's lazy chunk as soon as we know the signed-in user is an
  // admin, instead of only starting that fetch when they first navigate to /admin. The
  // dashboard bundle is large, so it stays code-split for everyone else — this just gets
  // it into the cache ahead of time for the one role that actually needs it.
  useEffect(() => {
    if (isAdmin) void import("@/admin/AdminDashboardPage");
  }, [isAdmin]);

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      login,
      register,
      logout,
      isAdmin,
    }),
    [token, user, loading, login, register, logout, isAdmin],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** @see AuthProvider */
// eslint-disable-next-line react-refresh/only-export-components -- хук рядом с провайдером
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
