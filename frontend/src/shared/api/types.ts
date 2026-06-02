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

export type PaymentProvider = "KASPI" | "YOOKASSA" | "PAYPAL";
export type PaymentStatus =
  | "PENDING"
  | "SUCCEEDED"
  | "CANCELLED"
  | "FAILED"
  | "REFUNDED";

export type DeliveryType = "PICKUP" | "TAXI" | "CDEK";

export type OrderStatus =
  | "NEW"
  | "CONFIRMED"
  | "IN_PRODUCTION"
  | "READY"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

export type OrderItemResponse = {
  id: number;
  productTitle: string;
  quantity: number;
  unitPrice: number;
  sizeLabel?: string | null;
  colorName?: string | null;
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
  productId: number;
  customDesignId?: number | null;
  quantity: number;
  size?: string | null;
  color?: string | null;
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
  productId: number;
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
  /** Для CDEK — сумма после «Рассчитать доставку»; для остальных не передаётся. */
  deliveryFee?: number | null;
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
  createdAt: string;
};

export type PaymentInitRequest = {
  orderId: number;
  provider: PaymentProvider;
  returnUrl?: string | null;
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
};
