import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import {
  archiveDesign,
  createDesign,
  deleteDesign,
  listCollections,
  listDesigns,
  listGroups,
  publishDesign,
  restoreDesign,
  updateDesign,
  type AdminDesign,
  type DesignStatus,
} from "@/shared/api/admin-catalog";
import { uploadMedia } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";
import { slugify } from "@/admin/AdminCategoriesPage";

const inputClass =
  "rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40";

// ─── Design status badge ──────────────────────────────────────────────────────

const STATUS_STYLES: Record<DesignStatus, { label: string; cls: string }> = {
  DRAFT:     { label: "ЧЕРНОВИК",    cls: "bg-zinc-700/60 text-zinc-400" },
  READY:     { label: "ГОТОВ",       cls: "bg-blue-900/40 text-blue-400" },
  PUBLISHED: { label: "ОПУБЛИКОВАН", cls: "bg-emerald-900/40 text-emerald-400" },
  ARCHIVED:  { label: "АРХИВ",       cls: "bg-orange-900/40 text-orange-400" },
};

function StatusBadge({ d }: { d: AdminDesign }) {
  const { label, cls } = STATUS_STYLES[d.status] ?? STATUS_STYLES.DRAFT;
  return (
    <span className={`inline-flex items-center rounded px-2 py-0.5 text-[0.6rem] font-bold uppercase tracking-[0.08em] ${cls}`}>
      {label}
    </span>
  );
}

// ─── Publish guidance ─────────────────────────────────────────────────────────

