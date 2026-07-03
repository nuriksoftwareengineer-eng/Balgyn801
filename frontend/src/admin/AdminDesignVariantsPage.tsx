import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import {
  createColor,
  createGarment,
  createSize,
  deleteGarment,
  listColors,
  listDesigns,
  listGarmentProfiles,
  listGarments,
  listInventory,
  listSizes,
  setInventory,
  updateGarment,
  upsertPrice,
  type AdminGarment,
} from "@/shared/api/admin-catalog";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

const inputClass =
  "rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40";

export function AdminDesignVariantsPage() {
  const { designId: designIdParam } = useParams();
  const designId = Number(designIdParam);
  const { token } = useAuth();
  const qc = useQueryClient();

  const { data: designs = [] } = useQuery({
    queryKey: ["admin", "designs"],
    queryFn: () => listDesigns(token!),
    enabled: !!token,
  });
  const design = designs.find((d) => d.id === designId);

  const { data: garments = [], isLoading } = useQuery({
    queryKey: ["admin", "garments", designId],
    queryFn: () => listGarments(designId, token!),
    enabled: !!token && Number.isFinite(designId),
  });

  const { data: profiles = [] } = useQuery({
    queryKey: ["admin", "garment-profiles"],
    queryFn: () => listGarmentProfiles(token!),
    enabled: !!token,
  });

  const [newProfileId, setNewProfileId] = useState<number | "">("");
  const [error, setError] = useState<string | null>(null);

  const usedProfileIds = new Set(garments.map((g) => g.garmentProfileId));
  const availableProfiles = profiles.filter((p) => !usedProfileIds.has(p.id));

  const addMut = useMutation({
    mutationFn: () => createGarment({ designId, garmentProfileId: newProfileId as number }, token!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "garments", designId] });
      setNewProfileId("");
      setError(null);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "Не удалось создать вариант"),
  });

  return (
    <div>
      <Link to="/admin/designs" className="text-xs text-zinc-500 hover:text-zinc-300">
        ← Дизайны
      </Link>
      <h1 className="mt-2 mb-1 text-2xl font-bold text-zinc-100">
        {design ? design.name : `Дизайн #${designId}`} — варианты
      </h1>
      {design && (
        <p className="mb-6 text-sm text-zinc-500">
          {design.groupName} / {design.collectionName}
          {design.status === "PUBLISHED" && (
            <>
              {" "}·{" "}
              <Link
                to={`/catalog/${design.groupSlug}/${design.collectionSlug}/${design.slug}`}
                target="_blank"
                className="text-sky-400 hover:text-sky-300"
              >
                открыть на витрине ↗
              </Link>
            </>
          )}
        </p>
      )}

      {/* Add variant */}
      <div className="mb-8 flex flex-wrap items-end gap-3 rounded-lg border border-white/10 bg-zinc-900/60 p-5">
        <label className="flex flex-col gap-1.5">
          <span className="text-xs text-zinc-400">Новый вариант</span>
          <select
            value={newProfileId}
            onChange={(e) => setNewProfileId(e.target.value ? Number(e.target.value) : "")}
            disabled={availableProfiles.length === 0}
            className={inputClass}
          >
            <option value="">{availableProfiles.length ? "— тип изделия —" : "все типы добавлены"}</option>
            {availableProfiles.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </label>
        <Button type="button" disabled={!newProfileId || addMut.isPending} onClick={() => addMut.mutate()}>
          {addMut.isPending ? "Создаём…" : "Добавить вариант"}
        </Button>
        {error && <p className="w-full text-xs text-red-400">{error}</p>}
      </div>

      {/* Variants */}
      {isLoading ? (
        <p className="text-sm text-zinc-500">Загружаем…</p>
      ) : garments.length === 0 ? (
        <p className="text-sm text-zinc-500">Вариантов пока нет — добавьте первый выше.</p>
      ) : (
        <div className="flex flex-col gap-5">
          {garments.map((g) => (
            <GarmentCard key={g.id} garment={g} designId={designId} />
          ))}
        </div>
      )}

      <DictionarySection />
    </div>
  );
}

// ─── Global colors / sizes dictionary (quick-add) ─────────────────────────────

