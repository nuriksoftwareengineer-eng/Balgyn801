import { apiFetch, getApiBaseUrl, ApiError } from "@/shared/api/http";
import type {
  AuthMeResponse,
  AuthResponse,
  CreateOrderRequest,
  CreateProductRequest,
  LoginRequest,
  MediaUploadResponse,
  OrderResponse,
  Product,
  RegisterRequest,
} from "@/shared/api/types";

/**
 * Живая карта маршрутов бэкенда (Spring Security).
 *
 * Публично (без JWT): GET product/**, POST order, POST custom-design, POST auth/register|login,
 * Swagger `/swagger-ui/**`, `/v3/api-docs/**`.
 *
 * Только ADMIN: GET order**, customer**, POST product, DELETE product/**, POST media/upload,
 * GET custom-design**, cdek-shipment**, delivery-address**, order-item**.
 *
 * С JWT (любая авторизованная роль): всё остальное, в т.ч. GET /auth/me.
 */
export const BACKEND_API = {
  baseUrl: "/api/v1",
  auth: {
    register: "POST /auth/register",
    login: "POST /auth/login",
    me: "GET /auth/me (+ Bearer)",
  },
  product: {
    list: "GET /product",
    getById: "GET /product/{id}",
    create: "POST /product (ADMIN)",
    delete: "DELETE /product/{id} (ADMIN)",
  },
  order: {
    list: "GET /order (ADMIN)",
    create: "POST /order",
    getById: "GET /order/{id} (ADMIN)",
  },
  customer: {
    crud: "/customer (ADMIN)",
  },
  customDesign: {
    create: "POST /custom-design",
    listGet: "GET /custom-design* (ADMIN)",
  },
  deliveryAddress: "/delivery-address/** (ADMIN)",
  orderItem: "/order-item/** (ADMIN)",
  cdekShipment: "/cdek-shipment/** (ADMIN)",
  media: {
    upload: "POST /media/upload (ADMIN, multipart file)",
  },
} as const;

export async function getProducts(category?: string | null): Promise<Product[]> {
  const q =
    category != null && category.length > 0
      ? `?category=${encodeURIComponent(category)}`
      : "";
  return apiFetch<Product[]>(`/product${q}`);
}

export async function getProduct(id: number): Promise<Product> {
  return apiFetch<Product>(`/product/${id}`);
}

export async function register(body: RegisterRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function login(body: LoginRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getMe(token: string): Promise<AuthMeResponse> {
  return apiFetch<AuthMeResponse>("/auth/me", { token });
}

export async function createProduct(
  body: CreateProductRequest,
  token: string,
): Promise<Product> {
  return apiFetch<Product>("/product", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

export async function deleteProduct(id: number, token: string): Promise<void> {
  return apiFetch<void>(`/product/${id}`, { method: "DELETE", token });
}

/** Загрузка картинки в S3/MinIO. Не задавайте Content-Type вручную — boundary подставит браузер. */
export async function uploadMedia(
  file: File,
  token: string,
): Promise<MediaUploadResponse> {
  const url = `${getApiBaseUrl()}/media/upload`;
  const body = new FormData();
  body.append("file", file);
  const response = await fetch(url, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body,
  });
  if (!response.ok) {
    let parsed: unknown;
    const ct = response.headers.get("content-type") ?? "";
    try {
      if (ct.includes("application/json")) {
        parsed = await response.json();
      } else {
        parsed = await response.text();
      }
    } catch {
      parsed = undefined;
    }
    const detail =
      typeof parsed === "object" &&
      parsed !== null &&
      "detail" in parsed &&
      typeof (parsed as { detail: unknown }).detail === "string"
        ? (parsed as { detail: string }).detail
        : `Запрос завершился с кодом ${response.status}`;
    throw new ApiError(detail, response.status, parsed);
  }
  const text = await response.text();
  return JSON.parse(text) as MediaUploadResponse;
}

/** Оформление заказа (публичный POST). Удобно вызвать из корзины, когда заполнены контакты и адрес. */
export async function createOrder(
  body: CreateOrderRequest,
): Promise<OrderResponse> {
  return apiFetch<OrderResponse>("/order", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getOrders(token: string): Promise<OrderResponse[]> {
  return apiFetch<OrderResponse[]>("/order", { token });
}

export async function getOrderByIdAdmin(
  id: number,
  token: string,
): Promise<OrderResponse> {
  return apiFetch<OrderResponse>(`/order/${id}`, { token });
}
