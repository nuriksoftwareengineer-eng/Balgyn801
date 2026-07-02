import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "@/app/auth-context";
import {
  adminListCoupons,
  adminCreateCoupon,
  adminUpdateCoupon,
  adminDeleteCoupon,
} from "@/shared/api/backend-api";
import type { CouponRequest, CouponResponse, DiscountType } from "@/shared/api/types";
import { Button } from "@/shared/ui/button";
import { AdminPagination, AdminSearchBar } from "@/shared/ui/admin-pagination";
import { useDebounce } from "@/shared/hooks/useDebounce";

function emptyForm(): CouponRequest {
  return { code: "", discountType: "PERCENTAGE", discountValue: 10, minOrderAmount: 0, maxUses: null, active: true, expiresAt: null };
}

function fmtDate(iso?: string | null) {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleDateString("ru-RU"); } catch { return iso; }
}

function TypeBadge({ type }: { type: DiscountType }) {
  return (
    <span className={`inline-flex items-center rounded px-2 py-0.5 text-[0.65rem] font-semibold uppercase tracking-wide ${type === "PERCENTAGE" ? "bg-blue-500/20 text-blue-300" : "bg-amber-500/20 text-amber-300"}`}>
      {type === "PERCENTAGE" ? "%" : "₸"}
    </span>
  );
}

function ActiveBadge({ active }: { active: boolean }) {
  return (
    <span className={`inline-flex items-center rounded px-2 py-0.5 text-[0.65rem] font-semibold uppercase tracking-wide ${active ? "bg-green-500/20 text-green-300" : "bg-zinc-700 text-zinc-400"}`}>
      {active ? "Активен" : "Неактивен"}
    </span>
  );
}

