import { useCurrency } from "@/app/currency-context";

/**
 * Отображает цену, хранящуюся в KZT, в валюте, выбранной пользователем
 * (конвертация + символ из CurrencyContext). Реагирует на смену валюты
 * без перезагрузки — просто ре-рендер контекста.
 */
export function Price({ kzt, className }: { kzt: number; className?: string }) {
  const { format } = useCurrency();
  return <span className={className}>{format(kzt)}</span>;
}
