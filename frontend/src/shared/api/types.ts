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
  tokenType: string;
  expiresInMs: number;
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

/** Тело POST `/order` — то же, что `CreateOrderRequest` на бэкенде. */
export type CreateOrderRequest = {
  customerName: string;
  customerPhone: string;
  telegramUsername?: string | null;
  deliveryType: DeliveryType;
  comment?: string | null;
  items: OrderItemRequest[];
  address?: DeliveryAddressRequest | null;
};

export type OrderResponse = {
  id: number;
  customerName: string;
  customerPhone: string;
  /** Может отсутствовать в ответе, если маппер пока не заполняет статус. */
  status?: OrderStatus | null;
  deliveryType: DeliveryType;
  totalPrice: number;
  comment?: string | null;
  items?: OrderItemResponse[];
  address?: DeliveryAddressResponse | null;
  createdAt: string;
};
