import { Link, useSearchParams } from "react-router-dom";
import { Container } from "@/shared/ui/container";
import { cn } from "@/shared/lib/cn";

function statusMeta(raw: string | null) {
  const s = (raw ?? "").toUpperCase();
  if (s === "SUCCEEDED") {
    return {
      title: "Оплата прошла успешно",
      hint: "Платёж подтверждён. Мы начинаем обработку заказа.",
      tone: "text-emerald-300 border-emerald-500/30 bg-emerald-500/10",
    };
  }
  if (s === "PENDING") {
    return {
      title: "Оплата в обработке",
      hint: "Провайдер ещё обрабатывает платёж. Обновите страницу чуть позже.",
      tone: "text-amber-300 border-amber-500/30 bg-amber-500/10",
    };
  }
  if (s === "FAILED" || s === "CANCELLED") {
    return {
      title: "Оплата не завершена",
      hint: "Попробуйте оплатить заказ ещё раз из корзины или обратитесь в поддержку.",
      tone: "text-red-300 border-red-500/30 bg-red-500/10",
    };
  }
  return {
    title: "Статус оплаты неизвестен",
    hint: "Проверьте заказ позже или свяжитесь с поддержкой.",
    tone: "text-zinc-300 border-white/15 bg-zinc-900/70",
  };
}

export function PaymentReturnPage() {
  const [params] = useSearchParams();
  const orderId = params.get("orderId");
  const paymentId = params.get("paymentId");
  const provider = params.get("provider");
  const status = params.get("status");
  const meta = statusMeta(status);

  return (
    <div className="py-14">
      <Container className="max-w-2xl">
        <h1 className="font-display mb-3 text-4xl tracking-wide text-zinc-100">
          Возврат после оплаты
        </h1>
        <p className="mb-6 text-zinc-400">
          Страница подтверждения платежа. Проверьте данные ниже.
        </p>

        <section
          className={cn(
            "rounded-[14px] border px-5 py-5",
            meta.tone,
          )}
        >
          <p className="m-0 text-lg font-semibold">{meta.title}</p>
          <p className="mt-2 mb-0 text-sm opacity-90">{meta.hint}</p>
        </section>

        <section className="mt-6 rounded-[14px] border border-white/10 bg-zinc-900/40 px-5 py-5 text-sm text-zinc-300">
          <p className="m-0">
            Заказ: <strong className="text-zinc-100">{orderId ?? "—"}</strong>
          </p>
          <p className="mt-2 mb-0">
            Платёж: <strong className="text-zinc-100">{paymentId ?? "—"}</strong>
          </p>
          <p className="mt-2 mb-0">
            Провайдер: <strong className="text-zinc-100">{provider ?? "—"}</strong>
          </p>
          <p className="mt-2 mb-0">
            Статус: <strong className="text-zinc-100">{status ?? "—"}</strong>
          </p>
        </section>

        <div className="mt-8 flex flex-wrap gap-3">
          <Link
            to="/cart"
            className="inline-flex items-center justify-center rounded-full bg-white px-6 py-3 font-semibold text-black transition hover:-translate-y-0.5 hover:bg-zinc-200"
          >
            Вернуться в корзину
          </Link>
          <Link
            to="/catalog"
            className="inline-flex items-center justify-center rounded-full border border-white/15 bg-white/5 px-6 py-3 font-semibold text-zinc-200 transition hover:bg-white/10"
          >
            В каталог
          </Link>
        </div>
      </Container>
    </div>
  );
}
