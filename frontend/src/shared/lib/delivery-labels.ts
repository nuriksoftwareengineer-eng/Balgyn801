import type { DeliveryType } from "@/shared/api/types";

const LABELS: Record<DeliveryType, string> = {
  PICKUP: "Самовывоз",
  TAXI: "Такси / курьер",
  CDEK: "СДЭК",
};

export function deliveryTypeLabel(type: DeliveryType): string {
  return LABELS[type] ?? type;
}
