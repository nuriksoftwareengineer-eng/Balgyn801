/** Ключ в `localStorage`; нужен для `storage`-синхронизации между вкладками. */
export const AUTH_TOKEN_STORAGE_KEY = "balgyn_access_token";

/**
 * JWT в localStorage — общий для всех вкладок одного origin.
 * Refresh-токен теперь хранится в HttpOnly cookie (устанавливается сервером).
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

export function writeStoredToken(access: string): void {
  try {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, access);
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  } catch {
    /* ignore */
  }
}

export function clearStoredToken(): void {
  try {
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    // Refresh-токен хранится в HttpOnly cookie — очищается сервером через POST /auth/logout
  } catch {
    /* ignore */
  }
}
