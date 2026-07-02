import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import {
  cancelOrderShipment,
  createOrderShipment,
  getOrderByIdAdmin,
  getOrderShipment,
  patchOrderStatusAdmin,
  syncOrderShipment,
} from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import type { CdekShipmentStatus, OrderStatus } from "@/shared/api/types";
import { deliveryTypeLabel } from "@/shared/lib/delivery-labels";
import { formatMoney } from "@/shared/lib/format-money";
import {
  ORDER_STATUS_VALUES,
  orderStatusLabel,
} from "@/shared/lib/order-status-labels";
import { Button } from "@/shared/ui/button";

function formatDate(iso: string | undefined) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("ru-RU", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

function resolveItemsTotal(totalPrice: number, deliveryFee?: number | null): number {
  const fee = deliveryFee ?? 0;
  return Math.max(0, totalPrice - fee);
}

const SHIPMENT_STATUS_LABEL: Record<CdekShipmentStatus, string> = {
  CREATED: "Создано",
  ACCEPTED: "Принято СДЭК",
  IN_TRANSIT: "В пути",
  ARRIVED: "Прибыло в ПВЗ",
  DELIVERED: "Доставлено",
  RETURNED: "Возврат",
  CANCELLED: "Отменено",
};

function shipmentStatusLabel(s?: CdekShipmentStatus | null): string {
  return s ? (SHIPMENT_STATUS_LABEL[s] ?? s) : "—";
}

function DetailRow({
  label,
  value,
  mono,
}: {
  label: string;
  value?: string | null;
  mono?: boolean;
}) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs uppercase tracking-wide text-zinc-600">{label}</dt>
      <dd
        className={
          "m-0 text-zinc-200" + (mono ? " break-all font-mono text-xs" : "")
        }
      >
        {value || "—"}
      </dd>
    </div>
  );
}

