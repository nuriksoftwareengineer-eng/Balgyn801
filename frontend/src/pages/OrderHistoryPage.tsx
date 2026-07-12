import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "@/app/auth-context";
import { useCurrency } from "@/app/currency-context";
import { useCart } from "@/app/use-cart";
import { makeCartLineKey, makeDesignLineKey } from "@/app/cart-context";
import { getMyOrders, getMyOrderTrackingInfo } from "@/shared/api/backend-api";
import type { OrderItemResponse, OrderResponse, OrderStatus, OrderTrackingResponse } from "@/shared/api/types";
import { Container } from "@/shared/ui/container";
import { garmentLabel } from "@/shared/types/catalog";
import { OrderTrackingPanel } from "@/shared/ui/OrderTrackingPanel";

// Status → Tailwind pill classes
const STATUS_PILL: Record<OrderStatus, string> = {
  NEW:             "bg-blue-50  text-blue-700  border-blue-200",
  CONFIRMED:       "bg-amber-50 text-amber-700 border-amber-200",
  IN_PRODUCTION:   "bg-zinc-100 text-zinc-600  border-zinc-200",
  READY:           "bg-emerald-50 text-emerald-700 border-emerald-200",
  SHIPPED:         "bg-sky-50   text-sky-700   border-sky-200",
  DELIVERED:       "bg-green-50 text-green-700 border-green-200",
  CANCELLED:       "bg-red-50   text-red-600   border-red-200",
  PENDING_PAYMENT: "bg-yellow-50 text-yellow-700 border-yellow-200",
  EXPIRED:         "bg-gray-100 text-gray-500  border-gray-200",
};

// Status → dot color for compact view
const STATUS_DOT: Record<OrderStatus, string> = {
  NEW:             "bg-blue-500",
  CONFIRMED:       "bg-amber-500",
  IN_PRODUCTION:   "bg-zinc-400",
  READY:           "bg-emerald-500",
  SHIPPED:         "bg-sky-500",
  DELIVERED:       "bg-green-600",
  CANCELLED:       "bg-red-500",
  PENDING_PAYMENT: "bg-yellow-400",
  EXPIRED:         "bg-gray-300",
};

const LANG_LOCALE: Record<string, string> = {
  ru: "ru-RU",
  kk: "kk-KZ",
  en: "en-US",
};