function DictionarySection() {
  const { token } = useAuth();
  const qc = useQueryClient();
  const { data: colors = [] } = useQuery({
    queryKey: ["admin", "colors"],
    queryFn: () => listColors(token!),
    enabled: !!token,
  });
  const { data: sizes = [] } = useQuery({
    queryKey: ["admin", "sizes"],
    queryFn: () => listSizes(token!),
    enabled: !!token,
  });

  const [colorName, setColorName] = useState("");
  const [colorHex, setColorHex] = useState("#000000");
  const [sizeLabel, setSizeLabel] = useState("");
  const [err, setErr] = useState<string | null>(null);

  const addColor = useMutation({
    mutationFn: () => createColor({ name: colorName.trim(), hexCode: colorHex }, token!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "colors"] });
      setColorName("");
      setErr(null);
    },
    onError: (e) => setErr(e instanceof ApiError ? e.message : "Ошибка добавления цвета"),
  });
  const addSize = useMutation({
    mutationFn: () => createSize({ label: sizeLabel.trim() }, token!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "sizes"] });
      setSizeLabel("");
      setErr(null);
    },
    onError: (e) => setErr(e instanceof ApiError ? e.message : "Ошибка добавления размера"),
  });

  return (
    <div className="mt-10 border-t border-white/10 pt-6">
      <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-zinc-500">
        Справочник: цвета и размеры
      </h2>
      <div className="grid gap-6 sm:grid-cols-2">
        <div>
          <div className="mb-2 flex flex-wrap gap-1.5">
            {colors.map((c) => (
              <span key={c.id} className="flex items-center gap-1.5 rounded border border-white/10 px-2 py-1 text-xs text-zinc-300">
                <span className="inline-block h-3 w-3 rounded-full border border-white/20" style={{ backgroundColor: c.hexCode || "#888" }} />
                {c.name}
              </span>
            ))}
            {colors.length === 0 && <span className="text-xs text-zinc-600">Цветов нет</span>}
          </div>
          <div className="flex items-end gap-2">
            <input value={colorName} onChange={(e) => setColorName(e.target.value)} placeholder="Чёрный" className={`${inputClass} flex-1`} />
            <input type="color" value={colorHex} onChange={(e) => setColorHex(e.target.value)} className="h-9 w-10 rounded border border-white/20 bg-zinc-800" />
            <Button type="button" disabled={!colorName.trim() || addColor.isPending} onClick={() => addColor.mutate()}>+ цвет</Button>
          </div>
        </div>
        <div>
          <div className="mb-2 flex flex-wrap gap-1.5">
            {sizes.map((s) => (
              <span key={s.id} className="rounded border border-white/10 px-2 py-1 text-xs text-zinc-300">{s.label}</span>
            ))}
            {sizes.length === 0 && <span className="text-xs text-zinc-600">Размеров нет</span>}
          </div>
          <div className="flex items-end gap-2">
            <input value={sizeLabel} onChange={(e) => setSizeLabel(e.target.value)} placeholder="XL" className={`${inputClass} flex-1`} />
            <Button type="button" disabled={!sizeLabel.trim() || addSize.isPending} onClick={() => addSize.mutate()}>+ размер</Button>
          </div>
        </div>
      </div>
      {err && <p className="mt-3 text-xs text-red-400">{err}</p>}
    </div>
  );
}

// ─── One garment variant ──────────────────────────────────────────────────────

