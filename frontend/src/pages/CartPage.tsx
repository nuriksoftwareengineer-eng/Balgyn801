import { Link } from "react-router-dom";
import { useCart } from "@/app/use-cart";
import { Container } from "@/shared/ui/container";

export function CartPage() {
  const { count } = useCart();

  return (
    <div className="py-14">
      <Container>
        <h1 className="font-display mb-4 text-4xl tracking-wide text-zinc-100">
          Корзина
        </h1>
        <p className="mb-6 text-zinc-400">
          Здесь позже будет оформление заказа и оплата. Сейчас демо-счётчик позиций:{" "}
          <strong className="text-zinc-100">{count}</strong>
        </p>
        <Link
          to="/catalog"
          className="font-semibold text-violet-400 hover:text-violet-300 hover:underline"
        >
          ← В каталог
        </Link>
      </Container>
    </div>
  );
}
