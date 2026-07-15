import { useRef, useState } from "react";
import { uploadMedia } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";

/**
 * Файловое поле загрузки картинки для админки: выбор файла с компьютера →
 * uploadMedia (тот же MinIO-пайплайн, что у товаров) → превью + кнопка «Убрать».
 * onChange получает публичный URL загруженного файла (или "" при удалении).
 */
export function ImageField({
  label,
  value,
  onChange,
  token,
  onError,
}: {
  label: string;
  value: string;
  onChange: (url: string) => void;
  token: string | null;
  onError: (msg: string) => void;
}) {
  const ref = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);

  async function handle(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !token) return;
    setBusy(true);
    try {
      const res = await uploadMedia(file, token);
      onChange(res.publicUrl);
    } catch (err) {
      onError(err instanceof ApiError ? err.message : "Ошибка загрузки");
    } finally {
      setBusy(false);
      if (ref.current) ref.current.value = "";
    }
  }

  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs text-zinc-400">{label}</span>
      <input
        ref={ref}
        type="file"
        accept="image/*"
        onChange={handle}
        disabled={busy}
        className="text-xs text-zinc-300 file:mr-3 file:rounded file:border-0 file:bg-zinc-700 file:px-3 file:py-1.5 file:text-xs file:text-zinc-200 hover:file:bg-zinc-600"
      />
      {busy && <p className="text-xs text-zinc-400">Загружаем…</p>}
      {value && (
        <div className="mt-1 flex items-center gap-2">
          <img src={value} alt={label} className="max-h-24 rounded object-contain bg-zinc-800" />
          <button
            type="button"
            onClick={() => onChange("")}
            className="text-xs text-red-400 hover:text-red-300"
          >
            Убрать
          </button>
        </div>
      )}
    </label>
  );
}
