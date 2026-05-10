import type { OrderStatus } from "@/shared/api/types";

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
