import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "@/app/auth-context";
import {
  createCustomer,
  deleteCustomer,
  updateCustomer,
  searchAdminCustomers,
} from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import type { CustomerRequest, CustomerResponse } from "@/shared/api/types";
import { Button } from "@/shared/ui/button";
import { AdminPagination, AdminSearchBar } from "@/shared/ui/admin-pagination";
import { useDebounce } from "@/shared/hooks/useDebounce";

function formatCustomerDate(iso: string | undefined | null) {
  if (!iso) return "—";
  try {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleString("ru-RU", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return iso;
  }
}

function normalizeTelegram(raw: string): string | null {
  const t = raw.trim().replace(/^@/, "");
  return t.length ? t : null;
}

function emptyForm(): CustomerRequest {
  return {
    name: "",
    phone: "",
    telegramUsername: null,
    createAt: null,
    id: null,
  };
}

export function AdminCustomersPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [formError, setFormError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [modal, setModal] = useState<"create" | "edit" | null>(null);
  const [form, setForm] = useState<CustomerRequest>(() => emptyForm());
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const q = useQuery({
    queryKey: ["admin-customers", debouncedSearch, page],
    queryFn: async () => {
      if (!token) throw new Error("Нет токена");
      return searchAdminCustomers(token, { q: debouncedSearch, page, size: 50 });
    },
    enabled: !!token,
  });

  const createMut = useMutation({
    mutationFn: () => {
      if (!token) throw new Error("Нет токена");
      const body: CustomerRequest = {
        name: form.name.trim(),
        phone: form.phone.trim(),
        telegramUsername: normalizeTelegram(form.telegramUsername ?? ""),
      };
      return createCustomer(body, token);
    },
    onSuccess: () => {
      setFormError(null);
      setModal(null);
      setForm(emptyForm());
      void queryClient.invalidateQueries({ queryKey: ["admin-customers"] });
    },
    onError: (err: unknown) => {
      setFormError(
        err instanceof ApiError ? err.message : "Не удалось создать клиента",
      );
    },
  });

  const updateMut = useMutation({
    mutationFn: () => {
      if (!token) throw new Error("Нет токена");
      if (!form.id) throw new Error("Нет ID");
      return updateCustomer(
        {
          id: form.id,
          name: form.name.trim(),
          phone: form.phone.trim(),
          telegramUsername: normalizeTelegram(form.telegramUsername ?? ""),
          createAt: form.createAt ?? null,
        },
        token,
      );
    },
    onSuccess: () => {
      setFormError(null);
      setModal(null);
      setForm(emptyForm());
      void queryClient.invalidateQueries({ queryKey: ["admin-customers"] });
    },
    onError: (err: unknown) => {
      setFormError(
        err instanceof ApiError ? err.message : "Не удалось сохранить",
      );
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => {
      if (!token) throw new Error("Нет токена");
      return deleteCustomer(id, token);
    },
    onMutate: () => setDeleteError(null),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin-customers"] });
    },
    onError: (err: unknown) => {
      setDeleteError(
        err instanceof ApiError ? err.message : "Не удалось удалить клиента",
      );
    },
  });

  function openCreate() {
    setFormError(null);
    setForm(emptyForm());
    setModal("create");
  }

  function openEdit(c: CustomerResponse) {
    setFormError(null);
    setForm({
      id: c.id,
      name: c.name,
      phone: c.phone,
      telegramUsername: c.telegramUsername ?? null,
      createAt: c.createAt ?? null,
    });
    setModal("edit");
  }

  function closeModal() {
    setModal(null);
    setForm(emptyForm());
    setFormError(null);
  }

  const busy = createMut.isPending || updateMut.isPending;

  const customers = q.data?.content ?? [];

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <h1 className="font-display text-4xl tracking-wide text-zinc-100">
          Клиенты
        </h1>
        <Button type="button" variant="primary" className="rounded-[10px]" onClick={openCreate}>
          Новый клиент
        </Button>
      </div>

      <div className="mb-5 flex flex-wrap items-center gap-3">
        <AdminSearchBar
          value={search}
          onChange={(v) => { setSearch(v); setPage(0); }}
          placeholder="Поиск по имени или телефону…"
        />
        {q.data && (
          <span className="text-xs text-zinc-500">{q.data.totalElements} клиентов</span>
        )}
      </div>

      {q.isPending ? (
        <p className="text-zinc-500">Загрузка…</p>
      ) : q.isError ? (
        <p className="text-red-400">
          {q.error instanceof Error ? q.error.message : "Ошибка загрузки"}
        </p>
      ) : customers.length === 0 ? (
        <p className="text-zinc-500">Клиентов не найдено.</p>
      ) : (
        <>
        <div className="overflow-x-auto rounded-[14px] border border-white/10">
          <table className="w-full min-w-[720px] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-white/10 bg-zinc-900/80 text-xs uppercase tracking-wide text-zinc-500">
                <th className="px-4 py-3 font-semibold">ID</th>
                <th className="px-4 py-3 font-semibold">Имя</th>
                <th className="px-4 py-3 font-semibold">Телефон</th>
                <th className="px-4 py-3 font-semibold">Telegram</th>
                <th className="px-4 py-3 font-semibold">Создан</th>
                <th className="px-4 py-3 font-semibold"> </th>
              </tr>
            </thead>
            <tbody>
              {customers.map((c) => (
                <tr
                  key={c.id}
                  className="border-b border-white/5 hover:bg-white/[0.03]"
                >
                  <td className="px-4 py-3 tabular-nums text-zinc-500">{c.id}</td>
                  <td className="max-w-[200px] truncate px-4 py-3 font-medium text-zinc-100">
                    {c.name}
                  </td>
                  <td className="px-4 py-3 tabular-nums text-zinc-400">{c.phone}</td>
                  <td className="max-w-[140px] truncate px-4 py-3 text-zinc-400">
                    {c.telegramUsername ? `@${c.telegramUsername}` : "—"}
                  </td>
                  <td className="px-4 py-3 text-zinc-500">
                    {formatCustomerDate(c.createAt)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        className="text-sm font-semibold text-zinc-300 hover:text-white"
                        onClick={() => openEdit(c)}
                      >
                        Изменить
                      </button>
                      <button
                        type="button"
                        className="text-sm font-semibold text-red-400/90 hover:text-red-300 disabled:opacity-40"
                        disabled={deleteMut.isPending}
                        onClick={() => {
                          if (
                            window.confirm(
                              `Удалить клиента «${c.name}» (ID ${c.id})? Если есть заказы с этим клиентом, удаление может быть запрещено БД.`,
                            )
                          ) {
                            void deleteMut.mutate(c.id);
                          }
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
        {q.data && (
          <AdminPagination
            page={q.data.page}
            totalPages={q.data.totalPages}
            totalElements={q.data.totalElements}
            size={q.data.size}
            onPage={setPage}
          />
        )}
        </>
      )}

      {deleteError ? (
        <p className="mt-4 text-sm text-red-400" role="alert">
          {deleteError}
        </p>
      ) : null}

      {modal ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          role="dialog"
          aria-modal
          aria-labelledby="customer-modal-title"
          onClick={closeModal}
          onKeyDown={(e) => {
            if (e.key === "Escape") closeModal();
          }}
        >
          <div
            className="w-full max-w-md rounded-[14px] border border-white/10 bg-zinc-900 p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2
              id="customer-modal-title"
              className="font-display text-xl tracking-wide text-zinc-100"
            >
              {modal === "create" ? "Новый клиент" : "Редактирование"}
            </h2>
            <form
              className="mt-5 flex flex-col gap-4"
              onSubmit={(e) => {
                e.preventDefault();
                if (!form.name.trim() || !form.phone.trim()) {
                  setFormError("Укажите имя и телефон");
                  return;
                }
                if (modal === "create") void createMut.mutateAsync();
                else void updateMut.mutateAsync();
              }}
            >
              <label className="flex flex-col gap-1 text-sm">
                <span className="text-zinc-400">Имя</span>
                <input
                  required
                  value={form.name}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, name: e.target.value }))
                  }
                  className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm">
                <span className="text-zinc-400">Телефон</span>
                <input
                  required
                  type="tel"
                  value={form.phone}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, phone: e.target.value }))
                  }
                  className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm">
                <span className="text-zinc-400">
                  Telegram <span className="font-normal text-zinc-600">(необязательно)</span>
                </span>
                <input
                  placeholder="@username"
                  value={form.telegramUsername ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      telegramUsername: e.target.value,
                    }))
                  }
                  className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
                />
              </label>
              {formError ? (
                <p className="text-sm font-medium text-red-400">{formError}</p>
              ) : null}
              <div className="mt-2 flex flex-wrap gap-3">
                <Button
                  type="submit"
                  variant="primary"
                  className="rounded-[10px]"
                  disabled={busy}
                >
                  {busy ? "Сохранение…" : modal === "create" ? "Создать" : "Сохранить"}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  className="rounded-[10px]"
                  disabled={busy}
                  onClick={closeModal}
                >
                  Отмена
                </Button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}