function GarmentCard({ garment, designId }: { garment: AdminGarment; designId: number }) {
  const { token } = useAuth();
  const qc = useQueryClient();
  const [cardError, setCardError] = useState<string | null>(null);

  const { data: colors = [] } = useQuery({
    queryKey: ["admin", "colors"],
    queryFn: () => listColors(token!),
    enabled: !!token,
  });
  const { data: sizes = [] } = useQuery({
    queryKey: ["admin", "sizes"],
    queryFn: () => listSizes(token!),
    enabled: !!token,
  });
  const { data: inventory = [] } = useQuery({
    queryKey: ["admin", "inventory", garment.id],
    queryFn: () => listInventory(garment.id, token!),
    enabled: !!token,
  });

  // Local selections, seeded from the saved garment assignments.
  const [selColorIds, setSelColorIds] = useState<number[]>(garment.colors.map((c) => c.id));
  const [selSizeIds, setSelSizeIds] = useState<number[]>(garment.sizes.map((s) => s.id));
  const kztPrice = garment.prices.find((p) => p.currency === "KZT");
  const [price, setPrice] = useState<string>(kztPrice ? String(kztPrice.amount) : "");

  function invalidate() {
    qc.invalidateQueries({ queryKey: ["admin", "garments", designId] });
    qc.invalidateQueries({ queryKey: ["admin", "inventory", garment.id] });
  }

  const updateMut = useMutation({
    mutationFn: (active: boolean) =>
      updateGarment(garment.id, { active, colorIds: selColorIds, sizeIds: selSizeIds }, token!),
    onSuccess: invalidate,
    onError: (e) => setCardError(e instanceof ApiError ? e.message : "Ошибка сохранения варианта"),
  });

  const priceMut = useMutation({
    mutationFn: () => {
      const amount = Number.parseFloat(price.replace(",", "."));
      if (!Number.isFinite(amount) || amount <= 0) throw new Error("Укажите цену > 0");
      return upsertPrice({ designGarmentId: garment.id, currency: "KZT", amount }, token!);
    },
    onSuccess: invalidate,
    onError: (e) => setCardError(e instanceof ApiError ? e.message : (e as Error).message),
  });

  const deleteMut = useMutation({
    mutationFn: () => deleteGarment(garment.id, token!),
    onSuccess: invalidate,
    onError: (e) => setCardError(e instanceof ApiError ? e.message : "Не удалось удалить"),
  });

  function toggle(list: number[], id: number): number[] {
    return list.includes(id) ? list.filter((x) => x !== id) : [...list, id];
  }

  return (
    <div className="rounded-lg border border-white/10 bg-zinc-900/60 p-5">
      {/* Header */}
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="rounded bg-white/10 px-2.5 py-1 text-sm font-semibold text-zinc-100">
            {garment.garmentType}
          </span>
          <button
            type="button"
            onClick={() => updateMut.mutate(!garment.active)}
            disabled={updateMut.isPending}
            className={`text-xs font-semibold ${garment.active ? "text-emerald-400" : "text-zinc-500"} hover:opacity-80`}
          >
            {garment.active ? "● активен" : "○ выключен"}
          </button>
        </div>
        <button
          type="button"
          onClick={() => {
            if (confirm("Удалить этот вариант?")) deleteMut.mutate();
          }}
          disabled={deleteMut.isPending}
          className="text-xs font-semibold text-red-400 hover:text-red-300 disabled:opacity-50"
        >
          Удалить
        </button>
      </div>

      {/* Price */}
      <div className="mb-4 flex items-end gap-3">
        <label className="flex flex-col gap-1.5">
          <span className="text-xs text-zinc-400">Цена, ₸ (KZT)</span>
          <input
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            inputMode="numeric"
            placeholder="12000"
            className={`${inputClass} w-36`}
          />
        </label>
        <Button type="button" disabled={priceMut.isPending} onClick={() => priceMut.mutate()}>
          {priceMut.isPending ? "…" : "Сохранить цену"}
        </Button>
      </div>

      {/* Sizes + colors */}
      <div className="mb-4 grid gap-4 sm:grid-cols-2">
        <div>
          <p className="mb-1.5 text-xs text-zinc-400">Размеры</p>
          {sizes.length === 0 ? (
            <p className="text-xs text-amber-400">Справочник размеров пуст — добавьте размеры (ниже).</p>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {sizes.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => setSelSizeIds((l) => toggle(l, s.id))}
                  className={`rounded border px-2.5 py-1 text-xs ${
                    selSizeIds.includes(s.id)
                      ? "border-white/40 bg-white/15 text-zinc-100"
                      : "border-white/10 text-zinc-400 hover:text-zinc-200"
                  }`}
                >
                  {s.label}
                </button>
              ))}
            </div>
          )}
        </div>
        <div>
          <p className="mb-1.5 text-xs text-zinc-400">Цвета</p>
          {colors.length === 0 ? (
            <p className="text-xs text-amber-400">Справочник цветов пуст — добавьте цвета (ниже).</p>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {colors.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => setSelColorIds((l) => toggle(l, c.id))}
                  className={`flex items-center gap-1.5 rounded border px-2.5 py-1 text-xs ${
                    selColorIds.includes(c.id)
                      ? "border-white/40 bg-white/15 text-zinc-100"
                      : "border-white/10 text-zinc-400 hover:text-zinc-200"
                  }`}
                >
                  <span
                    className="inline-block h-3 w-3 rounded-full border border-white/20"
                    style={{ backgroundColor: c.hexCode || "#888" }}
                  />
                  {c.name}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
      <Button
        type="button"
        disabled={updateMut.isPending}
        onClick={() => updateMut.mutate(garment.active)}
        className="mb-4"
      >
        {updateMut.isPending ? "Сохраняем…" : "Сохранить размеры и цвета"}
      </Button>

      {/* Inventory grid */}
      <InventoryGrid garment={garment} inventory={inventory} onSaved={invalidate} />

      {cardError && <p className="mt-3 text-xs text-red-400">{cardError}</p>}
    </div>
  );
}

