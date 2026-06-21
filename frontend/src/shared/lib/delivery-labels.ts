import type { DeliveryType } from "@/shared/api/types";

const LABELS: Record<DeliveryType, string> = {
  PICKUP: "Самовывоз",
  TAXI: "Такси / курьер",
  CDEK: "СДЭК",
  POSTAL: "Казпочта",
  INTERNATIONAL: "Международная доставка",
};

export function deliveryTypeLabel(type: DeliveryType): string {
  return LABELS[type] ?? type;
}
