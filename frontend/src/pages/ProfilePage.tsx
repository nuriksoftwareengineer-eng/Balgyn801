import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import { getMyOrders } from "@/shared/api/backend-api";
import { Container } from "@/shared/ui/container";
import type { OrderResponse } from "@/shared/api/types";

export function ProfilePage() {
  const { t } = useTranslation();
  const { user, logout, token } = useAuth();

  const { data: orders } = useQuery<OrderResponse[], Error>({
    queryKey: ["my-orders-profile", token],
    queryFn: () => getMyOrders(token!),
    enabled: !!token,
    staleTime: 60_000,
  });

  if (!user) return null;

  const isAdmin = user.roles?.includes("ADMIN");

  const cdekShipments = (orders ?? [])
    .filter((o) => o.cdekShipment?.cdekOrderUuid)
    .slice(0, 5);

  return (
    <>
      {/* Header */}
      <Container className="pb-4 pt-12 md:pt-16">
        <p className="mb-2 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
          {t("nav.profile")}
        </p>
        <h1 className="display text-[40px] uppercase text-black md:text-[56px]">
          {t("profile.title")}
        </h1>
      </Container>

      <Container className="py-8 md:py-10">
        <div className="max-w-[560px]">
          {/* User info */}
          <dl className="flex flex-col gap-6 border-t border-[--color-border] pt-8">
            <div>
              <dt className="mb-1.5 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
                Email
              </dt>
              <dd className="text-[15px] text-black">{user.email}</dd>
            </div>
          </dl>

          {/* Actions */}
          <div className="mt-8 flex flex-col gap-4 border-b border-[--color-border] pb-8">
            <Link
              to="/orders"
              className="text-[13px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-4 transition-opacity hover:opacity-60"
            >
              {t("profile.myOrders")}
            </Link>
            {isAdmin && (
              <Link
                to="/admin"
                className="text-[13px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-4 transition-opacity hover:opacity-60"
              >
                {t("profile.adminPanel")}
              </Link>
            )}
            <button
              type="button"
              onClick={logout}
              className="w-fit text-[13px] font-semibold uppercase tracking-[0.1em] text-[--color-muted] underline underline-offset-4 transition-colors hover:text-black"
            >
              {t("profile.logout")}
            </button>
          </div>

          {/* CDEK section */}
          <div className="mt-8">
            <p className="mb-4 text-[11px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
              {t("profile.cdek.title")}
            </p>

            {cdekShipments.length === 0 ? (
              <p className="text-[13px] text-[--color-muted]">
                {t("profile.cdek.noShipments")}
              </p>
            ) : (
              <ul className="flex flex-col divide-y divide-[--color-border] border border-[--color-border]">
                {cdekShipments.map((order) => {
                  const s = order.cdekShipment!;
                  const tn = s.trackingNumber || order.trackingNumber;
                  return (
                    <li key={order.id} className="px-4 py-3">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-[12px] font-semibold text-black">
                          #{order.id}
                        </span>
                        {tn && (
                          <a
                            href={`https://www.cdek.ru/ru/tracking/?order_id=${encodeURIComponent(tn)}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-mono text-[11px] font-semibold underline underline-offset-2 hover:text-zinc-600"
                          >
                            {tn}
                          </a>
                        )}
                      </div>
                      {s.status && (
                        <p className="mt-1 text-[11px] text-[--color-muted]">
                          {t(`orders.cdek.status_${s.status}`, s.status)}
                        </p>
                      )}
                      {(s.invoiceUrl || s.barcodeUrl) && (
                        <div className="mt-2 flex gap-3">
                          {s.invoiceUrl && (
                            <a
                              href={s.invoiceUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[10px] font-semibold uppercase tracking-[0.1em] underline underline-offset-2 hover:text-zinc-600"
                            >
                              {t("orders.cdek.openLabel")}
                            </a>
                          )}
                          {s.barcodeUrl && (
                            <a
                              href={s.barcodeUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[10px] font-semibold uppercase tracking-[0.1em] underline underline-offset-2 hover:text-zinc-600"
                            >
                              {t("orders.cdek.barcode")}
                            </a>
                          )}
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}

            {cdekShipments.length > 0 && (
              <Link
                to="/orders"
                className="mt-4 inline-block text-[11px] font-semibold uppercase tracking-[0.1em] text-[--color-muted] underline underline-offset-2 transition-colors hover:text-black"
              >
                {t("profile.cdek.viewAll")}
              </Link>
            )}
          </div>
        </div>
      </Container>
    </>
  );
}
