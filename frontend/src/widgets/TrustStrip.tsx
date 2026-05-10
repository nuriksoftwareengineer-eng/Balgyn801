import { Container } from "@/shared/ui/container";

const items = [
  { title: "Доставка", desc: "По Казахстану и за рубеж при необходимости" },
  { title: "Вышивка", desc: "Плотные стежки, стойкие нити" },
  { title: "Качество", desc: "Проверка перед отправкой" },
];

export function TrustStrip() {
  return (
    <Container className="pb-12">
      <div className="grid gap-4 sm:grid-cols-3">
        {items.map((item) => (
          <div
            key={item.title}
            className="rounded-[14px] border border-white/10 bg-zinc-900 px-5 py-5 text-center"
          >
            <strong className="mb-1.5 block text-xs font-bold uppercase tracking-[0.06em] text-zinc-100">
              {item.title}
            </strong>
            <span className="text-sm text-zinc-400">{item.desc}</span>
          </div>
        ))}
      </div>
    </Container>
  );
}