// ─── Inventory matrix (saved colors × saved sizes) ────────────────────────────

function InventoryGrid({
  garment,
  inventory,
  onSaved,
}: {
  garment: AdminGarment;
  inventory: { colorId: number; sizeId: number; quantity: number }[];
  onSaved: () => void;
}) {
  const { token } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const initial = useMemo(() => {
    const map: Record<string, string> = {};
    for (const c of garment.colors) {
      for (const s of garment.sizes) {
        const found = inventory.find((i) => i.colorId === c.id && i.sizeId === s.id);
        map[`${c.id}:${s.id}`] = String(found?.quantity ?? 0);
      }
    }
    return map;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [garment.id, garment.colors, garment.sizes, inventory]);

  const [cells, setCells] = useState<Record<string, string>>(initial);
  // Re-seed when assignments/inventory change.
  const sig = `${garment.colors.map((c) => c.id).join(",")}|${garment.sizes.map((s) => s.id).join(",")}|${inventory.length}`;
  const [seenSig, setSeenSig] = useState(sig);
  if (sig !== seenSig) {
    setSeenSig(sig);
    setCells(initial);
  }

  const saveMut = useMutation({
    mutationFn: async () => {
      for (const c of garment.colors) {
        for (const s of garment.sizes) {
          const key = `${c.id}:${s.id}`;
          const qty = Number.parseInt(cells[key] ?? "0", 10) || 0;
          await setInventory(
            { designGarmentId: garment.id, colorId: c.id, sizeId: s.id, quantity: qty },
            token!,
          );
        }
      }
    },
    onSuccess: () => {
      setError(null);
      onSaved();
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "Ошибка сохранения остатков"),
  });

  if (garment.colors.length === 0 || garment.sizes.length === 0) {
    return (
      <p className="text-xs text-zinc-500">
        Остатки задаются после сохранения хотя бы одного цвета и размера.
      </p>
    );
  }

  return (
    <div>
      <p className="mb-1.5 text-xs text-zinc-400">Остатки (цвет × размер)</p>
      <div className="overflow-x-auto">
        <table className="text-xs">
          <thead>
            <tr>
              <th className="px-2 py-1 text-left text-zinc-500" />
              {garment.sizes.map((s) => (
                <th key={s.id} className="px-2 py-1 font-semibold text-zinc-400">{s.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {garment.colors.map((c) => (
              <tr key={c.id}>
                <td className="whitespace-nowrap px-2 py-1 text-zinc-300">{c.name}</td>
                {garment.sizes.map((s) => {
                  const key = `${c.id}:${s.id}`;
                  return (
                    <td key={s.id} className="px-1 py-1">
                      <input
                        value={cells[key] ?? "0"}
                        onChange={(e) =>
                          setCells((m) => ({ ...m, [key]: e.target.value.replace(/[^0-9]/g, "") }))
                        }
                        className="w-14 rounded border border-white/20 bg-zinc-800 px-2 py-1 text-center text-zinc-100 outline-none focus:border-white/40"
                      />
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Button type="button" disabled={saveMut.isPending} onClick={() => saveMut.mutate()} className="mt-3">
        {saveMut.isPending ? "Сохраняем…" : "Сохранить остатки"}
      </Button>
      {error && <p className="mt-2 text-xs text-red-400">{error}</p>}
    </div>
  );
}
