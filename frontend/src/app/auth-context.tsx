import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
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
  const [loading, setLoading] = useState(() => !!readStoredToken());

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
      queueMicrotask(() => {
        setUser(null);
        setLoading(false);
      });
      return;
    }

    let cancelled = false;
    queueMicrotask(() => setLoading(true));
    (async () => {
      try {
        const me = await getMe(token);
        if (!cancelled) setUser(me);
      } catch (e) {
        if (!cancelled && e instanceof ApiError && e.status === 401) {
          clearStoredToken();
          setToken(null);
          setUser(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
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
