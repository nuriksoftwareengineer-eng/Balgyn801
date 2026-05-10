/** Лёгкий индикатор при подгрузке ленивого маршрута (без тяжёлых эффектов). */
export function PageLoadFallback() {
  return (
    <div
      className="flex min-h-[32vh] flex-col items-center justify-center gap-3 px-4 py-14 text-zinc-500"
      role="status"
      aria-live="polite"
    >
      <div
        className="h-8 w-8 animate-spin rounded-full border-2 border-white/10 border-t-violet-500"
        aria-hidden
      />
      <span className="text-sm">Загрузка…</span>
    </div>
  );
}