function PublishGuidance({ d }: { d: AdminDesign }) {
  if (d.status === "PUBLISHED" || d.status === "ARCHIVED") return null;
  const issues: string[] = [];
  if (!d.mainImageUrl) issues.push("нет главного изображения");
  if (d.activeGarmentCount === 0) issues.push("нет активных вариантов");
  if (issues.length === 0) return null;
  return (
    <p className="mt-0.5 text-[0.6rem] text-amber-500">
      Для публикации: {issues.join(", ")}
    </p>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function AdminDesignsPage() {
  const { token } = useAuth();
  const qc = useQueryClient();
  const navigate = useNavigate();

  const { data: groups = [] } = useQuery({
    queryKey: ["admin", "groups"],
    queryFn: () => listGroups(token!),
    enabled: !!token,
  });
  const { data: collections = [] } = useQuery({
    queryKey: ["admin", "collections"],
    queryFn: () => listCollections(token!),
    enabled: !!token,
  });
  const { data: designs = [], isLoading } = useQuery({
    queryKey: ["admin", "designs"],
    queryFn: () => listDesigns(token!),
    enabled: !!token,
  });

  const [editingId, setEditingId] = useState<number | null>(null);
  const [categoryId, setCategoryId] = useState("");
  const [collectionId, setCollectionId] = useState("");
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [description, setDescription] = useState("");
  const [mainImageUrl, setMainImageUrl] = useState("");
  const [gallery, setGallery] = useState<string[]>([]);
  const [isNewArrival, setIsNewArrival] = useState(false);
  const [mainBusy, setMainBusy] = useState(false);
  const [galleryBusy, setGalleryBusy] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const mainRef = useRef<HTMLInputElement>(null);
  const galleryRef = useRef<HTMLInputElement>(null);

  const collectionsForCategory = useMemo(
    () =>
      categoryId
        ? collections.filter((c) => String(c.groupId) === categoryId)
        : collections,
    [collections, categoryId],
  );

  function resetForm() {
    setEditingId(null);
    setCategoryId("");
    setCollectionId("");
    setName("");
    setSlug("");
    setSlugTouched(false);
    setDescription("");
    setMainImageUrl("");
    setGallery([]);
    setIsNewArrival(false);
    setFormError(null);
  }

  function startEdit(d: AdminDesign) {
    const coll = collections.find((c) => c.id === d.collectionId);
    setEditingId(d.id);
    setCategoryId(coll ? String(coll.groupId) : "");
    setCollectionId(String(d.collectionId));
    setName(d.name);
    setSlug(d.slug);
    setSlugTouched(true);
    setDescription(d.description ?? "");
    setMainImageUrl(d.mainImageUrl ?? "");
    setGallery(d.gallery ?? []);
    setIsNewArrival(d.isNewArrival ?? false);
    setFormError(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function handleMainUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !token) return;
    setMainBusy(true);
    try {
      const res = await uploadMedia(file, token);
      setMainImageUrl(res.publicUrl);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Ошибка загрузки");
    } finally {
      setMainBusy(false);
      if (mainRef.current) mainRef.current.value = "";
    }
  }

  async function handleGalleryUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length === 0 || !token) return;
    setGalleryBusy(true);
    try {
      const urls: string[] = [];
      for (const f of files) {
        const res = await uploadMedia(f, token);
        urls.push(res.publicUrl);
      }
      setGallery((g) => [...g, ...urls]);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Ошибка загрузки галереи");
    } finally {
      setGalleryBusy(false);
      if (galleryRef.current) galleryRef.current.value = "";
    }
  }

  const saveMut = useMutation({
    mutationFn: () => {
      if (!collectionId) throw new Error("Выберите коллекцию");
      const body = {
        collectionId: Number(collectionId),
        name: name.trim(),
        slug: (slug.trim() || slugify(name)).trim(),
        description: description.trim() || null,
        mainImageUrl: mainImageUrl || null,
        gallery,
        isNewArrival,
      };
      if (!body.name) throw new Error("Укажите название");
      if (!body.slug) throw new Error("Укажите slug");
      return editingId == null
        ? createDesign(body, token!)
        : updateDesign(editingId, body, token!);
    },
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ["admin", "designs"] });
      if (editingId == null) {
        navigate(`/admin/designs/${saved.id}/variants`);
      } else {
        resetForm();
      }
    },
    onError: (e) => setFormError(e instanceof ApiError ? e.message : (e as Error).message),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => deleteDesign(id, token!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "designs"] }),
    onError: (e) => setFormError(e instanceof ApiError ? e.message : "Не удалось удалить"),
  });

  const publishMut = useMutation({
    mutationFn: (id: number) => publishDesign(id, token!),
    onSuccess: () => {
      setFormError(null);
      qc.invalidateQueries({ queryKey: ["admin", "designs"] });
    },
    onError: (e) => {
      if (e instanceof ApiError) {
        try {
          const body = JSON.parse(e.message);
          const errs: string[] = body.errors ?? [];
          setFormError(`Нельзя опубликовать: ${errs.join(", ")}`);
        } catch {
          setFormError(e.message);
        }
      } else {
        setFormError("Не удалось опубликовать");
      }
    },
  });

  const archiveMut = useMutation({
    mutationFn: (id: number) => archiveDesign(id, token!),
    onSuccess: () => {
      setFormError(null);
      qc.invalidateQueries({ queryKey: ["admin", "designs"] });
    },
    onError: (e) => setFormError(e instanceof ApiError ? e.message : "Не удалось архивировать"),
  });

  const restoreMut = useMutation({
    mutationFn: (id: number) => restoreDesign(id, token!),
    onSuccess: () => {
      setFormError(null);
      qc.invalidateQueries({ queryKey: ["admin", "designs"] });
    },
    onError: (e) => setFormError(e instanceof ApiError ? e.message : "Не удалось восстановить"),
  });

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-zinc-100">Дизайны</h1>
      <p className="mb-6 text-sm text-zinc-500">
        Новый дизайн создаётся как <strong className="text-zinc-300">черновик</strong> и не отображается в каталоге.
        После создания добавьте варианты, затем опубликуйте.
      </p>

      {/* Form */}
      <div className="mb-8 max-w-2xl rounded-lg border border-white/10 bg-zinc-900/60 p-6">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-zinc-400">
          {editingId == null ? "Новый дизайн" : `Редактирование #${editingId}`}
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Категория *</span>
            <select
              value={categoryId}
              onChange={(e) => {
                setCategoryId(e.target.value);
                setCollectionId("");
              }}
              className={inputClass}
            >
              <option value="">— выберите —</option>
              {groups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Коллекция *</span>
            <select
              value={collectionId}
              onChange={(e) => setCollectionId(e.target.value)}
              disabled={!categoryId}
              className={inputClass}
            >
              <option value="">{categoryId ? "— выберите —" : "сначала категория"}</option>
              {collectionsForCategory.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Название *</span>
            <input
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (!slugTouched) setSlug(slugify(e.target.value));
              }}
              placeholder="Brand Of Sacrifice"
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
              placeholder="brand-of-sacrifice"
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

          {/* Main image */}
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Главное изображение</span>
            <input
              ref={mainRef}
              type="file"
              accept="image/*"
              onChange={handleMainUpload}
              disabled={mainBusy}
              className="text-xs text-zinc-300 file:mr-3 file:rounded file:border-0 file:bg-zinc-700 file:px-3 file:py-1.5 file:text-xs file:text-zinc-200 hover:file:bg-zinc-600"
            />
            {mainBusy && <p className="text-xs text-zinc-400">Загружаем…</p>}
            {mainImageUrl && (
              <div className="mt-1 flex items-center gap-2">
                <img src={mainImageUrl} alt="main" className="max-h-24 rounded object-contain bg-zinc-800" />
                <button type="button" onClick={() => setMainImageUrl("")} className="text-xs text-red-400 hover:text-red-300">
                  Убрать
                </button>
              </div>
            )}
          </label>

          {/* Gallery */}
          <label className="flex flex-col gap-1.5">
            <span className="text-xs text-zinc-400">Галерея (можно несколько)</span>
            <input
              ref={galleryRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handleGalleryUpload}
              disabled={galleryBusy}
              className="text-xs text-zinc-300 file:mr-3 file:rounded file:border-0 file:bg-zinc-700 file:px-3 file:py-1.5 file:text-xs file:text-zinc-200 hover:file:bg-zinc-600"
            />
            {galleryBusy && <p className="text-xs text-zinc-400">Загружаем…</p>}
          </label>

          {gallery.length > 0 && (
            <div className="sm:col-span-2 flex flex-wrap gap-2">
              {gallery.map((url, i) => (
                <div key={url + i} className="relative">
                  <img src={url} alt={`g${i}`} className="h-20 w-20 rounded object-cover bg-zinc-800" />
                  <button
                    type="button"
                    onClick={() => setGallery((g) => g.filter((_, idx) => idx !== i))}
                    className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs text-white hover:bg-red-400"
                    aria-label="Удалить из галереи"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* New arrival flag */}
        <label className="mt-4 flex cursor-pointer items-center gap-2 text-sm text-zinc-300">
          <input
            type="checkbox"
            checked={isNewArrival}
            onChange={e => setIsNewArrival(e.target.checked)}
            className="h-4 w-4"
          />
          Отметить как «Новинка» (показывать в секции новинок)
        </label>

        {formError && <p className="mt-4 text-xs text-red-400">{formError}</p>}

        <div className="mt-5 flex gap-3">
          <Button type="button" disabled={saveMut.isPending} onClick={() => saveMut.mutate()}>
            {saveMut.isPending
              ? "Сохраняем…"
              : editingId == null
                ? "Создать и перейти к вариантам →"
                : "Сохранить"}
          </Button>
          {editingId != null && (
            <button type="button" onClick={resetForm} className="text-sm font-semibold text-zinc-400 hover:text-zinc-200">
              Отмена
            </button>
          )}
        </div>
      </div>

      {/* List */}
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-400">Все дизайны</h2>
      {isLoading ? (
        <p className="text-sm text-zinc-500">Загружаем…</p>
      ) : designs.length === 0 ? (
        <p className="text-sm text-zinc-500">Дизайнов пока нет.</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-white/10">
          <table className="w-full text-sm">
            <thead className="bg-zinc-900 text-left text-xs uppercase tracking-wide text-zinc-500">
              <tr>
                <th className="px-4 py-2.5 font-semibold">Дизайн</th>
                <th className="px-4 py-2.5 font-semibold">Коллекция</th>
                <th className="px-4 py-2.5 font-semibold">Статус</th>
                <th className="px-4 py-2.5 font-semibold text-center">Вар.</th>
                <th className="px-4 py-2.5" />
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {designs.map((d) => (
                <tr key={d.id} className="bg-zinc-900/40">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      {d.mainImageUrl ? (
                        <img src={d.mainImageUrl} alt={d.name} className="h-9 w-9 rounded object-cover bg-zinc-800" />
                      ) : (
                        <div className="h-9 w-9 rounded bg-zinc-800 border border-white/10" />
                      )}
                      <div>
                        <span className="font-medium text-zinc-100">{d.name}</span>
                        <p className="text-[0.65rem] text-zinc-500">{d.groupName} / {d.collectionName}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-xs text-zinc-400">
                    {d.collectionName}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge d={d} />
                    <PublishGuidance d={d} />
                  </td>
                  <td className="px-4 py-3 text-center text-xs text-zinc-400">
                    {d.activeGarmentCount}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-3 flex-wrap">
                      {/* Publish / Restore */}
                      {(d.status === "DRAFT" || d.status === "READY") && (
                        <button
                          type="button"
                          onClick={() => publishMut.mutate(d.id)}
                          disabled={publishMut.isPending}
                          className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 disabled:opacity-50"
                        >
                          Опубликовать
                        </button>
                      )}
                      {d.status === "ARCHIVED" && (
                        <button
                          type="button"
                          onClick={() => restoreMut.mutate(d.id)}
                          disabled={restoreMut.isPending}
                          className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 disabled:opacity-50"
                        >
                          Восстановить
                        </button>
                      )}

                      {/* Archive */}
                      {d.status !== "ARCHIVED" && (
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm(`Архивировать дизайн «${d.name}»? Он будет скрыт из каталога.`)) {
                              archiveMut.mutate(d.id);
                            }
                          }}
                          disabled={archiveMut.isPending}
                          className="text-xs font-semibold text-amber-400 hover:text-amber-300 disabled:opacity-50"
                        >
                          Архивировать
                        </button>
                      )}

                      <Link
                        to={`/admin/designs/${d.id}/variants`}
                        className="text-xs font-semibold text-sky-400 hover:text-sky-300"
                      >
                        Варианты
                      </Link>

                      {d.status === "PUBLISHED" && (
                        <Link
                          to={`/catalog/${d.groupSlug}/${d.collectionSlug}/${d.slug}`}
                          className="text-xs text-zinc-500 hover:text-zinc-300"
                          target="_blank"
                          title="Открыть в каталоге"
                        >
                          ↗
                        </Link>
                      )}

                      <button
                        type="button"
                        onClick={() => startEdit(d)}
                        className="text-xs font-semibold text-zinc-300 hover:text-white"
                      >
                        Изменить
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm(`Удалить дизайн «${d.name}»?`)) deleteMut.mutate(d.id);
                        }}
                        disabled={deleteMut.isPending}
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
    </div>
  );
}