export function AdminCouponsPage() {
  const { token } = useAuth();
  const qc = useQueryClient();
  const [modal, setModal] = useState<"create" | "edit" | null>(null);
  const [form, setForm] = useState<CouponRequest>(emptyForm());
  const [editId, setEditId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const q = useQuery({
    queryKey: ["admin-coupons", debouncedSearch, page],
    queryFn: () => adminListCoupons(token!, { q: debouncedSearch, page, size: 20 }),
    enabled: !!token,
  });

  const invalidate = () => void qc.invalidateQueries({ queryKey: ["admin-coupons"] });

  const createMut = useMutation({
    mutationFn: () => adminCreateCoupon(token!, form),
    onSuccess: () => { setModal(null); setForm(emptyForm()); setFormError(null); invalidate(); },
    onError: (e: Error) => setFormError(e.message),
  });

  const updateMut = useMutation({
    mutationFn: () => adminUpdateCoupon(token!, editId!, form),
    onSuccess: () => { setModal(null); setForm(emptyForm()); setFormError(null); invalidate(); },
    onError: (e: Error) => setFormError(e.message),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => adminDeleteCoupon(token!, id),
    onSuccess: invalidate,
  });

  function openEdit(c: CouponResponse) {
    setEditId(c.id);
    setForm({
      code: c.code,
      discountType: c.discountType,
      discountValue: c.discountValue,
      minOrderAmount: c.minOrderAmount,
      maxUses: c.maxUses ?? null,
      active: c.active,
      expiresAt: c.expiresAt ?? null,
    });
    setFormError(null);
    setModal("edit");
  }

  function set<K extends keyof CouponRequest>(k: K, v: CouponRequest[K]) {
    setForm(f => ({ ...f, [k]: v }));
  }

  const data = q.data;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-white">Промокоды</h1>
        <Button variant="primary" onClick={() => { setForm(emptyForm()); setFormError(null); setModal("create"); }}>+ Создать</Button>
      </div>

      <div className="mb-4 flex items-center gap-3">
        <AdminSearchBar value={search} onChange={v => { setSearch(v); setPage(0); }} placeholder="Поиск по коду..." />
      </div>

      {q.isLoading && <p className="text-sm text-zinc-400">Загрузка...</p>}
      {q.isError && <p className="text-sm text-red-400">Ошибка загрузки</p>}

      {data && (
        <>
          <div className="overflow-x-auto rounded border border-white/10">
            <table className="w-full text-sm text-zinc-300">
              <thead className="border-b border-white/10 bg-zinc-800/60 text-[0.65rem] uppercase tracking-wide text-zinc-500">
                <tr>
                  <th className="px-4 py-3 text-left">Код</th>
                  <th className="px-4 py-3 text-left">Тип</th>
                  <th className="px-4 py-3 text-left">Скидка</th>
                  <th className="px-4 py-3 text-left">Мин. сумма</th>
                  <th className="px-4 py-3 text-left">Использований</th>
                  <th className="px-4 py-3 text-left">Истекает</th>
                  <th className="px-4 py-3 text-left">Статус</th>
                  <th className="px-4 py-3 text-right">Действия</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {data.content.map(c => (
                  <tr key={c.id} className="hover:bg-white/5 transition">
                    <td className="px-4 py-3 font-mono font-semibold text-white">{c.code}</td>
                    <td className="px-4 py-3"><TypeBadge type={c.discountType} /></td>
                    <td className="px-4 py-3">{c.discountType === "PERCENTAGE" ? `${c.discountValue}%` : `${c.discountValue} ₸`}</td>
                    <td className="px-4 py-3">{c.minOrderAmount > 0 ? `${c.minOrderAmount} ₸` : "—"}</td>
                    <td className="px-4 py-3">{c.usedCount}{c.maxUses ? `/${c.maxUses}` : ""}</td>
                    <td className="px-4 py-3 text-zinc-500">{fmtDate(c.expiresAt)}</td>
                    <td className="px-4 py-3"><ActiveBadge active={c.active} /></td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button className="text-xs text-zinc-400 hover:text-white transition" onClick={() => openEdit(c)}>Ред.</button>
                        <button
                          className="text-xs text-red-400 hover:text-red-300 transition"
                          onClick={() => { if (confirm(`Удалить промокод ${c.code}?`)) deleteMut.mutate(c.id); }}
                        >Удалить</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <AdminPagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} size={20} onPage={setPage} />
        </>
      )}

      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-lg border border-white/10 bg-zinc-900 p-6 shadow-2xl">
            <h2 className="mb-5 text-lg font-semibold text-white">{modal === "create" ? "Создать промокод" : "Редактировать"}</h2>
            <div className="flex flex-col gap-4">
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-300">Код *</label>
                <input
                  className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm font-mono uppercase text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                  value={form.code}
                  onChange={e => set("code", e.target.value.toUpperCase())}
                  placeholder="SUMMER20"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-300">Тип *</label>
                  <select
                    className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none"
                    value={form.discountType}
                    onChange={e => set("discountType", e.target.value as DiscountType)}
                  >
                    <option value="PERCENTAGE">Процент (%)</option>
                    <option value="FIXED">Фиксированная (₸)</option>
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-300">Значение *</label>
                  <input
                    type="number" min={0.01} step={0.01}
                    className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                    value={form.discountValue}
                    onChange={e => set("discountValue", Number(e.target.value))}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-300">Мин. сумма (₸)</label>
                  <input
                    type="number" min={0}
                    className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                    value={form.minOrderAmount ?? 0}
                    onChange={e => set("minOrderAmount", Number(e.target.value))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-zinc-300">Макс. использований</label>
                  <input
                    type="number" min={1}
                    className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                    value={form.maxUses ?? ""}
                    onChange={e => set("maxUses", e.target.value ? Number(e.target.value) : null)}
                    placeholder="Не ограничено"
                  />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-zinc-300">Истекает</label>
                <input
                  type="datetime-local"
                  className="w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-white/30"
                  value={form.expiresAt ? form.expiresAt.slice(0, 16) : ""}
                  onChange={e => set("expiresAt", e.target.value ? `${e.target.value}:00` : null)}
                />
              </div>
              <label className="flex cursor-pointer items-center gap-2 text-sm text-zinc-300">
                <input type="checkbox" checked={form.active} onChange={e => set("active", e.target.checked)} className="h-4 w-4" />
                Активен
              </label>
              {formError && <p className="text-xs text-red-400">{formError}</p>}
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