function fmtDate(iso: string, lang: string) {
  const locale = LANG_LOCALE[lang] ?? "ru-RU";
  return new Date(iso).toLocaleDateString(locale, {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

function ItemPlaceholder() {
  return (
    <div className="flex h-10 w-10 shrink-0 items-center justify-center bg-[--color-surface] text-[18px]">
      🧵
    </div>
  );
}

function OrderSkeleton() {
  return (
    <div className="flex flex-col gap-3">
      {[0, 1, 2].map((i) => (
        <div key={i} className="border border-[--color-border] bg-white p-5 md:p-6">
          <div className="mb-4 flex items-start justify-between gap-3">
            <div className="flex gap-3">
              <div className="h-4 w-16 animate-pulse rounded bg-[--color-surface]" />
              <div className="h-4 w-24 animate-pulse rounded bg-[--color-surface]" />
            </div>
            <div className="h-4 w-20 animate-pulse rounded bg-[--color-surface]" />
          </div>
          <div className="flex flex-col gap-2">
            <div className="h-3 w-3/4 animate-pulse rounded bg-[--color-surface]" />
            <div className="h-3 w-1/2 animate-pulse rounded bg-[--color-surface]" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function OrderHistoryPage() {
  const { t, i18n } = useTranslation();
  const { token } = useAuth();
  const { format } = useCurrency();
  const navigate = useNavigate();
  const { addDesignItem, addItem, increment } = useCart();

  const [openTracking, setOpenTracking] = useState<Record<number, boolean>>({});
  const [trackingData, setTrackingData] = useState<Record<number, OrderTrackingResponse>>({});
  const [trackingLoading, setTrackingLoading] = useState<Record<number, boolean>>({});

  const { data: orders, isLoading, error } = useQuery<OrderResponse[], Error>({
    queryKey: ["my-orders", token],
    queryFn: () => getMyOrders(token!),
    enabled: !!token,
    staleTime: 30_000,
  });

  async function loadTracking(orderId: number) {
    if (trackingData[orderId]) {
      setOpenTracking((prev) => ({ ...prev, [orderId]: !prev[orderId] }));
      return;
    }
    setTrackingLoading((prev) => ({ ...prev, [orderId]: true }));
    try {
      const result = await getMyOrderTrackingInfo(orderId, token!);
      setTrackingData((prev) => ({ ...prev, [orderId]: result }));
      setOpenTracking((prev) => ({ ...prev, [orderId]: true }));
    } catch {
      // silently ignore; existing order info still shows
    } finally {
      setTrackingLoading((prev) => ({ ...prev, [orderId]: false }));
    }
  }

  function handlePayOrder(order: OrderResponse) {
    navigate("/cart", {
      state: { retryOrderId: order.id, retryAmount: order.totalPrice },
    });
  }

  function handleRepeatOrder(order: OrderResponse) {
    let added = 0;
    (order.items ?? []).forEach((item: OrderItemResponse) => {
      const qty = item.quantity > 0 ? item.quantity : 1;
      if (
        item.designGarmentId != null &&
        item.colorId != null &&
        item.sizeId != null &&
        item.designSlug &&
        item.groupSlug &&
        item.collectionSlug
      ) {
        addDesignItem({
          designGarmentId: item.designGarmentId,
          designId: item.designGarmentId,
          designSlug: item.designSlug,
          groupSlug: item.groupSlug,
          collectionSlug: item.collectionSlug,
          title: item.designName ?? "",
          garmentType: item.garmentType ?? "",
          garmentLabel: garmentLabel(
            { garmentType: item.garmentType ?? "", garmentTypeRu: item.garmentTypeRu, garmentTypeKk: item.garmentTypeKk },
            i18n.language,
          ),
          price: item.unitPrice,
          imageUrl: item.mainImageUrl,
          colorId: item.colorId,
          colorName: item.colorName ?? "",
          colorHex: item.colorHex ?? "#000000",
          sizeId: item.sizeId,
          sizeLabel: item.sizeLabel ?? "",
        });
        const lineKey = makeDesignLineKey(item.designGarmentId, item.colorId, item.sizeId);
        for (let i = 1; i < qty; i += 1) increment(lineKey);
        added += 1;
      } else if (item.productId != null) {
        addItem(
          {
            id: item.productId,
            title: item.productTitle ?? "",
            price: item.unitPrice,
            imageUrl: item.mainImageUrl,
            inStock: true,
          },
          { size: item.sizeLabel, color: item.colorName },
        );
        const lineKey = makeCartLineKey(item.productId, item.sizeLabel, item.colorName);
        for (let i = 1; i < qty; i += 1) increment(lineKey);
        added += 1;
      }
    });
    if (added > 0) {
      navigate("/cart");
    }
  }

  return (
    <>
      {/* Header */}
      <Container className="pb-4 pt-12 md:pt-16">
        <p className="mb-2 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
          {t("nav.profile")}
        </p>
        <h1 className="display text-[40px] uppercase text-black md:text-[56px]">
          {t("orders.title")}
        </h1>
      </Container>

      <Container className="py-8 md:py-10">
        {error ? (
          <p className="text-[14px] text-[--color-danger]">{error.message}</p>
        ) : isLoading ? (
          <OrderSkeleton />
        ) : !orders || orders.length === 0 ? (
          /* Empty state */
          <div className="flex flex-col items-start gap-5 py-8">
            <div className="flex h-14 w-14 items-center justify-center bg-[--color-surface] text-2xl">
              🧵
            </div>
            <div>
              <p className="mb-1 text-[16px] font-semibold text-black">{t("orders.empty")}</p>
              <p className="text-[14px] text-[--color-muted]">
                {t("orders.emptySubtitle")}
              </p>
            </div>
            <Link
              to="/catalog"
              className="inline-block bg-black px-6 py-3 text-[12px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
            >
              {t("orders.toCatalog")}
            </Link>
          </div>
        ) : (
          <ul className="flex flex-col gap-4">
            {orders.map((order) => (
              <li
                key={order.id}
                className="border border-[--color-border] bg-white"
              >
                {/* ── Card header ── */}
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[--color-border] px-5 py-5 md:px-7">
                  <div className="flex items-center gap-4">
                    <span className="text-[13px] font-medium tabular-nums text-black">
                      {t("orders.order")} №{order.id}
                    </span>
                    {order.status && (
                      <span
                        className={`inline-flex items-center gap-1.5 border px-2.5 py-1 text-[10px] font-medium uppercase tracking-[0.12em] ${STATUS_PILL[order.status]}`}
                      >
                        <span className={`h-1.5 w-1.5 rounded-full ${STATUS_DOT[order.status]}`} />
                        {t(`orders.status.${order.status}`)}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-5">
                    <span className="text-[12px] text-[--color-muted]">
                      {fmtDate(order.createdAt, i18n.language)}
                    </span>
                    <span className="text-[15px] font-semibold tabular-nums text-black">
                      {format(order.totalPrice)}
                    </span>
                    {order.status === "PENDING_PAYMENT" && (
                      <button
                        type="button"
                        onClick={() => handlePayOrder(order)}
                        className="inline-flex items-center justify-center bg-black px-4 py-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
                      >
                        {t("orders.payNow")}
                      </button>
                    )}
                  </div>
                </div>

                {/* ── Items ── */}
                {order.items && order.items.length > 0 && (
                  <ul className="divide-y divide-[--color-border] px-5 md:px-6">
                    {order.items.map((item) => (
                      <li key={item.id} className="flex items-center gap-3 py-3">
                        <ItemPlaceholder />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-[13px] font-medium text-black">
                            {item.productTitle}
                          </p>
                          <p className="text-[11px] text-[--color-muted]">
                            {[item.sizeLabel, item.colorName].filter(Boolean).join(" · ")}
                          </p>
                        </div>
                        <div className="shrink-0 text-right">
                          <p className="text-[13px] font-semibold text-black">
                            {format(item.unitPrice)}
                          </p>
                          <p className="text-[11px] text-[--color-muted]">
                            × {item.quantity}
                          </p>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}

                {/* ── CDEK tracking summary ── */}
                {order.cdekShipment && order.cdekShipment.cdekOrderUuid && (() => {
                  const tn = order.cdekShipment!.trackingNumber || order.trackingNumber;
                  return (
                    <div className="border-t border-[--color-border] px-5 py-3 md:px-6">
                      <p className="mb-2 text-[11px] font-bold uppercase tracking-[0.1em] text-[--color-muted]">
                        {t("orders.cdek.title")}
                      </p>
                      <div className="flex flex-wrap items-center gap-x-6 gap-y-1">
                        {tn && (
                          <span className="text-[12px] text-black">
                            <span className="text-[--color-muted]">{t("tracking.trackNumber")}: </span>
                            <span className="font-mono font-semibold">{tn}</span>
                          </span>
                        )}
                        {order.cdekShipment!.status && (
                          <span className="text-[12px] text-black">
                            <span className="text-[--color-muted]">{t("orders.cdek.status")}: </span>
                            {t(`orders.cdek.status_${order.cdekShipment!.status}`, order.cdekShipment!.status)}
                          </span>
                        )}
                        {order.cdekShipment!.estimatedDeliveryDate && (
                          <span className="text-[12px] text-black">
                            <span className="text-[--color-muted]">{t("orders.cdek.eta")}: </span>
                            {order.cdekShipment!.estimatedDeliveryDate}
                          </span>
                        )}
                        {order.cdekShipment!.deliveryPointAddress && (
                          <span className="text-[12px] text-black">
                            <span className="text-[--color-muted]">{t("orders.cdek.point")}: </span>
                            {order.cdekShipment!.deliveryPointAddress}
                          </span>
                        )}
                      </div>
                      {tn && (
                        <div className="mt-2">
                          <a
                            href={`https://www.cdek.ru/ru/tracking/?order_id=${encodeURIComponent(tn)}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1 border border-black bg-black px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.1em] text-white transition-colors hover:bg-zinc-800"
                          >
                            {t("tracking.trackBtn")} →
                          </a>
                        </div>
                      )}
                      {(order.cdekShipment!.invoiceUrl || order.cdekShipment!.barcodeUrl) && (
                        <div className="mt-2 flex gap-4">
                          {order.cdekShipment!.invoiceUrl && (
                            <a
                              href={order.cdekShipment!.invoiceUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[11px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-2 hover:text-zinc-600"
                            >
                              {t("orders.cdek.invoice")}
                            </a>
                          )}
                          {order.cdekShipment!.barcodeUrl && (
                            <a
                              href={order.cdekShipment!.barcodeUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[11px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-2 hover:text-zinc-600"
                            >
                              {t("orders.cdek.barcode")}
                            </a>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })()}

                {/* ── KazPost / POSTAL tracking summary ── */}
                {order.deliveryType === "POSTAL" && order.trackingNumber && (
                  <div className="border-t border-[--color-border] px-5 py-3 md:px-6">
                    <p className="mb-2 text-[11px] font-bold uppercase tracking-[0.1em] text-[--color-muted]">
                      {t("tracking.carrierKazpost")}
                    </p>
                    <div className="flex flex-wrap items-center gap-3">
                      <span className="font-mono text-[13px] font-semibold text-black">
                        {order.trackingNumber}
                      </span>
                      <a
                        href="https://track.kazpost.kz/"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 border border-black bg-black px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.1em] text-white transition-colors hover:bg-zinc-800"
                      >
                        {t("tracking.trackBtn")} →
                      </a>
                    </div>
                  </div>
                )}

                {/* ── Status history (lazy-loaded) ── */}
                {openTracking[order.id] && trackingData[order.id] && (
                  <div className="border-t border-[--color-border] px-5 py-4 md:px-6">
                    <OrderTrackingPanel
                      tracking={trackingData[order.id]}
                      showDocs={true}
                    />
                  </div>
                )}

                {/* ── Footer ── */}
                <div className="flex flex-wrap items-center justify-between gap-x-5 gap-y-2 border-t border-[--color-border] bg-[--color-surface] px-5 py-3 md:px-6">
                  <div className="flex flex-wrap items-center gap-x-5 gap-y-1">
                    <span className="text-[11px] uppercase tracking-[0.1em] text-[--color-muted]">
                      {t(`orders.delivery_type.${order.deliveryType}`, order.deliveryType)}
                    </span>
                    {order.deliveryFee != null && order.deliveryFee > 0 && (
                      <span className="text-[11px] text-[--color-muted]">
                        {t("orders.delivery")}: {format(order.deliveryFee)}
                      </span>
                    )}
                    {order.couponCode && (
                      <span className="inline-flex items-center gap-1 text-[11px] font-mono text-emerald-600">
                        🏷 {order.couponCode}
                        {order.discountAmount != null && order.discountAmount > 0 && (
                          <span>(-{format(order.discountAmount)})</span>
                        )}
                      </span>
                    )}
                    {order.comment && (
                      <span className="text-[11px] italic text-[--color-muted]">
                        "{order.comment}"
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-4">
                    <button
                      type="button"
                      onClick={() => loadTracking(order.id)}
                      disabled={trackingLoading[order.id]}
                      className="text-[11px] font-bold uppercase tracking-[0.1em] text-black underline-offset-2 hover:underline disabled:opacity-40"
                    >
                      {trackingLoading[order.id]
                        ? "..."
                        : openTracking[order.id]
                        ? t("orders.hideHistory")
                        : t("orders.showHistory")}
                    </button>
                    {order.items && order.items.length > 0 && (
                      <button
                        type="button"
                        onClick={() => handleRepeatOrder(order)}
                        className="text-[11px] font-bold uppercase tracking-[0.1em] text-black underline-offset-2 hover:underline"
                      >
                        {t("orders.repeatOrder")}
                      </button>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Container>
    </>
  );
}
