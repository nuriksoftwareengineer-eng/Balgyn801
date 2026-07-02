/** Типы ответов/запросов под REST `/api/v1` (Spring). Числа — как приходит из JSON. */

export type ProductColorOption = {
  name: string;
  hex?: string | null;
};

export type Product = {
  id: number;
  title: string;
  description?: string | null;
  price: number;
  imageUrl?: string | null;
  inStock: boolean;
  category?: string | null;
  sizes?: string[] | null;
  colors?: ProductColorOption[] | null;
};

/** Ответ POST `/media/upload` (роль ADMIN, multipart). */
export type MediaUploadResponse = {
  publicUrl: string;
};

/** POST `/product` (роль ADMIN). */
export type CreateProductRequest = {
  title: string;
  description?: string | null;
  price: number;
  imageUrl?: string | null;
  inStock: boolean;
  category: string;
  sizes?: string[] | null;
  colors?: ProductColorOption[] | null;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  refreshExpiresInMs: number;
};

export type AuthMeResponse = {
  email: string;
  roles: string[];
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
};

export type RefreshTokenRequest = {
  refreshToken: string;
};

export type PaymentProvider = "FREEDOM_PAY" | "PAYPAL" | "VTB_KZ";

export type PaymentStatus =
  | "PENDING"
  | "SUCCEEDED"
  | "CANCELLED"
  | "FAILED"
  | "REFUNDED";

export type DeliveryType = "PICKUP" | "TAXI" | "CDEK" | "POSTAL" | "INTERNATIONAL";

/** One available delivery option returned by GET /api/v1/delivery/methods. */
export type DeliveryMethodResponse = {
  type: DeliveryType;
  /** Russian display label — render this, never hardcode method names. */
  labelRu: string;
  /** False only for PICKUP. */
  requiresAddress: boolean;
  /** True for CDEK — show city autocomplete widget. */
  requiresCitySearch: boolean;
  /** True for CDEK — show PVZ picker. */
  requiresPvz: boolean;
  /**
   * Non-null when the method is restricted to a specific city.
   * Display as "Доступно только в: {cityRestriction}".
   * Never hardcode city names in the frontend.
   */
  cityRestriction: string | null;
  /** 0 for PICKUP (free), flat amount for TAXI, null for variable-price methods. */
  estimatedFeeKzt: number | null;
};

export type OrderStatus =
  | "NEW"
  | "CONFIRMED"
  | "IN_PRODUCTION"
  | "READY"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "PENDING_PAYMENT"
  | "EXPIRED";

export type OrderItemResponse = {
  id: number;
  // Product-based (null for design-based orders)
  productId?: number | null;
  productTitle?: string | null;
  // Design-based (null for product-based orders)
  designGarmentId?: number | null;
  garmentType?: string | null;
  designName?: string | null;
  designSlug?: string | null;
  groupSlug?: string | null;
  collectionSlug?: string | null;
  colorId?: number | null;
  colorHex?: string | null;
  sizeId?: number | null;
  // Repeat-order convenience: design's main image (design-based) or product image (legacy)
  mainImageUrl?: string | null;
  // Shared
  quantity: number;
  unitPrice: number;
  sizeLabel?: string | null;
  colorName?: string | null;
  currency?: string | null;
};

export type DeliveryAddressRequest = {
  city: string;
  street: string;
  apartment: string;
  postalCode: string;
  recipientName: string;
  recipientPhone: string;
};

export type DeliveryAddressResponse = {
  city: string;
  street: string;
  apartment: string;
  postalCode: string;
  recipientName: string;
  recipientPhone: string;
};

export type OrderItemRequest = {
  // Legacy product path
  productId?: number | null;
  customDesignId?: number | null;
  size?: string | null;
  color?: string | null;
  // Design catalog path (mutually exclusive with productId)
  designGarmentId?: number | null;
  colorId?: number | null;
  sizeId?: number | null;
  currency?: string | null;
  // Shared
  quantity: number;
};

/** Тело `PATCH /order/{id}/status` (роль ADMIN). */
export type UpdateOrderStatusRequest = {
  status: OrderStatus;
};

/** Город из справочника СДЭК (ответ `GET /delivery/cdek/cities`). */
export type CdekCity = {
  code: number;
  city: string;
  region?: string | null;
  country?: string | null;
  countryCode?: string | null;
};

/** ПВЗ/постамат СДЭК (ответ `GET /delivery/cdek/points`). */
export type CdekDeliveryPoint = {
  code: string;
  name: string;
  address: string;
  longitude?: number | null;
  latitude?: number | null;
  workTime?: string | null;
  type?: string | null;
};

/** Тело `POST /delivery/cdek/calculate`. */
export type CdekTariffRequest = {
  toCityCode: number;
  weightGrams: number;
  tariffCode?: number | null;
};

/** Позиция корзины для расчёта СДЭК на бэкенде. */
export type CdekOrderItemRequest = {
  productId?: number | null;
  designGarmentId?: number | null;
  quantity: number;
};

/** Тело `POST /delivery/cdek/calculate-order`. */
export type CdekOrderTariffRequest = {
  toCityCode: number;
  items: CdekOrderItemRequest[];
  tariffCode?: number | null;
};

/** Ответ `POST /delivery/cdek/calculate`. */
export type CdekTariffResponse = {
  totalPrice: number;
  currency: string;
  minDays?: number | null;
  maxDays?: number | null;
  tariffCode?: number | null;
  /** true, если ответ собран заглушкой (нет CDEK_CLIENT_ID/CDEK_CLIENT_SECRET на бэке). */
  sourcedFromStub: boolean;
};

