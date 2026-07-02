import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "@/app/auth-context";
import {
  adminListReviews,
  adminCreateReview,
  adminUpdateReview,
  adminPublishReview,
  adminHideReview,
  adminDeleteReview,
} from "@/shared/api/backend-api";
import type { ShopReviewRequest, ShopReviewResponse, ShopReviewStatus } from "@/shared/types/reviews";
import { Button } from "@/shared/ui/button";
import { AdminPagination, AdminSearchBar } from "@/shared/ui/admin-pagination";
import { useDebounce } from "@/shared/hooks/useDebounce";

function emptyForm(): ShopReviewRequest {
  return {
    name: "",
    avatarUrl: null,
    city: null,
    rating: 5,
    body: "",
    photoUrls: [],
    status: "PUBLISHED",
  };
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit", year: "numeric" });
  } catch { return iso; }
}

function Stars({ n }: { n: number }) {
  return <span>{"★".repeat(n)}{"☆".repeat(5 - n)}</span>;
}

function StatusBadge({ status }: { status: ShopReviewStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded px-2 py-0.5 text-[0.65rem] font-semibold uppercase tracking-wide ${
        status === "PUBLISHED"
          ? "bg-green-100 text-green-800"
          : "bg-zinc-200 text-zinc-600"
      }`}
    >
      {status === "PUBLISHED" ? "Опубликован" : "Скрыт"}
    </span>
  );
}

function PhotoUrlsInput({
  value,
  onChange,
}: {
  value: string[];
  onChange: (v: string[]) => void;
}) {
  const [raw, setRaw] = useState(value.join("\n"));
  return (
    <div>
      <label className="mb-1 block text-xs font-medium text-zinc-300">
        Фото (по одному URL на строку)
      </label>
      <textarea
        className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-white/30"
        rows={3}
        value={raw}
        onChange={(e) => {
          setRaw(e.target.value);
          onChange(e.target.value.split("\n").map((s) => s.trim()).filter(Boolean));
        }}
        placeholder="https://example.com/photo.jpg"
      />
    </div>
  );
}

export function AdminReviewsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [modal, setModal] = useState<"create" | "edit" | null>(null);
  const [form, setForm] = useState<ShopReviewRequest>(emptyForm);
  const [editId, setEditId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const q = useQuery({
    queryKey: ["admin-reviews", debouncedSearch, page],
    queryFn: () => {
      if (!token) throw new Error("Нет токена");
      return adminListReviews(token, { q: debouncedSearch, page, size: 50 });
    },
    enabled: !!token,
  });

  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: ["admin-reviews"] });

  const createMut = useMutation({
    mutationFn: () => {
      if (!token) throw new Error();
      return adminCreateReview(token, form);
    },
    onSuccess: () => { setModal(null); setForm(emptyForm()); setFormError(null); invalidate(); },
    onError: (e: Error) => setFormError(e.message),
  });

  const updateMut = useMutation({
    mutationFn: () => {
      if (!token || editId == null) throw new Error();
      return adminUpdateReview(token, editId, form);
    },
    onSuccess: () => { setModal(null); setForm(emptyForm()); setFormError(null); invalidate(); },
    onError: (e: Error) => setFormError(e.message),
  });

  const publishMut = useMutation({
    mutationFn: (id: number) => {
      if (!token) throw new Error();
      return adminPublishReview(token, id);
    },
    onSuccess: invalidate,
  });

  const hideMut = useMutation({
    mutationFn: (id: number) => {
      if (!token) throw new Error();
      return adminHideReview(token, id);
    },
    onSuccess: invalidate,
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => {
      if (!token) throw new Error();
      return adminDeleteReview(token, id);
    },
    onSuccess: invalidate,
  });

  function openEdit(r: ShopReviewResponse) {
    setEditId(r.id);
    setForm({
      name: r.name,
      avatarUrl: r.avatarUrl,
      city: r.city,
      rating: r.rating,
      body: r.body,
      photoUrls: r.photoUrls,
      status: r.status,
    });
    setFormError(null);
    setModal("edit");
  }

  function openCreate() {
    setForm(emptyForm());
    setFormError(null);
    setModal("create");
  }

  function set<K extends keyof ShopReviewRequest>(k: K, v: ShopReviewRequest[K]) {
    setForm((f) => ({ ...f, [k]: v }));
  }

  const data = q.data;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-white">Отзывы</h1>
        <Button variant="primary" onClick={openCreate}>+ Добавить</Button>
      </div>

      <div className="mb-4 flex items-center gap-3">
        <AdminSearchBar
          value={search}
          onChange={(v) => { setSearch(v); setPage(0); }}
          placeholder="Поиск по имени, городу, тексту..."
        />
      </div>

      {q.isLoading && <p className="text-sm text-zinc-400">Загрузка...</p>}
      {q.isError && <p className="text-sm text-red-400">Ошибка загрузки</p>}

      {data && (
        <>
          <div className="overflow-x-auto rounded border border-white/10">
            <table className="w-full text-sm text-zinc-300">
              <thead className="border-b border-white/10 bg-zinc-800/60 text-[0.65rem] uppercase tracking-wide text-zinc-500">
                <tr>
                  <th className="px-4 py-3 text-left">Автор</th>
                  <th className="px-4 py-3 text-left">Рейтинг</th>
                  <th className="px-4 py-3 text-left">Отзыв</th>
                  <th className="px-4 py-3 text-left">Статус</th>
                  <th className="px-4 py-3 text-left">Дата</th>
                  <th className="px-4 py-3 text-right">Действия</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {data.content.map((r) => (
                  <tr key={r.id} className="hover:bg-white/5 transition">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {r.avatarUrl ? (
                          <img src={r.avatarUrl} alt="" className="h-7 w-7 rounded-full object-cover" />
                        ) : (
                          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-zinc-700 text-xs font-semibold uppercase text-white">
                            {r.name.charAt(0)}
                          </div>
                        )}
                        <div>
                          <p className="font-medium text-white">{r.name}</p>
                          {r.city && <p className="text-[0.65rem] text-zinc-500">{r.city}</p>}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-amber-400"><Stars n={r.rating} /></td>
                    <td className="max-w-[260px] px-4 py-3">
                      <p className="truncate">{r.body}</p>
                      {r.photoUrls.length > 0 && (
                        <p className="text-[0.65rem] text-zinc-500">{r.photoUrls.length} фото</p>
                      )}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                    <td className="px-4 py-3 text-zinc-500">{formatDate(r.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          className="text-xs text-zinc-400 hover:text-white transition"
                          onClick={() => openEdit(r)}
                        >
                          Ред.
                        </button>
                        {r.status === "HIDDEN" ? (
                          <button
                            className="text-xs text-green-400 hover:text-green-300 transition"
                            onClick={() => publishMut.mutate(r.id)}
                          >
                            Опубл.
                          </button>
                        ) : (
                          <button
                            className="text-xs text-zinc-400 hover:text-white transition"
                            onClick={() => hideMut.mutate(r.id)}
                          >
                            Скрыть
                          </button>
                        )}
                        <button
                          className="text-xs text-red-400 hover:text-red-300 transition"
                          onClick={() => {
                            if (confirm(`Удалить отзыв от ${r.name}?`)) deleteMut.mutate(r.id);
                          }}
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

          <AdminPagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={50}
            onPage={setPage}
          />
        </>
      )}

      {/* Modal */}
      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-lg rounded-lg border border-white/10 bg-zinc-900 p-6 shadow-2xl">
            <h2 className="mb-5 text-lg font-semibold text-white">
              {modal === "create" ? "Добавить отзыв" : "Редактировать отзыв"}
            </h2>

            <div className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-300">Имя *</label>
                  <input
                    className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                    value={form.name}
                    onChange={(e) => set("name", e.target.value)}
                    placeholder="Алия М."
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-300">Город</label>
                  <input
                    className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                    value={form.city ?? ""}
                    onChange={(e) => set("city", e.target.value || null)}
                    placeholder="Алматы"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-300">URL аватара</label>
                <input
                  className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                  value={form.avatarUrl ?? ""}
                  onChange={(e) => set("avatarUrl", e.target.value || null)}
                  placeholder="https://..."
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-300">Рейтинг *</label>
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((n) => (
                    <button
                      key={n}
                      type="button"
                      onClick={() => set("rating", n)}
                      className={`flex h-9 w-9 items-center justify-center rounded border text-base transition ${
                        form.rating >= n
                          ? "border-amber-400 bg-amber-400/20 text-amber-400"
                          : "border-white/20 text-zinc-500 hover:border-amber-400/50"
                      }`}
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-300">Текст отзыва *</label>
                <textarea
                  className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-white/30"
                  rows={4}
                  value={form.body}
                  onChange={(e) => set("body", e.target.value)}
                  placeholder="Очень довольна качеством вышивки..."
                />
              </div>

              <PhotoUrlsInput
                value={form.photoUrls}
                onChange={(v) => set("photoUrls", v)}
              />

              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-300">Статус</label>
                <select
                  className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none"
                  value={form.status}
                  onChange={(e) => set("status", e.target.value as ShopReviewStatus)}
                >
                  <option value="PUBLISHED">Опубликован</option>
                  <option value="HIDDEN">Скрыт</option>
                </select>
              </div>

              {formError && (
                <p className="text-xs text-red-400">{formError}</p>
              )}

              <div className="flex justify-end gap-3 pt-2">
                <Button variant="outline" onClick={() => setModal(null)}>Отмена</Button>
                <Button
                  variant="primary"
                  onClick={() => modal === "create" ? createMut.mutate() : updateMut.mutate()}
                  disabled={createMut.isPending || updateMut.isPending}
                >
                  {modal === "create" ? "Создать" : "Сохранить"}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
