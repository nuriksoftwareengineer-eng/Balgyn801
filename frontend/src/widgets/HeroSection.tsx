import { useNavigate } from "react-router-dom";
import { Container } from "@/shared/ui/container";
import { Button } from "@/shared/ui/button";

export function HeroSection() {
  const navigate = useNavigate();

  return (
    <section className="py-14 text-center md:py-[72px]">
      <Container>
        <p className="mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-violet-400">
          Вышивка • Стритвир • Под заказ
        </p>
        <h1 className="font-display mx-auto max-w-4xl text-[clamp(3rem,12vw,5.5rem)] leading-[0.95] tracking-[0.02em] text-zinc-100">
          НОВЫЙ ДРОП — ОДЕЖДА С ХАРАКТЕРОМ
        </h1>
        <p className="mx-auto mt-5 max-w-lg text-[1.0625rem] text-zinc-400">
          Яркие принты и плотная вышивка на качественном материале. Выберите модель из каталога или пришлите свой референс для индивидуального дизайна.
        </p>
        <Button
          type="button"
          variant="primary"
          className="mt-8 rounded-full"
          onClick={() => navigate("/catalog")}
        >
          Перейти в каталог
        </Button>
      </Container>
    </section>
  );
}
