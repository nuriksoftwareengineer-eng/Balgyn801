import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import {
  createGroup,
  deleteGroup,
  listGroups,
  updateGroup,
  type AdminGroup,
} from "@/shared/api/admin-catalog";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";
import { ImageField } from "@/admin/ImageField";

const inputClass =
  "rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40";

/** Простой slug: латиница/цифры в нижнем регистре, остальное → дефис. */
export function slugify(input: string): string {
  return input
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function AdminCategoriesPage() {
  const { token } = useAuth();
  const qc = useQueryClient();

  const { data: groups = [], isLoading } = useQuery({
    queryKey: ["admin", "groups"],
    queryFn: () => listGroups(token!),
    enabled: !!token,
  });

  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [nameKk, setNameKk] = useState("");
  const [nameEn, setNameEn] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [sortOrder, setSortOrder] = useState("0");
  const [coverImageUrl, setCoverImageUrl] = useState("");
  const [bannerImageUrl, setBannerImageUrl] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  function resetForm() {
    setEditingId(null);
    setName("");
    setNameKk("");
    setNameEn("");
    setSlug("");
    setSlugTouched(false);
    setSortOrder("0");
    setCoverImageUrl("");
    setBannerImageUrl("");
    setFormError(null);
  }

  function startEdit(g: AdminGroup) {
    setEditingId(g.id);
    setName(g.name);
    setNameKk(g.nameKk ?? "");
    setNameEn(g.nameEn ?? "");
    setSlug(g.slug);
    setSlugTouched(true);
    setSortOrder(String(g.sortOrder ?? 0));
    setCoverImageUrl(g.coverImageUrl ?? "");
    setBannerImageUrl(g.bannerImageUrl ?? "");
    setFormError(null);
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const body = {
        name: name.trim(),
        nameKk: nameKk.trim() || null,
        nameEn: nameEn.trim() || null,
        slug: (slug.trim() || slugify(name)).trim(),
        sortOrder: Number.parseInt(sortOrder, 10) || 0,
        coverImageUrl: coverImageUrl.trim() || null,
        bannerImageUrl: bannerImageUrl.trim() || null,
      };
      if (!body.name) throw new Error("Укажите название");
      if (!body.slug) throw new Error("Укажите slug");
      return editingId == null
        ? createGroup(body, token!)
        : updateGroup(editingId, body, token!);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "groups"] });
      resetForm();
    },
    onError: (e) =>
      setFormError(e instanceof ApiError ? e.message : (e as Error).message),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => deleteGroup(id, token!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "groups"] }),
    onError: (e) =>
      setFormError(
        e instanceof ApiError
          ? e.message
          : "Не удалось удалить (возможно, есть связанные коллекции)",
      ),
  });

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-zinc-100">Категории</h1>
      <p className="mb-6 text-sm text-zinc-500">
        Верхний уровень каталога: <code>/catalog/{"{"}categorySlug{"}"}</code> (например anime, games, movies).
      </p>

      {/* Form */}
      <div className="mb-8 max-w-lg rounded-lg border border-white/10 bg-zinc-900/60 p-6">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-zinc-400">
          {editingId == null ? "Новая категория" : `Редактирование #${editingId}`}
        </h2>
        <div className="flex flex-col gap-4">
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Название (RU) *</span>
            <input
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (!slugTouched) setSlug(slugify(e.target.value));
              }}
              placeholder="Аниме"
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Название (KZ)</span>
            <input
              value={nameKk}
              onChange={(e) => setNameKk(e.target.value)}
              placeholder="Аниме (қаз)"
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Название (EN)</span>
            <input
              value={nameEn}
              onChange={(e) => setNameEn(e.target.value)}
              placeholder="Anime"
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Slug *</span>
            <input
              value={slug}
              onChange={(e) => {
                setSlug(e.target.value);
                setSlugTouched(true);
              }}
              placeholder="anime"
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Порядок сортировки</span>
            <input
              type="number"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              className={`${inputClass} w-32`}
            />
          </label>
          <ImageField
            label="Обложка"
            value={coverImageUrl}
            onChange={setCoverImageUrl}
            token={token}
            onError={setFormError}
          />
          <ImageField
            label="Баннер"
            value={bannerImageUrl}
            onChange={setBannerImageUrl}
            token={token}
            onError={setFormError}
          />

          {formError && <p className="text-xs text-red-400">{formError}</p>}

          <div className="flex gap-3">
            <Button
              type="button"
              disabled={saveMut.isPending}
              onClick={() => saveMut.mutate()}
            >
              {saveMut.isPending ? "Сохраняем…" : editingId == null ? "Создать" : "Сохранить"}
            </Button>
            {editingId != null && (
              <button
                type="button"
                onClick={resetForm}
                className="text-sm font-semibold text-zinc-400 hover:text-zinc-200"
              >
                Отмена
              </button>
            )}
          </div>
        </div>
      </div>

      {/* List */}
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-400">
        Все категории
      </h2>
      {isLoading ? (
        <p className="text-sm text-zinc-500">Загружаем…</p>
      ) : groups.length === 0 ? (
        <p className="text-sm text-zinc-500">Категорий пока нет.</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-white/10">
          <table className="w-full text-sm">
            <thead className="bg-zinc-900 text-left text-xs uppercase tracking-wide text-zinc-500">
              <tr>
                <th className="px-4 py-2.5 font-semibold">Название</th>
                <th className="px-4 py-2.5 font-semibold">Slug</th>
                <th className="px-4 py-2.5 font-semibold">Порядок</th>
                <th className="px-4 py-2.5 font-semibold">Активна</th>
                <th className="px-4 py-2.5" />
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {groups.map((g) => (
                <tr key={g.id} className="bg-zinc-900/40">
                  <td className="px-4 py-2.5 font-medium text-zinc-100">{g.name}</td>
                  <td className="px-4 py-2.5 text-zinc-400">{g.slug}</td>
                  <td className="px-4 py-2.5 text-zinc-400">{g.sortOrder ?? 0}</td>
                  <td className="px-4 py-2.5">
                    <span className={g.active ? "text-emerald-400" : "text-zinc-600"}>
                      {g.active ? "да" : "нет"}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <button
                      type="button"
                      onClick={() => startEdit(g)}
                      className="mr-4 text-xs font-semibold text-zinc-300 hover:text-white"
                    >
                      Изменить
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (confirm(`Удалить категорию «${g.name}»?`)) deleteMut.mutate(g.id);
                      }}
                      disabled={deleteMut.isPending}
                      className="text-xs font-semibold text-red-400 hover:text-red-300 disabled:opacity-50"
                    >
                      Удалить
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
