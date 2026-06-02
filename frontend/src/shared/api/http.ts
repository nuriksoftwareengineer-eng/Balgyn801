const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export function getApiBaseUrl(): string {
  return API_BASE_URL.replace(/\/$/, "");
}

type FetchInit = RequestInit & { token?: string | null };

/** Выставляется из `AuthProvider`: при 401 пробуем обновить access по refresh и повторить запрос один раз. */
let accessTokenRefresher: (() => Promise<string | null>) | null = null;

export function setAccessTokenRefresher(
  fn: (() => Promise<string | null>) | null,
): void {
  accessTokenRefresher = fn;
}

export async function apiFetch<T>(path: string, init?: FetchInit): Promise<T> {
  return apiFetchInternal<T>(path, init, false);
}

async function apiFetchInternal<T>(
  path: string,
  init: FetchInit | undefined,
  retried: boolean,
): Promise<T> {
  const url = `${getApiBaseUrl()}${path.startsWith("/") ? path : `/${path}`}`;
  const headers = new Headers(init?.headers);
  const bearer = init?.token;
  if (bearer) {
    headers.set("Authorization", `Bearer ${bearer}`);
  }
  const hasBody = init?.body != null && init.body !== "";
  if (hasBody && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const fetchInit: RequestInit = init ? { ...init } : {};
  delete (fetchInit as FetchInit).token;

  const response = await fetch(url, { ...fetchInit, headers });

  const isRefreshPath = path.includes("/auth/refresh");
  if (
    response.status === 401 &&
    !retried &&
    !isRefreshPath &&
    init?.token &&
    accessTokenRefresher
  ) {
    const next = await accessTokenRefresher();
    if (next) {
      return apiFetchInternal<T>(path, { ...init, token: next }, true);
    }
  }

  if (!response.ok) {
    let body: unknown;
    const ct = response.headers.get("content-type") ?? "";
    try {
      if (ct.includes("application/json")) {
        body = await response.json();
      } else {
        body = await response.text();
      }
    } catch {
      body = undefined;
    }
    const detail =
      typeof body === "object" &&
      body !== null &&
      "detail" in body &&
      typeof (body as { detail: unknown }).detail === "string"
        ? (body as { detail: string }).detail
        : `Запрос завершился с кодом ${response.status}`;
    throw new ApiError(detail, response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text.trim()) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}
