import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import {
  createGarmentProfile,
  deleteGarmentProfile,
  listGarmentProfiles,
  updateGarmentProfile,
  type GarmentProfile,
  type GarmentProfileRequest,
} from "@/shared/api/admin-catalog";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

const inputClass =
  "rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40";

const emptyForm = (): GarmentProfileRequest => ({
  name: "",
  nameRu: "",
  nameKk: "",
  nameEn: "",
  weightKg: 0.5,
  lengthCm: 35,
  widthCm: 28,
  heightCm: 5,
  sortOrder: 0,
});

export function AdminGarmentProfilesPage() {
  const { token } = useAuth();
  const qc = useQueryClient();

  const { data: profiles = [], isLoading } = useQuery({
    queryKey: ["admin", "garment-profiles"],
    queryFn: () => listGarmentProfiles(token!),
    enabled: !!token,
  });

  const [editing, setEditing] = useState<GarmentProfile | null>(null);
  const [form, setForm] = useState<GarmentProfileRequest>(emptyForm());
  const [error, setError] = useState<string | null>(null);

  function startCreate() {
    setEditing(null);
    setForm(emptyForm());
    setError(null);
  }

  function startEdit(p: GarmentProfile) {
    setEditing(p);
    setForm({
      name: p.name,
      nameRu: p.nameRu ?? "",
      nameKk: p.nameKk ?? "",
      nameEn: p.nameEn ?? "",
      weightKg: p.weightKg,
      lengthCm: p.lengthCm,
      widthCm: p.widthCm,
      heightCm: p.heightCm,
      sortOrder: p.sortOrder,
    });
    setError(null);
  }

  function cancelForm() {
    setEditing(null);
    setForm(emptyForm());
    setError(null);
  }

  function set<K extends keyof GarmentProfileRequest>(key: K, value: GarmentProfileRequest[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  const saveMut = useMutation({
    mutationFn: () =>
      editing
        ? updateGarmentProfile(editing.id, form, token!)
        : createGarmentProfile(form, token!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "garment-profiles"] });
      cancelForm();
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "Ошибка сохранения"),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => deleteGarmentProfile(id, token!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "garment-profiles"] }),
    onError: (e) => setError(e instanceof ApiError ? e.message : "Не удалось удалить"),
  });

  const isFormOpen = editing !== null || form.name !== "";

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-zinc-100">Типы одежды</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Определяют вес и габариты посылки для СДЭК. Добавление нового типа не требует изменений в коде.
          </p>
        </div>
        {!isFormOpen && (
          <Button type="button" onClick={startCreate}>
            + Добавить тип
          </Button>
        )}
      </div>

      {/* Form */}
      {isFormOpen && (
        <div className="mb-8 rounded-lg border border-white/10 bg-zinc-900/60 p-6">
          <h2 className="mb-4 text-sm font-semibold text-zinc-300">
            {editing ? `Редактировать: ${editing.name}` : "Новый тип одежды"}
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Название (EN, ключ) *</span>
              <input
                value={form.name}
                onChange={(e) => set("name", e.target.value)}
                placeholder="Hoodie"
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Название RU</span>
              <input
                value={form.nameRu ?? ""}
                onChange={(e) => set("nameRu", e.target.value)}
                placeholder="Худи"
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Название KK</span>
              <input
                value={form.nameKk ?? ""}
                onChange={(e) => set("nameKk", e.target.value)}
                placeholder="Худи"
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Вес, кг *</span>
              <input
                type="number"
                min="0.001"
                step="0.001"
                value={form.weightKg}
                onChange={(e) => set("weightKg", parseFloat(e.target.value) || 0)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Длина, см *</span>
              <input
                type="number"
                min="1"
                value={form.lengthCm}
                onChange={(e) => set("lengthCm", parseInt(e.target.value) || 1)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Ширина, см *</span>
              <input
                type="number"
                min="1"
                value={form.widthCm}
                onChange={(e) => set("widthCm", parseInt(e.target.value) || 1)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Высота (толщина), см *</span>
              <input
                type="number"
                min="1"
                value={form.heightCm}
                onChange={(e) => set("heightCm", parseInt(e.target.value) || 1)}
                className={inputClass}
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-400">Порядок сортировки</span>
              <input
                type="number"
                min="0"
                value={form.sortOrder ?? 0}
                onChange={(e) => set("sortOrder", parseInt(e.target.value) || 0)}
                className={inputClass}
              />
            </label>
          </div>
          {error && <p className="mt-3 text-xs text-red-400">{error}</p>}
          <div className="mt-4 flex gap-3">
            <Button type="button" disabled={!form.name.trim() || saveMut.isPending} onClick={() => saveMut.mutate()}>
              {saveMut.isPending ? "Сохраняем…" : "Сохранить"}
            </Button>
            <button
              type="button"
              onClick={cancelForm}
              className="text-sm text-zinc-500 hover:text-zinc-300"
            >
              Отмена
            </button>
          </div>
        </div>
      )}

      {/* Table */}
      {isLoading ? (
        <p className="text-sm text-zinc-500">Загружаем…</p>
      ) : profiles.length === 0 ? (
        <p className="text-sm text-zinc-500">Типов одежды пока нет — добавьте первый выше.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-white/10">
          <table className="w-full text-sm">
            <thead className="border-b border-white/10 bg-zinc-900/40">
              <tr>
                <th className="px-4 py-3 text-left font-semibold text-zinc-400">Название</th>
                <th className="px-4 py-3 text-right font-semibold text-zinc-400">Вес, кг</th>
                <th className="px-4 py-3 text-right font-semibold text-zinc-400">Д×Ш×В, см</th>
                <th className="px-4 py-3 text-right font-semibold text-zinc-400">Порядок</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {profiles.map((p) => (
                <tr key={p.id} className="hover:bg-white/[0.02]">
                  <td className="px-4 py-3 font-medium text-zinc-100">{p.name}</td>
                  <td className="px-4 py-3 text-right tabular-nums text-zinc-300">{p.weightKg.toFixed(3)}</td>
                  <td className="px-4 py-3 text-right tabular-nums text-zinc-300">
                    {p.lengthCm}×{p.widthCm}×{p.heightCm}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums text-zinc-500">{p.sortOrder}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-3">
                      <button
                        type="button"
                        onClick={() => startEdit(p)}
                        className="text-xs font-semibold text-sky-400 hover:text-sky-300"
                      >
                        Изменить
                      </button>
                      <button
                        type="button"
                        disabled={deleteMut.isPending}
                        onClick={() => {
                          if (confirm(`Удалить тип «${p.name}»? Варианты дизайнов с этим типом будут заблокированы.`))
                            deleteMut.mutate(p.id);
                        }}
                        className="text-xs font-semibold text-red-400 hover:text-red-300 disabled:opacity-50"
                      >
                        Удалить
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {deleteMut.isError && (
        <p className="mt-3 text-xs text-red-400">
          {deleteMut.error instanceof ApiError ? deleteMut.error.message : "Ошибка удаления"}
        </p>
      )}
    </div>
  );
}
