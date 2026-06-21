import { apiFetch, getApiBaseUrl, ApiError } from "@/shared/api/http";
import type {
  AdminUserResponse,
  AuthMeResponse,
  AuthResponse,
  CdekCity,
  CdekDeliveryPoint,
  CdekOrderTariffRequest,
  CdekOrderTariffResponse,
  CdekShipmentResponse,
  CdekTariffRequest,
  CdekTariffResponse,
  CreateOrderRequest,
  CreateProductRequest,
  CustomerRequest,
  CustomerResponse,
  DeliveryMethodResponse,
  ExchangeRateResponse,
  LoginRequest,
  MediaUploadResponse,
  OrderResponse,
  PaymentInitRequest,
  PaymentResponse,
  PaymentWebhookRequest,
  PaymentProvider,
  PaymentStatus,
  PayPalCreateOrderRequest,
  Product,
  RefreshTokenRequest,
  RegisterRequest,
  SetExchangeRateRequest,
  UpdateOrderStatusRequest,
} from "@/shared/api/types";
import type {
  CatalogGroupSummary,
  CatalogGroupDetail,
  CollectionDetail,
  DesignDetail,
  DesignSummary,
} from "@/shared/types/catalog";

/**
 * Живая карта маршрутов бэкенда (Spring Security).
 *
 * Публично (без JWT): GET product/**, POST order, POST custom-design, POST auth/register|login|refresh,
 * Swagger `/swagger-ui/**`, `/v3/api-docs/**`.
 *
 * Только ADMIN: GET/PATCH order**, GET/POST/PUT/DELETE customer**, POST product, DELETE product/**, POST media/upload,
 * GET custom-design**, cdek-shipment**, delivery-address**, order-item**.
 *
 * С JWT (любая авторизованная роль): всё остальное, в т.ч. GET /auth/me.
 */
export const BACKEND_API = {
  baseUrl: "/api/v1",
  catalog: {
    groups:     "GET /catalog/groups (public)",
    group:      "GET /catalog/groups/:slug (public)",
    collection: "GET /catalog/collections/:slug (public)",
    designs:    "GET /catalog/designs?collectionId= (public)",
    design:     "GET /catalog/designs/:slug (public)",
  },
  auth: {
    register: "POST /auth/register",
    login: "POST /auth/login",
    refresh: "POST /auth/refresh",
    refreshCookie: "POST /auth/refresh-cookie (uses HttpOnly cookie)",
    logout: "POST /auth/logout (clears HttpOnly cookie)",
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
    patchStatus: "PATCH /order/{id}/status (ADMIN)",
  },
  customer: {
    list: "GET /customer (ADMIN)",
    getById: "GET /customer/{id} (ADMIN)",
    create: "POST /customer (ADMIN)",
    update: "PUT /customer (ADMIN)",
    delete: "DELETE /customer/{id} (ADMIN)",
  },
  customDesign: {
    create: "POST /custom-design",
    listGet: "GET /custom-design* (ADMIN)",
  },
  deliveryAddress: "/delivery-address/** (ADMIN)",
  orderItem: "/order-item/** (ADMIN)",
  cdekShipment: "/cdek-shipment/** (ADMIN)",
  delivery: {
    methods: "GET /delivery/methods?countryIso2= (public)",
    cities: "GET /delivery/cdek/cities?q=&limit=",
    points: "GET /delivery/cdek/points?cityCode=",
    calculate: "POST /delivery/cdek/calculate",
    calculateOrder: "POST /delivery/cdek/calculate-order",
  },
  media: {
    upload: "POST /media/upload (ADMIN, multipart file)",
  },
  payments: {
    init: "POST /payments/init",
    webhook: "POST /payments/webhook/{provider}",
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
    credentials: "include",
  });
}

export async function login(body: LoginRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
    credentials: "include",
  });
}

/** Обновить access-токен через HttpOnly refresh-cookie (без тела запроса). */
export async function refreshCookieAuth(): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/auth/refresh-cookie", {
    method: "POST",
    credentials: "include",
  });
}

/** Выйти: сервер очищает HttpOnly refresh-cookie. */
export async function logoutAuth(): Promise<void> {
  return apiFetch<void>("/auth/logout", {
    method: "POST",
    credentials: "include",
  });
}

/** @deprecated Используйте refreshCookieAuth() — refresh-токен теперь хранится в HttpOnly cookie. */
export async function refreshAuth(
  body: RefreshTokenRequest,
): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/auth/refresh", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getMe(token: string): Promise<AuthMeResponse> {
  return apiFetch<AuthMeResponse>("/auth/me", { token });
}

/** Выдать роль ADMIN существующему пользователю по email (только ADMIN). */
export async function grantAdminRole(
  email: string,
  token: string,
): Promise<AuthMeResponse> {
  return apiFetch<AuthMeResponse>("/auth/admin/grant", {
    method: "POST",
    body: JSON.stringify({ email }),
    token,
  });
}

