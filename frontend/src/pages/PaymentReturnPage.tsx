import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { capturePayment, verifyFreedomPayRedirect, verifyVtbReturn } from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import { Container } from "@/shared/ui/container";
import { loadLastPayment } from "@/shared/lib/pending-payment";

export function PaymentReturnPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const calledRef = useRef(false);

  // PayPal sets ?token=<paypalOrderId>&PayerID=<...>
  // FreedomPay sets ?pg_payment_id=...&pg_order_id=...  (pg_result may be absent in browser redirect)
  // VTB KZ sets ?orderId=<vtbInternalOrderId>
  const paypalToken = params.get("token");
  const fpPaymentId = params.get("pg_payment_id"); // always present in FP redirect
  const fpOrderId   = params.get("pg_order_id");   // always present in FP redirect
  const fpResult    = params.get("pg_result");      // present in callback, may be absent in redirect
  const vtbOrderId  = params.get("orderId");        // VTB KZ return param

  // FreedomPay redirect detected if ANY FP-specific param is in the URL
  const isFreedomPay = fpPaymentId !== null || fpOrderId !== null || fpResult !== null;
  // VTB: ?orderId= is present, but not PayPal token (?token=) and not FreedomPay params
  const isVtb = vtbOrderId !== null && !paypalToken && !isFreedomPay;

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    // Debug: log full URL and all params so mismatches are visible in browser console
    console.log("[PaymentReturn] URL:", window.location.href);
    console.log("[PaymentReturn] params:", Object.fromEntries(params.entries()));
    console.log("[PaymentReturn] paypalToken:", paypalToken,
      "| fpPaymentId:", fpPaymentId, "| fpOrderId:", fpOrderId, "| fpResult:", fpResult,
      "| vtbOrderId:", vtbOrderId, "| isFreedomPay:", isFreedomPay, "| isVtb:", isVtb);

    if (paypalToken) {
      // PayPal flow: capture must be completed client-side.
      // Retry once on transient network errors — PayPal capture is idempotent.
      const doCapture = () => capturePayment("PAYPAL", paypalToken);
      doCapture()
        .catch(() => new Promise<void>((r) => setTimeout(r, 2500)).then(doCapture))
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

    // FreedomPay flow: we're on pg_success_url so payment succeeded.
    // pg_result may be absent in the browser redirect — detect by pg_payment_id or pg_order_id.
    // We call check_payment.php to confirm in DB, then navigate to success regardless.
    if (isFreedomPay) {
      if (fpResult === "0") {
        // Explicit failure signal (e.g. failure URL misconfigured to /payment-return)
        const last = loadLastPayment();
        const orderIdStr = last?.orderId?.toString() ?? fpOrderId ?? "";
        navigate(
          `/payment/failed?error=${encodeURIComponent("PAYMENT_FAILED")}&orderId=${orderIdStr}`,
          { replace: true },
        );
        return;
      }

      // Success: pg_result === "1" or pg_result absent (browser success redirect)
      // Send ALL redirect params to backend for local pg_sig verification.
      // Backend: MD5(scriptName ; sorted_param_values ; secretKey) — no API call needed.
      const allParams = Object.fromEntries(params.entries());
      console.log("[PaymentReturn] FreedomPay success — sending params for sig verification:", allParams);
      const confirm = Object.keys(allParams).length > 0
        ? verifyFreedomPayRedirect(allParams).catch((err) => {
            console.warn("[PaymentReturn] verifyFreedomPayRedirect failed:", err);
            return null;
          })
        : Promise.resolve(null);
      confirm.finally(() => navigate("/payment/success", { replace: true }));
      return;
    }

    // VTB KZ flow: redirect back to /payment-return?orderId=<vtbInternalOrderId>
    // Call getOrderStatusExtended to confirm payment status server-side.
    if (isVtb) {
      console.log("[PaymentReturn] VTB KZ return — orderId:", vtbOrderId);
      const allParams = Object.fromEntries(params.entries());
      verifyVtbReturn(allParams)
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

    // No recognised params — redirect to failed with a generic error
    console.warn("[PaymentReturn] UNKNOWN_RETURN — no PayPal token, FreedomPay params, or VTB orderId found");
    navigate("/payment/failed?error=UNKNOWN_RETURN", { replace: true });
  }, [paypalToken, fpPaymentId, fpOrderId, fpResult, vtbOrderId, isFreedomPay, isVtb, navigate, params]);

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
