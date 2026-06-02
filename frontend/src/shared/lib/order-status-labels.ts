import type { OrderStatus } from "@/shared/api/types";

/** Все значения для селекта статуса в админке (совпадает с enum на бэкенде). */
export const ORDER_STATUS_VALUES: readonly OrderStatus[] = [
  "NEW",
  "CONFIRMED",
  "IN_PRODUCTION",
  "READY",
  "SHIPPED",
  "DELIVERED",
  "CANCELLED",
] as const;

const LABELS: Record<OrderStatus, string> = {
  NEW: "Новый",
  CONFIRMED: "Подтверждён",
  IN_PRODUCTION: "В производстве",
  READY: "Готов к отправке",
  SHIPPED: "Отправлен",
  DELIVERED: "Доставлен",
  CANCELLED: "Отменён",
};

export function orderStatusLabel(
  status: OrderStatus | null | undefined,
): string {
  if (!status) return "—";
  return LABELS[status] ?? status;
}
