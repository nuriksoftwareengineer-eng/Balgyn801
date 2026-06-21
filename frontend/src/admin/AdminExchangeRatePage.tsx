import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import {
  getExchangeRate,
  setExchangeRate,
  refreshExchangeRate,
} from "@/shared/api/backend-api";

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

export function AdminExchangeRatePage() {
  const { token } = useAuth();
  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["exchange-rate-admin"],
    queryFn: () => getExchangeRate(token!),
    enabled: !!token,
  });

  const [rateInput, setRateInput] = useState("");
  const [freezeInput, setFreezeInput] = useState(false);
  const [setMsg, setSetMsg] = useState<string | null>(null);
  const [refreshMsg, setRefreshMsg] = useState<string | null>(null);

  const setMutation = useMutation({
    mutationFn: () =>
      setExchangeRate(
        { kztPerUsd: parseFloat(rateInput), frozen: freezeInput },
        token!,
      ),
    onSuccess: (res) => {
      void qc.invalidateQueries({ queryKey: ["exchange-rate-admin"] });
      setSetMsg(`Курс обновлён: ${res.kztPerUsd} KZT/USD`);
      setRateInput("");
    },
    onError: (e) => {
      setSetMsg(`Ошибка: ${e instanceof Error ? e.message : "неизвестно"}`);
    },
  });

  const refreshMutation = useMutation({
    mutationFn: () => refreshExchangeRate(token!),
    onSuccess: (res) => {
      void qc.invalidateQueries({ queryKey: ["exchange-rate-admin"] });
      setRefreshMsg(`Обновлено от провайдера: ${res.kztPerUsd} KZT/USD`);
    },
    onError: (e) => {
      setRefreshMsg(
        `Ошибка обновления: ${e instanceof Error ? e.message : "неизвестно"}`,
      );
    },
  });

  const inputClass =
    "w-full rounded-[8px] border border-white/15 bg-zinc-800 px-3 py-2.5 text-sm text-zinc-100 outline-none focus:border-white/40";
  const btnClass =
    "rounded-[8px] bg-white px-5 py-2.5 text-sm font-semibold text-black transition hover:bg-zinc-200 disabled:opacity-50";
  const secBtnClass =
    "rounded-[8px] border border-white/15 bg-zinc-800 px-5 py-2.5 text-sm font-semibold text-zinc-200 transition hover:bg-zinc-700 disabled:opacity-50";

  return (
    <div className="max-w-xl">
      <h1 className="font-display mb-6 text-2xl text-zinc-100">
        Курс валют (KZT / USD)
      </h1>

      {isLoading && <p className="text-sm text-zinc-400">Загрузка…</p>}
      {error && (
        <p className="text-sm text-red-400">
          Ошибка: {error instanceof Error ? error.message : "Неизвестно"}
        </p>
      )}

      {data && (
        <div className="mb-8 rounded-[12px] border border-white/10 bg-zinc-900/60 px-6 py-5">
          <div className="grid grid-cols-2 gap-y-3 text-sm">
            <span className="text-zinc-400">Текущий курс</span>
            <span className="font-semibold text-zinc-100">
              {data.kztPerUsd} KZT / 1 USD
            </span>
            <span className="text-zinc-400">Источник</span>
            <span className="text-zinc-300">{data.source}</span>
            <span className="text-zinc-400">Заморожен</span>
            <span className={data.frozen ? "text-amber-400" : "text-zinc-300"}>
              {data.frozen ? "Да" : "Нет"}
            </span>
            <span className="text-zinc-400">Обновлён</span>
            <span className="text-zinc-300">{fmt(data.updatedAt)}</span>
          </div>
        </div>
      )}

      <div className="mb-6 rounded-[12px] border border-white/10 bg-zinc-900/60 px-6 py-5">
        <p className="mb-4 text-sm font-semibold text-zinc-200">
          Установить курс вручную
        </p>
        <div className="mb-3 flex gap-3">
          <input
            type="number"
            step="0.01"
            min="1"
            placeholder="Например: 485.50"
            value={rateInput}
            onChange={(e) => {
              setRateInput(e.target.value);
              setSetMsg(null);
            }}
            className={inputClass}
          />
        </div>
        <label className="mb-4 flex cursor-pointer items-center gap-2 text-sm text-zinc-300">
          <input
            type="checkbox"
            checked={freezeInput}
            onChange={(e) => setFreezeInput(e.target.checked)}
            className="h-4 w-4 rounded"
          />
          Заморозить курс (отключить автообновление)
        </label>
        <button
          type="button"
          disabled={
            !rateInput ||
            isNaN(parseFloat(rateInput)) ||
            parseFloat(rateInput) <= 0 ||
            setMutation.isPending
          }
          onClick={() => setMutation.mutate()}
          className={btnClass}
        >
          {setMutation.isPending ? "Сохраняем…" : "Сохранить"}
        </button>
        {setMsg && (
          <p
            className={`mt-2 text-sm ${setMsg.startsWith("Ошибка") ? "text-red-400" : "text-emerald-400"}`}
          >
            {setMsg}
          </p>
        )}
      </div>

      <div className="rounded-[12px] border border-white/10 bg-zinc-900/60 px-6 py-5">
        <p className="mb-2 text-sm font-semibold text-zinc-200">
          Обновить от провайдера (НБК)
        </p>
        <p className="mb-4 text-xs text-zinc-500">
          Запрашивает актуальный курс от Национального банка Казахстана. Не
          работает, если курс заморожен.
        </p>
        <button
          type="button"
          disabled={refreshMutation.isPending}
          onClick={() => {
            setRefreshMsg(null);
            refreshMutation.mutate();
          }}
          className={secBtnClass}
        >
          {refreshMutation.isPending ? "Обновляем…" : "Обновить от НБК"}
        </button>
        {refreshMsg && (
          <p
            className={`mt-2 text-sm ${refreshMsg.startsWith("Ошибка") ? "text-red-400" : "text-emerald-400"}`}
          >
            {refreshMsg}
          </p>
        )}
      </div>
    </div>
  );
}
