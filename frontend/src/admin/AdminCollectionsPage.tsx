import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import {
  createCollection,
  deleteCollection,
  listCollections,
  listGroups,
  updateCollection,
  type AdminCollection,
} from "@/shared/api/admin-catalog";
import { uploadMedia } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";
import { slugify } from "@/admin/AdminCategoriesPage";

const inputClass =
  "rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40";

// ── Single-image upload field ───────────────────────────────────────────────
function ImageField({
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

export function AdminCollectionsPage() {
  const { token } = useAuth();
  const qc = useQueryClient();

  const { data: groups = [] } = useQuery({
    queryKey: ["admin", "groups"],
    queryFn: () => listGroups(token!),
    enabled: !!token,
  });
  const { data: collections = [], isLoading } = useQuery({
    queryKey: ["admin", "collections"],
    queryFn: () => listCollections(token!),
    enabled: !!token,
  });

  const [editingId, setEditingId] = useState<number | null>(null);
  const [groupId, setGroupId] = useState<string>("");
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [description, setDescription] = useState("");
  const [coverImageUrl, setCoverImageUrl] = useState("");
  const [bannerImageUrl, setBannerImageUrl] = useState("");
  const [sortOrder, setSortOrder] = useState("0");
  const [formError, setFormError] = useState<string | null>(null);

  function resetForm() {
    setEditingId(null);
    setGroupId("");
    setName("");
    setSlug("");
    setSlugTouched(false);
    setDescription("");
    setCoverImageUrl("");
    setBannerImageUrl("");
    setSortOrder("0");
    setFormError(null);
  }

  function startEdit(c: AdminCollection) {
    setEditingId(c.id);
    setGroupId(String(c.groupId));
    setName(c.name);
    setSlug(c.slug);
    setSlugTouched(true);
    setDescription(c.description ?? "");
    setCoverImageUrl(c.coverImageUrl ?? "");
    setBannerImageUrl(c.bannerImageUrl ?? "");
    setSortOrder(String(c.sortOrder ?? 0));
    setFormError(null);
  }

  const saveMut = useMutation({
    mutationFn: () => {
      if (!groupId) throw new Error("Выберите категорию");
      const body = {
        groupId: Number(groupId),
        name: name.trim(),
        slug: (slug.trim() || slugify(name)).trim(),
        description: description.trim() || null,
        coverImageUrl: coverImageUrl || null,
        bannerImageUrl: bannerImageUrl || null,
        sortOrder: Number.parseInt(sortOrder, 10) || 0,
      };
      if (!body.name) throw new Error("Укажите название");
      if (!body.slug) throw new Error("Укажите slug");
      return editingId == null
        ? createCollection(body, token!)
        : updateCollection(editingId, body, token!);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "collections"] });
      resetForm();
    },
    onError: (e) => setFormError(e instanceof ApiError ? e.message : (e as Error).message),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => deleteCollection(id, token!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "collections"] }),
    onError: (e) =>
      setFormError(
        e instanceof ApiError ? e.message : "Не удалось удалить (возможно, есть дизайны внутри)",
      ),
  });

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-zinc-100">Коллекции</h1>
      <p className="mb-6 text-sm text-zinc-500">
        Внутри категории: <code>/catalog/{"{"}categorySlug{"}"}/{"{"}collectionSlug{"}"}</code> (например anime/berserk).
      </p>

      {/* Form */}
      <div className="mb-8 max-w-2xl rounded-lg border border-white/10 bg-zinc-900/60 p-6">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-zinc-400">
          {editingId == null ? "Новая коллекция" : `Редактирование #${editingId}`}
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Категория *</span>
            <select
              value={groupId}
              onChange={(e) => setGroupId(e.target.value)}
              className={inputClass}
            >
              <option value="">— выберите —</option>
              {groups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Порядок сортировки</span>
            <input
              type="number"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Название *</span>
            <input
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (!slugTouched) setSlug(slugify(e.target.value));
              }}
              placeholder="Berserk"
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
              placeholder="berserk"
              className={inputClass}
            />
          </label>
          <label className="flex flex-col gap-1.5 sm:col-span-2">
            <span className="text-xs text-zinc-400">Описание</span>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={`${inputClass} resize-y`}
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
        </div>

        {formError && <p className="mt-4 text-xs text-red-400">{formError}</p>}

        <div className="mt-5 flex gap-3">
          <Button type="button" disabled={saveMut.isPending} onClick={() => saveMut.mutate()}>
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

      {/* List */}
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-400">
        Все коллекции
      </h2>
      {isLoading ? (
        <p className="text-sm text-zinc-500">Загружаем…</p>
      ) : collections.length === 0 ? (
        <p className="text-sm text-zinc-500">Коллекций пока нет.</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-white/10">
          <table className="w-full text-sm">
            <thead className="bg-zinc-900 text-left text-xs uppercase tracking-wide text-zinc-500">
              <tr>
                <th className="px-4 py-2.5 font-semibold">Коллекция</th>
                <th className="px-4 py-2.5 font-semibold">Категория</th>
                <th className="px-4 py-2.5 font-semibold">URL</th>
                <th className="px-4 py-2.5 font-semibold">Медиа</th>
                <th className="px-4 py-2.5" />
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {collections.map((c) => (
                <tr key={c.id} className="bg-zinc-900/40">
                  <td className="px-4 py-2.5 font-medium text-zinc-100">{c.name}</td>
                  <td className="px-4 py-2.5 text-zinc-400">{c.groupName}</td>
                  <td className="px-4 py-2.5 text-zinc-500">/{c.slug}</td>
                  <td className="px-4 py-2.5 text-xs text-zinc-500">
                    {[c.coverImageUrl && "обложка", c.bannerImageUrl && "баннер"]
                      .filter(Boolean)
                      .join(", ") || "—"}
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <button
                      type="button"
                      onClick={() => startEdit(c)}
                      className="mr-4 text-xs font-semibold text-zinc-300 hover:text-white"
                    >
                      Изменить
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (confirm(`Удалить коллекцию «${c.name}»?`)) deleteMut.mutate(c.id);
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
