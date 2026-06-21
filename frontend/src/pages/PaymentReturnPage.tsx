import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { capturePayPalOrder } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { Container } from "@/shared/ui/container";
import { loadLastPayment } from "@/shared/lib/pending-payment";

export function PaymentReturnPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const calledRef = useRef(false);

  // PayPal sets ?token=<paypalOrderId>&PayerID=<...>
  // Freedom Pay sets ?pg_order_id=...&pg_result=...&pg_payment_id=...
  const paypalToken = params.get("token");
  const fpResult    = params.get("pg_result");

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    if (paypalToken) {
      // PayPal flow: capture must be completed client-side
      capturePayPalOrder(paypalToken)
        .then((payment) => {
          if (payment.status === "SUCCEEDED") {
            navigate("/payment/success", { replace: true });
          } else {
            navigate(`/payment/failed?error=${encodeURIComponent(payment.status ?? "FAILED")}`, {
              replace: true,
            });
          }
        })
        .catch((err) => {
          const msg = err instanceof ApiError ? err.message : "FAILED";
          navigate(`/payment/failed?error=${encodeURIComponent(msg)}`, { replace: true });
        });
      return;
    }

    // Freedom Pay flow: server already processed the callback before redirect.
    // pg_result=1 → success, anything else → failed.
    if (fpResult !== null) {
      if (fpResult === "1") {
        navigate("/payment/success", { replace: true });
      } else {
        const last = loadLastPayment();
        const orderId = last?.orderId ?? params.get("pg_order_id") ?? "";
        navigate(
          `/payment/failed?error=${encodeURIComponent("PAYMENT_FAILED")}&orderId=${orderId}`,
          { replace: true },
        );
      }
      return;
    }

    // No recognised params — redirect to failed with a generic error
    navigate("/payment/failed?error=UNKNOWN_RETURN", { replace: true });
  }, [paypalToken, fpResult, navigate, params]);

  return (
    <div className="py-16">
      <Container className="max-w-xl">
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-[--color-border] border-t-black" />
          <p className="text-[15px] text-[--color-muted]">
            {t("payment.confirming", "Подтверждаем платёж…")}
          </p>
        </div>
      </Container>
    </div>
  );
}
