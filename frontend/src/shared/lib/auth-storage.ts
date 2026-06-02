/** Ключ в `localStorage`; нужен для `storage`-синхронизации между вкладками. */
export const AUTH_TOKEN_STORAGE_KEY = "balgyn_access_token";
export const AUTH_REFRESH_STORAGE_KEY = "balgyn_refresh_token";

/**
 * JWT в localStorage — общий для всех вкладок одного origin.
 * Раньше использовался sessionStorage: новая вкладка («Открыть товар») не видела токен и выглядела как «автовыход».
 */
export function readStoredToken(): string | null {
  try {
    const fromLocal = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
    if (fromLocal) return fromLocal;
    const fromSession = sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
    if (fromSession) {
      localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, fromSession);
      sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
      return fromSession;
    }
    return null;
  } catch {
    return null;
  }
}

export function readStoredRefresh(): string | null {
  try {
    return localStorage.getItem(AUTH_REFRESH_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function writeStoredTokens(access: string, refresh: string): void {
  try {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, access);
    localStorage.setItem(AUTH_REFRESH_STORAGE_KEY, refresh);
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_REFRESH_STORAGE_KEY);
  } catch {
    /* ignore */
  }
}

/** @deprecated используйте {@link writeStoredTokens} */
export function writeStoredToken(token: string): void {
  try {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  } catch {
    /* ignore */
  }
}

export function clearStoredToken(): void {
  try {
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    localStorage.removeItem(AUTH_REFRESH_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_REFRESH_STORAGE_KEY);
  } catch {
    /* ignore */
  }
}