/** Снять роль ADMIN (нельзя у себя и у последнего админа). */
export async function revokeAdminRole(
  email: string,
  token: string,
): Promise<AuthMeResponse> {
  return apiFetch<AuthMeResponse>("/auth/admin/revoke", {
    method: "POST",
    body: JSON.stringify({ email }),
    token,
  });
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

/**
 * Оформление заказа. Checkout доступен только авторизованным пользователям (гейт во фронте),
 * поэтому передаём JWT — бэкенд привяжет заказ к аккаунту, и он попадёт в историю заказов.
 * `token` опционален для обратной совместимости; при его отсутствии заказ создаётся анонимно.
 */
export async function createOrder(
  body: CreateOrderRequest,
  token?: string | null,
): Promise<OrderResponse> {
  return apiFetch<OrderResponse>("/order", {
    method: "POST",
    body: JSON.stringify(body),
    token,
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

export async function patchOrderStatusAdmin(
  id: number,
  body: UpdateOrderStatusRequest,
  token: string,
): Promise<OrderResponse> {
  return apiFetch<OrderResponse>(`/order/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify(body),
    token,
  });
}

export async function getCustomers(token: string): Promise<CustomerResponse[]> {
  return apiFetch<CustomerResponse[]>("/customer", { token });
}

export async function createCustomer(
  body: CustomerRequest,
  token: string,
): Promise<CustomerResponse> {
  return apiFetch<CustomerResponse>("/customer", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

export async function updateCustomer(
  body: CustomerRequest,
  token: string,
): Promise<CustomerResponse> {
  return apiFetch<CustomerResponse>("/customer", {
    method: "PUT",
    body: JSON.stringify(body),
    token,
  });
}

export async function deleteCustomer(
  id: number,
  token: string,
): Promise<void> {
  return apiFetch<void>(`/customer/${id}`, { method: "DELETE", token });
}

/** Поиск городов СДЭК (для автодополнения на checkout). */
export async function searchCdekCities(
  query: string,
  limit = 10,
): Promise<CdekCity[]> {
  const q = encodeURIComponent(query);
  return apiFetch<CdekCity[]>(`/delivery/cdek/cities?q=${q}&limit=${limit}`);
}

/** Список ПВЗ СДЭК по коду города. */
export async function listCdekDeliveryPoints(
  cityCode: number,
): Promise<CdekDeliveryPoint[]> {
  return apiFetch<CdekDeliveryPoint[]>(
    `/delivery/cdek/points?cityCode=${cityCode}`,
  );
}

/** Доступные методы доставки для страны (публичный GET). */
export async function getDeliveryMethods(
  countryIso2: string,
): Promise<DeliveryMethodResponse[]> {
  return apiFetch<DeliveryMethodResponse[]>(
    `/delivery/methods?countryIso2=${encodeURIComponent(countryIso2)}`,
  );
}

/** Расчёт стоимости и срока доставки. */
export async function calculateCdekTariff(
  body: CdekTariffRequest,
): Promise<CdekTariffResponse> {
  return apiFetch<CdekTariffResponse>("/delivery/cdek/calculate", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** Расчёт СДЭК по корзине: бэкенд сам считает сумму товаров и вес. */
export async function calculateCdekTariffByOrder(
  body: CdekOrderTariffRequest,
): Promise<CdekOrderTariffResponse> {
  return apiFetch<CdekOrderTariffResponse>("/delivery/cdek/calculate-order", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** История заказов текущего пользователя (с JWT). */
export async function getMyOrders(token: string): Promise<OrderResponse[]> {
  return apiFetch<OrderResponse[]>("/me/orders", { token });
}

/** Инициализация оплаты по заказу (публично). */
export async function initPayment(
  body: PaymentInitRequest,
): Promise<PaymentResponse> {
  return apiFetch<PaymentResponse>("/payments/init", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** Создаёт PayPal-ордер и возвращает ссылку на одобрение покупателем. */
export async function createPayPalOrder(body: PayPalCreateOrderRequest): Promise<PaymentResponse> {
  return apiFetch<PaymentResponse>("/payments/paypal/create-order", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** Подтверждает (captures) ранее одобренный PayPal-ордер по его ID (token из URL возврата). */
export async function capturePayPalOrder(paypalOrderId: string): Promise<PaymentResponse> {
  return apiFetch<PaymentResponse>(
    `/payments/paypal/capture/${encodeURIComponent(paypalOrderId)}`,
    { method: "POST" },
  );
}

/** Вебхук-пайплайн (для локального/интеграционного теста без провайдера). */
export async function submitPaymentWebhook(
  provider: PaymentProvider,
  body: PaymentWebhookRequest,
): Promise<PaymentResponse> {
  return apiFetch<PaymentResponse>(`/payments/webhook/${provider}`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

// ── CDEK shipment (ADMIN) ───────────────────────────────────────────────────────

/** Отправление СДЭК по заказу (undefined, если ещё не создано). */
export async function getOrderShipment(
  orderId: number,
  token: string,
): Promise<CdekShipmentResponse | undefined> {
  return apiFetch<CdekShipmentResponse | undefined>(
    `/cdek-shipment/by-order/${orderId}`,
    { token },
  );
}

/** Создать / повторить создание отправления СДЭК (ADMIN). */
export async function createOrderShipment(
  orderId: number,
  token: string,
): Promise<CdekShipmentResponse> {
  return apiFetch<CdekShipmentResponse>(
    `/cdek-shipment/by-order/${orderId}/create`,
    { method: "POST", token },
  );
}

/** Синхронизировать статус отправления с СДЭК (ADMIN). */
export async function syncOrderShipment(
  orderId: number,
  token: string,
): Promise<CdekShipmentResponse> {
  return apiFetch<CdekShipmentResponse>(
    `/cdek-shipment/by-order/${orderId}/sync`,
    { method: "POST", token },
  );
}

/** Отменить отправление СДЭК (ADMIN). */
export async function cancelOrderShipment(
  orderId: number,
  token: string,
): Promise<CdekShipmentResponse> {
  return apiFetch<CdekShipmentResponse>(
    `/cdek-shipment/by-order/${orderId}/cancel`,
    { method: "POST", token },
  );
}

// ── Catalog storefront API (public, no auth required) ────────────────────────

/** All active catalog groups (top-level navigation). */
export async function getCatalogGroups(): Promise<CatalogGroupSummary[]> {
  return apiFetch<CatalogGroupSummary[]>("/catalog/groups");
}

/** One group by slug, with its collections list. */
export async function getCatalogGroup(slug: string): Promise<CatalogGroupDetail> {
  return apiFetch<CatalogGroupDetail>(`/catalog/groups/${encodeURIComponent(slug)}`);
}

/** One collection by slug, with its designs list. */
export async function getCatalogCollection(slug: string): Promise<CollectionDetail> {
  return apiFetch<CollectionDetail>(`/catalog/collections/${encodeURIComponent(slug)}`);
}

/** All active designs, optionally filtered by collectionId. */
export async function getCatalogDesigns(
  collectionId?: number | null,
): Promise<DesignSummary[]> {
  const q = collectionId != null ? `?collectionId=${collectionId}` : "";
  return apiFetch<DesignSummary[]>(`/catalog/designs${q}`);
}

/** One design by slug, with all garment variants, colors, sizes, and prices. */
export async function getCatalogDesign(slug: string): Promise<DesignDetail> {
  return apiFetch<DesignDetail>(`/catalog/designs/${encodeURIComponent(slug)}`);
}

// ── Size chart images ────────────────────────────────────────────────────────

export interface SizeChartImage {
  id: number;
  garmentType: string;
  imageUrl: string;
  title: string | null;
}

/** All active size chart images (public). */
export async function getSizeCharts(): Promise<SizeChartImage[]> {
  return apiFetch<SizeChartImage[]>("/catalog/size-charts");
}

/** Admin: create or replace size chart for a garment type. */
export async function upsertSizeChart(
  body: { garmentType: string; imageUrl: string; title?: string },
  token: string,
): Promise<SizeChartImage> {
  return apiFetch<SizeChartImage>("/admin/size-charts", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
}

/** Admin: soft-delete size chart for a garment type. */
export async function deleteSizeChart(garmentType: string, token: string): Promise<void> {
  return apiFetch<void>(`/admin/size-charts/${encodeURIComponent(garmentType)}`, {
    method: "DELETE",
    token,
  });
}

// ── Admin: payments ──────────────────────────────────────────────────────────

export async function getAdminPayments(
  token: string,
  filter?: { provider?: PaymentProvider; status?: PaymentStatus },
): Promise<PaymentResponse[]> {
  const params = new URLSearchParams();
  if (filter?.provider) params.set("provider", filter.provider);
  if (filter?.status) params.set("status", filter.status);
  const q = params.size > 0 ? `?${params.toString()}` : "";
  return apiFetch<PaymentResponse[]>(`/admin/payments${q}`, { token });
}

// ── Admin: exchange rate ─────────────────────────────────────────────────────

export async function getExchangeRate(token: string): Promise<ExchangeRateResponse> {
  return apiFetch<ExchangeRateResponse>("/admin/exchange-rate", { token });
}

export async function setExchangeRate(
  body: SetExchangeRateRequest,
  token: string,
): Promise<ExchangeRateResponse> {
  return apiFetch<ExchangeRateResponse>("/admin/exchange-rate", {
    method: "PUT",
    body: JSON.stringify(body),
    token,
  });
}

export async function refreshExchangeRate(token: string): Promise<ExchangeRateResponse> {
  return apiFetch<ExchangeRateResponse>("/admin/exchange-rate/refresh", {
    method: "POST",
    token,
  });
}

// ── Admin: users ─────────────────────────────────────────────────────────────

/** Список всех зарегистрированных пользователей (только ADMIN). */
export async function getAdminUsers(token: string): Promise<AdminUserResponse[]> {
  return apiFetch<AdminUserResponse[]>("/admin/users", { token });
}
