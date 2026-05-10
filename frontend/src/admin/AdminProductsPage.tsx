import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import {
  createProduct,
  deleteProduct,
  getProducts,
  uploadMedia,
} from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { formatMoney } from "@/shared/lib/format-money";
import { Button } from "@/shared/ui/button";

export function AdminProductsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [formError, setFormError] = useState<string | null>(null);
  const [imageUploadBusy, setImageUploadBusy] = useState(false);
  const imageFileInputRef = useRef<HTMLInputElement>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [inStock, setInStock] = useState(true);

  const productsQuery = useQuery({
    queryKey: ["products"],
    queryFn: getProducts,
  });

  const createMut = useMutation({
    mutationFn: () => {
      if (!token) throw new Error("Нет токена");
      const p = Number.parseFloat(price.replace(",", "."));
      if (!Number.isFinite(p) || p < 0) {
        throw new Error("Укажите корректную цену");
      }
      return createProduct(
        {
          title: title.trim(),
          description: description.trim() || null,
          price: p,
          imageUrl: imageUrl.trim() || null,
          inStock,
        },
        token,
      );
    },
    onMutate: () => setFormError(null),
    onSuccess: () => {
      setTitle("");
      setDescription("");
      setPrice("");
      setImageUrl("");
      setInStock(true);
      void queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (err: unknown) => {
      setFormError(err instanceof Error ? err.message : "Ошибка сохранения");
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => {
      if (!token) throw new Error("Нет токена");
      return deleteProduct(id, token);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    createMut.mutate();
  }

  async function handleImageFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    if (!token) {
      setFormError("Нужна авторизация администратора");
      return;
    }
    setFormError(null);
    setImageUploadBusy(true);
    try {
      const { publicUrl } = await uploadMedia(file, token);
      setImageUrl(publicUrl);
    } catch (err) {
      setFormError(
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : "Не удалось загрузить файл",
      );
    } finally {
      setImageUploadBusy(false);
    }
  }

  return (
    <div>
      <h1 className="font-display mb-8 text-4xl tracking-wide text-zinc-100">
        Товары
      </h1>

      <section className="mb-12 rounded-[14px] border border-white/10 bg-zinc-900/40 p-6">
        <h2 className="mb-4 text-lg font-semibold text-zinc-200">
          Новый товар
        </h2>
        <form onSubmit={handleCreate} className="grid gap-4 md:grid-cols-2">
          <label className="flex flex-col gap-1 text-sm md:col-span-2">
            <span className="text-zinc-400">Название</span>
            <input
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm md:col-span-2">
            <span className="text-zinc-400">Описание</span>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="resize-y rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            <span className="text-zinc-400">Цена (₸)</span>
            <input
              required
              inputMode="decimal"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
            />
          </label>
          <div className="flex flex-col gap-2 text-sm">
            <span className="text-zinc-400">Картинка</span>
            <div className="flex flex-wrap items-center gap-2">
              <input
                ref={imageFileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                disabled={imageUploadBusy || !token}
                onChange={(e) => void handleImageFileChange(e)}
              />
              <Button
                type="button"
                variant="outline"
                className="rounded-[10px]"
                disabled={imageUploadBusy || !token}
                onClick={() => imageFileInputRef.current?.click()}
              >
                {imageUploadBusy ? "Загрузка…" : "Загрузить файл"}
              </Button>
              <span className="text-xs text-zinc-500">
                или укажите URL ниже (до 8 МБ, image/*)
              </span>
            </div>
            <input
              type="url"
              placeholder="https://…"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-violet-500/40 focus:ring-2 focus:ring-violet-500/30"
            />
          </div>
          <label className="flex items-center gap-2 text-sm md:col-span-2">
            <input
              type="checkbox"
              checked={inStock}
              onChange={(e) => setInStock(e.target.checked)}
              className="h-4 w-4 rounded border-white/20 bg-zinc-950 text-violet-500"
            />
            <span className="text-zinc-300">В наличии</span>
          </label>

          {formError ? (
            <p className="md:col-span-2 text-sm font-medium text-red-400">
              {formError}
            </p>
          ) : null}

          <div className="md:col-span-2">
            <Button
              type="submit"
              variant="primary"
              className="rounded-[10px]"
              disabled={createMut.isPending}
            >
              {createMut.isPending ? "Сохраняем…" : "Добавить товар"}
            </Button>
          </div>
        </form>
      </section>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-200">
          Каталог ({productsQuery.data?.length ?? "…"})
        </h2>
        {productsQuery.isPending ? (
          <p className="text-zinc-500">Загрузка…</p>
        ) : productsQuery.isError ? (
          <p className="text-red-400">Ошибка загрузки списка</p>
        ) : (
          <div className="overflow-x-auto rounded-[14px] border border-white/10">
            <table className="w-full min-w-[640px] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-white/10 bg-zinc-900/80 text-xs uppercase tracking-wide text-zinc-500">
                  <th className="px-4 py-3 font-semibold">ID</th>
                  <th className="px-4 py-3 font-semibold">Название</th>
                  <th className="px-4 py-3 font-semibold">Цена</th>
                  <th className="px-4 py-3 font-semibold">Статус</th>
                  <th className="px-4 py-3 font-semibold">На сайте</th>
                  <th className="px-4 py-3 font-semibold"> </th>
                </tr>
              </thead>
              <tbody>
                {productsQuery.data?.map((p) => (
                  <tr
                    key={p.id}
                    className="border-b border-white/5 hover:bg-white/[0.03]"
                  >
                    <td className="px-4 py-3 tabular-nums text-zinc-500">
                      {p.id}
                    </td>
                    <td className="max-w-[220px] truncate px-4 py-3 font-medium text-zinc-100">
                      {p.title}
                    </td>
                    <td className="px-4 py-3 tabular-nums text-zinc-300">
                      {formatMoney(p.price)} ₸
                    </td>
                    <td className="px-4 py-3 text-zinc-400">
                      {p.inStock ? "В наличии" : "Нет"}
                    </td>
                    <td className="px-4 py-3">
                      <Link
                        to={`/catalog/${p.id}`}
                        className="font-semibold text-violet-400 hover:underline"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Открыть
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        className="text-sm font-semibold text-red-400 hover:text-red-300 disabled:opacity-40"
                        disabled={deleteMut.isPending}
                        onClick={() => {
                          if (
                            window.confirm(
                              `Удалить «${p.title}»? Это действие нельзя отменить.`,
                            )
                          ) {
                            void deleteMut.mutateAsync(p.id);
                          }
                        }}
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
      </section>
    </div>
  );
}