/** Ответ `POST /delivery/cdek/calculate-order`. */
export type CdekOrderTariffResponse = {
  deliveryPrice: number;
  itemsTotal: number;
  orderTotal: number;
  estimatedWeightGrams: number;
  currency: string;
  minDays?: number | null;
  maxDays?: number | null;
  tariffCode?: number | null;
  sourcedFromStub: boolean;
};

/** Тело POST `/order` — то же, что `CreateOrderRequest` на бэкенде. */
export type CreateOrderRequest = {
  customerName: string;
  customerPhone: string;
  telegramUsername?: string | null;
  deliveryType: DeliveryType;
  comment?: string | null;
  items: OrderItemRequest[];
  address?: DeliveryAddressRequest | null;
  /** ISO-2 код страны доставки (например "KZ", "RU"). Обязателен для всех типов кроме PICKUP. */
  countryIso2?: string | null;
  /** Код ПВЗ СДЭК. Обязателен для CDEK-заказов. */
  pvzCode?: string | null;
  /** Промокод — применяется сервером при создании заказа. */
  couponCode?: string | null;
};

/** Ответ `GET /customer`, `POST /customer`, `PUT /customer` (ADMIN). */
export type CustomerResponse = {
  id: number;
  name: string;
  phone: string;
  telegramUsername?: string | null;
  /** ISO-дата/время с бэкенда (`LocalDate`/`LocalDateTime`). */
  createAt?: string | null;
};

/** Тело `POST /customer` / `PUT /customer` (ADMIN). */
export type CustomerRequest = {
  id?: number | null;
  name: string;
  phone: string;
  telegramUsername?: string | null;
  createAt?: string | null;
};

export type OrderResponse = {
  id: number;
  customerName: string;
  customerPhone: string;
  /** Может отсутствовать в ответе, если маппер пока не заполняет статус. */
  status?: OrderStatus | null;
  deliveryType: DeliveryType;
  totalPrice: number;
  deliveryFee?: number | null;
  comment?: string | null;
  items?: OrderItemResponse[];
  address?: DeliveryAddressResponse | null;
  couponCode?: string | null;
  discountAmount?: number | null;
  createdAt: string;
};

export type WishlistItemResponse = {
  id: number;
  designId: number;
  designName: string;
  designSlug: string;
  mainImageUrl?: string | null;
  collectionName: string;
  groupSlug: string;
  addedAt: string;
};

export type DiscountType = "PERCENTAGE" | "FIXED";

export type CouponResponse = {
  id: number;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxUses?: number | null;
  usedCount: number;
  active: boolean;
  expiresAt?: string | null;
  createdAt: string;
};

export type CouponValidateResponse = {
  code: string;
  discountType: DiscountType;
  discountValue: number;
  discountAmount: number;
  finalTotal: number;
};

export type CouponRequest = {
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number;
  maxUses?: number | null;
  active: boolean;
  expiresAt?: string | null;
};

export type DesignResponse = {
  id: number;
  collectionId: number;
  collectionName: string;
  collectionSlug: string;
  groupName: string;
  groupSlug: string;
  name: string;
  slug: string;
  description?: string | null;
  mainImageUrl?: string | null;
  gallery?: string[];
  status: string;
  sortOrder?: number | null;
  publishedAt?: string | null;
  isNewArrival: boolean;
  viewCount: number;
};

export type CdekShipmentStatus =
  | "CREATED"
  | "ACCEPTED"
  | "IN_TRANSIT"
  | "ARRIVED"
  | "DELIVERED"
  | "RETURNED"
  | "CANCELLED";

/** Ответ admin-эндпоинтов отправления СДЭК (`/api/v1/cdek-shipment/by-order/...`). */
export type CdekShipmentResponse = {
  orderId?: number | null;
  cdekOrderUuid?: string | null;
  trackingNumber?: string | null;
  status?: CdekShipmentStatus | null;
  estimatedDeliveryDate?: string | null;
  tariffCode?: number | null;
  cdekDeliveryMode?: string | null;
  deliveryPointCode?: string | null;
  deliveryPointAddress?: string | null;
  deliveryPrice?: number | null;
  invoiceUrl?: string | null;
  barcodeUrl?: string | null;
  /** true, если отправление создано mock-провайдером. */
  mock: boolean;
};

export type PaymentInitRequest = {
  orderId: number;
  provider: PaymentProvider;
  returnUrl?: string | null;
  cancelUrl?: string | null;
};

export type ExchangeRateResponse = {
  kztPerUsd: number;
  source: string;
  frozen: boolean;
  updatedAt: string;
};

export type SetExchangeRateRequest = {
  kztPerUsd: number;
  frozen?: boolean | null;
};

export type PaymentWebhookRequest = {
  eventId?: string | null;
  paymentId?: number | null;
  orderId?: number | null;
  providerPaymentId?: string | null;
  status?: string | null;
  amount?: number | null;
  currency?: string | null;
  payload?: Record<string, unknown> | null;
};

/** Ответ `GET /admin/users` — один зарегистрированный пользователь (только ADMIN). */
export type AdminUserResponse = {
  id: number;
  email: string;
  roles: string[];
  createdAt: string;
};

export type PaymentResponse = {
  id: number;
  orderId: number;
  provider: PaymentProvider;
  status: PaymentStatus;
  amount: number;
  currency: string;
  providerPaymentId?: string | null;
  paymentUrl?: string | null;
  webhookEventId?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
  // Only present on PayPal create-order; used to authorise /cancel requests.
  cancelToken?: string | null;
};
