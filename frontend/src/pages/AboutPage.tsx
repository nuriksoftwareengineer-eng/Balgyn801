import { Container } from "@/shared/ui/container";

export function AboutPage() {
  return (
    <div className="py-14">
      <Container className="max-w-2xl">
        <h1 className="font-display mb-6 text-4xl tracking-wide text-zinc-100">
          О нас
        </h1>
        <p className="text-lg leading-relaxed text-zinc-400">
          Это страница-заготовка: расскажите историю бренда, как шьёте и вышиваете, сроки производства и контакты для оптовых заказов. Логику формы обратной связи можно подключить позже.
        </p>
      </Container>
    </div>
  );
}