export function AdminOrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const id = Number.parseInt(orderId ?? "", 10);
  const { token } = useAuth();
  const queryClient = useQueryClient();
  /** Локальный выбор в селекте; `null` — показываем статус с сервера. */
  const [statusDraft, setStatusDraft] = useState<OrderStatus | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [shipmentError, setShipmentError] = useState<string | null>(null);

  const q = useQuery({
    queryKey: ["admin-order", id],
    queryFn: async () => {
      if (!token) throw new Error("Нет токена");
      if (!Number.isFinite(id) || id <= 0) throw new Error("Некорректный ID");
      return getOrderByIdAdmin(id, token);
    },
    enabled: !!token && Number.isFinite(id) && id > 0,
  });

  const statusMut = useMutation({
    mutationFn: async (status: OrderStatus) => {
      if (!token) throw new Error("Нет токена");
      return patchOrderStatusAdmin(id, { status }, token);
    },
    onMutate: () => setStatusError(null),
    onSuccess: () => {
      setStatusDraft(null);
      void queryClient.invalidateQueries({ queryKey: ["admin-order", id] });
      void queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
    },
    onError: (err: unknown) => {
      setStatusError(
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : "Не удалось сохранить статус",
      );
    },
  });

  const shipmentQ = useQuery({
    queryKey: ["admin-shipment", id],
    queryFn: async () => {
      if (!token) throw new Error("Нет токена");
      return getOrderShipment(id, token);
    },
    enabled: !!token && Number.isFinite(id) && id > 0,
  });

  const shipmentMut = useMutation({
    mutationFn: async (action: "create" | "sync" | "cancel") => {
      if (!token) throw new Error("Нет токена");
      if (action === "create") return createOrderShipment(id, token);
      if (action === "sync") return syncOrderShipment(id, token);
      return cancelOrderShipment(id, token);
    },
    onMutate: () => setShipmentError(null),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin-shipment", id] });
    },
    onError: (err: unknown) => {
      setShipmentError(
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : "Не удалось выполнить операцию с отправлением",
      );
    },
  });

  if (!Number.isFinite(id) || id <= 0) {
    return (
      <div>
        <p className="text-red-400">Некорректная ссылка на заказ.</p>
        <Link
          to="/admin/orders"
          className="mt-4 inline-block text-zinc-300 hover:text-white hover:underline"
        >
          ← К списку заказов
        </Link>
      </div>
    );
  }

  if (q.isPending) {
    return <p className="text-zinc-500">Загрузка…</p>;
  }

  if (q.isError) {
    return (
      <div>
        <p className="text-red-400">
          {q.error instanceof Error ? q.error.message : "Ошибка загрузки"}
        </p>
        <Link
          to="/admin/orders"
          className="mt-4 inline-block text-zinc-300 hover:text-white hover:underline"
        >
          ← К списку заказов
        </Link>
      </div>
    );
  }

  const o = q.data!;
  const current = o.status ?? "NEW";
  const selectedStatus = statusDraft ?? current;
  const statusDirty = statusDraft !== null && statusDraft !== current;
  const shipment = shipmentQ.data;

  return (
    <div>
      <Link
        to="/admin/orders"
        className="mb-6 inline-block text-sm font-semibold text-zinc-300 hover:text-white hover:underline"
      >
        ← Все заказы
      </Link>
      <h1 className="font-display mb-2 text-4xl tracking-wide text-zinc-100">
        Заказ №{o.id}
      </h1>
      <p className="mb-8 text-sm text-zinc-500">{formatDate(o.createdAt)}</p>

      <div className="mb-8 grid gap-6 md:grid-cols-2">
        <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Клиент
          </h2>
          <p className="text-zinc-100">{o.customerName}</p>
          <p className="mt-1 text-zinc-400">{o.customerPhone}</p>
          {o.comment ? (
            <p className="mt-3 text-sm text-zinc-500">
              <span className="font-medium text-zinc-400">Комментарий: </span>
              {o.comment}
            </p>
          ) : null}
        </section>
        <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Доставка и статус
          </h2>
          <p className="text-zinc-100">{deliveryTypeLabel(o.deliveryType)}</p>
          <p className="mt-2 text-sm text-zinc-400">
            Текущий статус:{" "}
            <span className="font-medium text-zinc-200">
              {orderStatusLabel(o.status)}
            </span>
          </p>
          <p className="mt-3 text-lg font-bold text-zinc-100">
            Товары: {formatMoney(resolveItemsTotal(o.totalPrice, o.deliveryFee))} ₸
          </p>
          {o.deliveryType === "CDEK" ? (
            <p className="mt-1 text-sm text-zinc-400">
              Доставка СДЭК: оплата при получении
            </p>
          ) : (
            <p className="mt-1 text-sm text-zinc-400">
              Доставка: {formatMoney(o.deliveryFee ?? 0)} ₸
            </p>
          )}
          <p className="mt-2 text-lg font-bold text-zinc-100">
            Итого: {formatMoney(o.totalPrice)} ₸
          </p>
        </section>
      </div>

      <section className="mb-8 rounded-[14px] border border-white/15 bg-white/[0.04] p-5">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-400">
          Смена статуса
        </h2>
        <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
          <label className="flex min-w-[200px] flex-1 flex-col gap-1.5 text-sm">
            <span className="font-medium text-zinc-500">Новый статус</span>
            <select
              value={selectedStatus}
              onChange={(e) => {
                const v = e.target.value as OrderStatus;
                setStatusDraft(v === current ? null : v);
              }}
              className="rounded-[10px] border border-white/15 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none focus:border-white/40 focus:ring-2 focus:ring-white/20"
            >
              {ORDER_STATUS_VALUES.map((s) => (
                <option key={s} value={s}>
                  {orderStatusLabel(s)}
                </option>
              ))}
            </select>
          </label>
          <Button
            type="button"
            variant="primary"
            className="rounded-[10px] sm:shrink-0"
            disabled={
              !statusDirty ||
              statusMut.isPending ||
              current === "CANCELLED" ||
              current === "DELIVERED"
            }
            onClick={() => statusMut.mutate(selectedStatus)}
          >
            {statusMut.isPending ? "Сохранение…" : "Сохранить статус"}
          </Button>
        </div>
        {current === "CANCELLED" || current === "DELIVERED" ? (
          <p className="mt-3 text-sm text-zinc-500">
            {current === "CANCELLED"
              ? "Отменённый заказ нельзя вернуть в работу через API."
              : "Доставленный заказ нельзя менять через API."}
          </p>
        ) : null}
        {statusError ? (
          <p className="mt-3 text-sm font-medium text-red-400" role="alert">
            {statusError}
          </p>
        ) : null}
      </section>

      {o.deliveryType === "CDEK" ? (
        <section className="mb-8 rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <div className="mb-3 flex items-center gap-3">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500">
              Отправление СДЭК
            </h2>
            {shipment?.mock ? (
              <span className="rounded bg-white/10 px-2 py-0.5 text-[0.6rem] font-semibold uppercase tracking-wide text-zinc-300">
                mock
              </span>
            ) : null}
          </div>

          {shipmentQ.isPending ? (
            <p className="text-zinc-500">Загрузка…</p>
          ) : shipment ? (
            <dl className="grid gap-3 text-sm sm:grid-cols-2">
              <DetailRow label="Статус" value={shipmentStatusLabel(shipment.status)} />
              <DetailRow
                label="Стоимость доставки"
                value={
                  shipment.deliveryPrice != null
                    ? `${formatMoney(shipment.deliveryPrice)} ₸`
                    : null
                }
              />
              <DetailRow label="UUID" value={shipment.cdekOrderUuid} mono />
              <DetailRow label="Трек-номер" value={shipment.trackingNumber} mono />
              <DetailRow label="Код ПВЗ" value={shipment.deliveryPointCode} />
              <DetailRow label="Адрес ПВЗ" value={shipment.deliveryPointAddress} />
            </dl>
          ) : (
            <p className="text-zinc-500">Отправление ещё не создано.</p>
          )}

          {shipment?.invoiceUrl || shipment?.barcodeUrl ? (
            <div className="mt-3 flex flex-wrap gap-4 text-sm">
              {shipment.invoiceUrl ? (
                <a
                  href={shipment.invoiceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-semibold text-zinc-300 underline-offset-2 hover:text-white hover:underline"
                >
                  Накладная
                </a>
              ) : null}
              {shipment.barcodeUrl ? (
                <a
                  href={shipment.barcodeUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-semibold text-zinc-300 underline-offset-2 hover:text-white hover:underline"
                >
                  Штрихкод
                </a>
              ) : null}
            </div>
          ) : null}

          <div className="mt-4 flex flex-wrap gap-3">
            <Button
              type="button"
              variant="primary"
              className="rounded-[10px]"
              disabled={shipmentMut.isPending}
              onClick={() => {
                const label = shipment ? "Повторить создание отправления CDEK?" : "Создать отправление CDEK? Это выставит счёт через CDEK API.";
                if (window.confirm(label)) shipmentMut.mutate("create");
              }}
            >
              {shipmentMut.isPending
                ? "Выполняется…"
                : shipment
                  ? "Повторить создание"
                  : "Создать отправление"}
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-[10px]"
              disabled={!shipment || shipmentMut.isPending}
              onClick={() => shipmentMut.mutate("sync")}
            >
              Синхронизировать статус
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-[10px]"
              disabled={
                !shipment ||
                shipmentMut.isPending ||
                shipment.status === "CANCELLED"
              }
              onClick={() => shipmentMut.mutate("cancel")}
            >
              Отменить отправление
            </Button>
          </div>

          {shipmentError ? (
            <p className="mt-3 text-sm font-medium text-red-400" role="alert">
              {shipmentError}
            </p>
          ) : null}
        </section>
      ) : null}

      {o.address ? (
        <section className="mb-8 rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Адрес
          </h2>
          <p className="text-zinc-200">
            {o.address.postalCode}, {o.address.city}, {o.address.street},{" "}
            {o.address.apartment}
          </p>
          <p className="mt-2 text-sm text-zinc-400">
            Получатель: {o.address.recipientName}, {o.address.recipientPhone}
          </p>
        </section>
      ) : null}

      <section className="rounded-[14px] border border-white/10 bg-zinc-900/40 p-5">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-zinc-500">
          Позиции
        </h2>
        {!o.items?.length ? (
          <p className="text-zinc-500">Позиции не загружены или заказ пустой.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[480px] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-white/10 text-xs uppercase text-zinc-500">
                  <th className="py-2 pr-4 font-semibold">Товар</th>
                  <th className="py-2 pr-4 font-semibold">Вариант</th>
                  <th className="py-2 pr-4 font-semibold">Кол-во</th>
                  <th className="py-2 pr-4 font-semibold">Цена</th>
                  <th className="py-2 font-semibold">Сумма</th>
                </tr>
              </thead>
              <tbody>
                {o.items.map((line) => (
                  <tr key={line.id} className="border-b border-white/5">
                    <td className="py-3 pr-4 text-zinc-100">
                      {line.productTitle ?? line.designName ?? "—"}
                    </td>
                    <td className="py-3 pr-4 text-sm text-zinc-500">
                      {[line.garmentType, line.sizeLabel, line.colorName].filter(Boolean).join(" · ") ||
                        "—"}
                    </td>
                    <td className="py-3 pr-4 tabular-nums text-zinc-400">
                      {line.quantity}
                    </td>
                    <td className="py-3 pr-4 tabular-nums text-zinc-400">
                      {formatMoney(line.unitPrice)} ₸
                    </td>
                    <td className="py-3 tabular-nums text-zinc-200">
                      {formatMoney(line.unitPrice * line.quantity)} ₸
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
