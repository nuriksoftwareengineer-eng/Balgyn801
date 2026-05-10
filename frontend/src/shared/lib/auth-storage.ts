/** Ключ в `localStorage`; нужен для `storage`-синхронизации между вкладками. */
export const AUTH_TOKEN_STORAGE_KEY = "balgyn_access_token";

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
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  } catch {
    /* ignore */
  }
}
