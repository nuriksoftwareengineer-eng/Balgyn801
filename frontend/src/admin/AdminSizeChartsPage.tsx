import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import {
  deleteSizeChart,
  getSizeCharts,
  uploadMedia,
  upsertSizeChart,
} from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

const GARMENT_OPTIONS = [
  { value: "T_SHIRT", label: "Футболка" },
  { value: "OVERSIZE_TSHIRT", label: "Оверсайз футболка" },
  { value: "LONGSLEEVE", label: "Лонгслив" },
  { value: "SWEATSHIRT", label: "Свитшот" },
  { value: "HOODIE", label: "Худи" },
  { value: "ZIP_HOODIE", label: "Худи на молнии" },
] as const;

export function AdminSizeChartsPage() {
  const { token } = useAuth();
  const qc = useQueryClient();

  const { data: charts = [], isLoading } = useQuery({
    queryKey: ["size-charts"],
    queryFn: getSizeCharts,
  });

  const [garmentType, setGarmentType] = useState<string>("T_SHIRT");
  const [imageUrl, setImageUrl] = useState("");
  const [title, setTitle] = useState("");
  const [uploading, setUploading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const upsertMut = useMutation({
    mutationFn: () => upsertSizeChart({ garmentType, imageUrl, title: title || undefined }, token!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["size-charts"] });
      setImageUrl("");
      setTitle("");
      setFormError(null);
    },
    onError: (e) => setFormError(e instanceof ApiError ? e.message : "Ошибка сохранения"),
  });

  const deleteMut = useMutation({
    mutationFn: (gt: string) => deleteSizeChart(gt, token!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["size-charts"] }),
  });

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !token) return;
    setUploading(true);
    setFormError(null);
    try {
      const res = await uploadMedia(file, token);
      setImageUrl(res.publicUrl);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Ошибка загрузки");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-zinc-100">Размерные сетки</h1>

      {/* Existing charts */}
      <div className="mb-8">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Загруженные сетки
        </h2>
        {isLoading ? (
          <p className="text-sm text-zinc-500">Загружаем…</p>
        ) : charts.length === 0 ? (
          <p className="text-sm text-zinc-500">Нет загруженных сеток.</p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {charts.map((c) => (
              <div key={c.id} className="rounded-lg border border-white/10 bg-zinc-900 p-3">
                <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-zinc-300">
                  {GARMENT_OPTIONS.find((g) => g.value === c.garmentType)?.label ?? c.garmentType}
                </p>
                {c.title && <p className="mb-2 text-xs text-zinc-400">{c.title}</p>}
                <img
                  src={c.imageUrl}
                  alt={c.title ?? c.garmentType}
                  className="mb-3 max-h-40 w-full rounded object-contain bg-zinc-800"
                />
                <button
                  type="button"
                  onClick={() => deleteMut.mutate(c.garmentType)}
                  disabled={deleteMut.isPending}
                  className="text-xs font-semibold text-red-400 hover:text-red-300 disabled:opacity-50"
                >
                  Удалить
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Upload form */}
      <div className="rounded-lg border border-white/10 bg-zinc-900/60 p-6">
        <h2 className="mb-5 text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Добавить / заменить сетку
        </h2>

        <div className="flex flex-col gap-4 max-w-sm">
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Тип изделия</span>
            <select
              value={garmentType}
              onChange={(e) => setGarmentType(e.target.value)}
              className="rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 outline-none focus:border-white/40"
            >
              {GARMENT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Название (необязательно)</span>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Размерная сетка Худи"
              className="rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40"
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Изображение</span>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleUpload}
              disabled={uploading}
              className="text-xs text-zinc-300 file:mr-3 file:rounded file:border-0 file:bg-zinc-700 file:px-3 file:py-1.5 file:text-xs file:text-zinc-200 hover:file:bg-zinc-600"
            />
            {uploading && <p className="text-xs text-zinc-400">Загружаем…</p>}
            {imageUrl && (
              <img
                src={imageUrl}
                alt="preview"
                className="mt-2 max-h-48 w-full rounded object-contain bg-zinc-800"
              />
            )}
          </label>

          {formError && <p className="text-xs text-red-400">{formError}</p>}

          <Button
            type="button"
            disabled={!imageUrl || upsertMut.isPending}
            onClick={() => upsertMut.mutate()}
            className="mt-2"
          >
            {upsertMut.isPending ? "Сохраняем…" : "Сохранить"}
          </Button>
        </div>
      </div>
    </div>
  );
}
