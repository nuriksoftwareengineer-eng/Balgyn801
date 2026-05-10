export function formatMoney(value: number | string | undefined): string {
  const n = typeof value === "string" ? Number.parseFloat(value) : Number(value);
  const safe = Number.isFinite(n) ? n : 0;
  return new Intl.NumberFormat("ru-RU", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(safe);
}
